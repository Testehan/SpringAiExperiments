package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Lead;
import com.testehan.springai.immobiliare.model.ContactStatus;
import com.testehan.springai.immobiliare.repository.LeadRepository;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Optional;

@Service
public class LeadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public LeadService(LeadRepository leadRepository, MessageSource messageSource, LocaleUtils localeUtils) {
        this.leadRepository = leadRepository;
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

    public Optional<Lead> findLeadById(String leadId){
        return leadRepository.findById(new ObjectId(leadId));
    }

    public void updateLeadStatus(String phoneNumber){

        var leadOptional = findLeadByPhoneNumber(phoneNumber);
        if (leadOptional.isPresent()){
            var lead = leadOptional.get();
            lead.setStatus(ContactStatus.DONE);
            leadRepository.save(lead);
        } else {
            LOGGER.warn("No leads found for {}",phoneNumber);
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

    public void downloadCsv(String filter, HttpServletResponse response){
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
            LOGGER.error("Could not generatethe CSV. {}",e.getMessage());
        }
    }

    public void deleteLeadById(String leadId) {
        leadRepository.deleteById(new ObjectId(leadId));
    }
}
