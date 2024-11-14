package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public void deleteConversation(String user){
        conversationRepository.deleteUserConversation(user);
    }

    public List<String> getUserConversation(String user){
       return conversationRepository.getUserConversation(user);
    }

    public void addContentToConversation(String content, String user){
        conversationRepository.addContentToConversation(user,content);
    }


}
