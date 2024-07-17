package com.testehan.springai.immobiliare.model;

import java.util.List;

public record ResultsResponse (String message, List<Apartment> apartments){

    public boolean containsApartments(){
        return apartments.size()>0;
    }
}
