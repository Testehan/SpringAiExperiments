package com.testehan.springai.immobiliare.service;


import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import reactor.core.publisher.Flux;

public interface ApiService {

    ResultsResponse getChatResponse(String message);
    Flux<Event> getApartmentsFlux();


}
