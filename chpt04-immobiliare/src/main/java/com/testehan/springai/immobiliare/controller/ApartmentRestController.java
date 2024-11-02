package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentRestController.class);

    private final OpenAiService openAiService;

    private final ApartmentService apartmentService;
    private final UserService userService;

    public ApartmentRestController(OpenAiService openAiService, ApartmentService apartmentService, UserService userService)
    {
        this.openAiService = openAiService;
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    @GetMapping("/contact/{apartmentId}")
    @HxRequest
    public String contact(@PathVariable(value = "apartmentId") String apartmentId) {
        var apartmentOptional = apartmentService.findApartmentById(apartmentId);
        if (!apartmentOptional.isEmpty()) {
            return "Contact: " + apartmentOptional.get().getContact();
        } else {
            LOGGER.error("No apartment with id {} was found" , apartmentId);
            return "No apartment found!";
        }
    }

    @GetMapping("/favourite/{apartmentId}")
    @HxRequest
    public String favourite(@PathVariable(value = "apartmentId") String apartmentId, Authentication authentication) {
        String result;
        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");

        var user = userService.getImmobiliareUserByEmail(userEmail);
        if (!user.getFavouriteProperties().contains(apartmentId)){
            user.getFavouriteProperties().add(apartmentId);
            result = "&hearts;";
        } else {
            user.getFavouriteProperties().remove(apartmentId);
            result = "Save to Favourites";
        }
        userService.updateUser(user);
        return result;
    }

}
