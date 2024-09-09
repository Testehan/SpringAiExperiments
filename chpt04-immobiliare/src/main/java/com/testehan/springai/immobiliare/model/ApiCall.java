package com.testehan.springai.immobiliare.model;

public enum ApiCall {

    SET_RENT_OR_BUY("/setRentOrBuy"),
    SET_CITY("/setCity"),
    GET_APARTMENTS("/getApartments"),
    RESTART_CONVERSATION("/restart"),
    DEFAULT("/default");

    private final String path;

    ApiCall(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
