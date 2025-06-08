package com.testehan.springai.immobiliare.model.maytapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AcknowledgementMessage implements BaseMayTapiMessage {
    @JsonProperty("type")
    private String type;
    private String product_id;
    private Long phone_id;
    private List<AckData> data;
    private Long phoneId;

    @Data
    public static class AckData {
        private String ackType;
        private int ackCode;
        private String chatId;
        private String msgId;
        private long time;
        private String rxid;
    }
}
