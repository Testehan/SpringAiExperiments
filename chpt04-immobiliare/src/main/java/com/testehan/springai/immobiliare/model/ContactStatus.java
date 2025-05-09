package com.testehan.springai.immobiliare.model;

public enum ContactStatus {
    NOT_CONTACTED,
    NO_WHATSAPP,
    CONTACTED,
    DECLINED,
    ALREADY_RENTED,
    ACCEPTED,
    DONE        // after being accepted, a lead moves to done when the listing was handled/published
}
