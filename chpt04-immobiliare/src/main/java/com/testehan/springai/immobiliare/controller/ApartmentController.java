package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.constants.AmazonS3Constants;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import com.testehan.springai.immobiliare.util.AmazonS3Util;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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

    private final ResourceLoader resourceLoader;

    public ApartmentController(OpenAiService openAiService, ApartmentService apartmentService, UserService userService,
                               ResourceLoader resourceLoader)
    {
        this.openAiService = openAiService;
        this.apartmentService = apartmentService;
        this.userService = userService;
        this.resourceLoader = resourceLoader;
    }

    @PostMapping("/save")
    public String saveApartment(Apartment apartment, Authentication authentication, RedirectAttributes redirectAttributes,
                                @RequestParam(value="apartmentImages", required = false) MultipartFile[] apartmentImages) throws IOException {

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

            apartmentService.saveApartment(apartment);
            saveUploadedImages(apartment, apartmentImages);
            apartmentService.saveApartment(apartment);

            user.setMaxNumberOfListedApartments(user.getMaxNumberOfListedApartments() - 1);
            userService.updateUser(user);

            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage","ERROR: You have reached the maximum number of listed apartments!");
            return "redirect:/error";
        }


    }

    private void saveUploadedImages(Apartment apartment, MultipartFile[] apartmentImages) throws IOException {
        if (apartmentImages.length>0) {
            var uploadDir = "apartment-images/" + apartment.getId();
            for (MultipartFile extraImage : apartmentImages) {
                if (extraImage.isEmpty()) continue;

                String filename = StringUtils.cleanPath(extraImage.getOriginalFilename());
                AmazonS3Util.uploadFile(uploadDir, filename, extraImage.getInputStream());

                apartment.getImages().add(AmazonS3Constants.S3_BASE_URI + "/" + uploadDir + "/" + filename);
            }
        }
    }

}
