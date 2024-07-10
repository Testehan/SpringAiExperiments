package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartments;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ChatClient chatClient;

    public ApartmentController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // what i am trying to do with this method is to have the possibility that from one REST endpoint
    // to let the LLM decide which function to call based obviously on the user input
    // TODO Sau continui cu abordarea asta, sau poti vedea alta varianta, in clasa ApiController,
    // unde folosesc un fisier pt a descrie APIs supportate de backend, si in fct de ce scrie userul,
    // se determina care API ar fi mai potrivit.
    @GetMapping("/decide")
    public String decide(@RequestParam(value = "message") String message) {
        Prompt prompt = new Prompt(message);

        ChatResponse response = chatClient.prompt()
                .user(prompt.getContents())
                .functions("apartmentsFunction","apartmentsSaleFunction","emailApartmentsFunction")
                .call().chatResponse();

        return response.getResult().getOutput().getContent();
    }

    @GetMapping("/sale")
    public Apartments apartmentsForSale(HttpSession session, @RequestParam(value = "message") String message) {

        if (!Strings.isBlank(message)) {

            var outputParser = new BeanOutputConverter<>(Apartments.class);
            String format = outputParser.getFormat();
            System.out.println("format = " + format);

            var promptMessage = """
                    Generate a list of apartments for sale based on the following information: {message}.
                    {format}
                    """;

            PromptTemplate promptTemplate = new PromptTemplate(promptMessage, Map.of("message", message, "format", format));
            Prompt prompt = promptTemplate.create();

            ChatResponse response = chatClient.prompt()
                    .user(prompt.getContents())
                    .functions("apartmentsSaleFunction")
                    .call().chatResponse();

            Apartments apartments = outputParser.parse(response.getResult().getOutput().getContent());
            return apartments;
        } else {
            return new Apartments(new ArrayList<>());
        }


    }

}
