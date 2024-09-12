package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.advisor.CaptureMemoryAdvisor;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
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

    public ApiServiceImpl(ImmobiliareApiService immobiliareApiService, ApartmentService apartmentService, ChatModel chatmodel, VectorStore vectorStore, Executor executor, ConversationSession conversationSession, ConversationService conversationService) {
        this.immobiliareApiService = immobiliareApiService;
        this.apartmentService = apartmentService;
        this.chatmodel = chatmodel;
        this.vectorStore = vectorStore;
        this.executor = executor;
        this.conversationSession = conversationSession;
        this.conversationService = conversationService;
    }

    @Override
    public ResultsResponse getChatResponse(String message) {
        var serviceCall = immobiliareApiService.whichApiToCall(message);

        switch (serviceCall.apiCall()) {
            case SET_RENT_OR_BUY : { return setRentOrBuy(serviceCall);}
            case SET_CITY : { return setCity(serviceCall); }
            case GET_APARTMENTS:{ return getApartments(message); }
            case RESTART_CONVERSATION : { return restartConversation(); }
            case DEFAULT : return respondToUserMessage(message);
        }

        return new ResultsResponse(M00_IRELEVANT_PROMPT, new ArrayList<>());
    }

    private ResultsResponse getApartments(String message) {
        var apartmentDescription = immobiliareApiService.extractApartmentInformationFromProvidedDescription(message);

        var rentOrSale = conversationSession.getRentOrSale();
        var city = conversationSession.getCity();
        var apartments = apartmentService.getApartmentsSemanticSearch(PropertyType.valueOf(rentOrSale), city,apartmentDescription, message);

        ResultsResponse response;

        if (apartments.size() > 0) {
            var apartmentsFoundMessage = getApartmentsFoundMessage(apartments);
            response = new ResultsResponse(apartmentsFoundMessage, apartments);
        } else {
            response = new ResultsResponse(M04_NO_APARTMENTS_FOUND, new ArrayList<>());
        }

        return response;
    }

    private String getApartmentsFoundMessage(List<Apartment> apartments) {
        var resource = new ClassPathResource("prompts/apartments_found.txt");

        var promptTemplate = new PromptTemplate(resource);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("apartmentsFound", formatApartmentsFound(apartments));
        var prompt = promptTemplate.create(promptParameters);

        return respondToUserMessage(prompt.getContents()).message();

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
        return new ResultsResponse(M01_INITIAL_MESSAGE, new ArrayList<>());

    }

    private ResultsResponse setCity(ServiceCall serviceCall) {
        conversationSession.setCity(serviceCall.message());
        return new ResultsResponse(M03_DETAILS, new ArrayList<>());
    }

    private ResultsResponse setRentOrBuy(ServiceCall serviceCall) {
        conversationSession.setRentOrSale(serviceCall.message());
        return new ResultsResponse(M02_CITY, new ArrayList<>());
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
                .call()
                .chatResponse();

        return new ResultsResponse(chatResponse.getResult().getOutput().getContent(), new ArrayList<>());
    }


}
