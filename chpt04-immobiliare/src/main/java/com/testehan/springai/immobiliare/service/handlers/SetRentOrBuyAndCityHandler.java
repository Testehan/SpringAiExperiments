package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SetRentOrBuyAndCityHandler implements ApiChatCallHandler {

    private final ConversationSession conversationSession;
    private final SetCityHandler setCityHandler;

    public SetRentOrBuyAndCityHandler(ConversationSession conversationSession, SetCityHandler setCityHandler) {
        this.conversationSession = conversationSession;
        this.setCityHandler = setCityHandler;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        String[] parts = serviceCall.message().split(",");

        conversationSession.setRentOrSale(parts[0]);
        var cityName = parts[1];
        return setCityHandler.handle(new ServiceCall(ApiCall.SET_RENT_OR_BUY_AND_CITY,cityName),session);
    }

    @Override
    public ApiCall getApiCall() {
        return ApiCall.SET_RENT_OR_BUY_AND_CITY;
    }
}
