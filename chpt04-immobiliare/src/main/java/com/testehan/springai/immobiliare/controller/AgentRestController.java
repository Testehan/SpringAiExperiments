package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.model.ContactStatus;
import com.testehan.springai.immobiliare.model.Lead;
import com.testehan.springai.immobiliare.service.*;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/agent")
public class AgentRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRestController.class);

    private final LeadService leadService;
    private final LeadConversationService leadConversationService;
    private final ApartmentService apartmentService;
    private final ListingImageService listingImageService;
    private final WhatsAppService whatsAppService;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public AgentRestController(LeadService leadService, LeadConversationService leadConversationService, ApartmentService apartmentService, ListingImageService listingImageService, WhatsAppService whatsAppService, MessageSource messageSource, LocaleUtils localeUtils) {
        this.leadService = leadService;
        this.leadConversationService = leadConversationService;
        this.apartmentService = apartmentService;
        this.listingImageService = listingImageService;
        this.whatsAppService = whatsAppService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @GetMapping("/leads/url")
    public void getJsonContainingLeadURLs(HttpServletResponse response) {
        leadService.downloadJsonContainingLeadURLs(response);
    }

    @GetMapping("/leads/phone")
    public void getJsonContainingLeadPhones(HttpServletResponse response) {
        leadService.downloadJsonContainingLeadPhones(response);
    }

    @GetMapping("/leads/phone/{waUserId}")
    public String getConversation(@PathVariable String waUserId) {
        return leadConversationService.getConversation(waUserId);
    }

    @PatchMapping("/leads/status")
    public ResponseEntity<String> updateLeadStatusByPhone(@RequestParam String phoneNumber, @RequestParam String status)
    {
        var updateStatus = leadService.updateLeadStatus(phoneNumber, status);

        return ResponseEntity.ok(updateStatus);
    }

    @PostMapping("/leads/reply")
    public ResponseEntity<String> replyToLead(@RequestParam String phoneNumber, @RequestParam String reply, @RequestParam Boolean isFirstMessage) {
        return whatsAppService.sendMessage(phoneNumber, reply, isFirstMessage);
    }

    @PostMapping("/batchsave")
    public ResponseEntity<String> batchSaveApartment(Apartment apartment,
                                                     @RequestParam(value="apartmentImages", required = false) MultipartFile[] apartmentImages,
                                                     @RequestParam(value = "listingSourceUrl") String listingSourceUrl) throws IOException
    {
        LOGGER.info("Batch save - start");
        String phoneNumber = "";
        Optional<Lead> leadByListingUrl = leadService.findLeadByListingUrl(listingSourceUrl);
        if (leadByListingUrl.isPresent()){
            phoneNumber = leadByListingUrl.get().getPhoneNumber().substring(2); // i use substring to eliminate the country prefix part
        }
        apartment.setContact(phoneNumber);
        apartment.setWhatsapp(true);        // right now i assume that all listings that will get here will have whatsapp

        List<ApartmentImage> processedImages = listingImageService.processImages(apartmentImages);
        apartmentService.saveApartmentAndImages(apartment, processedImages, Optional.empty(),true);
        leadService.updateLeadStatus(apartment.getContact(), ContactStatus.DONE.toString());

        return ResponseEntity.ok(messageSource.getMessage("toastify.add.listing.success", null, localeUtils.getCurrentLocale()));

    }
}
