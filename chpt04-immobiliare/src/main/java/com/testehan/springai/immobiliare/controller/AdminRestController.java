package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.service.WhatsAppService;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.Lead;
import com.testehan.springai.immobiliare.service.LeadService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/a")
public class AdminRestController {

    private final LeadService leadService;
    private final WhatsAppService whatsAppService;
    private final ConversationSession conversationSession;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public AdminRestController(LeadService leadService, WhatsAppService whatsAppService, ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.leadService = leadService;
        this.whatsAppService = whatsAppService;
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @PostMapping("/leads")
    public ResponseEntity<String> createLead(@RequestBody Lead lead) {
        var user = conversationSession.getImmobiliareUser().get();
        if (user.isAdmin()) {

            Optional<Lead> leadOptional = leadService.findLeadByPhoneNumber(lead.getPhoneNumber());

            return leadService.saveOrUpdate(lead, leadOptional);

        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/leads/download")
    public void downloadCsv(@RequestParam String value, HttpServletResponse response) {
        leadService.downloadCsv(value, response);
    }

    // todo this is for testing purposes
    @GetMapping("/whatsapp")
    public void whatsapp() {
        // call whatsAppService.sendMessage
    }

}
