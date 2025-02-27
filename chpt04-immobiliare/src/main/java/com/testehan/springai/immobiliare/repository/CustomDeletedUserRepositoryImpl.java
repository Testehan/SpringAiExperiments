package com.testehan.springai.immobiliare.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.testehan.springai.immobiliare.model.auth.DeletedUser;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

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

        var formattedDateCustom = FormattingUtil.getFormattedDateCustom(date);

        Bson deleteOlderEntries = Filters.lt("deletionDate", formattedDateCustom);

        listings.deleteMany(deleteOlderEntries);
    }


}
