package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.controller.ApiController;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.RestCall;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.stream.Collectors;

@Service
public class ApiServiceImpl implements ApiService{

    @Autowired
    private ApiController apiController;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApartmentService apartmentService;

    @Override
    public String getChatResponse(HttpSession session, String message) {
        RestCall restCall = apiController.whichApiToCall(message);

        var url = "http://localhost:8080/api" + restCall.apiCall();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("message",  restCall.message());


        switch (restCall.apiCall()) {
            case "/getRentOrBuy" : { session.setAttribute("rentOrSale", restCall.message()); break;}
            case "/getCity" : {session.setAttribute("city", restCall.message()); break; }
            case "/apartments/getApartments" :{
                // TODO Yeah i know this is ugly...but i have to figure out a better way of keeping track of session data when
                // making another rest call, or some other approach, as i i make a rest call from the code, the session
                // in the endpoint will be different, and so the values set above will not be present
                var rentOrSale = (String) session.getAttribute("rentOrSale");
                var city = session.getAttribute("city");
                return apartmentService.getApartmentsSemanticSearch(PropertyType.valueOf(rentOrSale), message).stream().map(Object::toString).collect(Collectors.joining(", "));
            }
        }

        String response = restTemplate.getForObject(builder.toUriString(), String.class);
        return response;
    }
}
