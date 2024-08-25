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
import org.springframework.stereotype.Repository;

@Repository
public class ImmobiliareUserRepositoryImpl implements ImmobiliareUserRepository{

    private MongoDatabase mongoDatabase;

    public ImmobiliareUserRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    @Override
    public ImmobiliareUser findUserByEmail(String email) {
        var mongoCollection = mongoDatabase.getCollection("users", ImmobiliareUser.class);
        var customer = mongoCollection.find(new Document("email", email)).first();

        if (customer != null) {
            return customer;
        } else {
            return null;
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
                .append("authenticationType", user.getAuthenticationType());

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
                        .append("authenticationType", user.getAuthenticationType())
                        .append("favourites",user.getFavourites())
                        .append("maxNumberOfListedApartments",user.getMaxNumberOfListedApartments()));

        UpdateResult result = collection.updateOne(filter, update);
    }
}
