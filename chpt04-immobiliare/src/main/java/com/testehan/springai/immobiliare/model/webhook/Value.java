package com.testehan.springai.immobiliare.model.webhook;

import lombok.Data;

import java.util.List;

@Data
public class Value {
    private String messaging_product;
    private Metadata metadata;
    private List<Contact> contacts;
    private List<Message> messages;
}
