package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.Apartment;

import java.util.List;

public interface ApartmentsRepository {
    List<Apartment> findApartmentsByVector(List<Double> embedding);
}
