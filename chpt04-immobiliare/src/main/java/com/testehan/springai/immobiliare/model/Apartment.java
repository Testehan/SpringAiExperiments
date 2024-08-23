package com.testehan.springai.immobiliare.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Apartment {

    private ObjectId id;
    private String name;
    private String city;
    private String area;
    private String shortDescription;
    private Integer price;
    private PropertyType propertyType;
    private Integer surface;
    private Integer noOfRooms;
    private String floor;
    private List<String> tags;
    private List<String> images;

    private String contact;
    private LocalDateTime creationDateTime;
    private LocalDateTime lastUpdateDateTime;

    private List<Double> plot_embedding;
    private Double score;

    @JsonIgnore
    public String getApartmentInfoToEmbedd(){
        return name + "\n" +
                "The apartment is located in the city " + city + ", area or neighbourhood " + area + ". " +
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
                ", contact=" + contact +
                ", score=" + score +

                '}';
    }

    public String getCreationDateTimeFormatted(){
        if (Objects.nonNull(creationDateTime)) {
            DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateCustom = creationDateTime.format(customFormatter);
            return formattedDateCustom;
        } else {
            return "";
        }
    }

    public String getLastUpdateDateTimeFormatted(){
        if (Objects.nonNull(lastUpdateDateTime)) {
            DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateCustom = lastUpdateDateTime.format(customFormatter);
            return formattedDateCustom;
        } else {
            return "";
        }
    }
}
