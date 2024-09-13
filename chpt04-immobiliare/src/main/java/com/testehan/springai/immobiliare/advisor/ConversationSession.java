package com.testehan.springai.immobiliare.advisor;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class ConversationSession {

    private ChatMemory chatMemory;
    private Authentication authentication;
    private String city;
    private String rentOrSale;


    public ConversationSession(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.authentication = SecurityContextHolder.getContext().getAuthentication();
    }

    public ChatMemory getChatMemory() {
        return chatMemory;
    }

    public String getConversationId(){
        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");
        return userEmail;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRentOrSale() {
        return rentOrSale;
    }

    public void setRentOrSale(String rentOrSale) {
        this.rentOrSale = rentOrSale;
    }
}
