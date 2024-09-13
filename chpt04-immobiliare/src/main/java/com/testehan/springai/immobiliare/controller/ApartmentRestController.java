package com.testehan.springai.immobiliare.controller;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentRestController {

    private final OpenAiService openAiService;

    private final ApartmentService apartmentService;
    private final UserService userService;

    public ApartmentRestController(OpenAiService openAiService, ApartmentService apartmentService, UserService userService)
    {
        this.openAiService = openAiService;
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    @GetMapping("/getContact/{apartmentId}")
    @HxRequest
    public String getContact(@PathVariable(value = "apartmentId") String apartmentId) {
        var apartment = apartmentService.findApartmentById(apartmentId);
        return "Contact: " + apartment.getContact();
    }

    @GetMapping("/favourite/{apartmentId}")
    @HxRequest
    public String favourite(@PathVariable(value = "apartmentId") String apartmentId, Authentication authentication) {
        String result;
        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");

        var user = userService.getImmobiliareUserByEmail(userEmail);
        if (!user.getFavourites().contains(apartmentId)){
            user.getFavourites().add(apartmentId);
            result = "&hearts;";
        } else {
            user.getFavourites().remove(apartmentId);
            result = "Save to Favourites";
        }
        userService.updateUser(user);
        return result;
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

            UpdateResult result = collection.updateOne(
                    Filters.eq("_id", id),
                    Updates.set("plot_embedding", embeddings)
            );

            System.out.println("Modified elements " + result.getModifiedCount());
            System.out.println("Matched elements " + result.getMatchedCount());

        }

        return "success";
    }
}
