package com.testehan.springai.immobiliare.service;


import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import jakarta.servlet.http.HttpSession;
import reactor.core.publisher.Flux;

public interface ApiService {

    ResultsResponse getChatResponse(String message, HttpSession session);
    Flux<Event> getServerSideEventsFlux(HttpSession session);


}
