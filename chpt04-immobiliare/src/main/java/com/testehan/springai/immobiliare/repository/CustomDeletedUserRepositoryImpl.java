package com.testehan.springai.immobiliare.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.testehan.springai.immobiliare.model.auth.DeletedUser;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Repository
public class CustomDeletedUserRepositoryImpl implements CustomDeletedUserRepository{
    private final MongoDatabase mongoDatabase;

    public CustomDeletedUserRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    private MongoCollection<DeletedUser> getDeletedUsersCollection() {
        return mongoDatabase.getCollection("users_deleted", DeletedUser.class);
    }

    @Override
    public void deleteDeletedUsers(LocalDateTime date) {
        var listings = getDeletedUsersCollection();

        var formattedDateCustom = getFormattedDateCustom(date);

        Bson deleteOlderEntries = Filters.lt("deletionDate", formattedDateCustom);

        listings.deleteMany(deleteOlderEntries);
    }

    // TODO mOVE TO Util class all usages
    private static String getFormattedDateCustom(LocalDateTime date) {
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return date.format(customFormatter);
    }
}
