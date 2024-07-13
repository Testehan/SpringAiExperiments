package com.testehan.springai.immobiliare.controller;


import com.testehan.springai.immobiliare.model.RestCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ApiController {

    @Value("classpath:/prompts/ApiDescription.txt")
    private Resource ragPromptTemplate;

    private final ChatClient chatClient;

    public ApiController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/api/whichApiToCall")
    public RestCall whichApiToCall(@RequestParam(value = "message", defaultValue = "What are some apartments for sale in Marasti?") String message) {

        ChatResponse assistantResponse;

        var outputParser = new BeanOutputConverter<>(RestCall.class);
        String format = outputParser.getFormat();
        System.out.println("format = " + format);

        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input_here", message);
        promptParameters.put("format", format);
        Prompt prompt = promptTemplate.create(promptParameters);

        assistantResponse = chatClient.prompt()
                .user(prompt.getContents())
                .call().chatResponse();

        RestCall restCall = outputParser.parse(assistantResponse.getResult().getOutput().getContent());
        return restCall;
    }

    // what i am trying to do with this method is to have the possibility that from one REST endpoint
    // to let the LLM decide which function to call based obviously on the user input
    // maybe this is a better approach than to use the whichApiToCall from above approach ?
    @GetMapping("/decide")
    public String decide(@RequestParam(value = "message") String message) {
        Prompt prompt = new Prompt(message);

        ChatResponse response = chatClient.prompt()
                .user(prompt.getContents())
                .functions("apartmentsFunction","apartmentsSaleFunction","emailApartmentsFunction")
                .call().chatResponse();

        return response.getResult().getOutput().getContent();
    }

}