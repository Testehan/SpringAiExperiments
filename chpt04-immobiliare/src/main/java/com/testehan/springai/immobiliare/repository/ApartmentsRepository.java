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
    Optional<Apartment> findApartmentByContact(String phoneNumber);
    Optional<String> findApartmentIdBySocialId(String socialId);

    boolean isPhoneValid(String phoneNumber);

    List<Apartment> findAll();

    List<Apartment> findByLastUpdateDateTimeBefore(LocalDateTime date);

    Apartment saveApartment(Apartment apartment);

    List<String> deactivateApartments(LocalDateTime date);

    void deleteApartmentsByIds(List<String> apartmentIds);

}
