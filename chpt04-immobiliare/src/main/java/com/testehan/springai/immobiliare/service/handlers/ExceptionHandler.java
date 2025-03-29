package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import static com.testehan.springai.immobiliare.model.ApiCall.EXCEPTION;

@Component
public class ExceptionHandler implements ApiChatCallHandler {

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public ExceptionHandler(MessageSource messageSource, LocaleUtils localeUtils) {
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        return new ResultsResponse(messageSource.getMessage("chat.exception", null, localeUtils.getCurrentLocale()));
    }

    @Override
    public ApiCall getApiCall() {
        return EXCEPTION;
    }
}
