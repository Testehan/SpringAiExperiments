package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.service.LeadService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/agent")
public class AgentRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRestController.class);

    private final LeadService leadService;
    private final ApartmentApiController apartmentApiController;


    public AgentRestController(LeadService leadService, ApartmentApiController apartmentApiController) {
        this.leadService = leadService;
        this.apartmentApiController = apartmentApiController;
    }

    @GetMapping("/leads/url")
    public void getJsonContainingLeadURLs(HttpServletResponse response) {
        leadService.downloadJsonContainingLeadURLs(response);
    }

    // TODO ...sure..this delegation is not nice. it would be preferred to extract common code in a
    // new service method and then call that service method from both endpoints...But this is some fast fix ;)
    @PostMapping("/batchsave")
    public ResponseEntity<String> batchSaveApartment(Apartment apartment,
                                                     @RequestParam(value="apartmentImages", required = false) MultipartFile[] apartmentImages,
                                                     @RequestParam(value = "listingSourceUrl") String listingSourceUrl) throws IOException {
        return apartmentApiController.batchSaveApartment(apartment,apartmentImages, listingSourceUrl);
    }
}
