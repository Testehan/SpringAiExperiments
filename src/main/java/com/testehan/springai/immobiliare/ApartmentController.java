package com.testehan.springai.immobiliare;

import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ChatClient chatClient;

    public ApartmentController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping()
    public Apartments getApartmentsByLocation(@RequestParam(value = "location", required = false) String location) {
        if (!Strings.isBlank(location)) {

            var outputParser = new BeanOutputParser<>(Apartments.class);
            String format = outputParser.getFormat();
            System.out.println("format = " + format);

            var promptMessage = """
                    Generate a list of apartments from location {location}.
                    {format}
                    """;

            PromptTemplate promptTemplate = new PromptTemplate(promptMessage, Map.of("location", location, "format", format));
            Prompt prompt = promptTemplate.create();

            Generation generation = chatClient.call(prompt).getResult();
            Apartments apartments = outputParser.parse(generation.getOutput().getContent());
            return apartments;
        } else {
            return new Apartments(new ArrayList<>());
        }
    }

    @GetMapping("/location")
    public String weather(@RequestParam(value = "message") String message) {

        PromptTemplate promptTemplate = new PromptTemplate("What's the weather like in {message} ?");
        Map<String,Object> map  = new HashMap<>();
        map.put("message", message);
        Prompt prompt = promptTemplate.create(map);

        UserMessage userMessage = new UserMessage(prompt.getContents());

        ChatResponse response = chatClient.call(new Prompt(List.of(userMessage),
                OpenAiChatOptions.builder().withFunction("apartmentsFunction").build()));

        return response.getResult().getOutput().getContent();
    }

    @GetMapping("/sale")
    public Apartments apartmentsForSale(@RequestParam(value = "message") String message) {

        if (!Strings.isBlank(message)) {

            var outputParser = new BeanOutputParser<>(Apartments.class);
            String format = outputParser.getFormat();
            System.out.println("format = " + format);

            var promptMessage = """
                    Generate a list of apartments for sale based on the following information: {message}.
                    {format}
                    """;

            PromptTemplate promptTemplate = new PromptTemplate(promptMessage, Map.of("message", message, "format", format));
            Prompt prompt = promptTemplate.create();
            UserMessage userMessage = new UserMessage(prompt.getContents());

            Generation generation = chatClient.call(new Prompt(List.of(userMessage),
                    OpenAiChatOptions.builder().withFunction("apartmentsSaleFunction").build())).getResult();
            Apartments apartments = outputParser.parse(generation.getOutput().getContent());
            return apartments;
        } else {
            return new Apartments(new ArrayList<>());
        }


    }

}
