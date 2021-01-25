package com.hr.docsigning;

import com.adobe.platform.operation.ExecutionContext;
import com.adobe.platform.operation.auth.Credentials;
import com.adobe.platform.operation.exception.SdkException;
import com.adobe.platform.operation.exception.ServiceApiException;
import com.adobe.platform.operation.exception.ServiceUsageException;
import com.adobe.platform.operation.io.FileRef;
import com.adobe.platform.operation.pdfops.CreatePDFOperation;
import com.adobe.platform.operation.pdfops.options.createpdf.CreatePDFOptions;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class PersonController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonController.class);
    private String contractFilePath = "output/contract.pdf";

    @GetMapping("/")
    public String showForm(PersonForm personForm) {
        return "form";
    }

    @PostMapping("/")
    public String checkPersonInfo(@Valid PersonForm personForm, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "form";
        }

        CreateContract(personForm);

        return "contract-actions";
    }

    private void CreateContract(PersonForm personForm) {
        try {
            // Initial setup, create credentials instance.
            Credentials credentials = Credentials.serviceAccountCredentialsBuilder()
                    .fromFile("pdftools-api-credentials.json")
                    .build();

            //Create an ExecutionContext using credentials and create a new operation instance.
            ExecutionContext executionContext = ExecutionContext.create(credentials);
            CreatePDFOperation htmlToPDFOperation = CreatePDFOperation.createNew();

            // Set operation input from a source file.
            FileRef source = FileRef.createFromLocalFile("src/main/resources/contract/index.zip");
            htmlToPDFOperation.setInput(source);

            // Provide any custom configuration options for the operation
            // We pass person data here to dynamically fill out the HTML
            setCustomOptionsAndPersonData(htmlToPDFOperation, personForm);

            // Execute the operation.
            FileRef result = htmlToPDFOperation.execute(executionContext);

            // Save the result to the specified location. Delete previous file if exists
            File file = new File(contractFilePath);
            Files.deleteIfExists(file.toPath());

            result.saveAs(file.getPath());

        } catch (ServiceApiException | IOException | SdkException | ServiceUsageException ex) {
            LOGGER.error("Exception encountered while executing operation", ex);
        }
    }

    private static void setCustomOptionsAndPersonData(CreatePDFOperation htmlToPDFOperation,
                                                      PersonForm personForm) {
        //Set the dataToMerge field that needs to be populated in the HTML before its conversion
        JSONObject dataToMerge = new JSONObject();
        dataToMerge.put("personFullName", personForm.GetFullName());

        // Set the desired HTML-to-PDF conversion options.
        CreatePDFOptions htmlToPdfOptions = CreatePDFOptions.htmlOptionsBuilder()
                .includeHeaderFooter(false)
                .withDataToMerge(dataToMerge)
                .build();
        htmlToPDFOperation.setOptions(htmlToPdfOptions);
    }

    @RequestMapping("/pdf")
    public void downloadContract(HttpServletResponse response) {
        Path file = Paths.get(contractFilePath);

        response.setContentType("application/pdf");
        response.addHeader("Content-Disposition", "attachment; filename=contract.pdf");
        try {
            Files.copy(file, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
