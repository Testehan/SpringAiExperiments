package com.testehan.springai.immobiliare.model.webhook;

import lombok.Data;

@Data
public class Change {
    private String field;
    private Value value;
}
