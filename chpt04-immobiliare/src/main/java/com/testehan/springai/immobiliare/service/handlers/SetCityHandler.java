package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.service.CityService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SetCityHandler implements ApiChatCallHandler {

    private final CityService cityService;
    private final ConversationSession conversationSession;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public SetCityHandler(CityService cityService, ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.cityService = cityService;
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        var cityName = serviceCall.message();
        conversationSession.setCity(cityName);

        if (cityService.isEnabled(cityName)) {
            return new ResultsResponse(messageSource.getMessage("M03_BUDGET",  null, localeUtils.getCurrentLocale()));
        } else {
            cityService.requestCity(cityName);
            var supportedCities = cityService.getEnabledCityNames().stream().collect(Collectors.joining(", "));
            return new ResultsResponse(messageSource.getMessage("M021_SUPPORTED_CITIES",  new Object[]{supportedCities}, localeUtils.getCurrentLocale()));
        }
    }

    @Override
    public ApiCall getApiCall() {
        return ApiCall.SET_CITY;
    }


}
