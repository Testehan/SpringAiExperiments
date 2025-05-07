package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ContactAttempt;
import com.testehan.springai.immobiliare.service.ContactAttemptService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/a")
public class AdminRestController {

    private final ContactAttemptService contactAttemptService;
    private final ConversationSession conversationSession;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public AdminRestController(ContactAttemptService contactAttemptService, ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.contactAttemptService = contactAttemptService;
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @PostMapping("/contact-attempts")
    public ResponseEntity<String> createContactAttempt(@RequestBody ContactAttempt contactAttempt) {
        var user = conversationSession.getImmobiliareUser().get();
        if (user.isAdmin()) {

            Optional<ContactAttempt> contactAttemptOptional = contactAttemptService.findContactAttemptByPhoneNumber(contactAttempt.getPhoneNumber());

            return contactAttemptService.saveOrUpdate(contactAttempt, contactAttemptOptional);

        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/contact-attempts/download")
    public void downloadCsv(@RequestParam String value, HttpServletResponse response) {
        contactAttemptService.downloadCsv(value, response);
    }

}
