package com.testehan.springai.immobiliare.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<String> getUserConversation(String user) {
        MongoCollection<Document> collection = mongoDatabase.getCollection("conversation_history");
        List<String> conversation = new ArrayList<>();
        collection.find(new Document("metadata.user", user)).forEach(document -> conversation.add(document.get("content").toString()));
        return conversation;
    }
}
