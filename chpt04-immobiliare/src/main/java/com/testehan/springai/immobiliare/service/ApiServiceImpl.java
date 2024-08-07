package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.RestCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;

import static com.testehan.springai.immobiliare.constants.PromptConstants.*;



@Service
public class ApiServiceImpl implements ApiService{

    @Autowired
    private ImmobiliareApiService immobiliareApiService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApartmentService apartmentService;

    @Autowired HttpSession session;

    @Override
    public ResultsResponse getChatResponse(String message) {
        RestCall restCall = immobiliareApiService.whichApiToCall(message);

        var url = "http://localhost:8080/api" + restCall.apiCall();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("message",  restCall.message());


        // TODO Yeah i know this is ugly...but i have to figure out a better way of keeping track of session data when
        // making another rest call, or some other approach, as when i make a rest call from the code, the session
        // in the endpoint will be different, and so the values set above will not be present
        // Once security part is introduced in the app, this will be handled :
        // https://stackoverflow.com/questions/76590383/how-to-configure-resttemplate-to-use-browsers-session-for-api-call
        switch (restCall.apiCall()) {
            case "/getRentOrBuy" : { setRentOrBuy(restCall); break;}
            case "/getCity" : { setCity(restCall); break; }
            case "/restart" : { restartConversation(); break; }
            case "/apartments/getApartments" :{ return getApartments(message); }
        }

        String response = restTemplate.getForObject(builder.toUriString(), String.class);
        return new ResultsResponse(response, new ArrayList<>());
    }

    private ResultsResponse getApartments(String message) {
        var apartmentDescription = immobiliareApiService.extractApartmentInformationFromProvidedDescription(message);

        var rentOrSale = (String) session.getAttribute("rentOrSale");
        var city = (String) session.getAttribute("city");
        var apartments = apartmentService.getApartmentsSemanticSearch(PropertyType.valueOf(rentOrSale), city,apartmentDescription, message);

        ResultsResponse response;

        if (apartments.size() > 0) {
            response = new ResultsResponse(M04_APARTMENTS_FOUND, apartments);
        } else {
            response = new ResultsResponse(M04_NO_APARTMENTS_FOUND, new ArrayList<>());
        }

        return response;
    }

    private void restartConversation() {
        session.setAttribute("rentOrSale", "");
        session.setAttribute("city", "");
    }

    private void setCity(RestCall restCall) {
        session.setAttribute("city", restCall.message());
    }

    private void setRentOrBuy(RestCall restCall) {
        session.setAttribute("rentOrSale", restCall.message());
    }
}
