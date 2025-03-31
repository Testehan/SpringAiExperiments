package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.repository.ConversationRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public ChatMemory getChatMemoryForUser(String conversationId) {
        List<String> conversation = getUserConversation(conversationId);
        List<Message> messages = conversation.stream()
                .map(UserMessage::new)
                .collect(Collectors.toList());

        // create a local chatMemory
        ChatMemory localChatMemory = new InMemoryChatMemory();
        localChatMemory.add(conversationId, messages);
        return localChatMemory;
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
