package com.testehan.springai.ollama.chpt05.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OllamaService {
    private final OllamaChatModel ollamaChatModel;

    public OllamaService(OllamaChatModel ollamaChatModel) {
        this.ollamaChatModel = ollamaChatModel;
    }

    public String getResponseFromLlm(String input) {
        log.info("Received message {}",input);
        var response = ollamaChatModel.call(input);

        log.info("Response from deepseek {}",response);
        return response;
    }
}
