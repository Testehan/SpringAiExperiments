package com.testehan.springai.immobiliare.model;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "contact_attempt_conversation")
public class ContactAttemptConversation {

    private ObjectId id;

    private String waUserId;        // is the same as the user's phone number, in international format (without the + sign)
    private String messageId;      // WhatsApp message ID (e.g., wamid.HBgL...)
    private MessageType direction;   // SENT or RECEIVED
    private String text;
    private String timestamp;

}
