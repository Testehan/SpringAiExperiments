package com.testehan.springai.immobiliare.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ImmobiliareUserRepositoryImpl implements ImmobiliareUserRepository{

    private static final Logger LOGGER = LoggerFactory.getLogger(ImmobiliareUserRepositoryImpl.class);

    private MongoDatabase mongoDatabase;

    public ImmobiliareUserRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    @Override
    public Optional<ImmobiliareUser> findUserByEmail(String email) {
        var mongoCollection = mongoDatabase.getCollection("users", ImmobiliareUser.class);
        var customer = mongoCollection.find(new Document("email", email)).first();

        if (customer != null) {
            return Optional.of(customer);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void updateAuthenticationType(ObjectId id, AuthenticationType authenticationType) {
        MongoCollection<Document> collection = mongoDatabase.getCollection("users");
        UpdateResult result = collection.updateOne(
                Filters.eq("_id", id),
                Updates.set("authenticationType", authenticationType)
        );
    }

    @Override
    public void save(ImmobiliareUser user) {
        MongoCollection<Document> collection = mongoDatabase.getCollection("users");
        // Create a document
        Document document = new Document("name", user.getName())
                .append("email", user.getEmail())
                .append("password", user.getPassword())
                .append("refreshToken", user.getRefreshToken())
                .append("authenticationType", user.getAuthenticationType())
                .append("favouriteProperties",user.getFavouriteProperties())
                .append("maxNumberOfListedProperties",user.getMaxNumberOfListedProperties())
                .append("listedProperties",user.getListedProperties())
                .append("city", user.getCity())
                .append("propertyType",user.getPropertyType())
                .append("lastPropertyDescription",user.getLastPropertyDescription())
                .append("searchesAvailable",user.getSearchesAvailable())
                .append("isAdmin",user.getIsAdmin());

        // Insert the document
        collection.insertOne(document);
    }

    @Override
    public void update(ImmobiliareUser user) {
        MongoCollection<Document> collection = mongoDatabase.getCollection("users");

        // Filter to find the document to update
        Document filter = new Document("_id", user.getId());

        // Update multiple fields
        Document update = new Document("$set",
                new Document("name", user.getName())
                        .append("email", user.getEmail())
                        .append("password", user.getPassword())
                        .append("refreshToken", user.getRefreshToken())
                        .append("authenticationType", user.getAuthenticationType())
                        .append("favouriteProperties",user.getFavouriteProperties())
                        .append("maxNumberOfListedProperties",user.getMaxNumberOfListedProperties())
                        .append("listedProperties",user.getListedProperties())
                        .append("city", user.getCity())
                        .append("propertyType",user.getPropertyType())
                        .append("lastPropertyDescription",user.getLastPropertyDescription())
                        .append("searchesAvailable",user.getSearchesAvailable())
                        .append("isAdmin",user.getIsAdmin())
                        .append("gdprConsent",user.getGdprConsent())
                        .append("gdprTimestamp",user.getGdprTimestamp())
        );

        UpdateResult result = collection.updateOne(filter, update);
    }

    public void deleteById(final ObjectId id) {

        var mongoCollection = mongoDatabase.getCollection("users");
        var deleteResult = mongoCollection.deleteOne(new Document("_id", id));

        if (deleteResult.getDeletedCount() > 0) {
            LOGGER.info("Successfully deleted user with id: {}", id);
        } else {
            LOGGER.warn("No user found for deletion having id: {}", id);
        }
    }

    @Override
    public void resetSearchesAvailable() {
        MongoCollection<Document> collection = mongoDatabase.getCollection("users");
       // should update all users that are not admin
        Document filter = new Document("isAdmin", "false");

        Document update = new Document("$set", new Document("searchesAvailable",10));

        collection.updateMany(filter,update);

    }
}
