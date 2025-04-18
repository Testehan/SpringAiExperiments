package com.testehan.springai.immobiliare.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.lt;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository{

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationRepositoryImpl.class);

    private final MongoDatabase mongoDatabase;
    private final VectorStore vectorStore;
    private final FormattingUtil formattingUtil;


    public ConversationRepositoryImpl(MongoDatabase mongoDatabase, VectorStore vectorStore, FormattingUtil formattingUtil) {
        this.mongoDatabase = mongoDatabase;
        this.vectorStore = vectorStore;
        this.formattingUtil = formattingUtil;
    }

    @Override
    public void deleteUserConversation(String user) {
        MongoCollection<Document> collection = mongoDatabase.getCollection("conversation_history");

        collection.deleteMany(new Document("metadata.user", user));
    }

    @Override
    public void cleanConversationHistoryOlderThan(LocalDateTime date) {

        String formattedDateCustom = formattingUtil.getFormattedDateCustom(date);
        Bson filter = lt("metadata.creationDateTime", formattedDateCustom);

        long deletedCount = mongoDatabase.getCollection("conversation_history")
                .deleteMany(filter).getDeletedCount();

        if (deletedCount > 0){
            LOGGER.info("Successfully deleted {} conversation history entries", deletedCount);
        } else {
            LOGGER.info("No conversation history entries found for deletion before : {}", date);
        }
    }

    @Override
    public List<String> getUserConversation(String user) {
        MongoCollection<Document> collection = mongoDatabase.getCollection("conversation_history");
        List<String> conversation = new ArrayList<>();
        collection.find(new Document("metadata.user", user))
                    .sort(new Document("_id", -1))
                    .limit(50)      // get last 50 msj. otherwise the context gets too big and will not work
                    .forEach(document -> conversation.add(document.get("content").toString()));
        return conversation;
    }

    @Override
    public void addContentToConversation(String user, String content) {

        List<String> existingConversation = getUserConversation(user);

        // Check if the content is already present
        if (existingConversation.contains(content)) {
            return; // Skip adding duplicate content
        }

        LocalDateTime now = LocalDateTime.now();
        String formattedDateCustom = formattingUtil.getFormattedDateCustom(now);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("user", user);
        metadata.put("creationDateTime", formattedDateCustom);

        List<org.springframework.ai.document.Document> docs = List.of(
                new org.springframework.ai.document.Document(content, metadata));
        vectorStore.add(docs);
    }
}
