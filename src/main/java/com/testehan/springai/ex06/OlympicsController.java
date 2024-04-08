package com.testehan.springai.ex06;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OlympicsController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public OlympicsController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/api/faq")
    public String faq(@RequestParam(value = "message", defaultValue = "How can I buy tickets for the Olympic Games Paris 2024") String message) {
        System.out.println("I am working");

        return "ok";
    }
}
