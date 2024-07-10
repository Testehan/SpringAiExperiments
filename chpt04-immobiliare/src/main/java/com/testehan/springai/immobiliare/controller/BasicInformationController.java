package com.testehan.springai.immobiliare.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BasicInformationController {

    @GetMapping("/api/getNotRelevantPrompt")
    public String notRelevantPrompt(@RequestParam(value = "message") String message) {

       return String.format("The prompt provided is not relevant for this application. Try and provide something related to real estate");
    }

    @GetMapping("/api/getRentOrBuy")
    public void getRentOrBuy(HttpSession session,
                             @RequestParam(value = "message") String message) {

        session.setAttribute("rentOrBuy",message);
    }

    @GetMapping("/api/getCity")
    public void getCity(HttpSession session,
                        @RequestParam(value = "message") String message) {

        session.setAttribute("city",message);
    }

}
