package com.testehan.springai.immobiliare.service;


import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.RestCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ImmobiliareApiService {

    @Value("classpath:/prompts/ApiDescription.txt")
    private Resource apiDescriptionFile;

    @Value("classpath:/prompts/ApartmentDescription.txt")
    private Resource apartmentDescriptionFile;

    private final ChatClient chatClient;

    public ImmobiliareApiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public RestCall whichApiToCall(String message) {

        ChatResponse assistantResponse;

        var outputParser = new BeanOutputConverter<>(RestCall.class);
        String format = outputParser.getFormat();

        PromptTemplate promptTemplate = new PromptTemplate(apiDescriptionFile);
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

    public Apartment extractApartmentInformationFromProvidedDescription(String apartmentDescription) {

        ChatResponse assistantResponse;

        var outputParser = new BeanOutputConverter<>(Apartment.class);
        String format = outputParser.getFormat();

        PromptTemplate promptTemplate = new PromptTemplate(apartmentDescriptionFile);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("property_description", apartmentDescription);
        promptParameters.put("format", format);
        Prompt prompt = promptTemplate.create(promptParameters);

        assistantResponse = chatClient.prompt()
                .user(prompt.getContents())
                .call().chatResponse();

        Apartment apartment = outputParser.parse(assistantResponse.getResult().getOutput().getContent());
        return apartment;
    }

    // what i am trying to do with this method is to have the possibility that from one REST endpoint
    // to let the LLM decide which function to call based obviously on the user input
    // maybe this is a better approach than to use the whichApiToCall from above approach ?
//    @GetMapping("/decide")
//    public String decide(@RequestParam(value = "message") String message) {
//        Prompt prompt = new Prompt(message);
//
//        ChatResponse response = chatClient.prompt()
//                .user(prompt.getContents())
//                .functions("apartmentsFunction","apartmentsSaleFunction","emailApartmentsFunction")
//                .call().chatResponse();
//
//        return response.getResult().getOutput().getContent();
//    }

}
