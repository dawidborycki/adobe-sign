package com.hr.docsigning;

import io.swagger.client.api.AgreementsApi;
import io.swagger.client.api.BaseUrisApi;
import io.swagger.client.api.TransientDocumentsApi;
import io.swagger.client.model.ApiClient;
import io.swagger.client.model.ApiException;
import io.swagger.client.model.agreements.*;
import io.swagger.client.model.baseUris.BaseUriInfo;
import io.swagger.client.model.transientDocuments.TransientDocumentResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

@Controller
public class AdobeSignController {
    @RequestMapping("/send")
    public String sendContract(HttpServletResponse httpServletResponse) {
        try {
            ApiClient apiClient = new ApiClient();

            //Default baseUrl to make GET /baseUris API call.
            String baseUrl = "https://api.echosign.com/";
            String endpointUrl = "/api/rest/v6";
            apiClient.setBasePath(baseUrl + endpointUrl);

            // Provide an OAuth Access Token as "Bearer : access token" in authorization
            String authorization = "Bearer <type_your_access_token_here>";

            // Get the baseUris for the user and set it in apiClient.
            BaseUrisApi baseUrisApi = new BaseUrisApi(apiClient);
            BaseUriInfo baseUriInfo = baseUrisApi.getBaseUris(authorization);
            apiClient.setBasePath(baseUriInfo.getApiAccessPoint() + endpointUrl);

            // Get PDF file
            String filePath = "output/";
            String fileName = "contract.pdf";
            File file = new File(filePath+fileName);
            String mimeType = "application/pdf";

            //Get the id of the transient document.
            TransientDocumentsApi transientDocumentsApi = new TransientDocumentsApi(apiClient);
            TransientDocumentResponse response = transientDocumentsApi.createTransientDocument(authorization, file, null, null, fileName, mimeType);
            String transientDocumentId = response.getTransientDocumentId();

            // Create AgreementCreationInfo
            AgreementCreationInfo agreementCreationInfo = new AgreementCreationInfo();

            // Add file
            FileInfo fileInfo = new FileInfo();
            fileInfo.setTransientDocumentId(transientDocumentId);
            agreementCreationInfo.addFileInfosItem(fileInfo);

            // Set state to IN_PROCESS, so the agreement will be send immediately
            agreementCreationInfo.setState(AgreementCreationInfo.StateEnum.IN_PROCESS);
            agreementCreationInfo.setName("Contract");
            agreementCreationInfo.setSignatureType(AgreementCreationInfo.SignatureTypeEnum.ESIGN);

            // Provide emails of recipients to whom agreement will be sent
            // Employee
            ParticipantSetInfo participantSetInfo = new ParticipantSetInfo();
            ParticipantSetMemberInfo participantSetMemberInfo = new ParticipantSetMemberInfo();
            participantSetMemberInfo.setEmail("dawid@borycki.com.pl");
            participantSetInfo.addMemberInfosItem(participantSetMemberInfo);
            participantSetInfo.setOrder(1);
            participantSetInfo.setRole(ParticipantSetInfo.RoleEnum.SIGNER);
            agreementCreationInfo.addParticipantSetsInfoItem(participantSetInfo);

            // Manager
            participantSetInfo = new ParticipantSetInfo();
            participantSetMemberInfo = new ParticipantSetMemberInfo();
            participantSetMemberInfo.setEmail("david@cloud-dev.edu.pl");
            participantSetInfo.addMemberInfosItem(participantSetMemberInfo);
            participantSetInfo.setOrder(2);
            participantSetInfo.setRole(ParticipantSetInfo.RoleEnum.SIGNER);
            agreementCreationInfo.addParticipantSetsInfoItem(participantSetInfo);

            // Create agreement using the transient document.
            AgreementsApi agreementsApi = new AgreementsApi(apiClient);
            AgreementCreationResponse agreementCreationResponse = agreementsApi.createAgreement(
                    authorization, agreementCreationInfo, null, null);

            System.out.println("Agreement sent, ID: " + agreementCreationResponse.getId());
        }
        catch (ApiException e) {
            System.err.println(e.toString());
        }

        return "contract-actions";
    }
}
