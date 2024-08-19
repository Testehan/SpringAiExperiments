package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApartmentService {

    private final ApartmentsRepository apartmentsRepository;
    private final OpenAiService embedder;

    public ApartmentService(ApartmentsRepository apartmentsRepository, OpenAiService embedder) {
        this.apartmentsRepository = apartmentsRepository;
        this.embedder = embedder;
    }

    public List<Apartment> getApartmentsSemanticSearch(PropertyType propertyType, String city, Apartment apartment, String apartmentDescription) {
        var embedding = embedder.createEmbedding(apartmentDescription).block();
        return apartmentsRepository.findApartmentsByVector(propertyType, city, apartment, embedding);
    }

    public Apartment findApartmentById(String apartmentId) {
        return apartmentsRepository.findApartmentById(apartmentId);
    }

    // TODO this will have as parameter the user for which the apartments are retrieved
    public List<Apartment> getFavouriteApartments() {
        var apartment1 = apartmentsRepository.findApartmentById("66963adfe705bcd421d26b4d");
        var apartment2 = apartmentsRepository.findApartmentById("66963adfe705bcd421d26b4c");
        List<Apartment> apartments = new ArrayList<>();
        apartments.add(apartment1);
        apartments.add(apartment2);
        return apartments;
    }
}
