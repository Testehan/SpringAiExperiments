package com.testehan.springai.immobiliare.configuration;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
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

    public ScheduledTasks(UserService userService, ApartmentService apartmentService){
        this.userService = userService;
        this.apartmentService = apartmentService;
    }

    @Scheduled(cron = "0 00 12 * * ?")          // Code to run at 12 PM every day
    public void reportCurrentTime() {
        userService.resetSearchesAvailable();
        log.info("The user available searches number was reset");
    }

    // TODO create one that will deactivate listings that were updated more than 2 week ago
    // also send email to the owners with the reactivation link
//    @Scheduled(cron = "0 0 3 * * ?")        // Code to run at 3 AM every day

    @Scheduled(cron = "0 0/3 * * * ?")          // runs every 3 mins for testing purposes
    public void myScheduledMethod() {
        // Code to run at 3 AM every day
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minus(14, ChronoUnit.DAYS);

        var listings = apartmentService.findByLastUpdateDateTimeBefore(twoWeeksAgo);
        for (Apartment a : listings){
            log.info(a.getLastUpdateDateTime() + "       " + a.getName());
        }

        apartmentService.deactivateApartments(twoWeeksAgo);
        log.info("The listings last updated before " + twoWeeksAgo + " were deactivated.");


    }
}
