package com.testehan.springai.immobiliare.model.maytapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ErrorMessage implements BaseMayTapiMessage {
    @JsonProperty("type")
    private String type; // "error"
    private String product_id;
    private Long phone_id;
    private String code;
    @JsonProperty("message")
    private String errorMessage;
    private ErrorData data;
    private Long phoneId;

    @Data
    public static class ErrorData {
        private String to_number;
        private String type;
        @JsonProperty("message")
        private String sentMessage;
        private String id;
    }
}
