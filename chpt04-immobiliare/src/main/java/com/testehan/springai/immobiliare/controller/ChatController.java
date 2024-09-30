package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.service.ApiService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@CrossOrigin
public class ChatController {

    private final ApiService apiService;

    public ChatController(ApiService apiService) {
        this.apiService = apiService;
    }

    @HxRequest
    @PostMapping("/respond")
    public HtmxResponse respond(@RequestParam String message, Model model) {

        ResultsResponse response = apiService.getChatResponse(message);

        if (response.containsApartments()){
            model.addAttribute("response",response.message());
            model.addAttribute("apartments",response.apartments());

            return HtmxResponse.builder()
                    .view("response :: responseFragmentWithApartments")
                    .build();
        } else {
            model.addAttribute("response",response.message());

            return HtmxResponse.builder()
                    .view("response :: responseFragment")
                    .build();
        }
    }

    // Add a user message to model but don't ask to respond to it. This enables us to update the UI quickly.
    @HxRequest
    @PostMapping("/message")
    public HtmxResponse addUserMessage(@RequestParam(required = false) String message,
                                       @RequestParam(required = false) MultipartFile audioFile,
                                       Model model) {
        if (StringUtils.isEmpty(message)){
            // TODO means that we need to process the audioFile to extract de text
            message = "Processing of the audio file needs to be performed to extract text";
        }
        model.addAttribute("message",message);
        return HtmxResponse.builder()
                .view("response :: responseFragment")
                .build();
    }

}
