package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.service.ApartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class ReactivateController {

    private final ApartmentService apartmentService;

    public ReactivateController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @GetMapping("/reactivate")
    public ResponseEntity<String> favourite(@RequestParam String token,     // activation token
                                            @RequestParam String id) {      // listing id

        // 1. Validate token
        var optionalListing = validateToken(token, id);
        if (optionalListing.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired reactivation token.");
        }

        // 2. Reactivate the listing
        boolean reactivated = reactivateListing(optionalListing.get());
        if (!reactivated) {
            return ResponseEntity.internalServerError().body("Failed to reactivate listing.");
        }

        // 3. Return success response
        return ResponseEntity.ok("Listing successfully reactivated.");
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
