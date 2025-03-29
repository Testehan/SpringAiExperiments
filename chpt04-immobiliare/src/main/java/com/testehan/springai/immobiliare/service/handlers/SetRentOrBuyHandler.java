package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import static com.testehan.springai.immobiliare.model.ApiCall.SET_RENT_OR_BUY;

@Component
public class SetRentOrBuyHandler implements ApiChatCallHandler {

    private final ConversationSession conversationSession;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public SetRentOrBuyHandler(ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }


    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        conversationSession.setRentOrSale(serviceCall.message());
        return new ResultsResponse(messageSource.getMessage("M02_CITY", null, localeUtils.getCurrentLocale()));
    }

    @Override
    public ApiCall getApiCall() {
        return SET_RENT_OR_BUY;
    }
}
