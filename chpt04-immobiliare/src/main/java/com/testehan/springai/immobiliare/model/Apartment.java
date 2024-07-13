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
        List<String> tags,
        List<String> images,

        List<Double> plot_embedding)  {

    public String getApartmentInfoToEmbedd(){
        return name + "\n" +
                "The apartment is located in: " + location + ". " +
                shortDescription +
                "It has a surface of " + surface + " square meters." +
                "The price is " + price + " euro. " +
                "Number of rooms is " + noOfRooms + ". " +
                "Located at floor " + floor + ". " +
                "Additional keywords for this apartment are " + tags;
    }

    @Override
    public String toString() {
        return "Apartment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", price=" + price +
                ", surface=" + surface +
                ", noOfRooms=" + noOfRooms +
                ", floor='" + floor + '\'' +
                ", tags=" + tags +
                ", images=" + images +
                '}';
    }
}
