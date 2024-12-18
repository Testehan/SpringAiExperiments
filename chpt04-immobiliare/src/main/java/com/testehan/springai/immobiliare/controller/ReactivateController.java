package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.service.ApartmentService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

@RestController
public class ReactivateController {

    // todo this needs to be configured to your domain
    public static final String DOMAIN = "http://localhost:8080";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactivateController.class);
    private final ApartmentService apartmentService;

    public ReactivateController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @GetMapping("/reactivate")
    public ResponseEntity<String> favourite(@RequestParam String token,     // activation token
                                            @RequestParam String id,         // listing id
                                            HttpSession session) {

        // 1. Validate token
        var optionalListing = validateToken(token, id);
        if (optionalListing.isEmpty()) {
            LOGGER.warn("Invalid or expired reactivation token.");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(DOMAIN +"/error"))
                    .build();

        }

        // 2. Reactivate the listing
        boolean reactivated = reactivateListing(optionalListing.get());
        if (!reactivated) {
            LOGGER.warn("Failed to reactivate listing.");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(DOMAIN +"/error"))
                    .build();
        }

        // 3. Return success response
        session.setAttribute("confirmationMessage", "Your listing was reactivated with success!");
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(DOMAIN +"/confirmation"))
                .build();
    }

    private boolean reactivateListing(Apartment apartment) {
        try {
            apartment.setActive(true);
            apartmentService.saveApartment(apartment);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<Apartment> validateToken(String activationToken, String listingId) {
        var listingOptional = apartmentService.findApartmentById(listingId);
        if (listingOptional.isPresent()){
            var listing = listingOptional.get();
            if (listing.getActivationToken().equals(activationToken)){
                return Optional.of(listing);
            }
        }
        return Optional.empty();
    }

}
