package com.testehan.springai.immobiliare.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

public class LoggingAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAuthorizationRequestResolver.class);

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public LoggingAuthorizationRequestResolver(OAuth2AuthorizationRequestResolver defaultResolver) {
        this.defaultResolver = defaultResolver;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        var customizedRequest = customizeRequest(authorizationRequest);
        return customizedRequest;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        var customizedRequest = customizeRequest(authorizationRequest);
        return customizedRequest;
    }

    // this method only works and is needed for Google auth. Facebook uses another mechanism
    private OAuth2AuthorizationRequest customizeRequest(OAuth2AuthorizationRequest request) {
        if (request != null) {
            logger.debug("Original Authorization Request URI: {}", request.getAuthorizationUri());
            Map<String, Object> additionalParameters = new HashMap<>(request.getAdditionalParameters());
            additionalParameters.put("access_type", "offline");
            additionalParameters.put("prompt", "consent");
            OAuth2AuthorizationRequest customizedRequest = OAuth2AuthorizationRequest.from(request)
                    .additionalParameters(additionalParameters)
                    .build();
            logger.debug("Customized Authorization Request URI: {}", customizedRequest.getAuthorizationUri());
            return customizedRequest;
        }
        return request;
    }
}
