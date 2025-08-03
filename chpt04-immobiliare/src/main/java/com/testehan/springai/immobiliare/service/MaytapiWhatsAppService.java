package com.testehan.springai.immobiliare.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testehan.springai.immobiliare.model.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MaytapiWhatsAppService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaytapiWhatsAppService.class);

    @Value("${whatsapp.maytapi.api.access.token}")
    private String WHATS_MAYTAPI_APP_ACCESS_TOKEN;

    @Value(("${whatsapp.maytapi.api.phone.id}"))
    private String PHONE_NUMBER_ID;

    @Value(("${whatsapp.maytapi.api.product.id}"))
    private String PRODUCT_ID;

    private static final List<String> VIEW_LISTING_MESSAGES = new ArrayList<>(List.of(
            "Anunțul tău este acum publicat. Îl poți vizualiza aici : %s",
            "Anunțul tău a fost listat 👉 %s. Îl poți verifica acum și să-l distribui.",
            "Dă share la anunțul tău 👉 %s — șansele de a găsi chiriaș cresc cu 50–200%% 🚀",
            "Un share rapid pe Facebook sau Insta 📲 %s îți poate dubla șansele să închiriezi mai repede 🔥",
            "Distribuie anunțul %s — proprietarii care o fac găsesc chiriași de până la 3x mai repede 👌",
            "Ajută anunțul să circule! 🔁 %s — în medie, se închiriază de 2x mai repede 📈"
    ));

    private final LeadConversationService leadConversationService;
    private final LeadService leadService;

    public MaytapiWhatsAppService(LeadConversationService leadConversationService, LeadService leadService) {
        this.leadConversationService = leadConversationService;
        this.leadService = leadService;
    }

    public ResponseEntity<String> sendMessage(String recipientNumber, String messageText) {
        var apiUrl = String.format("https://api.maytapi.com/api/%s/%s/sendMessage", PRODUCT_ID, PHONE_NUMBER_ID);

        LOGGER.info("Will try to send message '{}' to {}", messageText, recipientNumber);
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-maytapi-key", WHATS_MAYTAPI_APP_ACCESS_TOKEN);

        WhatsAppMessageRequest requestBody = new WhatsAppMessageRequest(
                recipientNumber,
                "text",
                messageText
        );

        HttpEntity<WhatsAppMessageRequest> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
        String rawJson = response.getBody();
        LOGGER.info("Response: {} ", rawJson);
        try {
            ObjectMapper mapper = new ObjectMapper();

            MaytapiResponse sendMessageResponse = mapper.readValue(rawJson, MaytapiResponse.class);
            leadConversationService.saveConversationTextMessage(recipientNumber, sendMessageResponse.getData().getMsgId(), messageText, MessageType.SENT);
            return ResponseEntity.ok("Reply sent successfully");

        } catch (Exception e) {
            LOGGER.error("Error !!! parsing response payload: {} \n causes {}", rawJson, e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong");

        }
    }

    public void sendSocialListingMessage(String contact, String url) {
        var formatedMessage = String.format(VIEW_LISTING_MESSAGES.get(new Random().nextInt(VIEW_LISTING_MESSAGES.size())), url);
        sendMessage(contact, formatedMessage);

    }
}

// todo move to a separate class
class WhatsAppMessageRequest {

    @JsonProperty("to_number")
    private String toNumber;

    @JsonProperty("type")
    private String type;

    @JsonProperty("message")
    private String message;

    // Constructors
    public WhatsAppMessageRequest(String toNumber, String type, String message) {
        this.toNumber = toNumber;
        this.type = type;
        this.message = message;
    }

    // Getters and Setters (required for serialization)
    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    @Override
    public String toString() {
        return "WhatsAppMessageRequest{" +
                "toNumber='" + toNumber + '\'' +
                ", type='" + type + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

// todo move to a separate class
class MaytapiResponse {

    private boolean success;
    private Data data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        @JsonProperty("chatId")
        private String chatId;

        @JsonProperty("msgId")
        private String msgId;

        public String getChatId() {
            return chatId;
        }

        public void setChatId(String chatId) {
            this.chatId = chatId;
        }

        public String getMsgId() {
            return msgId;
        }

        public void setMsgId(String msgId) {
            this.msgId = msgId;
        }
    }
}

