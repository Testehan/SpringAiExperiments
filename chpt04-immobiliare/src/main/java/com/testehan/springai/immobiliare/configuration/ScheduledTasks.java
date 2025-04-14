package com.testehan.springai.immobiliare.configuration;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.*;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTasks.class);

    @Value("${app.url}")
    private String appUrl;

    private final UserService userService;
    private final ApartmentCrudService apartmentCrudService;
    private final ConversationService conversationService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final StatisticsService statisticsService;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;
    private final AppConfigurationsService appConfigurationsService;

    public ScheduledTasks(UserService userService, ApartmentCrudService apartmentCrudService, ConversationService conversationService, EmailService emailService, SmsService smsService, StatisticsService statisticsService, MessageSource messageSource,
                          LocaleUtils localeUtils, AppConfigurationsService appConfigurationsService){
        this.userService = userService;
        this.apartmentCrudService = apartmentCrudService;
        this.conversationService = conversationService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.statisticsService = statisticsService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
        this.appConfigurationsService = appConfigurationsService;
    }

    @Scheduled(cron = "0 0 2 * * ?")         // Code to run at 2 am every day
    public void resetSearchesAvailableForNonAdminUsers() {
        if (appConfigurationsService.isResetSearchesAvailableEnabled()) {
            userService.resetSearchesAvailable();
            LOGGER.info("Scheduled Task - The user available searches number was reset");
        } else{
            LOGGER.info("Scheduled Task - resetSearchesAvailable is disabled");
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")        // Code to run at 3 AM every day
    public void deactivatedListingsLastUpdatedMoreThan2WeeksAgo() {
        if (appConfigurationsService.isDeactivatedOldListingsEnabled()) {
            LocalDateTime twoWeeksAgo = LocalDateTime.now().minus(14, ChronoUnit.DAYS);
            apartmentCrudService.deactivateApartments(twoWeeksAgo);
            LOGGER.info("Scheduled Task - The listings last updated before {} were deactivated.", twoWeeksAgo);
        } else {
            LOGGER.info("Scheduled Task - deactivatedOldListings is disabled");
        }
    }

    @Scheduled(cron = "0 0 4 * * ?")        // Code to run at 4 AM every day
    public void deleteDeletedUsersMoreThan1DayAgo() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
        userService.deleteDeletedUsers(oneDayAgo);
        LOGGER.info("Scheduled Task - The deleted users before {} were removed.", oneDayAgo);
    }

    @Scheduled(cron = "0 0 4 * * ?")        // Code to run at 4 AM every day
    public void deleteConversationHistoryOlderThan1Week() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minus(1, ChronoUnit.WEEKS);
        conversationService.cleanConversationHistoryOlderThan(oneWeekAgo);

        LOGGER.info("Scheduled Task - The conversation history before {} was cleaned.", oneWeekAgo);
    }


    @Scheduled(cron = "0 0 8 * * ?")        // Code to run at 8 AM every day
//    @Scheduled(cron = "0 0/3 * * * ?")          // runs every 3 mins for testing purposes
    public void sendReactivationEmail() {

        if (!appConfigurationsService.isSendReactivationEmailEnabled()) {
            LocalDateTime twelveDaysAgo = LocalDateTime.now().minus(12, ChronoUnit.DAYS);
            LOGGER.info("Scheduled Task - Reactivation emails will be send to owners.");

            var listings = apartmentCrudService.findByLastUpdateDateTimeBefore(twelveDaysAgo);

            for (Apartment listing : listings) {
                LOGGER.info(listing.getLastUpdateDateTime() + "       " + listing.getName());
                var contact = listing.getContactEmail();
                var reactivateLink = appUrl + "/reactivate?token=" + listing.getActivationToken() + "&id=" + listing.getId().toString();

                if (ContactValidator.isValidEmail(contact)) {
                    emailService.sendReactivateListingEmail(contact, "", listing.getName(), reactivateLink, localeUtils.getCurrentLocale());
                }

            }
        } else {
            LOGGER.info("Scheduled Task - sendReactivationEmail is disabled");
        }

    }

    @Scheduled(cron = "0 0 8 * * ?")        // Code to run at 8 AM every day
//    @Scheduled(cron = "0 0/3 * * * ?")          // runs every 3 mins for testing purposes
    public void sendReactivationSMS() {
        if (!appConfigurationsService.isSendReactivationSMSEnabled()) {
            LocalDateTime thirteenDaysAgo = LocalDateTime.now().minus(13, ChronoUnit.DAYS);
            LOGGER.info("Scheduled Task - Reactivation sms will be send to owners.");

            var listings = apartmentCrudService.findByLastUpdateDateTimeBefore(thirteenDaysAgo);

            for (Apartment listing : listings) {
                LOGGER.info(listing.getLastUpdateDateTime() + "       " + listing.getName());
                var contact = listing.getContact();
                var reactivateLink = appUrl + "/reactivate?token=" + listing.getActivationToken() + "&id=" + listing.getId().toString();

                if (ContactValidator.isValidPhoneNumber(contact, "RO")) {
                    var phoneWithPrefix = ContactValidator.getPhoneNumberWithPrefix(contact, "RO");
                    if (phoneWithPrefix.isEmpty()) {
                        LOGGER.error("Can't send SMS for {} of listing named {}", contact, listing.getName());
                    } else {
                        var smsMessage = messageSource.getMessage("sms.keep.active", null, localeUtils.getCurrentLocale()) + " " + reactivateLink;
// TODO when you will use your domain, you can test this out, as right now the sms length exceeds the allowed number of chars, because of the currently long domain name..
                        //                    smsService.sendSms(phoneWithPrefix.get(), smsMessage);    // "or reply yes" => this is not implemented yet, see notes from SmsService
                    }

                }
            }
        } else {
            LOGGER.info("Scheduled Task - sendReactivationSMS is disabled");
        }

    }

    @Scheduled(cron = "0 0 7 * * ?")        // Code to run at 7 AM every day
    public void updateStatistics() {
        LOGGER.info("Scheduled Task - Update statistics");
        statisticsService.computeAndStoreStatistics();
    }
}
