package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.advisor.ConversationSession;
import com.testehan.springai.immobiliare.model.SupportedCity;
import com.testehan.springai.immobiliare.model.auth.UserProfile;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentCrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final ApartmentCrudService apartmentCrudService;
    private final ConversationSession conversationSession;

    public UserController(UserService userService,ApartmentCrudService apartmentCrudService, ConversationSession conversationSession) {
        this.userService = userService;
        this.apartmentCrudService = apartmentCrudService;
        this.conversationSession = conversationSession;
    }

    @PostMapping("/api/user/save")
    public String saveUserProfile(UserProfile userProfile)throws IOException {
        SupportedCity supportedCity = SupportedCity.getByName(userProfile.city());
        conversationSession.setCity(supportedCity.getName());
        conversationSession.setRentOrSale(userProfile.propertyType());
        conversationSession.setBudget(userProfile.budget());
        conversationSession.setLastPropertyDescription(userProfile.lastPropertyDescription());

        return "redirect:/profile";

    }

    @PostMapping("/api/user/delete")
    public String deleteUserAccount(@RequestParam String confirmDeletionEmail, Model model)throws IOException {
        var userFoundFromEmail = userService.getImmobiliareUserByEmail(confirmDeletionEmail);
        var loggedInUser = conversationSession.getImmobiliareUser().get();

        if (userFoundFromEmail.isEmpty()){
            // this means that the email provided by the user was not found in the DB.
            LOGGER.warn("User with email {} tried to delete user with nonexistent email {}",loggedInUser.getEmail(), confirmDeletionEmail);
            model.addAttribute("errorMessage", "Something went wrong. You can contact me and i will try to investigate the issue ASAP.");
            return "error";
        }

        if (loggedInUser.getEmail().equalsIgnoreCase(userFoundFromEmail.get().getEmail())){
            // do deletion of listed properties, conversations and the user
            apartmentCrudService.deleteApartmentsByIds(loggedInUser.getListedProperties());
            conversationSession.clearConversationAndPreferences();
            userService.deleteUser(loggedInUser);

        } else {
            LOGGER.warn("User with email {} tried to delete user with email {}",loggedInUser.getEmail(), userFoundFromEmail.get().getEmail());
            model.addAttribute("errorMessage", "Something went wrong. You can contact me and i will try to investigate the issue ASAP.");
            return "error";
        }

        return "redirect:/";

    }

    @GetMapping("/invite/{inviteUuid}")
    public String invite(@PathVariable(value = "inviteUuid") String inviteUuid) {
        var loggedInUser =conversationSession.getImmobiliareUser().get();
        LOGGER.info("User {} accepted an invitation {} ", loggedInUser.getEmail(), inviteUuid);

        var userOptional = userService.getImmobiliareUserByInviteUuid(inviteUuid);
        if (userOptional.isPresent()){
            var user = userOptional.get();
            if (user.getMaxSearchesAvailable()<25) {        // we don't want abuses :)
                user.setMaxSearchesAvailable(user.getMaxSearchesAvailable() + 1);
                userService.updateUser(user);
            } else {
                LOGGER.info("Invitation limit! More than 15 invitations using {} were accepted.", inviteUuid);
            }
        }

        return "redirect:/";
    }
}
