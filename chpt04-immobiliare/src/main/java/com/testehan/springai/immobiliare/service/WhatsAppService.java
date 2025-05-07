package com.testehan.springai.immobiliare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppService {

    @Value("${whatsapp.api.access.token}")
    private String WHATS_APP_ACCESS_TOKEN;
    //todo check to see if your phone number was reviewed in meta dev console
    @Value(("${whatsapp.api.phone.id}"))
    private String PHONE_NUMBER_ID;

    public void sendMessage(String to, String messageText) {
        String url = String.format("https://graph.facebook.com/v22.0/%s/messages", PHONE_NUMBER_ID);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(WHATS_APP_ACCESS_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // todo change with the other template. this can be done once the template is reviewed
        String json = String.format("""
            {
              "messaging_product": "whatsapp",
              "to": "%s",
              "type": "template",
              "template": {
                       "name": "hello_world",   
                       "language": {
                         "code": "en_US"
                       }
                }
            }
            """, to, messageText);

        HttpEntity<String> request = new HttpEntity<>(json, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        System.out.println("Response: " + response.getBody());
    }

}
