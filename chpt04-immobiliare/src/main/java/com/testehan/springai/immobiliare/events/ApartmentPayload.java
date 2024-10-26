package com.testehan.springai.immobiliare.events;

import com.testehan.springai.immobiliare.model.Apartment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ApartmentPayload implements EventPayload
{
    private Apartment apartment;


    @Override
    public Object getPayload() {
        return apartment;
    }
}
