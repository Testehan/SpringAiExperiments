package com.testehan.springai.immobiliare.service;


import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ServiceCall;
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

    public ServiceCall whichApiToCall(String message) {

        ChatResponse assistantResponse;

        var outputParser = new BeanOutputConverter<>(ServiceCall.class);
        String format = outputParser.getFormat();

        PromptTemplate promptTemplate = new PromptTemplate(apiDescriptionFile);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input_here", message);
        promptParameters.put("format", format);
        Prompt prompt = promptTemplate.create(promptParameters);

        assistantResponse = chatClient.prompt()
                .user(prompt.getContents())
                .call().chatResponse();

        ServiceCall serviceCall = outputParser.parse(assistantResponse.getResult().getOutput().getContent());
        return serviceCall;
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

}
