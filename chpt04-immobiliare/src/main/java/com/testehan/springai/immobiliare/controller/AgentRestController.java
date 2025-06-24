package com.testehan.springai.immobiliare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.model.ContactStatus;
import com.testehan.springai.immobiliare.model.Lead;
import com.testehan.springai.immobiliare.service.*;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/agent")
public class AgentRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRestController.class);

    @Value("${app.url}")
    private String appUrl;

    private final LeadService leadService;
    private final LeadConversationService leadConversationService;
    private final ApartmentService apartmentService;
    private final ApartmentCrudService apartmentCrudService;
    private final ListingImageService listingImageService;
    private final MaytapiWhatsAppService maytapiWhatsAppService;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public AgentRestController(LeadService leadService, LeadConversationService leadConversationService, ApartmentService apartmentService, ApartmentCrudService apartmentCrudService, ListingImageService listingImageService, MaytapiWhatsAppService maytapiWhatsAppService, MessageSource messageSource, LocaleUtils localeUtils) {
        this.leadService = leadService;
        this.leadConversationService = leadConversationService;
        this.apartmentService = apartmentService;
        this.apartmentCrudService = apartmentCrudService;
        this.listingImageService = listingImageService;
        this.maytapiWhatsAppService = maytapiWhatsAppService;
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

    @GetMapping("/leads/status")
    public void getLeadsWithStatus(@RequestParam String status, HttpServletResponse response)
    {
        List<Map<String,String>> phoneNumbersToReactivationLinks = new ArrayList<>();
        var phoneNumbers = leadService.getJsonLeadsHavingStatus(status);
        for (String phone : phoneNumbers){
            var listingOptional = apartmentCrudService.findApartmentByContact(phone.replaceFirst("^\\+40", "0"));
            if (listingOptional.isPresent()) {
                var listing = listingOptional.get();
                var reactivateLink = appUrl + "/reactivate?token=" + listing.getActivationToken() + "&id=" + listing.getId().toString();
                phoneNumbersToReactivationLinks.add(Map.of(
                        "phoneNumber", phone,
                        "url", reactivateLink));

            }
        }

        // Set headers for JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(response.getWriter(), phoneNumbersToReactivationLinks);
        } catch (IOException e) {
            LOGGER.error("Could not generate JSON. {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }


    }

    @PostMapping("/leads/reply")
    public ResponseEntity<String> replyToLead(@RequestParam String phoneNumber, @RequestParam String reply, @RequestParam Boolean isFirstMessage) {
//        return whatsAppService.sendMessage(phoneNumber, reply, isFirstMessage);
        return maytapiWhatsAppService.sendMessage(phoneNumber, reply);
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
        apartment.setActive(true);

        List<ApartmentImage> processedImages = listingImageService.processImages(apartmentImages);
        apartmentService.saveApartmentAndImages(apartment, processedImages, Optional.empty(),true);
        if (leadByListingUrl.isPresent()) {
            leadService.updateLeadStatus(leadByListingUrl.get().getPhoneNumber(), ContactStatus.DONE.toString());
        }



        return ResponseEntity.ok(messageSource.getMessage("toastify.add.listing.success", null, localeUtils.getCurrentLocale()));

    }
}
