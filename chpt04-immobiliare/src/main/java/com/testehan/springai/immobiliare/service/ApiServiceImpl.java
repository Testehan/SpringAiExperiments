package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.advisor.CaptureMemoryAdvisor;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.events.ApartmentPayload;
import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.events.ResponsePayload;
import com.testehan.springai.immobiliare.model.*;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.testehan.springai.immobiliare.model.SupportedCity.UNSUPPORTED;
import static com.testehan.springai.immobiliare.util.ListingUtil.isApartmentAlreadyFavourite;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;


@Service
public class ApiServiceImpl implements ApiService{

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);

    private ImmobiliareApiService immobiliareApiService;

    private ApartmentService apartmentService;

    private ChatModel chatmodel;

    private VectorStore vectorStore;

    private Executor executor;

    private ConversationSession conversationSession;
    private ConversationService conversationService;
    private UserSseService userSseService;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public ApiServiceImpl(ImmobiliareApiService immobiliareApiService, ApartmentService apartmentService,
                          ChatModel chatmodel, VectorStore vectorStore, @Qualifier("applicationTaskExecutor") Executor executor,
                          ConversationSession conversationSession, ConversationService conversationService,
                          UserSseService userSseService,
                          MessageSource messageSource, LocaleUtils localeUtils) {
        this.immobiliareApiService = immobiliareApiService;
        this.apartmentService = apartmentService;
        this.chatmodel = chatmodel;
        this.vectorStore = vectorStore;
        this.executor = executor;
        this.conversationSession = conversationSession;
        this.conversationService = conversationService;
        this.userSseService = userSseService;

        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @Override
    public ResultsResponse getChatResponse(String message, HttpSession session) {
        var serviceCall = immobiliareApiService.whichApiToCall(message);

        switch (serviceCall.apiCall()) {
            case SET_RENT_OR_BUY : { return setRentOrBuy(serviceCall);}
            case SET_CITY : { return setCity(serviceCall); }
            case SET_RENT_OR_BUY_AND_CITY: {return setRentOrBuyAndCity(serviceCall);}
            case SET_RENT_OR_BUY_AND_CITY_AND_DESCRIPTION: {return setRentOrBuyAndCityAndDescription(serviceCall, session);}
            case GET_APARTMENTS:{ return getApartments(message, session); }
            case RESTART_CONVERSATION : { return restartConversation(); }
            case DEFAULT : return respondToUserMessage(message);
            case NOT_SUPPORTED : return new ResultsResponse(messageSource.getMessage("M00_IRRELEVANT_PROMPT", null, localeUtils.getCurrentLocale()));

        }

        return new ResultsResponse(messageSource.getMessage("M00_IRRELEVANT_PROMPT", null, localeUtils.getCurrentLocale()));
    }

    private ResultsResponse getApartments(String description, HttpSession session) {

        session.setAttribute("sseIndex", 0);
        conversationSession.setLastPropertyDescription(description);
        // we do this clearing because we want our chat memory to contain only the latest listing results, on which
        // the user can ask additional questions. Otherwise, the chatMemory will contain results from previous
        // searches based on apartment descriptions, even from other Cities, and thus the results would be affected
        conversationSession.clearChatMemoryAndConversation();
        var apartmentDescription = immobiliareApiService.extractApartmentInformationFromProvidedDescription(description);

        var rentOrSale = conversationSession.getRentOrSale();
        // MAYBE the apartment description contains the city, in which case we will use that city with a priority higher than what the user stored
        var city = SupportedCity.getByName(apartmentDescription.getCity()) != UNSUPPORTED ? apartmentDescription.getCity() : SupportedCity.getByName(conversationSession.getCity()) != UNSUPPORTED ? conversationSession.getCity() : UNSUPPORTED.getName();
        var conversationId = conversationSession.getConversationId();
        final ImmobiliareUser immobiliareUser = conversationSession.getImmobiliareUser();
        var apartmentsFromSemanticSearch = apartmentService.getApartmentsSemanticSearch(PropertyType.fromString(rentOrSale), city,apartmentDescription, description);

        LOGGER.info("Apartments found from vector store semantic search:");
        apartmentsFromSemanticSearch.stream().forEach(ap -> LOGGER.info( "Apartment {}  : {}", ap.getId(), ap.getApartmentInfo()));

        ResultsResponse response = new ResultsResponse("");

        Locale currentLocale = localeUtils.getCurrentLocale();
        if (apartmentsFromSemanticSearch.size() > 0) {
            int batchSize = 2;  // apparently sending requests containing a smaller nr of apartment descriptions makes responses more accurate
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
                            if (!apartmentLLM.isEmpty()){
                                if (isFirst.getAndSet(false)) {
                                    userSseService.getUserSseConnection(session.getId())
                                            .tryEmitNext(new Event("response",new ResponsePayload(
                                                    messageSource.getMessage("M04_APARTMENTS_FOUND_START", null, currentLocale)))
                                            );
                                }
                                LOGGER.info("Found apartment id {}",  apartmentLLM.get().getId());
                                // basically adding the returned result apartments to the conversation; TODO this needs to be tested out for example what happens when there are multiple apartments added to the conversation vectorestore... does that screw up the conversation ?
                                // TODO i think we should only call this method, when a property is favourited... so that only those are in the context. Otherwise..there will be a very big context

                                var apartmentInfo = apartmentLLM.get().getApartmentInfo();
                                LOGGER.info("Adding apartment info to conversation memory {}",  apartmentInfo);
                                conversationService.addContentToConversation(apartmentInfo, conversationId);

                                var isFavourite = isApartmentAlreadyFavourite(apartmentLLM.get().getId().toString(), immobiliareUser);
                                userSseService.getUserSseConnection(session.getId())
                                        .tryEmitNext(new Event("apartment", new ApartmentPayload(apartmentLLM.get(), isFavourite)));
                            }

                        },
                        error -> {
                            LOGGER.error("Error: {}", error);
                        },
                        () -> {
                            if (isFirst.get()){     // this means that we processed stream and we got no match
                                userSseService.getUserSseConnection(session.getId())
                                        .tryEmitNext(new Event("response",new ResponsePayload(
                                                messageSource.getMessage("M04_NO_APARTMENTS_FOUND", null, currentLocale)))
                                        );
                                LOGGER.info("Search completed with no results for description : {}", description);
                            } else {
                                userSseService.getUserSseConnection(session.getId())
                                        .tryEmitNext(new Event("response", new ResponsePayload(
                                                messageSource.getMessage("M04_APARTMENTS_FOUND_END", null, currentLocale)))
                                        );
                                LOGGER.info("Search completed");
                            }

                        }
                );

        } else {
            response = new ResultsResponse(messageSource.getMessage("M04_NO_APARTMENTS_FOUND", null, currentLocale));
        }

        return response;
    }



    private boolean propertyIdContainsComma(String propertyId) {
        LOGGER.info("Current list of ids from llm: {}", propertyId);
        return true;
//        return propertyId.contains(",") && propertyId.charAt(propertyId.length() - 1) == ',';
    }

    public Flux<Event> getServerSideEventsFlux(HttpSession session) {
        return  userSseService.getUserSseConnection(session.getId()).asFlux();
    }


    private Flux<String> sendIdsInBatches(List<Apartment> apartments, String description, int batchSize) {
        return Flux.fromIterable(apartments)
                .buffer(batchSize)//.delayElements(Duration.ofSeconds(batchSize*5))
                .flatMap(batchApartments -> getBestMatchingApartmentIds(batchApartments,description));  // Send each batch to the service
    }

    private Flux<String> getBestMatchingApartmentIds(List<Apartment> apartments, String description) {
        try {
            // users will have time to look over the first results and in the mean time more listings are displayed
            Thread.sleep(2000);
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
            stringBuilder.append("Apartment " + apartment.getId() + " :" + apartment.getApartmentInfo() + "\n");
        }

        return stringBuilder.toString();
    }

    private ResultsResponse restartConversation() {
        conversationSession.clearConversation();
        return new ResultsResponse(messageSource.getMessage("M01_INITIAL_MESSAGE", null, localeUtils.getCurrentLocale()));

    }

    private ResultsResponse setCity(ServiceCall serviceCall) {
        SupportedCity supportedCity = getSupportedCity(serviceCall.message());
        var user = conversationSession.getImmobiliareUser();
        if (supportedCity.compareTo(UNSUPPORTED) != 0) {
            var propertyType =  messageSource.getMessage(user.getPropertyType(), null, localeUtils.getCurrentLocale());
            return new ResultsResponse(
                    messageSource.getMessage("M03_DETAILS",  new Object[]{propertyType, supportedCity.getName()}, localeUtils.getCurrentLocale()) +
                    messageSource.getMessage("M03_DETAILS_PART_2",  null, localeUtils.getCurrentLocale())
            );
        } else {
            var supportedCities = SupportedCity.getSupportedCities().stream().collect(Collectors.joining(", "));
            return new ResultsResponse(messageSource.getMessage("M021_SUPPORTED_CITIES",  new Object[]{supportedCities}, localeUtils.getCurrentLocale()));
        }
    }

    private SupportedCity getSupportedCity(String city) {
        SupportedCity supportedCity = SupportedCity.getByName(city);
        conversationSession.setCity(supportedCity);
        return supportedCity;
    }

    private ResultsResponse setRentOrBuy(ServiceCall serviceCall) {
        conversationSession.setRentOrSale(serviceCall.message());
        return new ResultsResponse(messageSource.getMessage("M02_CITY", null, localeUtils.getCurrentLocale()));
    }

    private ResultsResponse setRentOrBuyAndCity(ServiceCall serviceCall) {
        String[] parts = serviceCall.message().split(",");

        conversationSession.setRentOrSale(parts[0]);
        var cityName = parts[1];
        return setCity(new ServiceCall(ApiCall.SET_RENT_OR_BUY_AND_CITY,cityName));
    }

    private ResultsResponse setRentOrBuyAndCityAndDescription(ServiceCall serviceCall, HttpSession session) {
        String[] parts = serviceCall.message().split(",");

        conversationSession.setRentOrSale(parts[0]);
        var cityName = parts[1];

        SupportedCity supportedCity = getSupportedCity(cityName);
        var description = parts[2];
        if (supportedCity.compareTo(UNSUPPORTED) != 0) {
            return getApartments(description, session);
        } else {
            var supportedCities = SupportedCity.getSupportedCities().stream().collect(Collectors.joining(", "));
            return new ResultsResponse(messageSource.getMessage("M021_SUPPORTED_CITIES",  new Object[]{supportedCities}, localeUtils.getCurrentLocale()));
        }
    }

    private ChatClient createNewChatClientForStream(){
        return ChatClient
                .builder(chatmodel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(conversationSession.getChatMemory()),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    private ChatClient createNewChatClient(){
        return ChatClient
                .builder(chatmodel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(conversationSession.getChatMemory()),
                        new CaptureMemoryAdvisor(  vectorStore, chatmodel, executor, localeUtils),
//                        new QuestionAnswerAdvisor(      // TODO  this is an advisor to be used when you need RAG
//                                vectorStore,            //  KEEP IN mind that if we use this for all DEFAULT requests, it will only use what it knows in the "context", and it will not use its whole knowledge..
//                                SearchRequest.defaults().withSimilarityThreshold(.8)
//                        ),
                        new SimpleLoggerAdvisor()
                        )
//                .defaultSystem()        // conversationSession.promptResource()
                .build();
    }

    private Flux<String> respondToUserMessageStream(String userMessage) {

        var chatResponse = createNewChatClientForStream()
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

    private ResultsResponse respondToUserMessage(String userMessage) {

        var chatResponse = createNewChatClient()
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
                // todo it would be nice to have this response formatted ...
//                .system("Please generate the response inside HTML code. Your response must not start with ```html and must not contain \\n or other newline characters. The response must be valid HTML code and must have a div as top element.")
                .user(userMessage)
                .call().content();

        return new ResultsResponse(chatResponse);
    }


}
