package com.testehan.springai.immobiliare.model.webhook;

import lombok.Data;

@Data
public class Status {
    private String id;
    private String status;
    private String timestamp;
    private String recipient_id;
    private Conversation conversation;
    private Pricing pricing;
}
