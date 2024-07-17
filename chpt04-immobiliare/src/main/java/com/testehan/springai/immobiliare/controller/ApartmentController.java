package com.testehan.springai.immobiliare.controller;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final OpenAiService openAiService;

    private final ApartmentService apartmentService;

    public ApartmentController(OpenAiService openAiService, ApartmentService apartmentService)
    {
        this.openAiService = openAiService;
        this.apartmentService = apartmentService;
    }

    @GetMapping("/getApartments")
    public List<Apartment> getApartments(@RequestParam(value = "message") String message, HttpSession session) {
        var rentOrSale = (String) session.getAttribute("rentOrSale");
        var city = session.getAttribute("city");

        return apartmentService.getApartmentsSemanticSearch(PropertyType.valueOf(rentOrSale), message);
    }

    // the service will be used to create the embeddings for the apartments
    @GetMapping("/getEmbedding")
    public String getEmbedding(@RequestParam(value = "message") String message) {
        var mono = openAiService.createEmbedding(message);
        return mono.block().stream().map( d -> d.toString()).collect(Collectors.joining(" "));
    }


    @Autowired
    private MongoDatabase mongoDatabase;
    
    @GetMapping("/testEmbeddings")
    public String testEmbeddings() {

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

        return "success";
    }
}
