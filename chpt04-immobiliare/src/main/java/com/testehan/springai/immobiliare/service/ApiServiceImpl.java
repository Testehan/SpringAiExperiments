package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.controller.ApiController;
import com.testehan.springai.immobiliare.model.RestCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ApiServiceImpl implements ApiService{

    @Autowired
    private ApiController apiController;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String getChatResponse(String message) {
        RestCall restCall = apiController.whichApiToCall(message);

        var url = "http://localhost:8080/api" + restCall.apiCall();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("message",  restCall.message());


        String response = restTemplate.getForObject(builder.toUriString(), String.class);

        return response;
    }
}
