package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.LeadConversation;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LeadConversationRepository extends MongoRepository<LeadConversation, ObjectId> {

    List<LeadConversation> findByWaUserIdOrderByTimestampAsc(String waUserId);

    boolean existsByMessageId(String messageId);

}
