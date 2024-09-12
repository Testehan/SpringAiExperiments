package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.repository.ConversationRepository;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public void deleteConversation(String user){
        conversationRepository.deleteUserConversation(user);
    }
}
