package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.ListingStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ListingStatisticsRepository extends MongoRepository<ListingStatistics, String> {

    Optional<ListingStatistics> findByInputHash(String inputHash);

    Optional<List<ListingStatistics>> findByCityAndPropertyType(String city, String propertyType);
}
