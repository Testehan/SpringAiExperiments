package com.testehan.springai.immobiliare.repository;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.search.VectorSearchOptions;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.PropertyType;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.vectorSearch;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.search.SearchPath.fieldPath;
import static com.mongodb.client.model.search.VectorSearchOptions.vectorSearchOptions;
import static java.util.Arrays.asList;

@Repository
public class ApartmentsRepositoryImpl implements ApartmentsRepository{

    private static final double PERCENTAGE_INTERVAL = 0.1;  //10%

    private final MongoDatabase mongoDatabase;

    public ApartmentsRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    private MongoCollection<Apartment> getApartmentCollection() {
        return mongoDatabase.getCollection("apartments", Apartment.class);
    }

    @Override
    public List<Apartment> findApartmentsByVector(PropertyType propertyType, String city, Apartment apartment, List<Double> embedding) {
        String indexName = "vector_index";
        int numCandidates = 100;  // how many neighbours it will use when doing the nearest neighbour search; it should be
        // higher than the limit we set below...higher numbers of this variable will
        // provide higher accuracy, but it will also give some latency hits..
        int limit = 5;


        // TODO if you want to add more filters, remember that you must modify the existing index in MongoAtlas, and add the
        // fields there.
        // also you can see here all sorts of filering options :
        // https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/

        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("propertyType", propertyType));
        filters.add(Filters.or(Filters.eq("city", city),
                Filters.eq("city",city.toLowerCase()),
                Filters.eq("city",city.toUpperCase()),
                Filters.eq("city", StringUtils.capitalize(city))));

        // optional filters depending on user input
        if (Objects.nonNull(apartment.surface()) && apartment.surface()>0){
            filters.add(Filters.and(Filters.gte("surface", getMinValue(apartment.surface())),
                    Filters.lte("surface", getMaxValue(apartment.surface()))));
        }
        if (Objects.nonNull(apartment.price()) && apartment.price()>0){
            filters.add(Filters.and(Filters.gte("price", getMinValue(apartment.price())),
                    Filters.lte("price", getMaxValue(apartment.price()))));
        }

        Bson combinedFilters = filters.isEmpty() ? new Document() : Filters.and(filters);

        // todo this is for debugging purposes
        System.out.println("Combined Filter: " + combinedFilters.toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry()).toJson());

        VectorSearchOptions options = vectorSearchOptions().filter(combinedFilters);

        List<Bson> pipeline = asList(
                vectorSearch(
                        fieldPath("plot_embedding"),
                        embedding,
                        indexName,
                        numCandidates,
                        limit,
                        options),

                project(fields( exclude("plot_embedding"), metaVectorSearchScore("score")))
        );

        List<Apartment> apartments = new ArrayList<>();
        getApartmentCollection().aggregate(pipeline).spliterator().forEachRemaining(a->apartments.add(a));

        return apartments;
    }

    private static long getMinValue(Integer value) {
        return value - Math.round(value * PERCENTAGE_INTERVAL);
    }

    private static long getMaxValue(Integer value) {
        return value + Math.round(value * PERCENTAGE_INTERVAL);
    }
}
