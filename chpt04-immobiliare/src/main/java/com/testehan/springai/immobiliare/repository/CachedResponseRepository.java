package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.CachedResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CachedResponseRepository extends MongoRepository<CachedResponse, String> {

    Optional<CachedResponse> findByInputHash(String inputHash);

    @Transactional
    long deleteByCityAndPropertyType(String city, String propertyType);
}
