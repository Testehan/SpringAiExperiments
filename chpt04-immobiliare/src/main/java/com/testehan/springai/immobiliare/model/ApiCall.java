package com.testehan.springai.immobiliare.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum ApiCall {

    SET_RENT_OR_BUY("/setRentOrBuy"),
    SET_CITY("/setCity"),
    SET_BUDGET("/setBudget"),
    SET_RENT_OR_BUY_AND_CITY("/setRentOrBuyAndCity"),
    SET_RENT_OR_BUY_AND_CITY_AND_DESCRIPTION("/setRentOrBuyAndCityAndDescription"),
    GET_APARTMENTS("/getApartments"),
    GET_DETAILS("/details"),
    RESTART_CONVERSATION("/restart"),
    DEFAULT("/default"),
    NOT_SUPPORTED("/notSupported"),

    EXCEPTION("/exception");

    private final String path;

    ApiCall(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @JsonCreator
    public static ApiCall getByValue(String t) {
        return Arrays.stream(ApiCall.values())
                .filter(a -> a.name().equals(t)).findFirst().orElse(ApiCall.NOT_SUPPORTED);
    }
}
