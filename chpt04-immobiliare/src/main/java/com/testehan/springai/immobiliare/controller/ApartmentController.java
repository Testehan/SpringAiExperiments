package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.constants.AmazonS3Constants;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.service.ApartmentService;
import com.testehan.springai.immobiliare.service.OpenAiService;
import com.testehan.springai.immobiliare.util.AmazonS3Util;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
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
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final OpenAiService openAiService;

    private final ApartmentService apartmentService;
    private final UserService userService;
    private final ChatModel chatModel ;

    public ApartmentController(OpenAiService openAiService, ApartmentService apartmentService, UserService userService,
                               ChatModel chatModel)
    {
        this.openAiService = openAiService;
        this.apartmentService = apartmentService;
        this.userService = userService;
        this.chatModel = chatModel;
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
            generateMetadata(apartmentImages);
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

    private Map<String, Object> generateMetadata(MultipartFile[] apartmentImages) throws IOException {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        ChatClient.ChatClientRequestSpec chatClientRequest = chatClient.prompt();
        Resource image = new InputStreamResource(apartmentImages[0].getInputStream());
        Message userMessage = new UserMessage("describe the image", List.of(new Media(MimeTypeUtils.parseMimeType(apartmentImages[0].getContentType()), image)));
        Message systemMessage = new SystemMessage("describe the image");
        chatClientRequest.messages(List.of(systemMessage, userMessage));
        Map<String, Object> result = chatClientRequest.call().entity(new ParameterizedTypeReference<Map<String, Object>>() {});
//        LOG.info("Successfully generated image metadata for content item with id {} and property {}: {}", request.id, request.property, result);
        return result;
    }

}
