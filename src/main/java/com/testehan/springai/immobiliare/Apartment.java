package com.testehan.springai.immobiliare;

import java.util.List;

public record Apartment(
        String name,
        String shortDescription,
        Long price,
        Integer surface,
        Integer noOfRooms,
        String floor,
        List<String> tags)  {
}
