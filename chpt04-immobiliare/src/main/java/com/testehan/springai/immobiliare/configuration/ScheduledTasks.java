package com.testehan.springai.immobiliare.configuration;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.EmailService;
import com.testehan.springai.immobiliare.service.SmsService;
import com.testehan.springai.immobiliare.util.ContactValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class ScheduledTasks {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final UserService userService;
    private final ApartmentService apartmentService;
    private final EmailService emailService;
    private final SmsService smsService;

    public ScheduledTasks(UserService userService, ApartmentService apartmentService, EmailService emailService, SmsService smsService){
        this.userService = userService;
        this.apartmentService = apartmentService;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @Scheduled(cron = "0 00 12 * * ?")          // Code to run at 12 PM every day
    public void resetSearchesAvailableForNonAdminUsers() {
        userService.resetSearchesAvailable();
        log.info("The user available searches number was reset");
    }

    @Scheduled(cron = "0 0 3 * * ?")        // Code to run at 3 AM every day
//    @Scheduled(cron = "0 0/3 * * * ?")          // runs every 3 mins for testing purposes
    public void deactivatedListingsLastUpdated2WeeksAgoAndSendReactivationEmailOrSMS() {
        // Code to run at 3 AM every day
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minus(14, ChronoUnit.DAYS);

        var listings = apartmentService.findByLastUpdateDateTimeBefore(twoWeeksAgo);

        apartmentService.deactivateApartments(twoWeeksAgo);
        log.info("The listings last updated before {} were deactivated.", twoWeeksAgo);

        for (Apartment listing : listings){
            log.info(listing.getLastUpdateDateTime() + "       " + listing.getName());
            var contact = listing.getContact();
            var reactivateLink = "http://localhost:8080/reactivate?token="+listing.getActivationToken()+"&id=" + listing.getId().toString();

            if (ContactValidator.isValidEmail(contact)){
                emailService.sendReactivateListingEmail(contact,"",listing.getName(),reactivateLink);
            }
            if (ContactValidator.isValidPhoneNumber(contact,"RO")){
                var phoneWithPrefix = ContactValidator.getPhoneNumberWithPrefix(contact, "RO");
                if (phoneWithPrefix.isEmpty()){
                    log.error("Can't send SMS for + " + contact + " of listing " + listing.getName());
                } else {
                    // if you will need a longer message and thus to shorthen the URL, there are various services online that offer this
                    smsService.sendSms(phoneWithPrefix.get(),"Hi! Click link to reactivate your listing " + reactivateLink);    // "or reply yes" => this is not implemented yet, see notes from SmsService
                }

            }
        }

    }
}
