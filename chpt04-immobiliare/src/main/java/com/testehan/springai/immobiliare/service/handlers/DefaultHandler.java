package com.testehan.springai.immobiliare.service.handlers;

import com.testehan.springai.immobiliare.model.ApiCall;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.model.ServiceCall;
import com.testehan.springai.immobiliare.service.ChatClientService;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
public class DefaultHandler implements ApiChatCallHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHandler.class);

    private final ChatClientService chatClientService;

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public DefaultHandler(ChatClientService chatClientService, MessageSource messageSource, LocaleUtils localeUtils) {
        this.chatClientService = chatClientService;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
    }

    @Override
    public ResultsResponse handle(ServiceCall serviceCall, HttpSession session) {
        try {
            var userMessage = serviceCall.message();
            var chatResponse = callLLM(userMessage);

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

    private String callLLM(String userMessage){
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
                .system(localeUtils.getLocalizedPrompt("system_defaultResponses"))
                .user(userMessage)
                .call().content();
    }

}
