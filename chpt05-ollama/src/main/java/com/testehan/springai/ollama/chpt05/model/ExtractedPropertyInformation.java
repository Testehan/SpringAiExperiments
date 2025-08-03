package com.testehan.springai.ollama.chpt05.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExtractedPropertyInformation(

        @JsonProperty("name") String name,
        @JsonProperty("city") String city,
        @JsonProperty("area") String area,
        @JsonProperty("shortDescription") String shortDescription,
        @JsonProperty("price") Integer price,
        @JsonProperty("surface") Integer surface,
        @JsonProperty("noOfRooms") Integer noOfRooms,
        @JsonProperty("floor") String floor,
        @JsonProperty("ownerName") String ownerName,
        @JsonProperty("imageUrls") List<String> imageUrls

) {}