package com.testehan.springai.immobiliare.events;

import com.testehan.springai.immobiliare.model.Apartment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class ApartmentPayload implements EventPayload
{
    private Apartment apartment;
    private Boolean isFavourite;


    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("apartment", apartment);
        payload.put("isFavourite", isFavourite);
        return payload;
    }
}
