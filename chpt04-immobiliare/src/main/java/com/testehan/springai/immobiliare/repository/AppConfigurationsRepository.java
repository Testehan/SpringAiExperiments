package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.AppConfigurations;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppConfigurationsRepository extends MongoRepository<AppConfigurations, String> {
}
