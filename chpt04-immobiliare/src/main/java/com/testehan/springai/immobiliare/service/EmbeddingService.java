package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.TextEmbedding;
import com.testehan.springai.immobiliare.repository.TextEmbeddingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
public class EmbeddingService {

    private final TextEmbeddingRepository embeddingRepository;
    private final OpenAiService openAiService;
    private final MongoTemplate mongoTemplate;

    public EmbeddingService(TextEmbeddingRepository embeddingRepository, OpenAiService openAiService, MongoTemplate mongoTemplate) {
        this.embeddingRepository = embeddingRepository;
        this.openAiService = openAiService;
        this.mongoTemplate = mongoTemplate;
    }

    // Instead of searching by text, we hash the text and use it as _id.
    // MongoDB automatically indexes _id, making lookups very fast.
    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing text", e);
        }
    }

    public List<Double> getOrComputeEmbedding(String text) {
        var hashedText = hashText(text);

        // Check if embedding exists
        Optional<TextEmbedding> existing = embeddingRepository.findById(hashedText);
        if (existing.isPresent()) {
            incrementUsageCount(hashedText);
            return existing.get().getEmbedding();
        }

        // If not found, compute new embedding
        List<Double> newEmbedding = openAiService.createEmbedding(text).block();

        // Save it to DB
        var textEmbedding = new TextEmbedding(hashedText, text, newEmbedding);
        embeddingRepository.save(textEmbedding);

        return newEmbedding;
    }

    public List<TextEmbedding> getTopEmbeddingsByUsageCount() {
        Query query = new Query()
                .with(Sort.by(Sort.Order.desc("usageCount")))
                .limit(3);

        return mongoTemplate.find(query, TextEmbedding.class);
    }

    private void incrementUsageCount(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().inc("usageCount", 1);
        mongoTemplate.updateFirst(query, update, TextEmbedding.class);
    }
}
