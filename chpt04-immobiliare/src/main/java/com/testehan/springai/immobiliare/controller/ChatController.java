package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.service.ApiService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@CrossOrigin
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ApiService apiService;

    public ChatController(ApiService apiService) {
        this.apiService = apiService;
    }

    @HxRequest
    @PostMapping("/chat")
    public HtmxResponse generate(@RequestParam String message, HttpSession session, Model model) {

        ResultsResponse response = apiService.getChatResponse(message);

        if (response.containsApartments()){
            model.addAttribute("message",message);
            model.addAttribute("response",response.message());
            model.addAttribute("apartments",response.apartments());

            return HtmxResponse.builder()
                    .view("response :: responseFragmentWithApartments")
                    .build();
        } else {

            model.addAttribute("message",message);
            model.addAttribute("response",response.message());

            return HtmxResponse.builder()
                    .view("response :: responseFragment")
                    .build();
        }
    }

}
