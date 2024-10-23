package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.constants.AmazonS3Constants;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.util.AmazonS3Util;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApartmentService {

    @Value("classpath:/prompts/system/PictureMetadataGeneration.txt")
    private Resource systemPictureMetadataGeneration;
    @Value("classpath:/prompts/user/PictureMetadataGeneration.txt")
    private Resource userPictureMetadataGeneration;

    private final ApartmentsRepository apartmentsRepository;
    private final OpenAiService embedder;
    private final ChatModel chatModel ;
    private final UserService userService;

    public ApartmentService(ApartmentsRepository apartmentsRepository, OpenAiService embedder, ChatModel chatModel, UserService userService) {
        this.apartmentsRepository = apartmentsRepository;
        this.embedder = embedder;
        this.chatModel = chatModel;
        this.userService = userService;
    }

    public List<Apartment> getApartmentsSemanticSearch(PropertyType propertyType, String city, Apartment apartment, String apartmentDescription) {
        var embedding = embedder.createEmbedding(apartmentDescription).block();
        return apartmentsRepository.findApartmentsByVector(propertyType, city, apartment, embedding);
    }

    public Apartment findApartmentById(String apartmentId) {
        return apartmentsRepository.findApartmentById(apartmentId);
    }

    public List<Apartment> findApartmentsByIds(List<String> apartmentIds){
        List<Apartment> apartments = new ArrayList<>();
        for (String apartmentId : apartmentIds){
            apartments.add(findApartmentById(apartmentId));
        }
        return apartments;
    }

    public void saveApartment(Apartment apartment){
        apartmentsRepository.saveApartment(apartment);
    }

    public void saveApartmentAndImages(Apartment apartment, MultipartFile[] apartmentImages, ImmobiliareUser user) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateCustom = now.format(customFormatter);

        apartment.setCreationDateTime(formattedDateCustom);
        apartment.setLastUpdateDateTime(formattedDateCustom);

        saveApartment(apartment);
        saveUploadedImages(apartment, apartmentImages);
        generateImageMetadata(apartment, apartmentImages);

        var apartmentInfoToEmbed = apartment.getApartmentInfoToEmbedd();
        var mono = embedder.createEmbedding(apartmentInfoToEmbed);
        List<Double> embeddings = mono.block();
        System.out.println(embeddings.stream().map( d -> d.toString()).collect(Collectors.joining(" ")));
        apartment.setPlot_embedding(embeddings);

        saveApartment(apartment);

        user.setMaxNumberOfListedProperties(user.getMaxNumberOfListedProperties() - 1);
        userService.updateUser(user);
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

    private void generateImageMetadata(Apartment apartment, MultipartFile[] apartmentImages) throws IOException {
        if (apartmentImages.length>0) {

            ChatClient chatClient = ChatClient.builder(chatModel).build();
            ChatClient.ChatClientRequestSpec chatClientRequest = chatClient.prompt();
            StringBuilder stringBuilder = new StringBuilder();

            for (MultipartFile image : apartmentImages) {
                if (image.isEmpty()) continue;

                Resource imageResource = new InputStreamResource(image.getInputStream());
                Message userMessage = new UserMessage(
                        userPictureMetadataGeneration.getContentAsString(Charset.defaultCharset()),
                        List.of(new Media(MimeTypeUtils.parseMimeType(image.getContentType()), imageResource))
                );
                Message systemMessage = new SystemMessage(systemPictureMetadataGeneration.getContentAsString(Charset.defaultCharset()));
                chatClientRequest.messages(List.of(systemMessage, userMessage));
                Map<String, Object> result = chatClientRequest.call().entity(new ParameterizedTypeReference<>() {
                });
                stringBuilder.append(result.get("description"));
            }
            apartment.setImagesGeneratedDescription(stringBuilder.toString());
        }
    }

}
