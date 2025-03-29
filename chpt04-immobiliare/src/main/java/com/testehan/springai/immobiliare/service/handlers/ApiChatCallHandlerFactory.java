package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.model.ApiCall;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApiChatCallHandlerFactory {
    private final Map<ApiCall, ApiChatCallHandler> handlers = new HashMap<>();

    private final ExceptionHandler exceptionHandler;

    // Spring automatically provides this handlerList because all the classes implementing ApiCallHandler are @Components.
    public ApiChatCallHandlerFactory(List<ApiChatCallHandler> handlerList, ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        handlerList.forEach(handler -> handlers.put(handler.getApiCall(), handler));
    }

    public ApiChatCallHandler getHandler(ApiCall apiCall) {
        ApiChatCallHandler handler = handlers.get(apiCall);
        if (handler == null) {
            handler = exceptionHandler; // something is wrong..
        }
        return handler;
    }
}
