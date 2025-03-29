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
public class SetRentOrBuyAndCityAndDescriptionHandler implements ApiChatCallHandler {

    private final ConversationSession conversationSession;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;
    private final SearchListingsHandler searchListingsHandler;

    public SetRentOrBuyAndCityAndDescriptionHandler(ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils, SearchListingsHandler searchListingsHandler) {
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
        this.searchListingsHandler = searchListingsHandler;
    }


    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        String[] parts = serviceCall.message().split(",");

        conversationSession.setRentOrSale(parts[0]);
        var cityName = parts[1];

        SupportedCity supportedCity = SupportedCity.getByName(cityName);
        var description = parts[2];
        if (supportedCity.compareTo(UNSUPPORTED) != 0) {
            conversationSession.setCity(cityName);
            return searchListingsHandler.handle(new ServiceCall(ApiCall.GET_APARTMENTS, description), session);
        } else {
            var supportedCities = SupportedCity.getSupportedCities().stream().collect(Collectors.joining(", "));
            return new ResultsResponse(messageSource.getMessage("M021_SUPPORTED_CITIES",  new Object[]{supportedCities}, localeUtils.getCurrentLocale()));
        }
    }

    @Override
    public ApiCall getApiCall() {
        return ApiCall.SET_RENT_OR_BUY_AND_CITY_AND_DESCRIPTION;
    }
}
