package com.testehan.springai.prompt;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PromptTemplateController {

    @Value("classpath:/prompts/youtube.txt")
    private Resource youtubeResourceTemplate;

    private final ChatClient chatClient;

    public PromptTemplateController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/api/template-prompt")
    public String findPopularYoutubers(@RequestParam(value = "genre", defaultValue = "music") String genre){

        var message = """
                List the top 10 most popular youtubers in {genre} along with their current subscriber count.
                If you don't know the answer just say "I do not know.
                """;

        PromptTemplate promptTemplate = new PromptTemplate(message);
        Prompt prompt = promptTemplate.create(Map.of("genre",genre));

        return chatClient.call(prompt).getResult().getOutput().getContent();
    }

    @GetMapping("/api/template-from-file-prompt")
    public String findPopularYoutubers2(@RequestParam(value = "genre", defaultValue = "music") String genre){

        PromptTemplate promptTemplate = new PromptTemplate(youtubeResourceTemplate);
        Prompt prompt = promptTemplate.create(Map.of("genre",genre));

        return chatClient.call(prompt).getResult().getOutput().getContent();
    }
}
