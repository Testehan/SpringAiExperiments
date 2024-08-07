package com.testehan.springai.immobiliare.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

// TODO When/if houses will be added to the app, we should change the values to apartmentsale / apartmenrrent and
//  housesale / houserent
public enum PropertyType {

//    @JsonEnumDefaultValue
    sale,
    rent;

    @JsonCreator
    public static PropertyType getByValue(String t) {
        return Arrays.stream(PropertyType.values())
                .filter(a -> a.name().equals(t)).findFirst().orElse(PropertyType.sale);
    }


    public static PropertyType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return sale;
        }
        try {
            return PropertyType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return sale;
        }
    }
}
