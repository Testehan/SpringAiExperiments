package com.testehan.springai.immobiliare.advisor;

import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.security.CustomerUserDetails;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Optional;
import java.util.function.Consumer;

import static com.testehan.springai.immobiliare.model.SupportedCity.UNSUPPORTED;

@Component
@SessionScope
public class ConversationSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationSession.class);

    private final UserService userService;
    private final ConversationService conversationService;


    public ConversationSession(UserService userService, ConversationService conversationService) {
        this.userService = userService;
        this.conversationService = conversationService;
    }

    public String getConversationId(){
        return getImmobiliareUser().orElseThrow(()-> new IllegalStateException("User not found")).getEmail();
    }

    public String getCity() {
        return getImmobiliareUser().orElseThrow(()-> new IllegalStateException("User not found")).getCity();
    }

    public void setCity(String city) {
        setUserFieldAndUpdate(user -> user.setCity(city));
    }

    public void setBudget(String budget){
        setUserFieldAndUpdate(user -> user.setBudget(budget));
    }

    public String getBudget() {
        return getImmobiliareUser().orElseThrow(()-> new IllegalStateException("User not found")).getBudget();
    }

    public String getRentOrSale() {
        return getImmobiliareUser().orElseThrow(()-> new IllegalStateException("User not found")).getPropertyType();
    }

    public void setRentOrSale(String rentOrSale) {
        setUserFieldAndUpdate(user -> user.setPropertyType(rentOrSale));
    }

    public void setLastPropertyDescription(String lastPropertyDescription) {
        setUserFieldAndUpdate(user -> user.setLastPropertyDescription(lastPropertyDescription));
    }

    public Optional<ImmobiliareUser> getImmobiliareUser() {
        String userEmail = "";
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if  (authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal){
            userEmail = ((OAuth2AuthenticatedPrincipal) authentication.getPrincipal()).getAttribute("email");
        }
        if (authentication.getPrincipal() instanceof CustomerUserDetails){
            userEmail = ((CustomerUserDetails) authentication.getPrincipal()).getImmobiliareUser().getEmail();
        }

        return userService.getImmobiliareUserByEmail(userEmail);
    }

    public void clearConversationAndPreferences() {
        setRentOrSale("");
        setCity(UNSUPPORTED.name());
        setBudget("");
        clearChatMemory();
    }

    public void clearChatMemory() {
        conversationService.deleteConversation(getConversationId());
    }

    private void setUserFieldAndUpdate(Consumer<ImmobiliareUser> userSetter) {
        getImmobiliareUser().ifPresent(user -> {
            userSetter.accept(user);
            userService.updateUser(user);
        });
    }

}
