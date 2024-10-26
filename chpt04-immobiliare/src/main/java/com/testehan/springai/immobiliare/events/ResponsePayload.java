package com.testehan.springai.immobiliare.events;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ResponsePayload implements EventPayload{

    private String response;

    @Override
    public Object getPayload() {
        return response;
    }
}
