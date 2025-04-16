package com.testehan.springai.immobiliare.security;

import java.util.List;

public class InsecurePaths {
    public static final List<String> INSECURED_URLS = List.of(
            "/help","/blog","/contact", "/error", "/error-login", "/login-modal",
            "/reactivate","/confirmation","/accept-gdpr","/privacy-policy","/terms", "/s/",
            "/css/","/js/","/webjars/","/images/"


    );
}