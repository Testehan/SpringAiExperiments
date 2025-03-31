package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.util.ListingUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListingEmbeddingService {
    private final OpenAiService openAiService;
    private final ListingUtil listingUtil;


    public ListingEmbeddingService(OpenAiService openAiService, ListingUtil listingUtil) {
        this.openAiService = openAiService;
        this.listingUtil = listingUtil;
    }

    public List<Double> createEmbedding(Apartment apartment){
        var apartmentInfoToEmbed = listingUtil.getApartmentInfoToEmbedd(apartment);
        var mono = openAiService.createEmbedding(apartmentInfoToEmbed);
        return mono.block();
    }
}
