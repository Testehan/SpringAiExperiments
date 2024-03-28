package com.testehan.springai.chat;

import org.springframework.ai.chat.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/api/generate")
    public Map generate(@RequestParam(value = "message", defaultValue = "Tell me a Short fact about Romania") String message){
        return Map.of("generated", chatClient.call(message));
    }
}
