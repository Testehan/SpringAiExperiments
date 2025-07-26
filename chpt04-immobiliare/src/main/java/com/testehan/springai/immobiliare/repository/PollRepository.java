package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.Poll;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PollRepository extends MongoRepository<Poll, String> {
}
