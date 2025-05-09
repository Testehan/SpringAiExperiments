package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.ContactAttemptConversation;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ContactAttemptConversationRepository extends MongoRepository<ContactAttemptConversation, ObjectId> {

    List<ContactAttemptConversation> findByWaUserIdOrderByTimestampDesc(String waUserId);

    boolean existsByMessageId(String messageId);

}
