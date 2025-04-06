package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document(collection = "apartments")
public class Apartment {

    private ObjectId id;
    private String socialId;
    private String name;
    private String city;
    private String area;
    private List<AmenityCategory> nearbyAmenities = new ArrayList<>();
    private String shortDescription;
    private Integer price;
    private String availableFrom;
    private PropertyType propertyType;
    private Integer surface;
    private Integer noOfRooms;
    private String floor;
    private List<String> tags;
    private List<String> images = new ArrayList<>();
    private String imagesGeneratedDescription;

    private String contact;
    private Boolean whatsapp;
    private String contactEmail;
    private String ownerName;

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

    @Override
    public String toString() {
        return "Apartment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", area='" + area + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", price=" + price +
                ", availableFrom=" + availableFrom +
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

        this.tags = List.of(tags.replace(",","").split(" "));
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

    public String getIdString(){
        if (id == null) {
            return null; // or return "" if you prefer
        }

        return id.toString();
    }

    public String getPropertyType() {
        return propertyType != null ? propertyType.name() : null;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = PropertyType.valueOf(propertyType);
    }
}
