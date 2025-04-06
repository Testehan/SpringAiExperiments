package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.CachedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class CustomCachedResponseRepositoryImpl implements CustomCachedResponseRepository{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void decreaseFieldByOne(String id, String fieldName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));

        Update update = new Update();
        update.inc(fieldName, -1); // Decrease the field by 1 using dynamic field name

        mongoTemplate.updateFirst(query, update, CachedResponse.class);
    }
}
