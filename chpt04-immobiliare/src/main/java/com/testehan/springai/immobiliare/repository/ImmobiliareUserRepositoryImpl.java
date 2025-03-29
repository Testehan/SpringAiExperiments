package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ImmobiliareUserRepositoryImpl implements ImmobiliareUserRepository{

    private static final Logger LOGGER = LoggerFactory.getLogger(ImmobiliareUserRepositoryImpl.class);

    private final MongoTemplate mongoTemplate;

    public ImmobiliareUserRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<ImmobiliareUser> findUserByEmail(String email) {
        return findUserByField("email", email);
    }

    @Override
    public Optional<ImmobiliareUser> findUserByInviteUuid(String inviteUuid) {
        return findUserByField("inviteUuid", inviteUuid);
    }

    @Override
    public void updateAuthenticationType(ObjectId id, AuthenticationType authenticationType) {
        Query query = createIdQuery(id);
        Update update = new Update().set("authenticationType", authenticationType);
        mongoTemplate.updateFirst(query, update, ImmobiliareUser.class);
    }

    @Override
    public void save(ImmobiliareUser user) {
        mongoTemplate.save(user);
    }

    @Override
    public ImmobiliareUser update(ImmobiliareUser user) {
        return mongoTemplate.save(user);
    }

    @Override
    public void deleteById(final ObjectId id) {

        Query query = createIdQuery(id);
        var deleteResult = mongoTemplate.remove(query, ImmobiliareUser.class);

        if (deleteResult.getDeletedCount() > 0) {
            LOGGER.info("Successfully deleted user with id: {}", id);
        } else {
            LOGGER.warn("No user found for deletion having id: {}", id);
        }
    }

    @Override
    public void resetSearchesAvailable() {
        // Sets searchesAvailable to the value of maxSearchesAvailable if it exists.
        // otherwise it defaults to 10
        Update update = new Update().set("searchesAvailable",
                new Document("$ifNull", List.of("$maxSearchesAvailable", 10)));

        Query query = new Query(Criteria.where("isAdmin").is("false"));
        mongoTemplate.updateMulti(query, update, ImmobiliareUser.class);
    }

    private Optional<ImmobiliareUser> findUserByField(String fieldName, String fieldValue) {
        Query query = new Query(Criteria.where(fieldName).is(fieldValue));
        return Optional.ofNullable(mongoTemplate.findOne(query, ImmobiliareUser.class));
    }

    private Query createIdQuery(ObjectId id) {
        return new Query(Criteria.where("_id").is(id));
    }
}
