package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final UserService userService;


    public ApartmentController(ApartmentService apartmentService, UserService userService,
                               ChatModel chatModel)
    {
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    @PostMapping("/save")
    public String saveApartment(Apartment apartment, Authentication authentication, RedirectAttributes redirectAttributes,
                                @RequestParam(value="apartmentImages", required = false) MultipartFile[] apartmentImages) throws IOException {

        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");

        var user = userService.getImmobiliareUserByEmail(userEmail);
        if (user.getMaxNumberOfListedApartments() > 0){
            apartmentService.saveApartmentAndImages(apartment, apartmentImages, user);

            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage","ERROR: You have reached the maximum number of listed apartments!");
            return "redirect:/error";
        }
    }



}
