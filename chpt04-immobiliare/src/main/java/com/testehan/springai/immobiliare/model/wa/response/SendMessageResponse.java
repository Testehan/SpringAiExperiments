package com.testehan.springai.immobiliare.model.wa.response;

import lombok.Data;

import java.util.List;

@Data
public class SendMessageResponse {
    private String messaging_product;
    private List<Contact> contacts;
    private List<SentMessage> messages;
}
