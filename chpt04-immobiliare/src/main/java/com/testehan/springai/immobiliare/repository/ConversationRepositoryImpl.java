package com.testehan.springai.immobiliare.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository{

    private final MongoDatabase mongoDatabase;

    public ConversationRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    @Override
    public void deleteUserConversation(String user) {
        MongoCollection<Document> collection = mongoDatabase.getCollection("conversation_history");

        collection.deleteMany(new Document("metadata.user", user));
    }
}
