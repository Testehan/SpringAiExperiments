package com.testehan.springai.ex06;

import org.springframework.ai.chat.ChatClient;
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
    // i think that in the case of an actual usecase, one would need multiple vectore stores,
    // as in a vector store per city-type-type...for ex "london-apartment-rents" or "london-house-sells"
    private final VectorStore immobiliareVectorStore;

    @Value("classpath:/prompts/rag-prompt-template.txt")
    private Resource ragPromptTemplate;

    public ImmobiliareRagController(ChatClient chatClient, VectorStore immobiliareVectorStore) {
        this.chatClient = chatClient;
        this.immobiliareVectorStore = immobiliareVectorStore;
    }

    @GetMapping("/api/immobiliare")
    public String faq(@RequestParam(value = "message", defaultValue = "What are some apartments for sale in Marasti?") String message) {
        List<Document> similarDocuments = immobiliareVectorStore.similaritySearch(SearchRequest.query(message).withTopK(2));
        List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();

        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input", message);
        promptParameters.put("documents", String.join("\n", contentList));
        Prompt prompt = promptTemplate.create(promptParameters);

        return chatClient.call(prompt).getResult().getOutput().getContent();
    }
}
