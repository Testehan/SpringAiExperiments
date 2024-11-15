package com.testehan.springai.immobiliare.model.auth;

public record UserProfile(
        String email,
        String name,
        String city,
        String propertyType,
        String lastPropertyDescription,
        Integer searchesAvailable,
        Integer maxNumberOfListedProperties) {
}
