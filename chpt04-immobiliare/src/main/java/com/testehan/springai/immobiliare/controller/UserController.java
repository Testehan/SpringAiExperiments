package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.SupportedCity;
import com.testehan.springai.immobiliare.model.auth.UserProfile;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;


@Controller
@RequestMapping("/api/user")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final ApartmentService apartmentService;
    private final ConversationSession conversationSession;

    public UserController(UserService userService,ApartmentService apartmentService, ConversationSession conversationSession) {
        this.userService = userService;
        this.apartmentService = apartmentService;
        this.conversationSession = conversationSession;
    }

    @PostMapping("/save")
    public String saveUserProfile(UserProfile userProfile)throws IOException {
        SupportedCity supportedCity = SupportedCity.getByName(userProfile.city());
        conversationSession.setCity(supportedCity);
        conversationSession.setRentOrSale(userProfile.propertyType());
        conversationSession.setLastPropertyDescription(userProfile.lastPropertyDescription());

        return "redirect:/profile";

    }

    @PostMapping("/delete")
    public String deleteUserAccount(@RequestParam String confirmDeletionEmail, Model model)throws IOException {
        var userFoundFromEmail = userService.getImmobiliareUserByEmail(confirmDeletionEmail);
        var loggedInUser = conversationSession.getImmobiliareUser();

        if (userFoundFromEmail.isEmpty()){
            // this means that the email provided by the user was not found in the DB.
            LOGGER.warn("User with email {} tried to delete user with nonexistent email {}",loggedInUser.getEmail(), confirmDeletionEmail);
            model.addAttribute("errorMessage", "Something went wrong. You can contact me and i will try to investigate the issue ASAP.");
            return "error";
        }

        if (loggedInUser.getEmail().equalsIgnoreCase(userFoundFromEmail.get().getEmail())){
            // do deletion of listed properties, conversations and the user
            apartmentService.deleteApartmentsByIds(loggedInUser.getListedProperties());
            conversationSession.clearConversation();
            userService.deleteUser(loggedInUser);

        } else {
            LOGGER.warn("User with email {} tried to delete user with email {}",loggedInUser.getEmail(), userFoundFromEmail.get().getEmail());
            model.addAttribute("errorMessage", "Something went wrong. You can contact me and i will try to investigate the issue ASAP.");
            return "error";
        }

        return "redirect:/logout";

    }
}
