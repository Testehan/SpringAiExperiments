package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.service.ApartmentCrudService;
import com.testehan.springai.immobiliare.service.ChatClientService;
import com.testehan.springai.immobiliare.service.ConversationService;
import com.testehan.springai.immobiliare.service.LLMCacheService;
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

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
public class DetailsHandler implements ApiChatCallHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetailsHandler.class);

    private static final String RANDOM_STRING = "b04dd0d8721abae36c988efc3861b1924a6507d4d27d5804d9d5f9e199e34a6f";

    private final ApartmentCrudService apartmentCrudService;
    private final ConversationSession conversationSession;
    private final ConversationService conversationService;
    private final LLMCacheService llmCacheService;

    private final ChatClientService chatClientService;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;
    private final ListingUtil listingUtil;

    public DetailsHandler(ApartmentCrudService apartmentCrudService, ConversationSession conversationSession, ConversationService conversationService, LLMCacheService llmCacheService, ChatClientService chatClientService, MessageSource messageSource, LocaleUtils localeUtils, ListingUtil listingUtil) {
        this.apartmentCrudService = apartmentCrudService;
        this.conversationSession = conversationSession;
        this.conversationService = conversationService;
        this.llmCacheService = llmCacheService;
        this.chatClientService = chatClientService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
        this.listingUtil = listingUtil;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        try {
            var userMessage = serviceCall.message();

            final String detailFields = getDetailFields(userMessage);

            final String conversationId = conversationSession.getConversationId();
            var listingIdsFromChatHistory = conversationService.getUserConversation(conversationId)
                    .stream().filter(message -> ObjectId.isValid(message))
                    .collect(Collectors.toList());

            final String answer = getAnswer(userMessage, detailFields, listingIdsFromChatHistory);
            return new ResultsResponse(answer);

        } catch (Exception e) {
            // Catch-all for unexpected errors
            LOGGER.error("Unexpected error while calling LLM: {}", e.getMessage(), e);
            return new ResultsResponse(messageSource.getMessage("chat.exception", null, localeUtils.getCurrentLocale()));
        }
    }

    private String getDetailFields(String userMessage) {
        // we need to append RANDOM_STRING, because when this is reached llmCacheKey is already present in the cache
        // for the GET_DETAILS api call. so in order to distinguish between the 2 I add here this string
        final String llmCacheKey = RANDOM_STRING + userMessage;
        final String detailFields;

        var cachedResponse = llmCacheService.getCachedResponse(llmCacheKey);
        if (cachedResponse.isPresent()) {
            LOGGER.info("Performance Cache 1 -----------------------");
            detailFields = cachedResponse.get();
            LOGGER.info("Performance Cache 2 -----------------------");
        } else {
            detailFields = callLLM(userMessage,"system_detailGetFields");
            llmCacheService.saveToCache("", "", userMessage.trim(), detailFields);
        }
        LOGGER.info("The fields {} were identified to be relevant.",detailFields);
        return detailFields;
    }

    private String getAnswer(String userMessage, String commaSeparatedFields, List<String> listingIdsFromChatHistory) {
        // we need to append the listing ids, because we want the key to be question + listingIdsFromChatHistory, so that
        // we get the answer that refers to these ids
        final String llmCacheKey = userMessage + concatenateStringsWithComma(listingIdsFromChatHistory);

        final String answer;

        var cachedResponse = llmCacheService.getCachedResponse(llmCacheKey);
        if (cachedResponse.isPresent()) {
            LOGGER.info("Performance Cache 1 - Getting details answer -----------------------");
            answer = cachedResponse.get();
            LOGGER.info("Performance Cache 2 - Getting details answer -----------------------");
        } else {
            var fields = Arrays.stream(commaSeparatedFields.split(","))
                    .map(String::trim) // Remove leading/trailing whitespace
                    .collect(Collectors.toList());

            var listingsFromHistory = apartmentCrudService.findApartmentsByIds(listingIdsFromChatHistory);
            var firstListing = listingsFromHistory.stream().findFirst();

            String prompt = getDetailsPrompt(userMessage, listingsFromHistory, fields);

            var answerDirty = callLLM(prompt,"system_defaultResponses");
            var answerCleaned = answerDirty.replace("```html","").replace("```","");
            saveToCache(firstListing, llmCacheKey, answerCleaned);

            answer = answerCleaned;
        }

        return answer;
    }

    private void saveToCache(Optional<Apartment> listing, String llmCacheKey, String answerCleaned){

        final String city;
        final String propertyType;

        if (listing.isPresent()){
            city = listing.get().getCity();
            propertyType = listing.get().getPropertyType();
        } else {
            city = "";
            propertyType = "";
        }

        llmCacheService.saveToCache(city, propertyType, llmCacheKey, answerCleaned);
    }

    private String getDetailsPrompt(String userMessage, List<Apartment> listingsFromHistory, List<String> fields) {
        var listingsData = listingUtil.apartmentFieldDataToString(listingUtil.getListingDataByFields(listingsFromHistory, fields));
        LOGGER.info("The listingsData {} was computed.", listingsData);

        var detailsForResults = localeUtils.getLocalizedPrompt("system_detailsForResults");

        PromptTemplate promptTemplate = new PromptTemplate(detailsForResults);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("questionOrRequest", userMessage);
        promptParameters.put("listingsData", listingsData);
        Prompt prompt = promptTemplate.create(promptParameters);

        return prompt.getContents();
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

    private String concatenateStringsWithComma(List<String> stringList) {
        if (stringList == null || stringList.isEmpty()) {
            return ""; // Return an empty string for null or empty lists
        }

        return stringList.stream()
                .collect(Collectors.joining(", "));
    }

}
