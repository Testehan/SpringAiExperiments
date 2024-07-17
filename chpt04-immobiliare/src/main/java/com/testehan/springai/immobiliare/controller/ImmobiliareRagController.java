package com.testehan.springai.immobiliare.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ImmobiliareRagController {

    private final ChatClient chatClient;
    private final VectorStore immobiliareVectorStore;

    @Value("classpath:/prompts/rag-prompt-template-apartments-sale.txt")
    private Resource ragPromptTemplate;

    public ImmobiliareRagController(ChatClient chatClient, VectorStore immobiliareVectorStore) {
        this.chatClient = chatClient;
        this.immobiliareVectorStore = immobiliareVectorStore;
    }

    @GetMapping("/api/immobiliare")
    public String faq(HttpSession session,
                      @RequestParam(value = "message", defaultValue = "What are some apartments for sale in Marasti?") String message) {

        ChatResponse assistantResponse;

        List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(SearchRequest.query(message).withTopK(2));
        List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();

        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input", message);
        promptParameters.put("documents", String.join("\n", contentList));
        promptParameters.put("userEmail","dante@yahoo.com");
        Prompt prompt = promptTemplate.create(promptParameters);

        assistantResponse = chatClient.prompt()
                .user(prompt.getContents())
                .functions("emailApartmentsFunction")
                .call().chatResponse();

        return assistantResponse.getResult().getOutput().getContent();
    }
}
