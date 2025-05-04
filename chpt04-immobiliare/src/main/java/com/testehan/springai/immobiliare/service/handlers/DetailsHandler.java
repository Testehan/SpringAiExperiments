package com.testehan.springai.immobiliare.service.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.events.ApartmentPayload;
import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.events.EventPayload;
import com.testehan.springai.immobiliare.events.ResponsePayload;
import com.testehan.springai.immobiliare.model.*;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.service.*;
import com.testehan.springai.immobiliare.util.ListingUtil;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
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
    private final UserSseService userSseService;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;
    private final ListingUtil listingUtil;

    private final BeanOutputConverter outputParser;
    private final ObjectMapper objectMapper;

    public DetailsHandler(ApartmentCrudService apartmentCrudService, ConversationSession conversationSession, ConversationService conversationService, LLMCacheService llmCacheService, ChatClientService chatClientService, UserSseService userSseService, MessageSource messageSource, LocaleUtils localeUtils, ListingUtil listingUtil, ObjectMapper objectMapper) {
        this.apartmentCrudService = apartmentCrudService;
        this.conversationSession = conversationSession;
        this.conversationService = conversationService;
        this.llmCacheService = llmCacheService;
        this.chatClientService = chatClientService;
        this.userSseService = userSseService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
        this.listingUtil = listingUtil;
        this.objectMapper = objectMapper;

        this.outputParser = new BeanOutputConverter<>(ResultDetailsResponse.class);
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

            final String answer = getAnswer(userMessage, detailFields, listingIdsFromChatHistory, session);
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
        final String llmCacheKey = userMessage + " " + RANDOM_STRING;
        final String detailFields;

        var cachedResponse = llmCacheService.getCachedResponse(llmCacheKey);
        if (cachedResponse.isPresent()) {
            LOGGER.info("Performance Cache 1 -----------------------");
            detailFields = cachedResponse.get();
            LOGGER.info("Performance Cache 2 -----------------------");
        } else {
            detailFields = callLLM(userMessage,localeUtils.getLocalizedPrompt("system_detailGetFields"));
            llmCacheService.saveToCache("", "", llmCacheKey, detailFields);
        }
        LOGGER.info("The fields {} were identified to be relevant.",detailFields);
        return detailFields;
    }

    private String getAnswer(String userMessage, String commaSeparatedFields, List<String> listingIdsFromChatHistory, HttpSession session) {
        // we need to append the listing ids, because we want the key to be question + listingIdsFromChatHistory, so that
        // we get the answer that refers to these ids
        final String llmCacheKey = userMessage + concatenateStringsWithComma(listingIdsFromChatHistory);

        final String answer;
        final ResultDetailsResponse response;

        var cachedResponse = llmCacheService.getCachedResponse(llmCacheKey);
        if (cachedResponse.isPresent()) {
            LOGGER.info("Performance Cache 1 - Getting details answer -----------------------");
            response = (ResultDetailsResponse) outputParser.convert(cachedResponse.get());
            answer = response.answer();
            LOGGER.info("Performance Cache 2 - Getting details answer -----------------------");
        } else {
            var fields = Arrays.stream(commaSeparatedFields.split(","))
                    .map(String::trim) // Remove leading/trailing whitespace
                    .collect(Collectors.toList());

            var listingsFromHistory = apartmentCrudService.findApartmentsByIds(listingIdsFromChatHistory);
            var firstListing = listingsFromHistory.stream().findFirst();

            var userPrompt = getUserDetailsPrompt(userMessage, listingsFromHistory, fields);
            var systemPrompt = getSystemDetailsPrompt();

            String answerDirty = callLLM(userPrompt, systemPrompt);
            response = (ResultDetailsResponse) outputParser.convert(answerDirty);
            var answerCleaned = response.answer().replace("```html","").replace("```","");

            try {
                saveToCache(firstListing, llmCacheKey, objectMapper.writeValueAsString(new ResultDetailsResponse(answerCleaned, response.ids())));
            } catch (JsonProcessingException e) {
                LOGGER.error("The response for userMessage input {} could not be cached because of {}",userMessage, e.getMessage());

            }

            answer = answerCleaned;
        }


        if (!response.ids().isEmpty()){
            final String conversationId = conversationSession.getConversationId();
            emitEvent(session.getId(), "response", new ResponsePayload(answer,conversationId));

            final ImmobiliareUser immobiliareUser = conversationSession.getImmobiliareUser().get();
//            conversationService.deleteUserConversation(conversationId);
            // no point in deleting the conversation,since now i try to make sure that listing ids are presnt only 1 in the conversation history

            var listingsMentionedInResponse = apartmentCrudService.findApartmentsByIds(response.ids());
            for (Apartment apartment : listingsMentionedInResponse) {
                sendListing(session.getId(), apartment, conversationId, immobiliareUser);
            }

            return messageSource.getMessage("M05_APARTMENTS_FOUND_END", null, localeUtils.getCurrentLocale());
        }
        else {
            return answer;
        }

    }

    private String getSystemDetailsPrompt() {
        String format = outputParser.getFormat();

        var apiDescriptionPrompt = localeUtils.getLocalizedPrompt("system_detailResponses");
        PromptTemplate promptTemplate = new PromptTemplate(apiDescriptionPrompt);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("format", format);
        Prompt prompt = promptTemplate.create(promptParameters);

        return prompt.getContents();
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

    private String getUserDetailsPrompt(String userMessage, List<Apartment> listingsFromHistory, List<String> fields) {
        var listingsData = listingUtil.apartmentFieldDataToString(listingUtil.getListingDataByFields(listingsFromHistory, fields));
        LOGGER.info("The listingsData {} was computed.", listingsData);

        var detailsForResults = localeUtils.getLocalizedPrompt("user_detailsForResults");

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

    private String callLLM(String userMessage, String systemPrompt){
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
                .system(systemPrompt)
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

    private void sendListing(String sessionId, Apartment listingFound, String conversationId, ImmobiliareUser immobiliareUser) {

        conversationService.addContentToConversation(listingFound.getIdString(), conversationId);

        var isFavourite = listingUtil.isApartmentAlreadyFavourite(listingFound.getId().toString(), immobiliareUser);
        LOGGER.info("Sending SSE TO ----------------------- {}",userSseService.addUserSseId(sessionId));
        emitEvent(sessionId, "apartment",new ApartmentPayload(listingFound, isFavourite));
    }

    private void emitEvent(String sessionId, String eventType, EventPayload eventPayload) {
        var userSink = userSseService.getUserSseConnection(sessionId);
        if (Objects.nonNull(userSink)) {
            userSink.tryEmitNext(new Event(eventType, eventPayload));
        } else {
            LOGGER.error("There is no userSink for user with sessionId {}. No event will be sent.",sessionId);
        }
    }

}
