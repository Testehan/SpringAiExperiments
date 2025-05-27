package com.testehan.springai.immobiliare.model.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pricing {
    private boolean billable;
    private String pricing_model;
    private String category;
}
