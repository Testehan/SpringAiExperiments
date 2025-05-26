package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.Lead;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends MongoRepository<Lead, ObjectId> {

    Optional<Lead> findByPhoneNumber(String phoneNumber);

    // If you want to find by phoneNumber + listingUrl (optional extra methods)
    Optional<Lead> findByPhoneNumberAndListingUrl(String phoneNumber, String listingUrl);

    List<Lead> findByListingUrlContaining(String filterUrlValue);

    List<Lead> findByListingUrlContainingAndStatus(String filterUrlValue, String statusValue);

    List<Lead> findByStatus(String statusValue);

    @Query("{ '$or': [ " +
            " { 'phoneNumber': { $regex: ?0, $options: 'i' } }, " +
            " { 'listingUrl':  { $regex: ?0, $options: 'i' } } " +
            "] }")
    Page<Lead> searchLeads(String search, Pageable pageable);
}
