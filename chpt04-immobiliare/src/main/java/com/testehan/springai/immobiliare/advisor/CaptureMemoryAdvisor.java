package com.testehan.springai.immobiliare.advisor;

import com.testehan.springai.immobiliare.util.FormattingUtil;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

public class CaptureMemoryAdvisor implements CallAroundAdvisor {

    public static final int MAX_ATTEMPTS = 3;
    private Logger logger = LoggerFactory.getLogger(CaptureMemoryAdvisor.class);

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final Executor executor;
    private final ChatClient chatClient;
    private final FormattingUtil formattingUtil;

    private RetryTemplate retryTemplate  = new RetryTemplateBuilder().maxAttempts(MAX_ATTEMPTS).fixedBackoff(1000).build();
    private MemoryBasisExtractor lastMessageMemoryBasisExtractor
            = (AdvisedRequest request) -> Collections.singletonList(new UserMessage(request.userText()));

    public CaptureMemoryAdvisor(VectorStore vectorStore, ChatModel chatModel, Executor executor, LocaleUtils localeUtils, FormattingUtil formattingUtil) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.executor = executor;
        this.formattingUtil = formattingUtil;
        var captureMemoryPrompt = localeUtils.getLocalizedPrompt("capture_memory");
        this.chatClient = ChatClient
                .builder(chatModel)
                .defaultSystem(captureMemoryPrompt)
                .build();
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

        var backgroundTask = new Runnable() {
            @Override
            public void run() {
                try {
                    retryTemplate.execute((RetryCallback<Boolean, Throwable>) context -> extractMemoryIfPossible(advisedRequest));

                } catch (Throwable t) {
                    logger.error("We several times to extract a memory but something is not working. ", t);
                }
            }
        };
        executor.execute(backgroundTask);

        return chain.nextAroundCall(advisedRequest);
    }

    private boolean extractMemoryIfPossible(AdvisedRequest request) {
        Map<String, Object> adviseContext = request.adviseContext();

        try {
            logger.info("Trying to extract memory for : {}", lastMessageMemoryBasisExtractor.extract(request));

            var memoryResponse = chatClient
                    .prompt()
                    .messages(lastMessageMemoryBasisExtractor.extract(request))
                    .call()
                    .entity(MemoryResponse.class);

            if (memoryResponse.worthKeeping()) {
                logger.info("Adding memory to vector store: {}", memoryResponse);
                Map<String, Object> metadata = createMetadata(adviseContext);


                List<Document> docs = List.of(
                        new Document(memoryResponse.content(), metadata));
                vectorStore.add(docs);
                return true;
            }

            logger.info("Ignoring useless memory: {}", memoryResponse);
            return false;
        } catch (Exception e) {
            logger.error("Something went wrong when trying to extract a memory : {}", e );
            return false;
        }
    }

    @NotNull
    private Map<String, Object> createMetadata(Map<String, Object> adviseContext) {
        LocalDateTime now = LocalDateTime.now();
        String formattedDateCustom = formattingUtil.getFormattedDateCustom(now);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("user", adviseContext.get(CHAT_MEMORY_CONVERSATION_ID_KEY));
        metadata.put("creationDateTime", formattedDateCustom);

        return metadata;
    }

    // Advisors with lower order values are executed first
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public String getName() {
        return "CaptureMemoryAdvisor";
    }

    private record MemoryResponse(String content, boolean useful){
        public boolean worthKeeping() {
            return useful && content != null;
        }
    }
}

