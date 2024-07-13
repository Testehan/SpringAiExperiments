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

import java.util.Map;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ChatClient chatClient;

    public ApartmentController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/getApartmentsForSale")
    public String apartmentsForSale(HttpSession session, @RequestParam(value = "message") String message) {

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
            return apartments.toString();
        } else {
            return "No apartments were found for the given search criteria";
        }

    }

    // TODO right now, in the ApiDescription file this endpoint is not described..all apartment descriptions go to
    // the apartmentsForSale method
    @GetMapping("/getApartmentsForRent")
    public String apartmentsForRent(@RequestParam(value = "message") String message) {
        return "No apartments for rent for now";
    }

}
