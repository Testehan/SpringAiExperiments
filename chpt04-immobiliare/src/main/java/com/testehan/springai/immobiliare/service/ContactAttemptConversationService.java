package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.ContactAttemptConversation;
import com.testehan.springai.immobiliare.model.MessageType;
import com.testehan.springai.immobiliare.repository.ContactAttemptConversationRepository;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ContactAttemptConversationService {

    private final ContactAttemptConversationRepository contactAttemptConversationRepository;
    private final FormattingUtil formattingUtil;

    public ContactAttemptConversationService(ContactAttemptConversationRepository contactAttemptConversationRepository, FormattingUtil formattingUtil) {
        this.contactAttemptConversationRepository = contactAttemptConversationRepository;
        this.formattingUtil = formattingUtil;
    }

    public void saveConversationTextMessage(String waUserId, String messageId, String text, MessageType direction) {
        if (!contactAttemptConversationRepository.existsByMessageId(messageId)) {
            ContactAttemptConversation message = new ContactAttemptConversation();
            message.setWaUserId(waUserId);
            message.setMessageId(messageId);
            message.setDirection(direction);
            message.setText(text);
            message.setTimestamp(formattingUtil.getFormattedDateCustom(LocalDateTime.now()));

            contactAttemptConversationRepository.save(message);
        }
    }
}
