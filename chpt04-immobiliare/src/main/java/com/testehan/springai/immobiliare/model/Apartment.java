package com.testehan.springai.immobiliare.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;

import java.util.List;

public class Apartment {
//        @BsonProperty("_id")
        ObjectId id;
        String name;
        String city;
        String area;
        String shortDescription;
        Integer price;
        PropertyType propertyType;
        Integer surface;
        Integer noOfRooms;
        String floor;
        List<String> tags;
        List<String> images;

        String contact;

        List<Double> plot_embedding;
        Double score;

    public Apartment() {}

//    @BsonCreator
    public Apartment(//@BsonProperty("_id")
                     ObjectId _id, String name, String city, String area, String shortDescription, Integer price, PropertyType propertyType, Integer surface, Integer noOfRooms, String floor, List<String> tags, List<String> images, String contact, List<Double> plot_embedding, Double score) {
        this.id = _id;
        this.name = name;
        this.city = city;
        this.area = area;
        this.shortDescription = shortDescription;
        this.price = price;
        this.propertyType = propertyType;
        this.surface = surface;
        this.noOfRooms = noOfRooms;
        this.floor = floor;
        this.tags = tags;
        this.images = images;
        this.contact = contact;
        this.plot_embedding = plot_embedding;
        this.score = score;
    }

    public Apartment(//@BsonProperty("_id")
                     ObjectId _id, String name, String city, String area, String shortDescription, Integer price, String propertyType, Integer surface, Integer noOfRooms, String floor, List<String> tags, List<String> images, String contact, List<Double> plot_embedding, Double score) {


        this(_id, name, city, area, shortDescription , price, PropertyType.fromString(propertyType), surface, noOfRooms, floor, tags , images, contact, plot_embedding, score);
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public Integer getSurface() {
        return surface;
    }

    public void setSurface(Integer surface) {
        this.surface = surface;
    }

    public Integer getNoOfRooms() {
        return noOfRooms;
    }

    public void setNoOfRooms(Integer noOfRooms) {
        this.noOfRooms = noOfRooms;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public List<Double> getPlot_embedding() {
        return plot_embedding;
    }

    public void setPlot_embedding(List<Double> plot_embedding) {
        this.plot_embedding = plot_embedding;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

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
}
