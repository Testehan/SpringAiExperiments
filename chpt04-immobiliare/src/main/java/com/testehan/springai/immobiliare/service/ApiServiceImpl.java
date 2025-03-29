package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.service.handlers.ApiChatCallHandler;
import com.testehan.springai.immobiliare.service.handlers.ApiChatCallHandlerFactory;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;


@Service
public class ApiServiceImpl implements ApiService{

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);

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

    private ServiceCall whichApiToCall(String message) {
        try {
            var cachedResponse = llmCacheService.getCachedResponse(message);
            if (cachedResponse.isPresent()) {
                var response = cachedResponse.get();
                String[] parts = response.split(",");
                return new ServiceCall(ApiCall.getByValue(parts[0]), parts[1]);
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
                        .user(message)  // Keep dynamic elements in user messages, as system messages don't require repeating.
                        .call().chatResponse();

                var response = assistantResponse.getResult().getOutput().getContent();
                LOGGER.info(response);
                ServiceCall serviceCall = outputParser.convert(response);

                var valueToBeCached = serviceCall.apiCall().toString() + "," + serviceCall.message();
                llmCacheService.saveToCache("","",message, valueToBeCached);

                return serviceCall;
            }
        } catch (Exception e) {
            // Catch-all for unexpected errors
            LOGGER.error("Unexpected error while calling LLM: {}", e.getMessage(), e);
            return new ServiceCall(ApiCall.EXCEPTION, "");
        }
    }

}
