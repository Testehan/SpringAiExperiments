package com.testehan.springai.immobiliare.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.ArrayList;
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
    private List<String> images = new ArrayList<>();
    private String imagesGeneratedDescription;

    private String contact;
    private String creationDateTime;
    private String lastUpdateDateTime;

    private List<Double> plot_embedding;
    private Double score;

    private Integer noOfFavourite;
    private transient boolean isMostFavourite;
    private Integer noOfContact;
    private transient boolean isMostContacted;

    private String activationToken;
    private boolean active = true;  // i want properties to be visible by default

    @JsonIgnore
    public String getApartmentInfoToEmbedd(){
        return name + "\n" +
                "The apartment is located in the city " + city + ", area or neighbourhood " + area + ". " +
                shortDescription +
                "It has a surface of " + surface + " square meters." +
                "The price is " + price + " euro. " +
                "Number of rooms is " + noOfRooms + ". " +
                "Located at floor " + floor + ". " +
                "Additional keywords for this apartment are " + tags + ". " +
                "Description of provided images: " + imagesGeneratedDescription;
    }

    @JsonIgnore
    public String getApartmentInfo(){
        return "The apartment called \"" + name + "\" is located in the city " + city + ", area or neighbourhood " + area + ". " +
                shortDescription +
                "It has a surface of " + surface + " square meters." +
                "The price is " + price + " euro. " +
                "Number of rooms is " + noOfRooms + ". " +
                "Located at floor " + floor + ". " +
                "Additional keywords for this apartment are " + tags + ". " +
                "Description of provided images: " + imagesGeneratedDescription;
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

    public void setTagsWithSplit(String tags){
        this.tags = List.of(tags.split(" "));
    }

    public String getTagsWithSplit(){
        StringBuilder result = new StringBuilder();
        if (Objects.nonNull(tags)) {
            for (String element : this.tags) {
                result.append(element).append(", ");
            }
            return result.toString().substring(0, result.length() - 2);
        } else {
            return "";
        }
    }

    public Integer getNoOfFavourite() {
        return Objects.nonNull(noOfFavourite) ? noOfFavourite : 0;
    }

    public Integer getNoOfContact() {
        return Objects.nonNull(noOfContact) ? noOfContact : 0;
    }
}
