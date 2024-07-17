package com.testehan.springai.immobiliare.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.Apartments;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.util.Strings;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ChatClient chatClient;
    private final OpenAiService openAiService;

    private final ApartmentService apartmentService;

    public ApartmentController(ChatClient chatClient, OpenAiService openAiService, ApartmentService apartmentService)
    {
        this.chatClient = chatClient;
        this.openAiService = openAiService;
        this.apartmentService = apartmentService;
    }

    @GetMapping("/getApartments")
    public List<Apartment> getApartments(@RequestParam(value = "message") String message, HttpSession session) {
        var rentOrSale = (String) session.getAttribute("rentOrSale");
        var city = session.getAttribute("city");

        return apartmentService.getApartmentsSemanticSearch(PropertyType.valueOf(rentOrSale), message);
    }

    @GetMapping("/getApartmentsForSale")
    public String apartmentsForSale(@RequestParam(value = "message") String message) {

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

    // the service will be used to create the embeddings for the apartments
    @GetMapping("/getEmbedding")
    public String getEmbedding(@RequestParam(value = "message") String message) {
        var mono = openAiService.createEmbedding(message);
        return mono.block().stream().map( d -> d.toString()).collect(Collectors.joining(" "));
    }


    @Value("classpath:/docs/immobiliare.json")
    private Resource immobiliareData;

    @Autowired
    private MongoDatabase mongoDatabase;

    // todo this is temprorary here because i want to trigger the execution of the code when rest call is made
    @GetMapping("/testEmbeddings")
    public String testEmbeddings() {

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Apartment> apartments = mapper.readValue(new File(immobiliareData.getURI()), new TypeReference<List<Apartment>>() {});

            MongoCollection<Document> collection = mongoDatabase.getCollection("apartments");

            for (Document document : collection.find()){
                ObjectId id = document.getObjectId("_id");
                String json = document.toJson();
                // Create Gson instance
                Gson gson = new Gson();
                // Convert JSON string to POJO
                Apartment apartment = gson.fromJson(json, Apartment.class);

                var apartmentInfoToEmbedd = apartment.getApartmentInfoToEmbedd();
                System.out.println(apartmentInfoToEmbedd);

                var mono = openAiService.createEmbedding(apartmentInfoToEmbedd);
                List<Double> embeddings = mono.block();
                System.out.println(embeddings.stream().map( d -> d.toString()).collect(Collectors.joining(" ")));

                UpdateResult result =collection.updateOne(
                        Filters.eq("_id", id),
                        Updates.set("plot_embedding", embeddings)
                );

                System.out.println("Modified elements " + result.getModifiedCount());
                System.out.println("Matched elements " + result.getMatchedCount());

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return "success";
    }
}
