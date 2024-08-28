package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final OpenAiService openAiService;

    private final ApartmentService apartmentService;
    private final UserService userService;

    public ApartmentController(OpenAiService openAiService, ApartmentService apartmentService, UserService userService)
    {
        this.openAiService = openAiService;
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    @PostMapping("/save")
    public String saveApartment(Apartment apartment, Authentication authentication, RedirectAttributes redirectAttributes){

        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");

        var user = userService.getImmobiliareUserByEmail(userEmail);
        if (user.getMaxNumberOfListedApartments() > 0){
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateCustom = now.format(customFormatter);

            apartment.setCreationDateTime(formattedDateCustom);
            apartment.setLastUpdateDateTime(formattedDateCustom);
            var apartmentInfoToEmbed = apartment.getApartmentInfoToEmbedd();

            var mono = openAiService.createEmbedding(apartmentInfoToEmbed);
            List<Double> embeddings = mono.block();
            System.out.println(embeddings.stream().map( d -> d.toString()).collect(Collectors.joining(" ")));
            apartment.setPlot_embedding(embeddings);

            // TODO Add pictures to an apartment
            apartmentService.saveApartment(apartment);

            user.setMaxNumberOfListedApartments(user.getMaxNumberOfListedApartments() - 1);
            userService.updateUser(user);

            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage","ERROR: You have reached the maximum number of listed apartments!");
            return "redirect:/error";
        }


    }


}
