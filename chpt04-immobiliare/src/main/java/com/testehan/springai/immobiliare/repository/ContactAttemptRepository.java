package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.ContactAttempt;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ContactAttemptRepository extends MongoRepository<ContactAttempt, ObjectId> {

    Optional<ContactAttempt> findByPhoneNumber(String phoneNumber);

    // If you want to find by phoneNumber + listingUrl (optional extra methods)
    Optional<ContactAttempt> findByPhoneNumberAndListingUrl(String phoneNumber, String listingUrl);

}
