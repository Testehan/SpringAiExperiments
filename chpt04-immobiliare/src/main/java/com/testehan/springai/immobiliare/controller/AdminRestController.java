package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ContactAttempt;
import com.testehan.springai.immobiliare.repository.ContactAttemptRepository;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminRestController {

    private final ContactAttemptRepository contactAttemptRepository;
    private final ConversationSession conversationSession;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public AdminRestController(ContactAttemptRepository contactAttemptRepository, ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.contactAttemptRepository = contactAttemptRepository;
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @PostMapping("/contact-attempts")
    public ResponseEntity<String> createContactAttempt(@RequestBody ContactAttempt contactAttempt) {
        var user = conversationSession.getImmobiliareUser().get();
        if (user.isAdmin()) {

            var isPhoneUsed = contactAttemptRepository.findByPhoneNumber(contactAttempt.getPhoneNumber()).isPresent();
            if (isPhoneUsed){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(messageSource.getMessage("toastify.admin.contact.attempt.failure.phone", null,localeUtils.getCurrentLocale()));
            } else {
                contactAttemptRepository.save(contactAttempt);
            }
        }

        return ResponseEntity.ok("ok");
    }

}
