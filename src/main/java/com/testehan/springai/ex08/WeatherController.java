package com.testehan.springai.ex08;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final ChatClient chatClient;

    public WeatherController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/location")
    public String weather(@RequestParam(value = "location", defaultValue = "New York") String location) {

        PromptTemplate promptTemplate = new PromptTemplate("What's the weather like in {location} ?");
        Map<String,Object> map  = new HashMap<>();
        map.put("location", location);
        Prompt prompt = promptTemplate.create(map);

        UserMessage userMessage = new UserMessage(prompt.getContents());

        ChatResponse response = chatClient.call(new Prompt(List.of(userMessage),
                OpenAiChatOptions.builder().withFunction("weatherFunction").build()));

        return response.getResult().getOutput().getContent();
    }
}
