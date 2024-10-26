package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ResultsResponse;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.ApiService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final UserService userService;
    private final ApiService apiService;
    private final SpringWebFluxTemplateEngine templateEngine;


    public ApartmentController(ApartmentService apartmentService, UserService userService, ApiService apiService, SpringWebFluxTemplateEngine templateEngine)
    {
        this.apartmentService = apartmentService;
        this.userService = userService;
        this.apiService = apiService;
        this.templateEngine = templateEngine;
    }

    @PostMapping("/save")
    public String saveApartment(Apartment apartment, Authentication authentication, RedirectAttributes redirectAttributes,
                                @RequestParam(value="apartmentImages", required = false) MultipartFile[] apartmentImages) throws IOException {

        String userEmail = ((OAuth2AuthenticatedPrincipal)authentication.getPrincipal()).getAttribute("email");

        var user = userService.getImmobiliareUserByEmail(userEmail);
        if ((apartment.getId() != null && apartment.getId().toString() != null && !user.getListedProperties().contains(apartment.getId().toString())) && !user.isAdmin()){ // make sure that only owners can edit the ap
            redirectAttributes.addFlashAttribute("errorMessage","ERROR: You can't make this edit!");
            return "redirect:/error";
        }

        if (user.getMaxNumberOfListedProperties() > 0){
            apartmentService.saveApartmentAndImages(apartment, apartmentImages, user);

            return "redirect:/add";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage","ERROR: You have reached the maximum number of listed apartments!");
            return "redirect:/error";
        }
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> streamApartments() {

        return apiService.getApartmentsFlux()
                .map(this::renderResponseFragment);

    }

    private String renderResponseFragment(ResultsResponse resultsResponse){
        Context context = new Context();
        Set<String> selectors = new HashSet<>();
        selectors.add("responseFragmentWithApartments");
        context.setVariable("response",resultsResponse.message());
        context.setVariable("apartments", resultsResponse.apartments());
        return templateEngine.process("response",selectors, context).
                replaceAll("[\\n\\r]+", "");    // because we don't want our result to contain new lines
    }


}
