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
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
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

    private final MongoTemplate mongoTemplate;

    public ApartmentsRepositoryImpl(MongoDatabase mongoDatabase, MongoTemplate mongoTemplate) {
        this.mongoDatabase = mongoDatabase;
        this.mongoTemplate = mongoTemplate;
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
        if (Objects.nonNull(city)){
            filters.add(Filters.or(Filters.eq("city", city),
                    Filters.eq("city",city.toLowerCase()),
                    Filters.eq("city",city.toUpperCase()),
                    Filters.eq("city", StringUtils.capitalize(city))));
        }

        // optional filters depending on user input
        if (Objects.nonNull(apartment.getSurface()) && apartment.getSurface()>0){
            filters.add(Filters.and(Filters.gte("surface", getMinValue(apartment.getSurface())),
                    Filters.lte("surface", getMaxValue(apartment.getSurface()))));
        }
        if (Objects.nonNull(apartment.getPrice()) && apartment.getPrice()>0){
            filters.add(Filters.and(Filters.gte("price", getMinValue(apartment.getPrice())),
                    Filters.lte("price", getMaxValue(apartment.getPrice()))));
        }

        Bson combinedFilters = filters.isEmpty() ? new Document() : Filters.and(filters);

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

    @Override
    public Apartment findApartmentById(final String apartmentId) {

        var mongoCollection = mongoDatabase.getCollection("apartments", Apartment.class);
        ObjectId objectId = new ObjectId(apartmentId);
        var apartment = mongoCollection.find(new Document("_id", objectId)).first();

        if (apartment != null) {
            return apartment;
        } else {
                throw new NoSuchElementException("Apartment not found");
        }
    }

    @Override
    public List<Apartment> findAll() {
        List<Apartment> apartments = new ArrayList<>();
        getApartmentCollection().find().forEach(a -> apartments.add(a));
        return apartments;
    }

    @Override
    public void saveApartment(Apartment apartment) {
        mongoTemplate.save(apartment, "apartments");
    }

    private static long getMinValue(Integer value) {
        return value - Math.round(value * PERCENTAGE_INTERVAL);
    }

    private static long getMaxValue(Integer value) {
        return value + Math.round(value * PERCENTAGE_INTERVAL);
    }
}
