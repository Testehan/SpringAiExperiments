package com.testehan.springai.immobiliare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testehan.springai.immobiliare.model.ContactStatus;
import com.testehan.springai.immobiliare.model.Lead;
import com.testehan.springai.immobiliare.repository.LeadRepository;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.utils.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;

    private final LeadConversationService leadConversationService;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public LeadService(LeadRepository leadRepository, LeadConversationService leadConversationService, MessageSource messageSource, LocaleUtils localeUtils) {
        this.leadRepository = leadRepository;
        this.leadConversationService = leadConversationService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    public Optional<Lead> findLeadByPhoneNumber(String phoneNumber){

        if (!phoneNumber.startsWith("+4")) {
            // setting the prefix and then searching to see if another phone with that prefix is present.
            phoneNumber = "+4" + phoneNumber;
        }

        return leadRepository.findByPhoneNumber(phoneNumber);

    }

    public Optional<Lead> findLeadByListingUrl(String listingUrl) {
        return leadRepository.findByListingUrl(listingUrl);

    }

    public Optional<Lead> findLeadById(String leadId){
        return leadRepository.findById(new ObjectId(leadId));
    }

    public String updateLeadStatus(String phoneNumber, String status){
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }

        var leadOptional = findLeadByPhoneNumber(phoneNumber);
        if (leadOptional.isPresent()){
            var lead = leadOptional.get();
            lead.setStatus(ContactStatus.valueOf(status.toUpperCase()));
            lead.setUpdatedAt(System.currentTimeMillis());
            leadRepository.save(lead);
            return "Lead status updated successfully";
        } else {
            LOGGER.warn("No leads found for {}",phoneNumber);
            return "No leads found for " + phoneNumber;
        }

    }

    public ResponseEntity<String> saveOrUpdate(Lead lead, Optional<Lead> leadOptionalFromDB){
        var isPhoneUsed = leadOptionalFromDB.isPresent();
        if (Objects.isNull(lead.getId())) {
            if (isPhoneUsed) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(messageSource.getMessage("toastify.admin.contact.attempt.failure.phone", null, localeUtils.getCurrentLocale()));
            }
            lead.setCreatedAt(System.currentTimeMillis());
            leadRepository.save(lead);
        } else {
            lead.setUpdatedAt(System.currentTimeMillis());
            leadRepository.save(lead);
        }

        return ResponseEntity.ok("ok");
    }

    public void downloadCsvContainingLeadURLs(String filter, HttpServletResponse response){
        // we want the accepted contacts + ones from a particular url
        var leads = leadRepository.findByListingUrlContainingAndStatus(filter, String.valueOf(ContactStatus.ACCEPTED));

        // Set headers for file download
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"data.csv\"");

        // Write CSV to response
        try (PrintWriter writer = response.getWriter()) {
            writer.println("\"Origin URL\",\"Property Images Limit\"");// headers

            for (Lead lead : leads){
                writer.println("\"" + lead.getListingUrl() + "\", 10");
            }
        } catch (IOException e) {
            LOGGER.error("Could not generate the CSV. {}",e.getMessage());
        }
    }

    public void downloadJsonContainingLeadURLs(HttpServletResponse response) {
        // Fetch leads matching the criteria
        var leads = leadRepository.findByStatusIn(List.of(String.valueOf(ContactStatus.ACCEPTED)));

        // Extract listing URLs into a list
        List<String> urls = leads.stream()
                .map(Lead::getListingUrl)
                .collect(Collectors.toList());

        // Set headers for JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Use your preferred JSON serializer. Here's one using Jackson:
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(response.getWriter(), urls);
        } catch (IOException e) {
            LOGGER.error("Could not generate JSON. {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public void downloadJsonContainingLeadPhones(HttpServletResponse response) {
        // Fetch leads matching the criteria
        var leads = leadRepository.findByStatusIn(List.of(String.valueOf(ContactStatus.NOT_CONTACTED), String.valueOf(ContactStatus.CONTACTED)));

        // Extract listing URLs into a list
        List<Map<String, String>> phoneNumbersToURLs = leads.stream()
                .filter(lead -> leadConversationService.doWeNeedToContinueConversation(lead.getPhoneNumber()))
                .map(lead -> Map.of(
                        "phoneNumber", lead.getPhoneNumber(),
                        "url", lead.getListingUrl()
                ))
                .collect(Collectors.toList());

        // Set headers for JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Use your preferred JSON serializer. Here's one using Jackson:
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(response.getWriter(), phoneNumbersToURLs);
        } catch (IOException e) {
            LOGGER.error("Could not generate JSON. {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    public void downloadCsvContainingLeadPhones(String filter, HttpServletResponse response){
        // we want the accepted contacts + ones from a particular url
        var leads = leadRepository.findByListingUrlContainingAndStatus(filter, String.valueOf(ContactStatus.NOT_CONTACTED));

        // Set headers for file download
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"data.csv\"");

        // Write CSV to response
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Lead phone");// headers

            for (Lead lead : leads){
                writer.println(lead.getPhoneNumber());
            }
        } catch (IOException e) {
            LOGGER.error("Could not generate the CSV. {}",e.getMessage());
        }
    }


    public void deleteLeadById(String leadId) {
        leadRepository.deleteById(new ObjectId(leadId));
    }

    public Page<Lead> findAll(PageRequest pageRequest, String searchText) {
        if (!StringUtils.isEmpty(searchText)) {
            return leadRepository.searchLeads(searchText.toLowerCase(), pageRequest);
        }

        return leadRepository.findAll(pageRequest);
    }
}
