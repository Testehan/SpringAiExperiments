package com.testehan.springai.immobiliare.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationRepository {
    void deleteUserConversation(String user);
    void cleanConversationHistoryOlderThan(LocalDateTime date);

    List<String> getUserConversation(String user);

    void addContentToConversation(String user, String content);
}
