package com.testehan.springai.immobiliare.model.webhook;

import lombok.Data;

import java.util.List;

@Data
public class WhatsAppWebhookPayload {
    private String object;
    private List<Entry> entry;
}
