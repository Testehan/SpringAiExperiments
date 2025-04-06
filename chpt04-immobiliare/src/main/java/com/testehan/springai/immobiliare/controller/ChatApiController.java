package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.service.LLMCacheService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
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
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public ChatApiController(ConversationSession conversationSession, LLMCacheService llmCacheService, MessageSource messageSource, LocaleUtils localeUtils) {
        this.conversationSession = conversationSession;
        this.llmCacheService = llmCacheService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @PostMapping("/report")
    public ResponseEntity<String> reportResponse(@RequestBody String response) {

        var user = conversationSession.getImmobiliareUser();
        if (user.isEmpty()) {
            var notOk =  messageSource.getMessage("toastify.not.logged.in", null, localeUtils.getCurrentLocale());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(notOk);
        }

        llmCacheService.reportInaccurateResponse(response);

        // Return a response to the frontend
        var messageOk =  messageSource.getMessage("toastify.ok.reporting", null, localeUtils.getCurrentLocale());
        return ResponseEntity.ok(messageOk);

    }

}
