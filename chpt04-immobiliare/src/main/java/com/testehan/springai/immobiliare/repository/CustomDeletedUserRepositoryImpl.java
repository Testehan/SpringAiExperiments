package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.auth.DeletedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class CustomDeletedUserRepositoryImpl implements CustomDeletedUserRepository{

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomDeletedUserRepositoryImpl.class);

    private final MongoTemplate mongoTemplate;

    public CustomDeletedUserRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void deleteDeletedUsers(LocalDateTime date) {
        Query query = new Query(Criteria.where("deletionDate").lt(date));
        var deleteResult = mongoTemplate.remove(query, DeletedUser.class);

        if (deleteResult.getDeletedCount() > 0){
            LOGGER.info("Successfully deleted {} users", deleteResult.getDeletedCount());
        } else {
            LOGGER.info("No user found for deletion before : {}", date);
        }
    }


}
