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
import org.springframework.context.MessageSource;
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
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public ScheduledTasks(UserService userService, ApartmentService apartmentService, EmailService emailService, SmsService smsService, MessageSource messageSource,
                          LocaleUtils localeUtils){
        this.userService = userService;
        this.apartmentService = apartmentService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @Scheduled(cron = "0 0 2 * * ?")         // Code to run at 2 am every day
    public void resetSearchesAvailableForNonAdminUsers() {
        userService.resetSearchesAvailable();
        log.info("Scheduled Task - The user available searches number was reset");
    }
    @Scheduled(cron = "0 0 3 * * ?")        // Code to run at 3 AM every day
    public void deactivatedListingsLastUpdatedMoreThan2WeeksAgo() {
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minus(14, ChronoUnit.DAYS);
        apartmentService.deactivateApartments(twoWeeksAgo);
        log.info("Scheduled Task - The listings last updated before {} were deactivated.", twoWeeksAgo);
    }

    @Scheduled(cron = "0 0 4 * * ?")        // Code to run at 4 AM every day
    public void deleteDeletedUsersMoreThan1DayAgo() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
        userService.deleteDeletedUsers(oneDayAgo);
        log.info("Scheduled Task - The deleted users were removed.", oneDayAgo);
    }


    @Scheduled(cron = "0 0 8 * * ?")        // Code to run at 8 AM every day
//    @Scheduled(cron = "0 0/3 * * * ?")          // runs every 3 mins for testing purposes
    public void sendReactivationEmail() {

        LocalDateTime twelveDaysAgo = LocalDateTime.now().minus(12, ChronoUnit.DAYS);
        log.info("Scheduled Task - Reactivation emails will be send to owners.");

        var listings = apartmentService.findByLastUpdateDateTimeBefore(twelveDaysAgo);

        for (Apartment listing : listings){
            log.info(listing.getLastUpdateDateTime() + "       " + listing.getName());
            var contact = listing.getContactEmail();
            var reactivateLink = appUrl + "/reactivate?token="+listing.getActivationToken()+"&id=" + listing.getId().toString();

            if (ContactValidator.isValidEmail(contact)){
                emailService.sendReactivateListingEmail(contact,"",listing.getName(),reactivateLink, localeUtils.getCurrentLocale());
            }
            
        }

    }

    @Scheduled(cron = "0 0 8 * * ?")        // Code to run at 8 AM every day
//    @Scheduled(cron = "0 0/3 * * * ?")          // runs every 3 mins for testing purposes
    public void sendReactivationSMS() {

        LocalDateTime thirteenDaysAgo = LocalDateTime.now().minus(13, ChronoUnit.DAYS);
        log.info("Scheduled Task - Reactivation sms will be send to owners.");

        var listings = apartmentService.findByLastUpdateDateTimeBefore(thirteenDaysAgo);

        for (Apartment listing : listings){
            log.info(listing.getLastUpdateDateTime() + "       " + listing.getName());
            var contact = listing.getContact();
            var reactivateLink = appUrl + "/reactivate?token="+listing.getActivationToken()+"&id=" + listing.getId().toString();

            if (ContactValidator.isValidPhoneNumber(contact,"RO")){
                var phoneWithPrefix = ContactValidator.getPhoneNumberWithPrefix(contact, "RO");
                if (phoneWithPrefix.isEmpty()){
                    log.error("Can't send SMS for {} of listing named {}", contact , listing.getName());
                } else {
                    var smsMessage = messageSource.getMessage("sms.keep.active", null,localeUtils.getCurrentLocale()) + " " + reactivateLink;
// TODO when you will use your domain, you can test this out, as right now the sms length exceeds the allowed number of chars, because of the currently long domain name..
                    //                    smsService.sendSms(phoneWithPrefix.get(), smsMessage);    // "or reply yes" => this is not implemented yet, see notes from SmsService
                }

            }
        }

    }
}
