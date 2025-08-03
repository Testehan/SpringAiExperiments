package com.testehan.springai.ollama.chpt05.controller;

import com.testehan.springai.ollama.chpt05.service.OllamaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ollama")
@Slf4j
public class OllamaController {

    private final OllamaService ollamaService;

    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping(consumes = "text/plain", produces = "application/json", path = "/prompt")
    public String getResponseFromLlm(@RequestBody String input){
        return ollamaService.getResponseFromLlm(input);
    }

    @PostMapping(consumes = "text/plain", produces = "application/json", path = "/format")
    public String getFormatListing(@RequestBody String input){
        return ollamaService.getFormatListing(input);
    }
}
