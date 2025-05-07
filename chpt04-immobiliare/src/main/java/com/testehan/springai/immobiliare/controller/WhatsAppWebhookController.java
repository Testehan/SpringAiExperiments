package com.testehan.springai.immobiliare.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    @Value("${whatsapp.api.webhook.token}")
    private String VERIFY_TOKEN;

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
    public ResponseEntity<String> receiveMessage(@RequestBody String payload) {
        System.out.println("Incoming webhook: " + payload);
        // Optionally: parse JSON and handle message content
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
