package com.testehan.springai.immobiliare.service;


import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ImmobiliareApiService {

//    @Value("classpath:/prompts/ApiDescription.txt")
//    private Resource apiDescriptionFile;
//
//    @Value("classpath:/prompts/ApartmentDescription.txt")
//    private Resource apartmentDescriptionFile;

    private final ChatClient chatClient;
    private final LocaleUtils localeUtils;

    public ImmobiliareApiService(ChatClient chatClient, LocaleUtils localeUtils) {
        this.chatClient = chatClient;
        this.localeUtils = localeUtils;
    }

    public ServiceCall whichApiToCall(String message) {

        ChatResponse assistantResponse;

        var outputParser = new BeanOutputConverter<>(ServiceCall.class);
        String format = outputParser.getFormat();

        var apiDescriptionPrompt = localeUtils.getLocalizedPrompt("ApiDescription");
        PromptTemplate promptTemplate = new PromptTemplate(apiDescriptionPrompt);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input_here", message);
        promptParameters.put("format", format);
        Prompt prompt = promptTemplate.create(promptParameters);

        assistantResponse = chatClient.prompt()
                .user(prompt.getContents())
                .call().chatResponse();

        ServiceCall serviceCall = outputParser.convert(assistantResponse.getResult().getOutput().getContent());
        return serviceCall;
    }

    public ApartmentDescription extractApartmentInformationFromProvidedDescription(String apartmentDescription) {

        ChatResponse assistantResponse;

        var outputParser = new BeanOutputConverter<>(ApartmentDescription.class);
        String format = outputParser.getFormat();

        var apartmentDescriptionPrompt = localeUtils.getLocalizedPrompt("ApartmentDescription");
        PromptTemplate promptTemplate = new PromptTemplate(apartmentDescriptionPrompt);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("property_description", apartmentDescription);
        promptParameters.put("format", format);
        Prompt prompt = promptTemplate.create(promptParameters);

        assistantResponse = chatClient.prompt()
                .user(prompt.getContents())
                .call().chatResponse();

        ApartmentDescription apartment = outputParser.convert(assistantResponse.getResult().getOutput().getContent());
        return apartment;
    }

}
