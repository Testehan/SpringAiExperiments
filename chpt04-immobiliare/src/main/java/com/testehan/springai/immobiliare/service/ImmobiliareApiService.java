package com.testehan.springai.immobiliare.service;


import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ImmobiliareApiService.class);

    private final ChatClient chatClient;
    private final LocaleUtils localeUtils;

    public ImmobiliareApiService(ChatClient chatClient, LocaleUtils localeUtils) {
        this.chatClient = chatClient;
        this.localeUtils = localeUtils;
    }

    public ServiceCall whichApiToCall(String message) {
        try {
            ChatResponse assistantResponse;

            var outputParser = new BeanOutputConverter<>(ServiceCall.class);
            String format = outputParser.getFormat();

            var apiDescriptionPrompt = localeUtils.getLocalizedPrompt("ApiDescription");
            PromptTemplate promptTemplate = new PromptTemplate(apiDescriptionPrompt);
            Map<String, Object> promptParameters = new HashMap<>();
//        promptParameters.put("input_here", message);
            promptParameters.put("format", format);
            Prompt prompt = promptTemplate.create(promptParameters);

            assistantResponse = chatClient.prompt()
                    .system(prompt.getContents())   //Move large static content to the system message field
                    .user(message)  // Keep dynamic elements in user messages, as system messages don't require repeating.
                    .call().chatResponse();

            var response = assistantResponse.getResult().getOutput().getContent();
            LOGGER.info(response);
            ServiceCall serviceCall = outputParser.convert(response);
            return serviceCall;
        } catch (Exception e) {
            // Catch-all for unexpected errors
            LOGGER.error("Unexpected error while calling LLM: {}", e.getMessage(), e);
            return new ServiceCall(ApiCall.EXCEPTION, "");
        }
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
