package com.testehan.springai.immobiliare.repository;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.search.VectorSearchOptions;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.PropertyType;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.vectorSearch;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.search.SearchPath.fieldPath;
import static com.mongodb.client.model.search.VectorSearchOptions.vectorSearchOptions;
import static java.util.Arrays.asList;

@Repository
public class ApartmentsRepositoryImpl implements ApartmentsRepository{

    private static final double PERCENTAGE_INTERVAL = 0.1;  //10%

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentsRepositoryImpl.class);

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
    public List<Apartment> findApartmentsByVector(PropertyType propertyType, String city, ApartmentDescription apartment, List<Double> embedding) {
        String indexName = "vector_index";
        int numCandidates = 100;  // how many neighbours it will use when doing the nearest neighbour search; it should be
        // higher than the limit we set below...higher numbers of this variable will
        // provide higher accuracy, but it will also give some latency hits..
        int limit = 50;


        // TODO if you want to add more filters, remember that you must modify the existing index in MongoAtlas, and add the
        // fields there.
        // also you can see here all sorts of filering options :
        // https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/

        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("propertyType", propertyType));
        filters.add(Filters.eq("active", true));
        if (Objects.nonNull(city)){
            filters.add(Filters.eq("city", city));
        }

        // optional filters depending on user input
        if (Objects.nonNull(apartment.getMinimumSurface()) && apartment.getMinimumSurface()>0){
            filters.add(Filters.and(Filters.gte("surface", apartment.getMinimumSurface())));
        }
        if (Objects.nonNull(apartment.getMaximumSurface()) && apartment.getMaximumSurface()>0){
            filters.add(Filters.and(Filters.lte("surface", apartment.getMaximumSurface())));
        }

        if (Objects.nonNull(apartment.getMinimumPrice()) && apartment.getMinimumPrice()>0){
            filters.add(Filters.and(Filters.gte("price", apartment.getMinimumPrice())));
        }
        if (Objects.nonNull(apartment.getMaximumPrice()) && apartment.getMaximumPrice()>0){
            filters.add(Filters.and(Filters.lte("price", apartment.getMaximumPrice())));
        }

        if (Objects.nonNull(apartment.getMinimumNumberOfRooms()) && apartment.getMinimumNumberOfRooms()>0){
            filters.add(Filters.and(Filters.gte("noOfRooms", apartment.getMinimumNumberOfRooms())));
        }
        if (Objects.nonNull(apartment.getMaximumNumberOfRooms()) && apartment.getMaximumNumberOfRooms()>0){
            filters.add(Filters.and(Filters.lte("noOfRooms", apartment.getMaximumNumberOfRooms())));
        }

        Bson combinedFilters = filters.isEmpty() ? new Document() : Filters.and(filters);

        LOGGER.info("Using MongoDB Filter : {}", combinedFilters.toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry()).toJson());

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
    public Optional<Apartment> findApartmentById(final String apartmentId) {

        var mongoCollection = mongoDatabase.getCollection("apartments", Apartment.class);
        Apartment apartment;
        try {
            ObjectId objectId = new ObjectId(apartmentId);
            apartment = mongoCollection.find(new Document("_id", objectId)).first();
        } catch (IllegalArgumentException ex){
            LOGGER.error("Apartment id {} is not valid.", apartmentId);
            return Optional.empty();
        }

        if (apartment != null) {
            return Optional.of(apartment);
        } else {
            LOGGER.error("Apartment with id {} was not found.", apartmentId);
            return Optional.empty();
        }
    }

    @Override
    public List<Apartment> findAll() {
        List<Apartment> apartments = new ArrayList<>();
        getApartmentCollection().find().forEach(a -> apartments.add(a));
        return apartments;
    }

    @Override
    public List<Apartment> findByLastUpdateDateTimeBefore(LocalDateTime date) {
        var listings = mongoDatabase.getCollection("apartments", Apartment.class);

        var formattedDateCustom = getFormattedDateCustom(date);
        Bson condition1 = Filters.lt("lastUpdateDateTime", formattedDateCustom);
        Bson condition2 = Filters.eq("active", true);
        Bson combinedFilter = Filters.and(condition1, condition2);

        List<Apartment> result = new ArrayList<>();
        listings.find(combinedFilter).forEach(listing -> result.add(listing));

        return result;
    }

    private static String getFormattedDateCustom(LocalDateTime date) {
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return date.format(customFormatter);
    }

    @Override
    public void saveApartment(Apartment apartment) {
        mongoTemplate.save(apartment, "apartments");
    }

    @Override
    public void deactivateApartments(LocalDateTime date) {
        var listings = mongoDatabase.getCollection("apartments", Apartment.class);

        var formattedDateCustom = getFormattedDateCustom(date);

        Bson condition1 = Filters.lt("lastUpdateDateTime", formattedDateCustom);
        Bson condition2 = Filters.eq("active", true);
        Bson combinedFilter = Filters.and(condition1, condition2);

        var listOfUpdates = new ArrayList<Bson>();
        listOfUpdates.add(set("active", false));
        listOfUpdates.add(set("noOfFavourite", 0));
        listOfUpdates.add(set("noOfContact", 0));
        listings.updateMany(combinedFilter, listOfUpdates);
    }

    @Override
    public void deleteApartmentsByIds(final List<String> apartmentIds) {
        var mongoCollection = mongoDatabase.getCollection("apartments", Apartment.class);

        // Convert string IDs to ObjectId
        List<ObjectId> objectIds = apartmentIds.stream()
                .map(ObjectId::new)
                .toList();

        // Delete apartments with matching IDs
        var deleteResult = mongoCollection.deleteMany(new Document("_id", new Document("$in", objectIds)));

        // Log the result
        if (deleteResult.getDeletedCount() > 0) {
            LOGGER.info("Deleted {} apartments.", deleteResult.getDeletedCount());
        } else {
            LOGGER.warn("No apartments were deleted. Check if the IDs are correct.");
        }
    }

}
