package com.testehan.springai.immobiliare.model.maytapi;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AcknowledgementMessage.class, name = "ack"),
        @JsonSubTypes.Type(value = ErrorMessage.class, name = "error"),
        @JsonSubTypes.Type(value = NormalMessage.class, name = "message")
})
public interface BaseMayTapiMessage {
    String getType();
}
