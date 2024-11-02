package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.advisor.CaptureMemoryAdvisor;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.constants.PromptConstants;
import com.testehan.springai.immobiliare.events.ApartmentPayload;
import com.testehan.springai.immobiliare.events.Event;
import com.testehan.springai.immobiliare.events.ResponsePayload;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.testehan.springai.immobiliare.constants.PromptConstants.*;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;


@Service
public class ApiServiceImpl implements ApiService{

    private ImmobiliareApiService immobiliareApiService;

    private ApartmentService apartmentService;

    private ChatModel chatmodel;

    private VectorStore vectorStore;

    private Executor executor;

    private ConversationSession conversationSession;
    private ConversationService conversationService;
    private UserSseService userSseService;

    public ApiServiceImpl(ImmobiliareApiService immobiliareApiService, ApartmentService apartmentService,
                          ChatModel chatmodel, VectorStore vectorStore, @Qualifier("applicationTaskExecutor") Executor executor,
                          ConversationSession conversationSession, ConversationService conversationService,
                          UserSseService userSseService) {
        this.immobiliareApiService = immobiliareApiService;
        this.apartmentService = apartmentService;
        this.chatmodel = chatmodel;
        this.vectorStore = vectorStore;
        this.executor = executor;
        this.conversationSession = conversationSession;
        this.conversationService = conversationService;
        this.userSseService = userSseService;
    }

    @Override
    public ResultsResponse getChatResponse(String message, HttpSession session) {
        var serviceCall = immobiliareApiService.whichApiToCall(message);

        switch (serviceCall.apiCall()) {
            case SET_RENT_OR_BUY : { return setRentOrBuy(serviceCall);}
            case SET_CITY : { return setCity(serviceCall); }
            case GET_APARTMENTS:{ return getApartments(message, session); }
            case RESTART_CONVERSATION : { return restartConversation(); }
            case DEFAULT : return respondToUserMessage(message);
        }

        return new ResultsResponse(M00_IRRELEVANT_PROMPT);
    }

    private ResultsResponse getApartments(String description, HttpSession session) {

        conversationSession.setLastPropertyDescription(description);
        var apartmentDescription = immobiliareApiService.extractApartmentInformationFromProvidedDescription(description);

        var rentOrSale = conversationSession.getRentOrSale();
        var city = conversationSession.getCity();
        var apartmentsFromSemanticSearch = apartmentService.getApartmentsSemanticSearch(PropertyType.valueOf(rentOrSale), city,apartmentDescription, description);

        ResultsResponse response = new ResultsResponse("");

        if (apartmentsFromSemanticSearch.size() > 0) {
            var bestMatchingApartmentIds = getBestMatchingApartmentIds(apartmentsFromSemanticSearch, description);
            AtomicBoolean isFirst = new AtomicBoolean(true);
            bestMatchingApartmentIds
                    .scan(new StringBuilder(), (acc, next) -> acc.append(next))  // Append characters
                    .filter(buffer -> propertyIdContainsComma(buffer.toString()))
                    .map(buffer-> buffer.toString().split(","))
                    .flatMap(idsArray -> Flux.fromArray(idsArray))
                    .distinct()
                    .subscribe(
                        apId -> {
                           var apartmentLLM = apartmentsFromSemanticSearch.stream()
                                .filter(item -> apId.equals(item.getId().toString()))
                                .findFirst();
                           if (!apartmentLLM.isEmpty()){
                               if (isFirst.getAndSet(false)) {
                                   userSseService.getUserSseConnection(session.getId())
                                           .tryEmitNext(new Event("response",new ResponsePayload(M04_APARTMENTS_FOUND)));
                               }
                               System.out.println("Found apartment id" + apartmentLLM.get().getId());
                               userSseService.getUserSseConnection(session.getId())
                                       .tryEmitNext(new Event("apartment", new ApartmentPayload(apartmentLLM.get())));
                           }

                        },
                        error -> {
                            System.err.println("Error: " + error);
                        },
                        () -> {
                            if (isFirst.get()){     // this means that we processed stream and we got no match
                                userSseService.getUserSseConnection(session.getId())
                                        .tryEmitNext(new Event("response",new ResponsePayload(M04_NO_APARTMENTS_FOUND)));
                            }
                            System.out.println("Flux completed");
                        }
                    );
        } else {
            response = new ResultsResponse(M04_NO_APARTMENTS_FOUND);
        }

        return response;
    }

    private boolean propertyIdContainsComma(String propertyId) {
        return propertyId.contains(",") && propertyId.charAt(propertyId.length() - 1) == ',';
    }

    public Flux<Event> getServerSideEventsFlux(HttpSession session) {
        return  userSseService.getUserSseConnection(session.getId()).asFlux();
    }

    private Flux<String> getBestMatchingApartmentIds(List<Apartment> apartments, String description) {
        var resource = new ClassPathResource("prompts/apartments_found.txt");

        var promptTemplate = new PromptTemplate(resource);
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
        conversationSession.setRentOrSale("");
        conversationSession.setCity("");
        conversationSession.getChatMemory().clear(conversationSession.getConversationId());
        conversationService.deleteConversation(conversationSession.getConversationId());
        return new ResultsResponse(M01_INITIAL_MESSAGE);

    }

    private ResultsResponse setCity(ServiceCall serviceCall) {
        conversationSession.setCity(serviceCall.message());
        var user = conversationSession.getImmobiliareUser();
        return new ResultsResponse(String.format(PromptConstants.M03_DETAILS,user.getPropertyType(), user.getCity()));
    }

    private ResultsResponse setRentOrBuy(ServiceCall serviceCall) {
        conversationSession.setRentOrSale(serviceCall.message());
        return new ResultsResponse(M02_CITY);
    }

    private ChatClient createNewChatClient(){
        return ChatClient
                .builder(chatmodel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(conversationSession.getChatMemory()),
                        new CaptureMemoryAdvisor(  vectorStore, chatmodel, executor),
                        new QuestionAnswerAdvisor(      // this is an advisor to be used when you need RAG
                                vectorStore,
                                SearchRequest.defaults().withSimilarityThreshold(.8)
                        ),
                        new SimpleLoggerAdvisor()
                        )
//                .defaultSystem()        // conversationSession.promptResource()
                .build();
    }

    private Flux<String> respondToUserMessageStream(String userMessage) {

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
            .user(userMessage)
                .stream().content();

        return chatResponse;
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
                .user(userMessage)
                .call().content();

        return new ResultsResponse(chatResponse);
    }


}
