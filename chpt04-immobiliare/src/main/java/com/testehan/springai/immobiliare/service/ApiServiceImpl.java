package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.service.handlers.ApiChatCallHandler;
import com.testehan.springai.immobiliare.service.handlers.ApiChatCallHandlerFactory;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;


@Service
public class ApiServiceImpl implements ApiService{

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);
    public static final char SEPARATOR_USED_IN_CACHE = ',';

    private final ApiChatCallHandlerFactory apiCallHandlerFactory;

    private final ChatClient chatClient;
    private final UserSseService userSseService;
    private final LLMCacheService llmCacheService;
    private final LocaleUtils localeUtils;

    public ApiServiceImpl(ApiChatCallHandlerFactory apiCallHandlerFactory, ChatClient chatClient,
                          UserSseService userSseService,  LLMCacheService llmCacheService, LocaleUtils localeUtils) {
        this.localeUtils = localeUtils;
        this.apiCallHandlerFactory = apiCallHandlerFactory;
        this.chatClient = chatClient;
        this.userSseService = userSseService;
        this.llmCacheService = llmCacheService;
    }

    @Override
    public ResultsResponse getChatResponse(String message, HttpSession session) {
        LOGGER.info("Performance -1 -----------------------");
        var serviceCall = whichApiToCall(message);
        LOGGER.info("Performance 0 -----------------------");

        ApiChatCallHandler handler = apiCallHandlerFactory.getHandler(serviceCall.apiCall());
        return handler.handle(serviceCall, session);
    }

    @Override
    public Flux<Event> getServerSideEventsFlux(HttpSession session) {
        return  userSseService.getUserSseConnection(session.getId()).asFlux();
    }

    private ServiceCall whichApiToCall(final String message) {
        if (message == null || message.isBlank()) {
            LOGGER.error("Input message is null or blank.");
            return new ServiceCall(ApiCall.EXCEPTION, "Input message was empty.");
        }

        var trimmedMessage = message.trim();

        try {
            var cachedResponse = llmCacheService.getCachedResponse(trimmedMessage);
            if (cachedResponse.isPresent()) {
                return parseCachedResponse(cachedResponse.get());
            } else {

                ChatResponse assistantResponse;

                var outputParser = new BeanOutputConverter<>(ServiceCall.class);
                String format = outputParser.getFormat();

                var apiDescriptionPrompt = localeUtils.getLocalizedPrompt("ApiDescription");
                PromptTemplate promptTemplate = new PromptTemplate(apiDescriptionPrompt);
                Map<String, Object> promptParameters = new HashMap<>();
                promptParameters.put("format", format);
                Prompt prompt = promptTemplate.create(promptParameters);

                assistantResponse = chatClient.prompt()
                        .system(prompt.getContents())   //Move large static content to the system message field
                        .user(trimmedMessage)  // Keep dynamic elements in user messages, as system messages don't require repeating.
                        .call().chatResponse();

                var rawResponse = assistantResponse.getResult().getOutput().getContent();
                LOGGER.info("Raw LLM response: {}", rawResponse);
                if (!StringUtils.hasText(rawResponse)) {
                    LOGGER.error("LLM returned an empty or null response for message: {}", trimmedMessage);
                    return new ServiceCall(ApiCall.EXCEPTION, "LLM returned an empty or null response for message " + trimmedMessage);
                }

                ServiceCall serviceCall = parseLlmResponse(rawResponse, outputParser);
                if (serviceCall.apiCall() != ApiCall.EXCEPTION) {
                    var valueToBeCached = serviceCall.apiCall().toString() + "," + serviceCall.message();
                    llmCacheService.saveToCache("","",trimmedMessage, valueToBeCached);

                    LOGGER.debug("Cached LLM response for message: {}", trimmedMessage);
                } else {
                    LOGGER.warn("LLM returned an EXCEPTION API type for message '{}'. Not caching.", trimmedMessage);
                }

                return serviceCall;
            }
        } catch (Exception e) {
            // Catch-all for unexpected errors
            LOGGER.error("Unexpected error while calling LLM: {}", e.getMessage(), e);
            return new ServiceCall(ApiCall.EXCEPTION, "Unexpected error while calling LLM: " + e.getMessage());
        }
    }

    @NotNull
    private ServiceCall parseCachedResponse(final String cachedData) {
        try {
            String[] parts = splitByFirstCommaOccurrence(cachedData);
            return new ServiceCall(ApiCall.getByValue(parts[0]), parts[1]);

        } catch (Exception e) {
            LOGGER.error("Error parsing cached data '{}': {}", cachedData, e.getMessage(), e);
            return new ServiceCall(ApiCall.EXCEPTION, "Error parsing cached data");
        }
    }

    private String[] splitByFirstCommaOccurrence(final String data){
        final int index = data.indexOf(SEPARATOR_USED_IN_CACHE);

        if (index != -1) {
            var result = new String[2];
            var firstPart = data.substring(0, index);
            var secondPart = data.substring(index + 1);
            result[0] = firstPart;
            result[1] = secondPart;
            return result;
        }
        
        throw new IllegalArgumentException("Delimiter '" + SEPARATOR_USED_IN_CACHE + "' not found in input: " + data);
    }

    private ServiceCall parseLlmResponse(final String rawResponse, final BeanOutputConverter<ServiceCall> parser) {
        try {
            ServiceCall serviceCall = parser.convert(rawResponse);

            if (serviceCall == null) {
                LOGGER.error("LLM response parsed to null object. Raw response: {}", rawResponse);
                return new ServiceCall(ApiCall.EXCEPTION, "LLM response parsed to null object. Raw response: " + rawResponse);
            }

            if (serviceCall.apiCall() == null) {
                LOGGER.error("LLM response resulted in null API. Raw response: {}", rawResponse);
                return new ServiceCall(ApiCall.EXCEPTION, "LLM response resulted in null API. Raw response: " + rawResponse);
            }

            return serviceCall;

        } catch (Exception e) {
            LOGGER.error("Failed to parse LLM response. Raw response: '{}'. Error: {}", rawResponse, e.getMessage(), e);
            return new ServiceCall(ApiCall.EXCEPTION, "Failed to parse LLM response. Raw response: " + rawResponse + ". Error: " + e.getMessage());
        }
    }

}
