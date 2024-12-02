package com.testehan.springai.immobiliare.util;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

public class EmailTemplateMain {
    public static void main(String[] args) {

        createWelcomeTemplate();
//        deleteTemplate("WelcomeEmailTemplate");
    }

    private static void deleteTemplate(String templateName){
        // Create the SES client
        SesClient sesClient = buildSesClient();

        try {
            // Create the request
            DeleteTemplateRequest deleteTemplateRequest = DeleteTemplateRequest.builder()
                    .templateName(templateName)
                    .build();

            // Call SES to delete the template
            DeleteTemplateResponse response = sesClient.deleteTemplate(deleteTemplateRequest);
            System.out.println("Template deleted successfully: " + response);

        } catch (SesException e) {
            System.err.println("Error deleting template: " + e.awsErrorDetails().errorMessage());
        } finally {
            sesClient.close();
        }
    }

    private static void createWelcomeTemplate() {
        SesClient sesClient = buildSesClient();

        try {
            // Define the email template
            String templateName = "WelcomeEmailTemplate";
            String subjectPart = "Welcome to CasaMia.ai!";
            String htmlBody = "<h1>Welcome, {{name}}!</h1><p>Thank you for joining our service. " +
                    "We hope that you will find the right property or the right customer &#128522;.</p>";
            String textBody = "Welcome, {{name}}!\nThank you for joining our service.";

            // Create the request
            CreateTemplateRequest createTemplateRequest = CreateTemplateRequest.builder()
                    .template(Template.builder()
                            .templateName(templateName)
                            .subjectPart(subjectPart)
                            .htmlPart(htmlBody)
                            .textPart(textBody)
                            .build())
                    .build();

            // Call SES to create the template
            CreateTemplateResponse response = sesClient.createTemplate(createTemplateRequest);
            System.out.println("Template created successfully: " + response);

        } catch (SesException e) {
            System.err.println("Error creating template: " + e.awsErrorDetails().errorMessage());
        } finally {
            sesClient.close();
        }
    }

    private static SesClient buildSesClient() {
        return SesClient.builder()
                .region(Region.EU_NORTH_1) // Replace with your region
                .build();
    }
}
