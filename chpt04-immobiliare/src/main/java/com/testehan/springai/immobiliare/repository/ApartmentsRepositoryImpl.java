package com.testehan.springai.immobiliare.repository;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.search.VectorSearchOptions;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thymeleaf.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.*;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.search.SearchPath.fieldPath;
import static com.mongodb.client.model.search.VectorSearchOptions.vectorSearchOptions;

@Repository
public class ApartmentsRepositoryImpl implements ApartmentsRepository{

    private static final double PERCENTAGE_INTERVAL = 0.1;  //10%

    private static final List<String> ALLOWED_SORTING_FIELDS = List.of("price", "surface","creationDateTime","lastUpdateDateTime");

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentsRepositoryImpl.class);

    private final MongoDatabase mongoDatabase;
    private final MongoTemplate mongoTemplate;
    private final FormattingUtil formattingUtil;

    public ApartmentsRepositoryImpl(MongoDatabase mongoDatabase, MongoTemplate mongoTemplate, FormattingUtil formattingUtil) {
        this.mongoDatabase = mongoDatabase;
        this.mongoTemplate = mongoTemplate;
        this.formattingUtil = formattingUtil;
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
        int limit = 8;


        // TODO if you want to add more filters, remember that you must modify the existing index in MongoAtlas, and add the
        // fields there.
        // also you can see here all sorts of filering options :
        // https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/

        List<Bson> filters = new ArrayList<>();
        filters.add(eq("propertyType", propertyType));
        filters.add(eq("active", true));
        if (Objects.nonNull(city)){
            filters.add(eq("city", city));
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

        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(
                vectorSearch(
                        fieldPath("plot_embedding"),
                        embedding,
                        indexName,
                        numCandidates,
                        limit,
                        options));
        pipeline.add(project(fields( exclude("plot_embedding"), metaVectorSearchScore("score"))));


        // Apply sorting only if it's present
        if (!StringUtils.isEmpty(apartment.getSortingField())) {
            if (ALLOWED_SORTING_FIELDS.contains(apartment.getSortingField())) {
                Bson sortCriteria = apartment.isAscending() ? ascending(apartment.getSortingField()) : descending(apartment.getSortingField());
                pipeline.add(sort(orderBy(sortCriteria)));
                LOGGER.info("Sorting field {} ", apartment.getSortingField());
            } else {
                LOGGER.error("Sorting field {} is invalid", apartment.getSortingField());
            }
        }

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
    public Optional<Apartment> findApartmentByContact(String phoneNumber) {
        var mongoCollection = mongoDatabase.getCollection("apartments", Apartment.class);
        Apartment apartment;
        try {
            apartment = mongoCollection.find(new Document("contact", phoneNumber)).first();
        } catch (IllegalArgumentException ex){
            LOGGER.error("PhoneNumber {} is not valid.", phoneNumber);
            return Optional.empty();
        }

        if (apartment != null) {
            return Optional.of(apartment);
        } else {
            LOGGER.error("PhoneNumber {} was not found in apartment list.", phoneNumber);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> findApartmentIdBySocialId(final String socialId) {

        var mongoCollection = mongoDatabase.getCollection("apartments");
        String apartmentId = null;
        try {

            Document projection = new Document("_id", 1);
            Document result = mongoCollection
                    .find(new Document("socialId", socialId))
                    .projection(projection)
                    .first();
            if (result != null) {
                ObjectId objectId = result.getObjectId("_id"); // Extract ObjectId properly
                apartmentId = objectId.toHexString(); // Convert ObjectId to String
            }
            
        } catch (IllegalArgumentException ex){
            LOGGER.error("Social id {} is not valid.", socialId);
            return Optional.empty();
        }

        if (apartmentId != null) {
            return Optional.of(apartmentId);
        } else {
            LOGGER.error("Social id {} was not found.", socialId);
            return Optional.empty();
        }
    }

    @Override
    public boolean isPhoneValid(String phoneNumber) {
        var mongoCollection = mongoDatabase.getCollection("apartments", Apartment.class);
        Bson filter = eq("contact", phoneNumber); // Create the filter

        long count = mongoCollection.countDocuments(filter); // Count matching documents

        return count < 1; // Return true if no documents exist

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

        var formattedDateCustom = formattingUtil.getFormattedDateCustom(date);
        Bson condition1 = Filters.lt("lastUpdateDateTime", formattedDateCustom);
        Bson condition2 = eq("active", true);

        // Additional filter for reactivatedMessageId: must be null, empty, or not present
        Bson condition3 = Filters.or(
                Filters.eq("reactivateMessageId", null), // check if it's null
                Filters.eq("reactivateMessageId", ""),  // check if it's an empty string
                Filters.exists("reactivateMessageId", false)  // check if the field does not exist
        );

        Bson combinedFilter = Filters.and(condition1, condition2, condition3);

        List<Apartment> result = new ArrayList<>();
        listings.find(combinedFilter).forEach(listing -> result.add(listing));

        return result;
    }

    @Override
    public Apartment saveApartment(Apartment apartment) {
        return mongoTemplate.save(apartment, "apartments");
    }

    @Override
    public List<String> deactivateApartments(LocalDateTime date) {
        var listings = mongoDatabase.getCollection("apartments", Apartment.class);

        var formattedDateCustom = formattingUtil.getFormattedDateCustom(date);

        Bson condition1 = Filters.lt("lastUpdateDateTime", formattedDateCustom);
        Bson condition2 = eq("active", true);
        Bson combinedFilter = Filters.and(condition1, condition2);

        // Step 1: Find the listings that will be deactivated
        List<String> phoneNumbers = listings.find(combinedFilter)
                .projection(Projections.include("contact")) // only fetch phone numbers
                .map(listing -> listing.getContact()) // if Apartment.class has a getPhoneNumber()
                .into(new ArrayList<>());

        var listOfUpdates = new ArrayList<Bson>();
        listOfUpdates.add(set("active", false));
        listOfUpdates.add(set("noOfFavourite", 0));
        listOfUpdates.add(set("noOfContact", 0));
        listings.updateMany(combinedFilter, listOfUpdates);

        return phoneNumbers;
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
            LOGGER.warn("No apartments were deleted for Ids {}. Check if the IDs are correct.", apartmentIds.stream().collect(Collectors.joining()));
        }
    }

    @Override
    public Page<Apartment> searchApartment(String search, String cityFilter, String propertyTypeFilter, Integer minPrice, Integer maxPrice, String sortBy, String sortDir, int page, int size) {
        Query query = new Query();

        // Search by name, city, or area (case-insensitive regex)
        if (search != null && !search.isEmpty()) {
            String regexPattern = ".*" + search + ".*";
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("name").regex(regexPattern, "i"),
                    Criteria.where("city").regex(regexPattern, "i"),
                    Criteria.where("area").regex(regexPattern, "i")
            );
            query.addCriteria(searchCriteria);
        }

        // Filter by city (case-insensitive)
        if (cityFilter != null && !cityFilter.isEmpty()) {
            query.addCriteria(Criteria.where("city").regex(cityFilter, "i"));
        }

        // Filter by propertyType (assuming PropertyType is an enum stored as string)
        if (propertyTypeFilter != null && !propertyTypeFilter.isEmpty()) {
            query.addCriteria(Criteria.where("propertyType").is(propertyTypeFilter));
        }

        // Price range filter
        if (minPrice != null) {
            query.addCriteria(Criteria.where("price").gte(minPrice));
        }
        if (maxPrice != null) {
            query.addCriteria(Criteria.where("price").lte(maxPrice));
        }

        // Only active listings
        query.addCriteria(Criteria.where("active").is(true));

        // Sorting
        Sort sort = Sort.by(sortBy != null ? sortBy : "creationDateTime");
        sort = "asc".equalsIgnoreCase(sortDir) ? sort.ascending() : sort.descending();
        query.with(sort);

        // Pagination
        long total = mongoTemplate.count(query, Apartment.class);
        query.skip((long) page * size).limit(size);

        List<Apartment> apartments = mongoTemplate.find(query, Apartment.class);
        Pageable pageable = PageRequest.of(page, size, sort);
        return new PageImpl<>(apartments, pageable, total);

    }

}
