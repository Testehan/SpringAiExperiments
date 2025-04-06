package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.service.LLMCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatApiController.class);

    private final ConversationSession conversationSession;
    private final LLMCacheService llmCacheService;

    public ChatApiController(ConversationSession conversationSession, LLMCacheService llmCacheService) {
        this.conversationSession = conversationSession;
        this.llmCacheService = llmCacheService;
    }

    @PostMapping("/report")
    public ResponseEntity<String> reportResponse(@RequestBody String response) {

        var user = conversationSession.getImmobiliareUser();
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You must be logged in");
        }

        llmCacheService.reportInaccurateResponse(response);

        // Return a response to the frontend
        return ResponseEntity.ok("Thanks for reporting issue.");

    }

}
