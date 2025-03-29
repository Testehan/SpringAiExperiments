package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import jakarta.servlet.http.HttpSession;

public interface ApiChatCallHandler {
    ResultsResponse handle(ServiceCall serviceCall, HttpSession session);
    ApiCall getApiCall();
}
