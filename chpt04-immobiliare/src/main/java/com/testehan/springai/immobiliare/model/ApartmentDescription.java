package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ApartmentDescription {

    private String city;
    private String area;
    private String shortDescription;
    private Integer minimumPrice;
    private Integer maximumPrice;
    private Integer minimumSurface;
    private Integer maximumSurface;
    private Integer minimumNumberOfRooms;
    private Integer maximumNumberOfRooms;
    private String floor;       // TODO think about this if you want it to be a string or a number so that you can easily compare..

}
