package com.testehan.springai.immobiliare.util;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class EmailTemplateMain {
    public static void main(String[] args) throws IOException {

        deleteTemplats();
        createTemplates();

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

    private static void deleteTemplats() throws IOException {
        deleteTemplate("WelcomeEmailTemplate");
        deleteTemplate("ReactivateListingEmailTemplate");
        deleteTemplate("ReactivateListingEmailTemplate_RO");
        deleteTemplate("WelcomeEmailTemplate_RO");
        deleteTemplate("ListingAddedEmailTemplate");
        deleteTemplate("ListingAddedEmailTemplate_RO");
    }

    private static void createTemplates() throws IOException {
        createTemplate("WelcomeEmailTemplate", "Welcome to CasaMia.ai!", readFileFromResources("emails/en/welcome.html"));
        createTemplate("WelcomeEmailTemplate_RO", "Bun venit pe CasaMia.ai!", readFileFromResources("emails/ro/welcome.html"));

        createTemplate("ReactivateListingEmailTemplate", "Keep the listing active", readFileFromResources("emails/en/listingReactivate.html"));
        createTemplate("ReactivateListingEmailTemplate_RO", "Menține anunțul activ", readFileFromResources("emails/ro/listingReactivate.html"));

        createTemplate("ListingAddedEmailTemplate", "Listing Added Successfully!", readFileFromResources("emails/en/listingAdded.html"));
        createTemplate("ListingAddedEmailTemplate_RO", "Anunț Adăugat cu Succes!", readFileFromResources("emails/ro/listingAdded.html"));
    }

    private static String readFileFromResources(String filePath) throws IOException {
        ClassLoader classLoader = EmailTemplateMain.class.getClassLoader();
        Path path = Paths.get(Objects.requireNonNull(classLoader.getResource(filePath)).getPath());
        return Files.readString(path);
    }

    private static void createTemplate(String templateName, String subject, String htmlBody) {
        SesClient sesClient = buildSesClient();

        try {

            // Create the request
            CreateTemplateRequest createTemplateRequest = CreateTemplateRequest.builder()
                    .template(Template.builder()
                            .templateName(templateName)
                            .subjectPart(subject)
                            .htmlPart(htmlBody)
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
