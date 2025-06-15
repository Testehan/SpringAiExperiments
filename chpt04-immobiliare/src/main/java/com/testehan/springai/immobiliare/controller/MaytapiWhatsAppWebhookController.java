package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.MessageType;
import com.testehan.springai.immobiliare.model.maytapi.AcknowledgementMessage;
import com.testehan.springai.immobiliare.model.maytapi.BaseMayTapiMessage;
import com.testehan.springai.immobiliare.model.maytapi.ErrorMessage;
import com.testehan.springai.immobiliare.model.maytapi.NormalMessage;
import com.testehan.springai.immobiliare.service.LeadConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/maytapiwhatsapp")
public class MaytapiWhatsAppWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaytapiWhatsAppWebhookController.class);

    private final LeadConversationService leadConversationService;

    @Value("${whatsapp.maytapi.api.access.token}")
    private String WHATS_MAYTAPI_APP_ACCESS_TOKEN;

    public MaytapiWhatsAppWebhookController(LeadConversationService leadConversationService) {
        this.leadConversationService = leadConversationService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody BaseMayTapiMessage message,
            @RequestParam("token") String apiToken) {

        // --- SECURITY CHECK ---
        // Verify that the token received in the header matches our expected token.
        if (apiToken == null || !apiToken.equals(WHATS_MAYTAPI_APP_ACCESS_TOKEN)) {
            LOGGER.warn("Unauthorized webhook attempt: Invalid or missing token.");
            // Reject the request with a 401 Unauthorized status.
            return new ResponseEntity<>("Invalid authentication token", HttpStatus.UNAUTHORIZED);
        }

        // If the token is valid, proceed with processing the message.
        LOGGER.info("--- New Authenticated Webhook Received ---");

        if (message instanceof AcknowledgementMessage ack) {
            // handle ack
            LOGGER.info("================== acknowledgement message");
            LOGGER.info(ack.toString());
            LOGGER.info("==================");

            return ResponseEntity.ok("Acknowledgement handled");
        } else if (message instanceof NormalMessage normalMessage) {
            // handle normal
            LOGGER.info("================== normal message");
            LOGGER.info(normalMessage.toString());
            LOGGER.info("==================");
            handleIncomingNormalMessage(normalMessage);

            return ResponseEntity.ok("Message handled");
        } else if (message instanceof ErrorMessage error) {
            // handle error
            LOGGER.info("================== error message");
            LOGGER.info(error.toString());
            LOGGER.info("==================");
            return ResponseEntity.ok("Error handled");
        } else {
            return ResponseEntity.badRequest().body("Unknown message type");
        }


    }

    private void handleIncomingNormalMessage(NormalMessage normalMessage) {
        var sender = normalMessage.getUser().getPhone();
        var messageText = normalMessage.getMessage().getText();
        var messageId = normalMessage.getMessage().getId();
        var isFromMe = normalMessage.getMessage().isFromMe();

        LOGGER.info("From: " + sender);
        LOGGER.info("Message: " + messageText);

        if (!isFromMe) {
            leadConversationService.saveConversationTextMessage(sender, messageId, messageText, MessageType.RECEIVED);
            // TODO how to mark the message as read.. ?!!?
        }
    }

}