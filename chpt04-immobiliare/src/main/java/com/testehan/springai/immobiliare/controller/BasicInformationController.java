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
    public String getRentOrBuy(HttpSession session,
                             @RequestParam(value = "message") String message) {

        // TODO Dan: the next attribute is never really used ..maybe it can be removed in the future
        session.setAttribute("rentOrBuy",message);
        return "Which city are you interested in ?";
    }

    @GetMapping("/api/getCity")
    public String getCity(HttpSession session,
                        @RequestParam(value = "message") String message) {

        // TODO Dan: the next attribute is never really used ..maybe it can be removed in the future
        session.setAttribute("city",message);
        return "I understand. Give me more details about the location you are searching for";
    }

}
