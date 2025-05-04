package com.testehan.springai.immobiliare.service.handlers;

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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
public class SearchListingsHandler implements ApiChatCallHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchListingsHandler.class);

    private final ApartmentCrudService apartmentCrudService;
    private final CityService cityService;
    private final ApartmentService apartmentService;
    private final ChatClientService chatClientService;
    private final EmbeddingService embeddingService;
    private final LLMCacheService llmCacheService;

    private final ChatClient chatClient;

    private final ConversationSession conversationSession;
    private final ConversationService conversationService;
    private final UserSseService userSseService;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;
    private final ListingUtil listingUtil;

    public SearchListingsHandler(ApartmentCrudService apartmentCrudService, CityService cityService, ApartmentService apartmentService,
                                 ChatClientService chatClientService, EmbeddingService embeddingService,
                                 LLMCacheService llmCacheService, ChatClient chatClient, ConversationSession conversationSession,
                                 ConversationService conversationService, UserSseService userSseService, MessageSource messageSource,
                                 LocaleUtils localeUtils, ListingUtil listingUtil) {
        this.apartmentCrudService = apartmentCrudService;
        this.cityService = cityService;
        this.apartmentService = apartmentService;
        this.chatClientService = chatClientService;
        this.embeddingService = embeddingService;
        this.llmCacheService = llmCacheService;
        this.chatClient = chatClient;
        this.conversationSession = conversationSession;
        this.conversationService = conversationService;
        this.userSseService = userSseService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
        this.listingUtil = listingUtil;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        try {
            session.setAttribute("sseIndex", 0);
            var description = serviceCall.message();

            conversationSession.setLastPropertyDescription(description);
            final String conversationId = conversationSession.getConversationId();
            final ImmobiliareUser immobiliareUser = conversationSession.getImmobiliareUser().get();

            // we do this clearing because we want our chat memory to contain only the latest listing results, on which
            // the user can ask additional questions. Otherwise, the chatMemory will contain results from previous
            // searches based on apartment descriptions, even from other Cities, and thus the results would be affected

            conversationService.deleteUserConversation(conversationId);

            var propertyType = immobiliareUser.getPropertyType();
            final String city;
            if (cityService.isEnabled(immobiliareUser.getCity())){
                city = immobiliareUser.getCity();
            } else {
                var supportedCities = cityService.getEnabledCityNames().stream().collect(Collectors.joining(", "));
                return new ResultsResponse(messageSource.getMessage("M021_SUPPORTED_CITIES",  new Object[]{supportedCities}, localeUtils.getCurrentLocale()));
            }

            var budgetInfo = messageSource.getMessage("prompt.budget", new Object[]{conversationSession.getBudget()}, localeUtils.getCurrentLocale());
            var descriptionWithBudgetInfo = description + budgetInfo;

            conversationService.addContentToConversation(descriptionWithBudgetInfo, conversationId);

            ResultsResponse response = new ResultsResponse("");

            // the hash will be for all these items
            final String llmCacheKey = city + " " + propertyType + " " + descriptionWithBudgetInfo;
            var cachedResponse = llmCacheService.getCachedResponse(llmCacheKey);
            if (cachedResponse.isPresent()) {
                LOGGER.info("Performance Cache 1 -----------------------");
                String[] listingIds = cachedResponse.get().split("\\,");
                sendResultsFoundResponse(session, conversationId);

                var listingsFound = apartmentCrudService.findApartmentsByIds(List.of(listingIds))
                        .stream().filter(listing -> listing.isActive()).collect(Collectors.toList());
                listingUtil.setIsMostFavouriteAndContacted(listingsFound);
                for (Apartment listing : listingsFound){
                    sendListing(session.getId(), listing, conversationId, immobiliareUser);
                }

                 sendSearchComplete(session.getId(), conversationId);

                LOGGER.info("Performance Cache 2 -----------------------");
                return response;
            } else {

                LOGGER.info("Performance 1 -----------------------");

                CompletableFuture<ApartmentDescription> getListingDescriptionFuture =
                        CompletableFuture.supplyAsync(() -> extractApartmentInformationFromProvidedDescription(descriptionWithBudgetInfo));

                CompletableFuture<List<Double>> getDescriptionEmbeddingFuture =
                        CompletableFuture.supplyAsync(() -> embeddingService.getOrComputeEmbedding(description));

                final ApartmentDescription apartmentDescription = getListingDescriptionFuture.get();
                LOGGER.info("Performance 2 -----------------------");

                // MAYBE the apartment description contains the city, in which case we will use that city with a priority higher than what the user stored
//                city = SupportedCity.getByName(apartmentDescription.getCity()) != UNSUPPORTED ? apartmentDescription.getCity() : SupportedCity.getByName(conversationSession.getCity()) != UNSUPPORTED ? conversationSession.getCity() : UNSUPPORTED.getName();

                var apartmentsFromSemanticSearch = apartmentService.getApartmentsSemanticSearch(PropertyType.fromString(propertyType), city, apartmentDescription, getDescriptionEmbeddingFuture.get());
                LOGGER.info("Performance 3 -----------------------");

                LOGGER.info("Apartments found from vector store semantic search: {}" , apartmentsFromSemanticSearch.size());
                apartmentsFromSemanticSearch.stream().forEach(ap -> LOGGER.info("Apartment {}  : {}", ap.getId(), ap.getName()));

                Locale currentLocale = localeUtils.getCurrentLocale();
                StringBuilder resultsToCache = new StringBuilder();
                if (apartmentsFromSemanticSearch.size() > 0) {
                    int batchSize = 5;  // apparently sending requests containing a smaller nr of apartment descriptions makes responses more accurate
                    AtomicBoolean isFirst = new AtomicBoolean(true);

                    var bestMatchingApartmentIds = sendIdsInBatches(apartmentsFromSemanticSearch, description, batchSize);
                    bestMatchingApartmentIds
                            .filter(id -> ObjectId.isValid(id))
                            .distinct()
                            .subscribe(
                                    apId -> {
                                        var apartmentLLM = apartmentsFromSemanticSearch.stream()
                                                .filter(item -> apId.equals(item.getId().toString()))
                                                .findFirst();
                                        if (!apartmentLLM.isEmpty()) {
                                            if (isFirst.getAndSet(false)) {
                                                sendResultsFoundResponse(session, conversationId);
                                            }
                                            resultsToCache.append(apartmentLLM.get().getId().toString()).append(",");
                                            sendListing(session.getId(), apartmentLLM.get(), conversationId, immobiliareUser);
                                        }

                                    },
                                    error -> {
                                        LOGGER.error("Error: {}", error);
                                        throw new RuntimeException(error.getMessage());
                                    },
                                    () -> {
                                        if (isFirst.get()) {     // this means that we processed stream and we got no match
                                            sendSearchCompletedNoResults(description, session.getId(),conversationId);
                                        } else {
                                            llmCacheService.saveToCache(city,propertyType,llmCacheKey, resultsToCache.toString());
                                            sendSearchComplete(session.getId(), conversationId);
                                        }

                                    }
                            );

                } else {
                    response = new ResultsResponse(messageSource.getMessage("M05_NO_APARTMENTS_FOUND", null, currentLocale));
                }

                return response;
            }
        } catch (RuntimeException | InterruptedException | ExecutionException e){
            LOGGER.error(e.getMessage());
            return new ResultsResponse(messageSource.getMessage("chat.exception", null, localeUtils.getCurrentLocale()));
        }
    }

    @Override
    public ApiCall getApiCall() {
        return ApiCall.GET_APARTMENTS;
    }

    private void sendSearchCompletedNoResults(String description, String sessionId, String conversationId) {
        LOGGER.info("Sending SSE TO ----------------------- {}",userSseService.addUserSseId(sessionId));
        var payload = messageSource.getMessage("M05_NO_APARTMENTS_FOUND", null, localeUtils.getCurrentLocale());
        emitEvent(sessionId, "response", new ResponsePayload(payload, conversationId));
        LOGGER.info("Search completed with no results for description : {}", description);
    }

    private void sendListing(String sessionId, Apartment listingFound, String conversationId, ImmobiliareUser immobiliareUser) {
        LOGGER.info("Performance 4 -----------------------");
        LOGGER.info("Found apartment id {}", listingFound.getId());

        LOGGER.info("Adding apartment info to conversation memory {}", listingFound.getName());
        conversationService.addContentToConversation(listingFound.getIdString(), conversationId);

        var isFavourite = listingUtil.isApartmentAlreadyFavourite(listingFound.getId().toString(), immobiliareUser);
        LOGGER.info("Sending SSE TO ----------------------- {}",userSseService.addUserSseId(sessionId));
        emitEvent(sessionId, "apartment",new ApartmentPayload(listingFound, isFavourite));
    }

    private void sendResultsFoundResponse(HttpSession session, String conversationId) {
        var sessionId = session.getId();
        LOGGER.info("Sending SSE TO ----------------------- {}",userSseService.addUserSseId(sessionId));
        var payload = messageSource.getMessage("M05_APARTMENTS_FOUND_START", null, localeUtils.getCurrentLocale());
        emitEvent(sessionId, "response", new ResponsePayload(payload, conversationId));
    }

    private void sendSearchComplete(String sessionId, String conversationId){
        LOGGER.info("Sending SSE TO ----------------------- {}",userSseService.addUserSseId(sessionId));
        var payload = messageSource.getMessage("M05_APARTMENTS_FOUND_END", null, localeUtils.getCurrentLocale());
        emitEvent(sessionId, "response", new ResponsePayload(payload, conversationId));
        LOGGER.info("Search completed");
    }

    private void emitEvent(String sessionId, String eventType, EventPayload eventPayload) {
        var userSink = userSseService.getUserSseConnection(sessionId);
        if (Objects.nonNull(userSink)) {
            userSink.tryEmitNext(new Event(eventType, eventPayload));
        } else {
            LOGGER.error("There is no userSink for user with sessionId {}. No event will be sent.",sessionId);
        }
    }


    private boolean propertyIdContainsComma(String propertyId) {
        LOGGER.info("Current list of ids from llm: {}", propertyId);
        return true;
    }

    private Flux<String> sendIdsInBatches(List<Apartment> apartments, String description, int batchSize) {
        return Flux.fromIterable(apartments)
                .buffer(batchSize)//.delayElements(Duration.ofSeconds(batchSize*5))
                .flatMap(batchApartments -> getBestMatchingApartmentIds(batchApartments,description));  // Send each batch to the service
    }

    private Flux<String> getBestMatchingApartmentIds(List<Apartment> apartments, String description) {
        try {
            // users will have time to look over the first results and in the mean time more listings are displayed
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        var listingIds = apartments.stream()
                .map(Apartment::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        LOGGER.info("Sending apartments descriptions to LLM {}", listingIds);
        var apartmentsFoundPrompt = localeUtils.getLocalizedPrompt("apartments_found");

        var promptTemplate = new PromptTemplate(apartmentsFoundPrompt);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("apartmentsFound", formatApartmentsFound(apartments));
        promptParameters.put("description", description);
        var prompt = promptTemplate.create(promptParameters);

        return respondToUserMessageStream(prompt.getContents());

    }

    private String formatApartmentsFound(List<Apartment> apartments) {
        var stringBuilder = new StringBuilder();
        for (Apartment apartment : apartments){
            stringBuilder.append("Apartment " + apartment.getId() + " :" + listingUtil.getApartmentInfo(apartment) + "\n");
        }

        return stringBuilder.toString();
    }

    private Flux<String> respondToUserMessageStream(String userMessage) {

        var chatResponse = chatClientService.createChatClient()
                .prompt()
                .advisors (new Consumer<ChatClient.AdvisorSpec>() {
                    @Override
                    public void accept(ChatClient.AdvisorSpec advisorSpec) {
                        advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationSession.getConversationId());
                    }
                })
                .advisors(new Consumer<ChatClient.AdvisorSpec>() {
                    @Override
                    public void accept(ChatClient.AdvisorSpec advisorSpec) {
                        advisorSpec.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50);
                    }
                })
                .user(userMessage)
                .stream().content();

        return chatResponse.scan(new StringBuilder(), (acc, next) -> acc.append(next))  // Append characters
                .filter(buffer -> propertyIdContainsComma(buffer.toString()))
                .map(buffer-> buffer.toString().replace("\\s+", "").split(","))
                .flatMap(idsArray -> Flux.fromArray(idsArray));
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

        ApartmentDescription apartment = outputParser.convert(assistantResponse.getResult().getOutput().getText());
        return apartment;
    }
}
