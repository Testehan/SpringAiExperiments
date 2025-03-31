package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.util.GoogleMapsUtil;
import org.springframework.stereotype.Service;

@Service
public class ListingAmenitiesService {

    private final GoogleMapsUtil googleMapsUtil;

    public ListingAmenitiesService(GoogleMapsUtil googleMapsUtil) {
        this.googleMapsUtil = googleMapsUtil;
    }

    public void getAmenitiesAndSetInApartment(Apartment apartment) {
        var nearbyAmenities = googleMapsUtil.getNearbyAmenities(apartment.getArea() + " " + apartment.getCity());
        apartment.setNearbyAmenities(nearbyAmenities);
    }

}
