package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.City;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends MongoRepository<City, ObjectId> {

    Optional<City> findByName(String name);

    Optional<City> findBySlug(String slug);

    List<City> findByIsEnabledTrue();
}
