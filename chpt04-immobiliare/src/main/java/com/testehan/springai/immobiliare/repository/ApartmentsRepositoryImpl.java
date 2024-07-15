package com.testehan.springai.immobiliare.repository;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.testehan.springai.immobiliare.model.Apartment;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.vectorSearch;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.search.SearchPath.fieldPath;
import static java.util.Arrays.asList;

@Repository
public class ApartmentsRepositoryImpl implements ApartmentsRepository{

    private final MongoDatabase mongoDatabase;

    public ApartmentsRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    private MongoCollection<Apartment> getApartmentCollection() {
        return mongoDatabase.getCollection("apartments", Apartment.class);
    }

    @Override
    public List<Apartment> findApartmentsByVector(List<Double> embedding) {
        String indexName = "vector_index";
        int numCandidates = 100;  // how many neighbours it will use when doing the nearest neighbour search; it should be
        // higher than the limit we set below...higher numbers of this variable will
        // provide higher accuracy, but it will also give some latency hits..
        int limit = 5;

        // right now we only include the vector search, but you can include more (i guess more fields to verify etc)
        // but this will take longer to compute and it will get you better results
        List<Bson> pipeline = asList(
                vectorSearch(
                        fieldPath("plot_embedding"),
                        embedding,
                        indexName,
                        numCandidates,
                        limit),
                project(
                        fields(exclude("_id"), include("name"), include("shortDescription"),
                                metaVectorSearchScore("score"))));

        getApartmentCollection().aggregate(pipeline).forEach(doc -> System.out.println(doc.toString()));

        List<Apartment> apartments = new ArrayList<>();
        Gson gson = new Gson();
        for (Bson b : pipeline){
            String json = b.toBsonDocument().toJson();
            Apartment apartment = gson.fromJson(json, Apartment.class);
            apartments.add(apartment);
        }

        return apartments;
    }
}
