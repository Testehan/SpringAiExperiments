package com.testehan.springai.prompt;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimplePromptController {

    private final ChatClient chatClient;

    public SimplePromptController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/api/simple-prompt")
    public String generate(){
        return chatClient.call(new Prompt("Give me a random fact about the Earth")).getResult().getOutput().getContent();
    }
}
