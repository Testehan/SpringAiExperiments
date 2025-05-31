package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.service.WhatsAppService;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.Lead;
import com.testehan.springai.immobiliare.service.LeadService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
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
            lead.setPhoneNumber(lead.getPhoneNumber().replace(" ", ""));
            Optional<Lead> leadOptional = leadService.findLeadByPhoneNumber(lead.getPhoneNumber());

            return leadService.saveOrUpdate(lead, leadOptional);

        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/leads/download-url")
    public void downloadCsvContainingLeadURLs(@RequestParam String value, HttpServletResponse response) {
        leadService.downloadCsvContainingLeadURLs(value, response);
    }

    @GetMapping("/leads/download-phone")
    public void downloadCsvContaining(@RequestParam String value, HttpServletResponse response) {
        leadService.downloadCsvContainingLeadPhones(value, response);
    }

    @PostMapping("/leads/delete/{leadId}")
    public ResponseEntity<String> deleteLead(@PathVariable(value = "leadId") String leadId) {

        var lead = leadService.findLeadById(leadId);
        if (lead.isPresent()) {
            leadService.deleteLeadById(leadId);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(messageSource.getMessage("toastify.delete.listing.failure.notowner", null,localeUtils.getCurrentLocale()));
        }
        return ResponseEntity.ok(messageSource.getMessage("toastify.delete.listing.success", null,localeUtils.getCurrentLocale()));
    }

}
