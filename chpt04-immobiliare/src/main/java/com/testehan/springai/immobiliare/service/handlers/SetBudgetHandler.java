package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import static com.testehan.springai.immobiliare.model.ApiCall.SET_BUDGET;

@Component
public class SetBudgetHandler implements ApiChatCallHandler {

    private final ConversationSession conversationSession;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public SetBudgetHandler(ConversationSession conversationSession, MessageSource messageSource, LocaleUtils localeUtils) {
        this.conversationSession = conversationSession;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        var user = conversationSession.getImmobiliareUser().get();
        var budget = serviceCall.message();
        conversationSession.setBudget(budget);
        var propertyType =  messageSource.getMessage(user.getPropertyType(), null, localeUtils.getCurrentLocale());
        return new ResultsResponse(
                messageSource.getMessage("M04_DETAILS",  new Object[]{propertyType, user.getCity(), budget}, localeUtils.getCurrentLocale()) +
                        messageSource.getMessage("M04_DETAILS_PART_2",  null, localeUtils.getCurrentLocale())
        );
    }

    @Override
    public ApiCall getApiCall() {
        return SET_BUDGET;
    }
}
