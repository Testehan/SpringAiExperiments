package com.testehan.springai.immobiliare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testehan.springai.immobiliare.model.MessageType;
import com.testehan.springai.immobiliare.model.wa.response.SendMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppService.class);

    public static final String WHATSAPP_API_22_MESSAGES = "https://graph.facebook.com/v22.0/%s/messages";

    @Value("${whatsapp.api.access.token}")
    private String WHATS_APP_ACCESS_TOKEN;
    //todo check to see if your phone number was reviewed in meta dev console
    @Value(("${whatsapp.api.phone.id}"))
    private String PHONE_NUMBER_ID;

    private final LeadConversationService leadConversationService;

    public WhatsAppService(LeadConversationService leadConversationService) {
        this.leadConversationService = leadConversationService;
    }

    public void sendMessage(String to, String messageText) {
        String url = String.format(WHATSAPP_API_22_MESSAGES, PHONE_NUMBER_ID);

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

        String rawJson = response.getBody();
        System.out.println("Response: " + rawJson);

        ObjectMapper mapper = new ObjectMapper();
        try {
            // Try deserializing to your POJO
            SendMessageResponse sendMessageResponse = mapper.readValue(rawJson, SendMessageResponse.class);
            String waUserId = sendMessageResponse.getContacts().get(0).getWa_id();
            String messageId = sendMessageResponse.getMessages().get(0).getId();
            leadConversationService.saveConversationTextMessage(waUserId, messageId, messageText, MessageType.SENT);

        } catch (Exception e) {
            LOGGER.error("Error !!! parsing response payload: {} \n causes {}",rawJson, e.getMessage());
        }
    }

    public void markMessageAsRead(String messageId, String phoneNumberId) {
        String url = String.format(WHATSAPP_API_22_MESSAGES, phoneNumberId);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(WHATS_APP_ACCESS_TOKEN);

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("status", "read");
        payload.put("message_id", messageId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            LOGGER.info("Marked message {} as read. Response: {}", messageId, response.getBody());
        } catch (Exception e) {
            LOGGER.error("Failed to mark message {} as read. {}",messageId, e.getMessage());
        }
    }


}
