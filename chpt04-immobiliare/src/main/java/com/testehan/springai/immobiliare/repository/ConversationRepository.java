package com.testehan.springai.immobiliare.repository;

import java.util.List;

public interface ConversationRepository {
    void deleteUserConversation(String user);

    List<String> getUserConversation(String user);

    void addContentToConversation(String user, String content);
}
