package com.testehan.springai.immobiliare.advisor;

import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.security.UserService;
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
    private final UserService userService;
    private String city;
    private String rentOrSale;
    private String lastPropertyDescription;


    public ConversationSession(ChatMemory chatMemory, UserService userService) {
        this.chatMemory = chatMemory;
        this.authentication = SecurityContextHolder.getContext().getAuthentication();
        this.userService = userService;
        this.city = getImmobiliareUser().getCity();
        this.rentOrSale = getImmobiliareUser().getPropertyType();
    }

    public ChatMemory getChatMemory() {
        return chatMemory;
    }

    public String getConversationId(){
        return getImmobiliareUser().getEmail();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        var user = getImmobiliareUser();
        user.setCity(city);
        userService.updateUser(user);
        this.city = city;
    }

    public String getRentOrSale() {
        return rentOrSale;
    }

    public void setRentOrSale(String rentOrSale) {
        var user = getImmobiliareUser();
        user.setPropertyType(rentOrSale);
        userService.updateUser(user);
        this.rentOrSale = rentOrSale;
    }

    public String getLastPropertyDescription() {
        return lastPropertyDescription;
    }

    public void setLastPropertyDescription(String lastPropertyDescription) {
        var user = getImmobiliareUser();
        user.setLastPropertyDescription(lastPropertyDescription);
        userService.updateUser(user);
        this.lastPropertyDescription = lastPropertyDescription;
    }

    public ImmobiliareUser getImmobiliareUser() {
        String userEmail = ((OAuth2AuthenticatedPrincipal) authentication.getPrincipal()).getAttribute("email");
        var user = userService.getImmobiliareUserByEmail(userEmail);
        return user;
    }
}
