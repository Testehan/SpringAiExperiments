package com.testehan.springai.immobiliare.events;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class ResponsePayload implements EventPayload{

    private String response;
    private String conversationId;

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("response", response);
        payload.put("conversationId", conversationId);
        return payload;
    }
}
