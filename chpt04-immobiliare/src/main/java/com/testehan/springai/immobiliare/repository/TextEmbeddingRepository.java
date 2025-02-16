package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.TextEmbedding;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TextEmbeddingRepository extends MongoRepository<TextEmbedding, String> {
}
