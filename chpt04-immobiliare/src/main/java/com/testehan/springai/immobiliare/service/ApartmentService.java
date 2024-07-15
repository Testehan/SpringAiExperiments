package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApartmentService {

    private final ApartmentsRepository apartmentsRepository;
    private final OpenAiService embedder;

    public ApartmentService(ApartmentsRepository apartmentsRepository, OpenAiService embedder) {
        this.apartmentsRepository = apartmentsRepository;
        this.embedder = embedder;
    }

    public List<Apartment> getApartmentsSemanticSearch(PropertyType propertyType, String apartmentDescription) {
        var embedding = embedder.createEmbedding(apartmentDescription).block();
        return apartmentsRepository.findApartmentsByVector(propertyType, embedding);
    }
}
