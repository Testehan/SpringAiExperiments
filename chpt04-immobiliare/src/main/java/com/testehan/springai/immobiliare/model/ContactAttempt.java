package com.testehan.springai.immobiliare.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "contact_attempt")
public class ContactAttempt {

    private ObjectId id;
    @Indexed //tells MongoDB to automatically create an index on that field, which makes queries faster
    private String phoneNumber;
    private String listingUrl;
    private ContactStatus status;
    private Long createdAt;

    public ContactAttempt() {
        this.createdAt = System.currentTimeMillis();
    }

}
