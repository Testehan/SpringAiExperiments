package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.advisor.CaptureMemoryAdvisor;
import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
public class ChatClientService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final Executor executor;
    private final ConversationService conversationService;
    private final ConversationSession conversationSession;
    private final LocaleUtils localeUtils;
    private final FormattingUtil formattingUtil;


    public ChatClientService(ChatModel chatModel, VectorStore vectorStore, @Qualifier("applicationTaskExecutor") Executor executor, ConversationService conversationService, ConversationSession conversationSession, LocaleUtils localeUtils, FormattingUtil formattingUtil) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.executor = executor;
        this.conversationService = conversationService;
        this.conversationSession = conversationSession;
        this.localeUtils = localeUtils;
        this.formattingUtil = formattingUtil;
    }

    public ConversationSession getConversationSession() {
        return conversationSession;
    }

    public ChatClient createChatClient() {
        ChatMemory chatMemoryForUser = conversationService.getChatMemoryForUser(conversationSession.getConversationId());

        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemoryForUser),
                        new CaptureMemoryAdvisor(vectorStore, chatModel, executor, localeUtils, formattingUtil),
//                        new QuestionAnswerAdvisor(      // TODO  this is an advisor to be used when you need RAG
//                                vectorStore,            //  KEEP IN mind that if we use this for all DEFAULT requests, it will only use what it knows in the "context", and it will not use its whole knowledge..
//                                SearchRequest.defaults().withSimilarityThreshold(.8)
//                        ),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}
