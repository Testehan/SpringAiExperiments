package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.service.ApartmentCrudService;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@RestController
public class ReactivateController {

    @Value("${app.url}")
    private String appUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactivateController.class);
    private final ApartmentCrudService apartmentCrudService;
    private final MessageSource messageSource;
    private final FormattingUtil formattingUtil;

    public ReactivateController(ApartmentCrudService apartmentCrudService, MessageSource messageSource, FormattingUtil formattingUtil) {
        this.apartmentCrudService = apartmentCrudService;
        this.messageSource = messageSource;
        this.formattingUtil = formattingUtil;
    }

    @GetMapping("/reactivate")
    public ResponseEntity<String> favourite(@RequestParam String token,     // activation token
                                            @RequestParam String id,         // listing id
                                            HttpSession session,
                                            Locale locale) {

        // 1. Validate token
        var optionalListing = validateToken(token, id);
        if (optionalListing.isEmpty()) {
            LOGGER.warn("Invalid or expired reactivation token.");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(appUrl +"/error"))
                    .build();

        }

        // 2. Reactivate the listing
        boolean reactivated = reactivateListing(optionalListing.get());
        if (!reactivated) {
            LOGGER.warn("Failed to reactivate listing.");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(appUrl +"/error"))
                    .build();
        }

        // 3. Return success response
        var message = messageSource.getMessage("listing.reactivate.success", null, locale);
        session.setAttribute("confirmationMessage", message);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(appUrl +"/confirmation"))
                .build();
    }

    private boolean reactivateListing(Apartment apartment) {
        try {
            var lastUpdatedTime = formattingUtil.getFormattedDateCustom(LocalDateTime.now());
            apartment.setLastUpdateDateTime(lastUpdatedTime);
            apartment.setActive(true);
            apartmentCrudService.saveApartment(apartment);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<Apartment> validateToken(String activationToken, String listingId) {
        var listingOptional = apartmentCrudService.findApartmentById(listingId);
        if (listingOptional.isPresent()){
            var listing = listingOptional.get();
            if (listing.getActivationToken().equals(activationToken)){
                return Optional.of(listing);
            }
        }
        return Optional.empty();
    }

}
