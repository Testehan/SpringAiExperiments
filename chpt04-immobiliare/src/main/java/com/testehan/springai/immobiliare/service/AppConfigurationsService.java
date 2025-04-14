package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.AppConfigurations;
import com.testehan.springai.immobiliare.repository.AppConfigurationsRepository;
import org.springframework.stereotype.Service;

@Service
public class AppConfigurationsService {

    private final AppConfigurationsRepository appConfigurationsRepository;


    public AppConfigurationsService(AppConfigurationsRepository appConfigurationsRepository) {
        this.appConfigurationsRepository = appConfigurationsRepository;
    }

    public boolean isResetSearchesAvailableEnabled(){
        return getConfig().isScheduled_task_resetSearchesAvailable();
    }

    public boolean isDeactivatedOldListingsEnabled(){
        return getConfig().isScheduled_task_deactivatedOldListings();
    }

    public boolean isSendReactivationEmailEnabled(){
        return getConfig().isScheduled_task_sendReactivationEmail();
    }

    public boolean isSendReactivationSMSEnabled() {
        return getConfig().isScheduled_task_sendReactivationSMS();
    }

    public boolean isSendWelcomeEmailEnabled(){
        return getConfig().isEmail_sendWelcomeEmail();
    }

    public boolean isSendListingAddedEmail(){
        return getConfig().isEmail_sendListingAddedEmail();
    }


    private AppConfigurations getConfig() {
        return appConfigurationsRepository.findById("configurations").orElse(new AppConfigurations());
    }
}
