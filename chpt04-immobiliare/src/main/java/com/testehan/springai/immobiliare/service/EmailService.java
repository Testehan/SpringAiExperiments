package com.testehan.springai.immobiliare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testehan.springai.immobiliare.configuration.BeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.*;

@Service
public class EmailService {

    @Value("${app.url}")
    private String appUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SesClient sesClient;
    private final AppConfigurationsService appConfigurationsService;

    public EmailService(BeanConfig beanConfig, AppConfigurationsService appConfigurationsService) {
        this.appConfigurationsService = appConfigurationsService;
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
        if (appConfigurationsService.isSendWelcomeEmailEnabled()){
            var chatUrl = appUrl + "/chat";
            var addUrl = appUrl + "/add";

            SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                    .source("CasaMia.ai" + " " + "<admin@casamia.ai>") // Replace with a verified email
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .template(getWelcomeEmailTemplate(locale)) // Template name
                    .templateData("{\"userName\":\"" + name + "\", \"chatUrl\":\"" + chatUrl + "\", \"addUrl\":\"" + addUrl + "\" }") // JSON string to replace placeholders
                    .build();

            sesClient.sendTemplatedEmail(request);
        }
    }

    public Optional<String> sendReactivateListingEmail(String to, String name, String listingTitle, String reactivateListingUrl, Locale locale){
        SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                .source("CasaMia.ai" + " " +"<admin@casamia.ai>") // Replace with a verified email
                .destination(Destination.builder()
                        .toAddresses(to)
                        .build())
                .template(getReactivateListingEmailTemplate(locale)) // Template name
                .templateData("{\"userName\":\""+name+"\",\"listingTitle\":\""+listingTitle+"\",\"reactivateListingUrl\":\""+reactivateListingUrl+"\"}")
                .build();

        var response = sesClient.sendTemplatedEmail(request);

        var messageId = response.messageId();
        if (StringUtils.hasText(messageId)){
            return Optional.of(messageId);
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> sendAdminReactivateListingEmail(List<String> reactivateListingUrl, List<String> contacts, Locale locale) {

        if (reactivateListingUrl.size() > 0 && contacts.size() > 0) {

            var tableRows = buildHtmlTableRows(contacts, reactivateListingUrl);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> data = new HashMap<>();
            data.put("tableOfPhonesAndUrls", tableRows);
            String templateData = "";
            try {
                // Use ObjectMapper to escape it as a JSON string
                templateData = mapper.writeValueAsString(data);
            } catch (Exception e) {
                LOGGER.error("Failed to escape HTML table for JSON", e);
            }

            SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                    .source("CasaMia.ai" + " " + "<admin@casamia.ai>") // Replace with a verified email
                    .destination(Destination.builder()
                            .toAddresses("tdan89@yahoo.com")
                            .build())
                    .template(getAdminReactivateListingEmailTemplate(locale)) // Template name
                    .templateData(templateData)
                    .build();

            var response = sesClient.sendTemplatedEmail(request);

            var messageId = response.messageId();
            if (StringUtils.hasText(messageId)) {
                return Optional.of(messageId);
            }
        }
        return Optional.empty();
    }

    public void sendListingAddedEmail(String to, String userName, String listingName, String viewUrl, String editUrl, Locale locale) {
        if (appConfigurationsService.isSendListingAddedEmail()) {
            SendTemplatedEmailRequest request = SendTemplatedEmailRequest.builder()
                    .source("CasaMia.ai" + " " + "<admin@casamia.ai>") // Replace with a verified email
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .template(getListingAddedEmailTemplate(locale)) // Template name
                    .templateData("{\"userName\":\"" + userName + "\"," +
                            "\"listingTitle\":\"" + listingName +
                            "\",\"viewListingUrl\":\"" + viewUrl +
                            "\", \"editListingUrl\":\"" + editUrl + "\"}")
                    .build();

            sesClient.sendTemplatedEmail(request);
        }
    }

    private static String getListingAddedEmailTemplate(Locale locale) {
        if (locale.getLanguage().equals("ro")) {
            return "ListingAddedEmailTemplate_RO";
        }
        return "ListingAddedEmailTemplate";
    }

    private static String getReactivateListingEmailTemplate(Locale locale) {
        if (locale.getLanguage().equals("ro")) {
            return "ReactivateListingEmailTemplate_RO";
        }
        return "ReactivateListingEmailTemplate";
    }

    private static String getAdminReactivateListingEmailTemplate(Locale locale) {
        if (locale.getLanguage().equals("ro")) {
            return "AdminReactivateListingEmailTemplate_RO";
        }
        return "AdminReactivateListingEmailTemplate";
    }



    private static String getWelcomeEmailTemplate(Locale locale) {
        if (locale.getLanguage().equals("ro")) {
            return "WelcomeEmailTemplate_RO";
        }
        return "WelcomeEmailTemplate";
    }

    public static String buildHtmlTableRows(List<String> phoneNumbers, List<String> urls) {
        if (phoneNumbers.size() != urls.size()) {
            throw new IllegalArgumentException("Phone numbers and URLs lists must be the same size.");
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < phoneNumbers.size(); i++) {
            String phone = phoneNumbers.get(i);
            String url = urls.get(i);

            sb.append("<tr>")
                    .append("<td><a href=\"https://wa.me/")
                    .append(phone.replace("+",""))
                    .append("\" target=\"_blank\">")
                    .append(phone)
                    .append("</a></td>")
                    .append("<td><a href=\"")
                    .append(url)
                    .append("\" target=\"_blank\">")
                    .append(url)
                    .append("</a></td>")
                    .append("</tr>");
        }

       return sb.toString();
    }
}
