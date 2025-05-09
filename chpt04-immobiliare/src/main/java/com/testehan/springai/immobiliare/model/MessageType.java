package com.testehan.springai.immobiliare.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum MessageType {

    SENT,
    RECEIVED,
    UNKNOWN; // <- Fallback for invalid values

    @JsonCreator
    public static MessageType getByValue(String t) {
        return Arrays.stream(MessageType.values())
                .filter(a -> a.name().equals(t)).findFirst().orElse(MessageType.UNKNOWN);
    }


    public static MessageType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }
        try {
            return MessageType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
