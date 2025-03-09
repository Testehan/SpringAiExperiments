package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.CachedResponse;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CachedResponseRepository extends MongoRepository<CachedResponse, String> {

    Optional<CachedResponse> findByInputHash(String inputHash);

}
