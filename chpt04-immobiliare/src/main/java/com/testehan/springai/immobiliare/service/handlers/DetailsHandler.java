package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.service.ApartmentCrudService;
import com.testehan.springai.immobiliare.service.ChatClientService;
import com.testehan.springai.immobiliare.service.ConversationService;
import com.testehan.springai.immobiliare.util.ListingUtil;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
public class DetailsHandler implements ApiChatCallHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetailsHandler.class);

    private final ApartmentCrudService apartmentCrudService;
    private final ConversationSession conversationSession;
    private final ConversationService conversationService;

    private final ChatClientService chatClientService;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;
    private final ListingUtil listingUtil;

    public DetailsHandler(ApartmentCrudService apartmentCrudService, ConversationSession conversationSession, ConversationService conversationService, ChatClientService chatClientService, MessageSource messageSource, LocaleUtils localeUtils, ListingUtil listingUtil) {
        this.apartmentCrudService = apartmentCrudService;
        this.conversationSession = conversationSession;
        this.conversationService = conversationService;
        this.chatClientService = chatClientService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
        this.listingUtil = listingUtil;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        try {
            var userMessage = serviceCall.message();
            var chatResponse = callLLM(userMessage,"system_detailGetFields");
            LOGGER.info("The fields {} were identified to be relevant.",chatResponse);

            var fields = Arrays.stream(chatResponse.split(","))
                    .map(String::trim) // Remove leading/trailing whitespace
                    .collect(Collectors.toList());

            final String conversationId = conversationSession.getConversationId();
            var listingIdsFromHistory = conversationService.getUserConversation(conversationId)
                    .stream().filter(message -> ObjectId.isValid(message))
                    .collect(Collectors.toList());

            var listingsFromHistory = apartmentCrudService.findApartmentsByIds(listingIdsFromHistory);
            var listingsData = listingUtil.apartmentFieldDataToString(listingUtil.getListingDataByFields(listingsFromHistory, fields));
            LOGGER.info("The listingsData {} was computed.", listingsData);

            var detailsForResults = localeUtils.getLocalizedPrompt("system_detailsForResults");
            PromptTemplate promptTemplate = new PromptTemplate(detailsForResults);
            Map<String, Object> promptParameters = new HashMap<>();
            promptParameters.put("questionOrRequest", userMessage);
            promptParameters.put("listingsData", listingsData);
            Prompt prompt = promptTemplate.create(promptParameters);

            chatResponse = callLLM(prompt.getContents(),"system_detailsForResults");

            return new ResultsResponse(chatResponse);
        } catch (Exception e) {
            // Catch-all for unexpected errors
            LOGGER.error("Unexpected error while calling LLM: {}", e.getMessage(), e);
            return new ResultsResponse(messageSource.getMessage("chat.exception", null, localeUtils.getCurrentLocale()));
        }
    }

    @Override
    public ApiCall getApiCall() {
        return ApiCall.GET_DETAILS;
    }

    private String callLLM(String userMessage, String systemPromptFile){
        ChatClient chatClient = chatClientService.createChatClient();
        return chatClient.prompt()
                .advisors(new Consumer<ChatClient.AdvisorSpec>() {
                    @Override
                    public void accept(ChatClient.AdvisorSpec advisorSpec) {
                        advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatClientService.getConversationSession().getConversationId());
                    }
                })
                .advisors(new Consumer<ChatClient.AdvisorSpec>() {
                    @Override
                    public void accept(ChatClient.AdvisorSpec advisorSpec) {
                        advisorSpec.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50);
                    }
                })
                .system(localeUtils.getLocalizedPrompt(systemPromptFile))
                .user(userMessage)
                .call().content();
    }

}
