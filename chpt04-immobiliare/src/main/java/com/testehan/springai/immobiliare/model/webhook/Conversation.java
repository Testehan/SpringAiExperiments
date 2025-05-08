package com.testehan.springai.immobiliare.model.webhook;

import lombok.Data;

@Data
public class Conversation {
    private String id;
    private Origin origin;
    private String expiration_timestamp;
}
