package com.testehan.springai.immobiliare.advisor;

import com.testehan.springai.immobiliare.model.SupportedCity;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.security.CustomerUserDetails;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ConversationService;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.List;
import java.util.stream.Collectors;

import static com.testehan.springai.immobiliare.model.SupportedCity.UNSUPPORTED;

@Component
@SessionScope
public class ConversationSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationSession.class);

    private ChatMemory chatMemory;
    private Authentication authentication;
    private final UserService userService;
    private final ConversationService conversationService;
    private SupportedCity city;
    private String rentOrSale;
    private String lastPropertyDescription;


    public ConversationSession(ChatMemory chatMemory, UserService userService, ConversationService conversationService) {
        this.authentication = SecurityContextHolder.getContext().getAuthentication();
        this.userService = userService;
        var user = getImmobiliareUser();
        this.city = user != null ?
                StringUtils.isNotEmpty(user.getCity()) ?
                        SupportedCity.getByName(user.getCity()) : SupportedCity.UNSUPPORTED
                : SupportedCity.UNSUPPORTED;
        this.rentOrSale = user != null ? user.getPropertyType() : null;
        this.chatMemory = chatMemory;
        this.conversationService = conversationService;
    }

    private void initializeChatMemory() {
        var conversation = conversationService.getUserConversation(getConversationId()) ;
        List<Message> messages = conversation.stream().map(message -> new UserMessage(message)).collect(Collectors.toList());
        chatMemory.add(getConversationId(),messages);
    }

    public ChatMemory getChatMemory() {
        initializeChatMemory();
        return chatMemory;
    }

    public String getConversationId(){
        return getImmobiliareUser().getEmail();
    }

    public String getCity() {
        return city.getName();
    }

    public void setCity(String city) {
        var user = getImmobiliareUser();
        user.setCity(city);
        userService.updateUser(user);
        this.city = SupportedCity.getByName(city);
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
        String userEmail = "";
        if  (authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal){
            userEmail = ((OAuth2AuthenticatedPrincipal) authentication.getPrincipal()).getAttribute("email");
        }
        if (authentication.getPrincipal() instanceof CustomerUserDetails){
            userEmail = ((CustomerUserDetails) authentication.getPrincipal()).getImmobiliareUser().getEmail();
        }

        var user = userService.getImmobiliareUserByEmail(userEmail);
        return user.get();
    }

    public void clearConversation() {
        setRentOrSale("");
        setCity(UNSUPPORTED.name());
        clearChatMemory();
        conversationService.deleteConversation(getConversationId());
    }

    public void clearChatMemory() {
        getChatMemory().clear(getConversationId());
    }

}
