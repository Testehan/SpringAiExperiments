package com.testehan.springai.immobiliare.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Event {

    private String eventType;
    private EventPayload payload;
}
