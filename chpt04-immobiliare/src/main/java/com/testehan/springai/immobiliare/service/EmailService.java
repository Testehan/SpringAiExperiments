package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.configuration.BeanConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Locale;

@Service
public class EmailService {

    private final SesClient sesClient;

    public EmailService(BeanConfig beanConfig) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(beanConfig.getAwsAccessKeyId(), beanConfig.getAwsAccessSecret());

        sesClient = SesClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(beanConfig.getRegionName()))
                .build();
    }

    public void sendEmail(String from, String to, String subject, String body) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(from)
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).build())
                        .body(Body.builder()
                                .text(Content.builder().data(body).build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }

    public void sendWelcomeEmail(String to, String name){
        SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                .source("admin@casamia.ai") // Replace with a verified email
                .destination(Destination.builder()
                        .toAddresses(to)
                        .build())
                .template("WelcomeEmailTemplate") // Template name
                .templateData("{\"name\":\""+name+"\"}") // JSON string to replace placeholders
                .build();

        sesClient.sendTemplatedEmail(request);
    }

    public void sendReactivateListingEmail(String to, String name, String listingName, String reactivateLink, Locale locale){
        SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                .source("admin@casamia.ai") // Replace with a verified email
                .destination(Destination.builder()
                        .toAddresses(to)
                        .build())
                .template(getReactivateListingEmailTemplate(locale)) // Template name
                .templateData("{\"name\":\""+name+"\",\"listingName\":\""+listingName+"\",\"reactivateLink\":\""+reactivateLink+"\"}")
                .build();

        sesClient.sendTemplatedEmail(request);
    }

    private static String getReactivateListingEmailTemplate(Locale locale) {
        if (locale.getLanguage().equals("ro")) {
            return "ReactivateListingEmailTemplate_RO";
        }
        return "ReactivateListingEmailTemplate";
    }
}
