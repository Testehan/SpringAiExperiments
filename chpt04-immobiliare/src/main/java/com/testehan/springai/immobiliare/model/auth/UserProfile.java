package com.testehan.springai.immobiliare.model.auth;

public record UserProfile(
        String email,
        String phoneNumber,
        String name,
        String city,
        String propertyType,
        String budget,
        String lastPropertyDescription,
        Integer searchesAvailable,
        String inviteUuid,
        Integer maxNumberOfListedProperties) {
}
