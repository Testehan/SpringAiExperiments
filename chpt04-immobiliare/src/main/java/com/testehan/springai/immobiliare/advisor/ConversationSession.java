package com.testehan.springai.immobiliare.advisor;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.List;

@Component
@SessionScope
public class ConversationSession {

    private ChatMemory chatMemory;
    private Authentication authentication;

    public ConversationSession(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.authentication = SecurityContextHolder.getContext().getAuthentication();
    }

    // TODO Is this actually used ?
    public List<Message> messages() {
        return chatMemory.get(getConversationId(), 100);
    }

    public ChatMemory getChatMemory() {
        return chatMemory;
    }

    public String getConversationId(){
        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");
        return userEmail;
    }
}
