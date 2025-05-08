package com.testehan.springai.immobiliare.model.webhook;

import lombok.Data;

@Data
public class Contact {
    private Profile profile;
    private String wa_id;
}
