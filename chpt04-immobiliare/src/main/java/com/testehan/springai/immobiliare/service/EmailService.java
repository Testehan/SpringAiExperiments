package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.configuration.BeanConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Locale;

@Service
public class EmailService {

    @Value("${app.url}")
    private String appUrl;

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
                .source("CasaMia.ai" + " <" + from+">")
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

    public void sendWelcomeEmail(String to, String name, Locale locale){
        var chatUrl = appUrl + "/chat";
        var addUrl = appUrl + "/add";

        SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                .source("CasaMia.ai" +" "+ "<admin@casamia.ai>") // Replace with a verified email
                .destination(Destination.builder()
                        .toAddresses(to)
                        .build())
                .template(getWelcomeEmailTemplate(locale)) // Template name
                .templateData("{\"userName\":\""+name+"\", \"chatUrl\":\""+chatUrl+"\", \"addUrl\":\""+addUrl+"\" }") // JSON string to replace placeholders
                .build();

        sesClient.sendTemplatedEmail(request);
    }

    public void sendReactivateListingEmail(String to, String name, String listingTitle, String reactivateListingUrl, Locale locale){
        SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                .source("CasaMia.ai" + " " +"<admin@casamia.ai>") // Replace with a verified email
                .destination(Destination.builder()
                        .toAddresses(to)
                        .build())
                .template(getReactivateListingEmailTemplate(locale)) // Template name
                .templateData("{\"userName\":\""+name+"\",\"listingTitle\":\""+listingTitle+"\",\"reactivateListingUrl\":\""+reactivateListingUrl+"\"}")
                .build();

        sesClient.sendTemplatedEmail(request);
    }

    private static String getReactivateListingEmailTemplate(Locale locale) {
        if (locale.getLanguage().equals("ro")) {
            return "ReactivateListingEmailTemplate_RO";
        }
        return "ReactivateListingEmailTemplate";
    }

    private static String getWelcomeEmailTemplate(Locale locale) {
        if (locale.getLanguage().equals("ro")) {
            return "WelcomeEmailTemplate_RO";
        }
        return "WelcomeEmailTemplate";
    }
}
