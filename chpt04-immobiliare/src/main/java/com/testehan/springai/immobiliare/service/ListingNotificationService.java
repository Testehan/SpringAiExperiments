package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ListingNotificationService {
    @Value("${app.url}")
    private String appUrl;

    private final EmailService emailService;
    private final MaytapiWhatsAppService maytapiWhatsAppService;
    private final LocaleUtils localeUtils;

    public ListingNotificationService(EmailService emailService, MaytapiWhatsAppService maytapiWhatsAppService, LocaleUtils localeUtils) {
        this.emailService = emailService;
        this.maytapiWhatsAppService = maytapiWhatsAppService;
        this.localeUtils = localeUtils;
    }

    public void sendListingAddedEmail(Apartment listing, ImmobiliareUser user) {
        var listingId = listing.getId().toString();
        var viewUrl = appUrl + "/view/" + listingId;
        var editUrl = appUrl + "/edit/" + listingId;
        emailService.sendListingAddedEmail(user.getEmail(), user.getName(), listing.getName(), viewUrl, editUrl ,localeUtils.getCurrentLocale());
    }

    public void sendWhatsAppSocialListingMessage(Apartment listing){
        maytapiWhatsAppService.sendSocialListingMessage(listing.getContact(), appUrl + "/s/" + listing.getSocialId());
    }


}
