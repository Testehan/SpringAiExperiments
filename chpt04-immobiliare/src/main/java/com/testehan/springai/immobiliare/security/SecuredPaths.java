package com.testehan.springai.immobiliare.security;

import java.util.List;

public class SecuredPaths {
    public static final List<String> SECURED_URLS = List.of(
            "/a/**",
            "/chat",
            "/respond",
            "/message",
            "/favourites",
            "/add",
            "/edit/**",
            "/actuator/**",
            "/api/**",
            "/profile",
            "/view/**",
            "/invite/**"

    );
}
