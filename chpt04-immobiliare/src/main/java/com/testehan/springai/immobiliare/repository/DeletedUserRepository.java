package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.auth.DeletedUser;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeletedUserRepository extends MongoRepository<DeletedUser, String>, CustomDeletedUserRepository {
}
