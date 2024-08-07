package com.testehan.springai.immobiliare.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.List;

public record Apartment(
        @BsonProperty("_id")
        ObjectId _id,
        String name,
        String city,
        String area,
        String shortDescription,
        Long price,
        PropertyType propertyType,
        Integer surface,
        Integer noOfRooms,
        String floor,
        List<String> tags,
        List<String> images,

        List<Double> plot_embedding,
        Double score)  {

    @JsonIgnore
    public String getApartmentInfoToEmbedd(){
        return name + "\n" +
                "The apartment is located in the city " + city + ", area or neighbourhood " + area +
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
                "id='" + _id + '\'' +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", area='" + area + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", price=" + price +
                ", propertyType=" + propertyType +
                ", surface=" + surface +
                ", noOfRooms=" + noOfRooms +
                ", floor='" + floor + '\'' +
                ", tags=" + tags +
                ", images=" + images +
                ", score=" + score +

                '}';
    }
}
