package com.testehan.springai.immobiliare.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class EmailService {
    private final SesClient sesClient;

    public EmailService() {
        sesClient = SesClient.builder()
                .region(Region.EU_NORTH_1)
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
}
