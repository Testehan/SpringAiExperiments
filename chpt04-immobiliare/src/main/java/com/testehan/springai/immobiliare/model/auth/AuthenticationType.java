package com.testehan.springai.immobiliare.model.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.testehan.springai.immobiliare.model.PropertyType;

import java.util.Arrays;

public enum AuthenticationType {
    DATABASE, GOOGLE, FACEBOOK;

    @JsonCreator
    public static PropertyType getByValue(String t) {
        return Arrays.stream(PropertyType.values())
                .filter(a -> a.name().equals(t)).findFirst().orElse(PropertyType.sale);
    }


    public static AuthenticationType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return DATABASE;
        }
        try {
            return AuthenticationType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return DATABASE;
        }
    }
}
