package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ContactAttempt;
import com.testehan.springai.immobiliare.model.ContactStatus;
import com.testehan.springai.immobiliare.repository.ContactAttemptRepository;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/a")
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

            Optional<ContactAttempt> contactAttemptOptional = contactAttemptRepository.findByPhoneNumber(contactAttempt.getPhoneNumber());
            var isPhoneUsed = contactAttemptOptional.isPresent();
            if (Objects.isNull(contactAttempt.getId())) {
                if (isPhoneUsed) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(messageSource.getMessage("toastify.admin.contact.attempt.failure.phone", null, localeUtils.getCurrentLocale()));
                }
                contactAttempt.setCreatedAt(System.currentTimeMillis());
                contactAttemptRepository.save(contactAttempt);
            } else {
                    contactAttempt.setUpdatedAt(System.currentTimeMillis());
                    contactAttemptRepository.save(contactAttempt);
            }

        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/contact-attempts/download")
    public void downloadCsv(@RequestParam String value, HttpServletResponse response) throws IOException {
        // we want the accepted contacts + ones from a particular url
        var contactAttempts = contactAttemptRepository.findByListingUrlContainingAndStatus(value, String.valueOf(ContactStatus.ACCEPTED));

        // Set headers for file download
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"data.csv\"");

        // Write CSV to response
        try (PrintWriter writer = response.getWriter()) {
            writer.println("\"Origin URL\",\"Property Images Limit\"");// headers

            for (ContactAttempt contactAttempt : contactAttempts){
                writer.println("\"" + contactAttempt.getListingUrl() + "\", 10");
            }
        }
    }

}
