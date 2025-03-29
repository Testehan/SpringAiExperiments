package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.advisor.CaptureMemoryAdvisor;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.service.*;
import com.testehan.springai.immobiliare.util.ListingUtil;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
public class DefaultHandler implements ApiChatCallHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHandler.class);

    private final ChatModel chatmodel;

    private final VectorStore vectorStore;

    private final Executor executor;

    private final ConversationSession conversationSession;
    private final ConversationService conversationService;
    private final UserSseService userSseService;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;
    private final ListingUtil listingUtil;

    public DefaultHandler(ChatModel chatmodel, VectorStore vectorStore, @Qualifier("applicationTaskExecutor") Executor executor, ConversationSession conversationSession, ConversationService conversationService, UserSseService userSseService, MessageSource messageSource, LocaleUtils localeUtils, ListingUtil listingUtil) {
        this.chatmodel = chatmodel;
        this.vectorStore = vectorStore;
        this.executor = executor;
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
            var userMessage = serviceCall.message();
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
                    .system( localeUtils.getLocalizedPrompt("system_defaultResponses"))
                    .user(userMessage)
                    .call().content();

            return new ResultsResponse(chatResponse);
        } catch (Exception e) {
            // Catch-all for unexpected errors
            LOGGER.error("Unexpected error while calling LLM: {}", e.getMessage(), e);
            return new ResultsResponse(messageSource.getMessage("chat.exception", null, localeUtils.getCurrentLocale()));
        }
    }

    @Override
    public ApiCall getApiCall() {
        return ApiCall.DEFAULT;
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
}
