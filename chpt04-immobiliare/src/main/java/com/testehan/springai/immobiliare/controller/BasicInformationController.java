package com.testehan.springai.immobiliare.controller;

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
    public String getRentOrBuy(@RequestParam(value = "message") String message) {
        return "Which city are you interested in ?";
    }

    @GetMapping("/api/getCity")
    public String getCity(@RequestParam(value = "message") String message) {

        return "I understand. Give me more details about the location you are searching for";
    }

}
