package com.testehan.springai.immobiliare.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.testehan.springai.immobiliare.constants.PromptConstants.*;

@RestController
public class BasicInformationController {

    @GetMapping("/api/getNotRelevantPrompt")
    public String notRelevantPrompt(@RequestParam(value = "message") String message) {

       return M00_IRRELEVANT_PROMPT;
    }

    @GetMapping("/api/restart")
    public String restart(@RequestParam(value = "message") String message) {

        return M01_INITIAL_MESSAGE;
    }

    @GetMapping("/api/getRentOrBuy")
    public String getRentOrBuy(@RequestParam(value = "message") String message) {
        return M02_CITY;
    }

    @GetMapping("/api/getCity")
    public String getCity(@RequestParam(value = "message") String message) {

        return M03_DETAILS;
    }

}
