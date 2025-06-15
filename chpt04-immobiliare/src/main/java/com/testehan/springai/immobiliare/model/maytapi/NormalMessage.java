package com.testehan.springai.immobiliare.model.maytapi;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NormalMessage implements BaseMayTapiMessage {
    @JsonProperty("type")
    private String type; // "message"
    private String product_id;
    private Long phone_id;
    private InnerMessage message;
    private User user;
    private String conversation;
    private String conversation_name;
    private String receiver;
    private long timestamp;
    private String reply;
    private String productId;
    private Long phoneId;

    @Data
    public static class InnerMessage {
        private String type;
        private String text;
        private String id;
        private String _serialized;
        private boolean fromMe;
    }

    @Data
    public static class User {
        private String id;
        private String name;
        private String phone;
        private String image;
    }
}
