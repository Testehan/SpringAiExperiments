package com.testehan.springai.immobiliare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testehan.springai.immobiliare.model.webhook.Change;
import com.testehan.springai.immobiliare.model.webhook.Entry;
import com.testehan.springai.immobiliare.model.webhook.Message;
import com.testehan.springai.immobiliare.model.webhook.WhatsAppWebhookPayload;
import com.testehan.springai.immobiliare.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Value("${whatsapp.api.webhook.token}")
    private String VERIFY_TOKEN;

    private final WhatsAppService whatsAppService;

    public WhatsAppWebhookController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    // 1. Webhook verification (GET)
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        } else {
            return ResponseEntity.status(403).body("Verification failed");
        }
    }


    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody String rawJson) {
        LOGGER.info("Incoming webhook Raw JSON received:\n: {} ", rawJson);
        // Optionally: parse JSON and handle message content
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Try deserializing to your POJO
            WhatsAppWebhookPayload payload = mapper.readValue(rawJson, WhatsAppWebhookPayload.class);

            // Only process text messages
            if (payload.getEntry() != null) {
                for (Entry entry : payload.getEntry()) {
                    for (Change change : entry.getChanges()) {
                        com.testehan.springai.immobiliare.model.webhook.Value value = change.getValue();

                        if (value.getMessages() != null) {
                            for (Message message : value.getMessages()) {
                                if (Message.MESSAGE_TYPE_TEXT.equals(message.getType())) {
                                    String body = message.getText().getBody();
                                    String from = message.getFrom();
                                    LOGGER.info("Text from {} : {}",from, body);

                                    // TODO: process the message

                                    whatsAppService.markMessageAsRead(message.getId(),value.getMetadata().getPhone_number_id());
                                } else {
                                    LOGGER.warn("Ignored message of type: {}", message.getType());
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            // 🔥 Handle unexpected or malformed payloads gracefully
            LOGGER.error("Error !!! parsing webhook payload: {} \n causes {}",rawJson, e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid_payload");
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
