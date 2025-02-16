package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.TextEmbedding;
import com.testehan.springai.immobiliare.repository.TextEmbeddingRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
public class EmbeddingService {

    private TextEmbeddingRepository embeddingRepository;

    private OpenAiService openAiService;

    public EmbeddingService(TextEmbeddingRepository embeddingRepository, OpenAiService openAiService) {
        this.embeddingRepository = embeddingRepository;
        this.openAiService = openAiService;
    }

    // Hash function for text
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
        var textHash = hashText(text);

        // Check if embedding exists
        Optional<TextEmbedding> existing = embeddingRepository.findById(textHash);
        if (existing.isPresent()) {
            return existing.get().getEmbedding();
        }

        // If not found, compute new embedding
        List<Double> newEmbedding = openAiService.createEmbedding(text).block();

        // Save it to DB
        var textEmbedding = new TextEmbedding(textHash, text, newEmbedding);
        embeddingRepository.save(textEmbedding);

        return newEmbedding;
    }
}
