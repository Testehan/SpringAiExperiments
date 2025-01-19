package com.testehan.springai.immobiliare.configuration;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.EmailService;
import com.testehan.springai.immobiliare.service.SmsService;
import com.testehan.springai.immobiliare.util.ContactValidator;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class ScheduledTasks {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Value("${app.url}")
    private String appUrl;

    private final UserService userService;
    private final ApartmentService apartmentService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final LocaleUtils localeUtils;

    public ScheduledTasks(UserService userService, ApartmentService apartmentService, EmailService emailService, SmsService smsService,
                          LocaleUtils localeUtils){
        this.userService = userService;
        this.apartmentService = apartmentService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.localeUtils = localeUtils;
    }

    @Scheduled(cron = "0 00 12 * * ?")          // Code to run at 12 PM every day
    public void resetSearchesAvailableForNonAdminUsers() {
        userService.resetSearchesAvailable();
        log.info("Scheduled Task - The user available searches number was reset");
    }
    @Scheduled(cron = "0 0 3 * * ?")        // Code to run at 3 AM every day
//    @Scheduled(cron = "0 0/3 * * * ?")          // runs every 3 mins for testing purposes
    public void deactivatedListingsLastUpdatedMoreThan2WeeksAgo() {
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minus(14, ChronoUnit.DAYS);
        apartmentService.deactivateApartments(twoWeeksAgo);
        log.info("Scheduled Task - The listings last updated before {} were deactivated.", twoWeeksAgo);
    }

    @Scheduled(cron = "0 0 8 * * ?")        // Code to run at 8 AM every day
//    @Scheduled(cron = "0 0/3 * * * ?")          // runs every 3 mins for testing purposes
    public void sendReactivationEmailOrSMS() {

        LocalDateTime twelveDaysAgo = LocalDateTime.now().minus(12, ChronoUnit.DAYS);
        log.info("Scheduled Task - Reactivation emails or sms will be send to owners.");

        var listings = apartmentService.findByLastUpdateDateTimeBefore(twelveDaysAgo);

        for (Apartment listing : listings){
            log.info(listing.getLastUpdateDateTime() + "       " + listing.getName());
            var contact = listing.getContact();
            var reactivateLink = appUrl + "/reactivate?token="+listing.getActivationToken()+"&id=" + listing.getId().toString();

            if (ContactValidator.isValidEmail(contact)){
                emailService.sendReactivateListingEmail(contact,"",listing.getName(),reactivateLink, localeUtils.getCurrentLocale());
            }
            if (ContactValidator.isValidPhoneNumber(contact,"RO")){
                var phoneWithPrefix = ContactValidator.getPhoneNumberWithPrefix(contact, "RO");
                if (phoneWithPrefix.isEmpty()){
                    log.error("Can't send SMS for + " + contact + " of listing " + listing.getName());
                } else {
                    var smsMessage = "Hi! Reactivate listing: " + reactivateLink;
                    smsService.sendSms(phoneWithPrefix.get(), smsMessage);    // "or reply yes" => this is not implemented yet, see notes from SmsService
                }

            }
        }

    }
}
