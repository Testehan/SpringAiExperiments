package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.model.SupportedCity;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static com.testehan.springai.immobiliare.model.SupportedCity.UNSUPPORTED;

@Component
public class SetCityHandler implements ApiChatCallHandler {

    private final ConversationSession conversationSession;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public SetCityHandler(ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        SupportedCity supportedCity = SupportedCity.getByName(serviceCall.message());

        if (supportedCity.compareTo(UNSUPPORTED) != 0) {
            conversationSession.setCity(serviceCall.message());
            return new ResultsResponse(messageSource.getMessage("M03_BUDGET",  null, localeUtils.getCurrentLocale()));
        } else {
            conversationSession.setCity(serviceCall.message());
            var supportedCities = SupportedCity.getSupportedCities().stream().collect(Collectors.joining(", "));
            return new ResultsResponse(messageSource.getMessage("M021_SUPPORTED_CITIES",  new Object[]{supportedCities}, localeUtils.getCurrentLocale()));
        }
    }

    @Override
    public ApiCall getApiCall() {
        return ApiCall.SET_CITY;
    }


}
