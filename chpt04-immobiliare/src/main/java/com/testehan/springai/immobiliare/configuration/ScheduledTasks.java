package com.testehan.springai.immobiliare.configuration;

import com.testehan.springai.immobiliare.security.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final UserService userService;

    public ScheduledTasks(UserService userService){
        this.userService = userService;
    }

    @Scheduled(cron = "0 00 12 * * ?")
    public void reportCurrentTime() {
        userService.resetSearchesAvailable();
        log.info("The user searches available was reset");
    }
}
