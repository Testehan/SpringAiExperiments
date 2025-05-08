package com.testehan.springai.immobiliare.model.webhook;

import lombok.Data;

@Data
public class Message {
    public static final String MESSAGE_TYPE_TEXT="text";

    private String from;
    private String id;
    private String timestamp;
    private String type;
    private Text text;
}
