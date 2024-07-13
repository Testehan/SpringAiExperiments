package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.service.ApiService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("")
    public String home(Model model) {
        model.addAttribute("initialMessage", "Hi..do you want to rent or buy an apartment?");
        return "index";
    }

    @HxRequest
    @PostMapping("/api/chat")
    public HtmxResponse generate(@RequestParam String message, Model model) {

        String response = apiService.getChatResponse(message);

        model.addAttribute("response",response);
        model.addAttribute("message",message);

        return HtmxResponse.builder()
                .view("response :: responseFragment")
                .build();
    }

}