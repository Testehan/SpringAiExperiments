package com.testehan.springai.immobiliare.model;

import java.util.List;

public record Apartment(
        String id,
        String name,
        String location,
        String shortDescription,
        Long price,
        Integer surface,
        Integer noOfRooms,
        String floor,
        List<String> tags)  {
}
