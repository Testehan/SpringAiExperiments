package com.testehan.springai.immobiliare.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository{

    private final MongoDatabase mongoDatabase;
    private final VectorStore vectorStore;

    public ConversationRepositoryImpl(MongoDatabase mongoDatabase, VectorStore vectorStore) {
        this.mongoDatabase = mongoDatabase;
        this.vectorStore = vectorStore;
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
        collection.find(new Document("metadata.user", user))
                    .sort(new Document("_id", -1))
                    .limit(30)      // get last 30 msj. otherwise the context gets too big and will not work
                    .forEach(document -> conversation.add(document.get("content").toString()));
        return conversation;
    }

    @Override
    public void addContentToConversation(String user, String content) {
        List<org.springframework.ai.document.Document> docs = List.of(
                new org.springframework.ai.document.Document(content, Map.of("user", user)));
        vectorStore.add(docs);
    }
}
