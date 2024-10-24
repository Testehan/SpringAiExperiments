package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.PropertyType;

import java.util.List;

public interface ApartmentsRepository {
    List<Apartment> findApartmentsByVector(PropertyType propertyType, String city, Apartment apartment, List<Double> embedding);

    Apartment findApartmentById(String apartmentId);

    List<Apartment> findAll();

    void saveApartment(Apartment apartment);
}
