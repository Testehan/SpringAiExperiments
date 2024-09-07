package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.advisor.CaptureMemoryAdvisor;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.RestCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import jakarta.servlet.http.HttpSession;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static com.testehan.springai.immobiliare.constants.PromptConstants.*;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;


@Service
public class ApiServiceImpl implements ApiService{

    @Autowired
    private ImmobiliareApiService immobiliareApiService;

    @Autowired
    private ApartmentService apartmentService;

    @Autowired
    private HttpSession session;

    @Autowired
    private ChatModel chatmodel;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private Executor executor;

    @Autowired
    private ConversationSession conversationSession;

    @Override
    public ResultsResponse getChatResponse(String message) {
        RestCall restCall = immobiliareApiService.whichApiToCall(message);

        // TODO Yeah i know this is ugly...but i have to figure out a better way of keeping track of session data when
        // making another rest call, or some other approach, as when i make a rest call from the code, the session
        // in the endpoint will be different, and so the values set above will not be present
        // Once security part is introduced in the app, this will be handled :
        // https://stackoverflow.com/questions/76590383/how-to-configure-resttemplate-to-use-browsers-session-for-api-call
        switch (restCall.apiCall()) {
            case "/getRentOrBuy" : { return setRentOrBuy(restCall);}
            case "/getCity" : { return setCity(restCall); }
            case "/restart" : { return restartConversation(); }
            case "/apartments/getApartments" :{ return getApartments(message); }
            case "/default" : return respondToUserMessage(conversationSession,message);
        }

        return new ResultsResponse(M00_IRELEVANT_PROMPT, new ArrayList<>());
    }

    private ResultsResponse getApartments(String message) {
        var apartmentDescription = immobiliareApiService.extractApartmentInformationFromProvidedDescription(message);

        var rentOrSale = (String) session.getAttribute("rentOrSale");
        var city = (String) session.getAttribute("city");
        var apartments = apartmentService.getApartmentsSemanticSearch(PropertyType.valueOf(rentOrSale), city,apartmentDescription, message);

        ResultsResponse response;

        if (apartments.size() > 0) {
            response = new ResultsResponse(M04_APARTMENTS_FOUND, apartments);
        } else {
            response = new ResultsResponse(M04_NO_APARTMENTS_FOUND, new ArrayList<>());
        }

        return response;
    }

    // TODO this should also REMOVE from vectore store all information related to the user
    private ResultsResponse restartConversation() {
        session.setAttribute("rentOrSale", "");
        session.setAttribute("city", "");
        return new ResultsResponse(M01_INITIAL_MESSAGE, new ArrayList<>());

    }

    private ResultsResponse setCity(RestCall restCall) {
        session.setAttribute("city", restCall.message());
        return new ResultsResponse(M03_DETAILS, new ArrayList<>());
    }

    private ResultsResponse setRentOrBuy(RestCall restCall) {
        session.setAttribute("rentOrSale", restCall.message());
        return new ResultsResponse(M02_CITY, new ArrayList<>());
    }

    private ChatClient createNewChatClient(ConversationSession conversationSession){
        return ChatClient
                .builder(chatmodel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(conversationSession.getChatMemory()),
                        new CaptureMemoryAdvisor(  vectorStore, chatmodel, executor),
                        new QuestionAnswerAdvisor(
                                vectorStore,
                                SearchRequest.defaults().withSimilarityThreshold(.8)
                        ),
                        new SimpleLoggerAdvisor()
                        )
//                .defaultSystem()        // conversationSession.promptResource()
                .build();
    }

    private ResultsResponse respondToUserMessage(ConversationSession conversationSession, String userMessage) {

        val chatResponse = createNewChatClient(conversationSession)
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
