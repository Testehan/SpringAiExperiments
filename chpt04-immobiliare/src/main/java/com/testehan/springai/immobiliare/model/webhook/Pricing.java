package com.testehan.springai.immobiliare.model.webhook;

import lombok.Data;

@Data
public class Pricing {
    private boolean billable;
    private String pricing_model;
    private String category;
}
