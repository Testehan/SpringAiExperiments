package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.ContactAttempt;
import com.testehan.springai.immobiliare.model.ContactStatus;
import com.testehan.springai.immobiliare.repository.ContactAttemptRepository;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpServletResponse;
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
public class ContactAttemptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactAttemptService.class);

    private final ContactAttemptRepository contactAttemptRepository;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public ContactAttemptService(ContactAttemptRepository contactAttemptRepository, MessageSource messageSource, LocaleUtils localeUtils) {
        this.contactAttemptRepository = contactAttemptRepository;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    public Optional<ContactAttempt> findContactAttemptByPhoneNumber(String phoneNumber){

        if (!phoneNumber.startsWith("+4")) {
            // setting the prefix and then searching to see if another phone with that prefix is present.
            phoneNumber = "+4" + phoneNumber;
        }

        return contactAttemptRepository.findByPhoneNumber(phoneNumber);

    }

    public void updateContactAttemptStatus(String phoneNumber){

        var contactAttemptOptional = findContactAttemptByPhoneNumber(phoneNumber);
        if (contactAttemptOptional.isPresent()){
            var contactAttempt = contactAttemptOptional.get();
            contactAttempt.setStatus(ContactStatus.DONE);
            contactAttemptRepository.save(contactAttempt);
        } else {
            LOGGER.warn("No contact attempt found for {}",phoneNumber);
        }

    }

    public ResponseEntity<String> saveOrUpdate(ContactAttempt contactAttempt, Optional<ContactAttempt> contactAttemptOptionalFromDB){
        var isPhoneUsed = contactAttemptOptionalFromDB.isPresent();
        if (Objects.isNull(contactAttempt.getId())) {
            if (isPhoneUsed) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(messageSource.getMessage("toastify.admin.contact.attempt.failure.phone", null, localeUtils.getCurrentLocale()));
            }
            contactAttempt.setCreatedAt(System.currentTimeMillis());
            contactAttemptRepository.save(contactAttempt);
        } else {
            contactAttempt.setUpdatedAt(System.currentTimeMillis());
            contactAttemptRepository.save(contactAttempt);
        }

        return ResponseEntity.ok("ok");
    }

    public void downloadCsv(String filter, HttpServletResponse response){
        // we want the accepted contacts + ones from a particular url
        var contactAttempts = contactAttemptRepository.findByListingUrlContainingAndStatus(filter, String.valueOf(ContactStatus.ACCEPTED));

        // Set headers for file download
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"data.csv\"");

        // Write CSV to response
        try (PrintWriter writer = response.getWriter()) {
            writer.println("\"Origin URL\",\"Property Images Limit\"");// headers

            for (ContactAttempt contactAttempt : contactAttempts){
                writer.println("\"" + contactAttempt.getListingUrl() + "\", 10");
            }
        } catch (IOException e) {
            LOGGER.error("Could not generatethe CSV. {}",e.getMessage());
        }
    }
}
