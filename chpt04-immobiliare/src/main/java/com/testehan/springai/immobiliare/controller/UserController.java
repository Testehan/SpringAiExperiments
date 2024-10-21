package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.auth.UserProfile;
import com.testehan.springai.immobiliare.security.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;


@Controller
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final ConversationSession conversationSession;

    public UserController(UserService userService, ConversationSession conversationSession) {
        this.userService = userService;
        this.conversationSession = conversationSession;
    }

    @PostMapping("/save")
    public String saveApartment(UserProfile userProfile)throws IOException {
        var user = conversationSession.getImmobiliareUser();
        user.setCity(userProfile.city());
        user.setPropertyType(userProfile.propertyType());
        user.setLastPropertyDescription(userProfile.lastPropertyDescription());
        userService.updateUser(user);

        return "redirect:/profile";

    }
}
