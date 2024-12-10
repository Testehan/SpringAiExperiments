package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.PropertyType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApartmentsRepository {
    List<Apartment> findApartmentsByVector(PropertyType propertyType, String city, ApartmentDescription apartment, List<Double> embedding);

    Optional<Apartment> findApartmentById(String apartmentId);

    List<Apartment> findAll();

    List<Apartment> findByLastUpdateDateTimeBefore(LocalDateTime date);

    void saveApartment(Apartment apartment);

    void deactivateApartments(LocalDateTime date);

    void deleteApartmentsByIds(List<String> apartmentIds);
}
