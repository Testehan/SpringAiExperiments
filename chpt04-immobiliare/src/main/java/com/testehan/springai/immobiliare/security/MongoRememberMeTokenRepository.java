package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.model.auth.PersistentRememberToken;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
public class MongoRememberMeTokenRepository implements PersistentTokenRepository {

    public static final String PERSISTENT_REMEMBER_TOKEN_COLLECTION = "persistent_remember_token";
    private final MongoTemplate mongoTemplate;

    public MongoRememberMeTokenRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {

        var persistentRememberToken = new PersistentRememberToken(token.getUsername(), token.getSeries(), token.getTokenValue(), token.getDate());
        mongoTemplate.save(persistentRememberToken, "persistent_remember_token");
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        // Query the database to find the existing token by series
        Query query = new Query(Criteria.where("series").is(series));

        // Update the tokenValue and lastUsed fields
        Update update = new Update()
                .set("tokenValue", tokenValue)
                .set("date", lastUsed);

        // Perform the update operation
        mongoTemplate.updateFirst(query, update, PersistentRememberToken.class, "persistent_remember_token");
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        PersistentRememberToken token = mongoTemplate.findOne(
                query(where("series").is(seriesId)),
                PersistentRememberToken.class, PERSISTENT_REMEMBER_TOKEN_COLLECTION
        );

        // If token is found, map it to PersistentRememberMeToken
        if (token != null) {
            return new PersistentRememberMeToken(
                    token.getUsername(),
                    token.getSeries(),
                    token.getTokenValue(),
                    token.getDate()
            );
        }

        // If no token is found, return null
        return null;
    }

    @Override
    public void removeUserTokens(String username) {
        mongoTemplate.remove(query(where("username").is(username)), "persistent_remember_token");
    }
}
