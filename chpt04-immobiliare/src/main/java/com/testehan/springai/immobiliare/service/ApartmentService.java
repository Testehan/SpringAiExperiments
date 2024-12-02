package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.constants.AmazonS3Constants;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.util.AmazonS3Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ApartmentService {

    @Value("classpath:/prompts/system/PictureMetadataGeneration.txt")
    private Resource systemPictureMetadataGeneration;
    @Value("classpath:/prompts/user/PictureMetadataGeneration.txt")
    private Resource userPictureMetadataGeneration;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentService.class);

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

    public List<Apartment> getApartmentsSemanticSearch(PropertyType propertyType, String city, ApartmentDescription apartment, String apartmentDescription) {
        var embedding = embedder.createEmbedding(apartmentDescription).block();
        return apartmentsRepository.findApartmentsByVector(propertyType, city, apartment, embedding);
    }

    public Optional<Apartment> findApartmentById(String apartmentId) {
        return apartmentsRepository.findApartmentById(apartmentId);
    }

    public List<Apartment> findAll(){
        return apartmentsRepository.findAll();
    }

    public List<Apartment> findApartmentsByIds(List<String> apartmentIds){
        List<Apartment> apartments = new ArrayList<>();
        for (String apartmentId : apartmentIds){
            findApartmentById(apartmentId).ifPresent(apartment -> apartments.add(apartment));
        }
        return apartments;
    }

    public boolean saveApartment(Apartment apartment){
        var isPropertyNew = isPropertyNew(apartment);
        apartmentsRepository.saveApartment(apartment);
        return isPropertyNew;
    }

    public void deleteApartmentsByIds(List<String> apartmentIds){
        apartmentsRepository.deleteApartmentsByIds(apartmentIds);
    }

    @Async
    public void saveApartmentAndImages(Apartment apartment,  List<ApartmentImage> apartmentImages, ImmobiliareUser user) throws IOException {
        apartment.setShortDescription(apartment.getShortDescription().replace("\n", " ")); // no newlines in description

        var isPropertyNew = saveApartment(apartment);

        var imagesWereUploaded = saveUploadedImages(apartment, apartmentImages);
        var imagesWereDeleted = deleteUploadedImages(apartment);
        if (imagesWereUploaded || imagesWereDeleted) {
            generateImageMetadata(apartment);
        }

        var apartmentInfoToEmbed = apartment.getApartmentInfoToEmbedd();
        var mono = embedder.createEmbedding(apartmentInfoToEmbed);
        List<Double> embeddings = mono.block();

        apartment.setPlot_embedding(embeddings);

        saveApartment(apartment);

        if (isPropertyNew) {
            user.getListedProperties().add(apartment.getId().toString());
            user.setMaxNumberOfListedProperties(user.getMaxNumberOfListedProperties() - 1);
            userService.updateUser(user);
        }

        LOGGER.info("Apartment was added with success!");
    }

    private static boolean isPropertyNew(Apartment apartment) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateCustom = now.format(customFormatter);

        var isPropertyNew = false;
        if (Objects.isNull(apartment.getId())) {
            apartment.setCreationDateTime(formattedDateCustom);
            isPropertyNew = true;
        }

        apartment.setLastUpdateDateTime(formattedDateCustom);
        apartment.setActivationToken(UUID.randomUUID().toString());

        return isPropertyNew;
    }

    private boolean deleteUploadedImages(Apartment apartment) {
        boolean imagesWereDeleted = false;
        var uploadDir = "apartment-images/" + apartment.getId().toString();

        List<String> objectKeys = AmazonS3Util.listFolder(uploadDir);
        for (String key : objectKeys){
            int lastIndexOfSlash = key.lastIndexOf("/");
            var filename = key.substring(lastIndexOfSlash + 1);
            if (apartment.getImages().stream().filter(imageURL -> {
                int lastSlash = imageURL.lastIndexOf("/");
                return imageURL.substring(lastSlash+1).equals(filename);
            }).count()<1){
                AmazonS3Util.deleteFile(key);
                imagesWereDeleted = true;
            }
        }

        return imagesWereDeleted;
    }

    private boolean saveUploadedImages(Apartment apartment, List<ApartmentImage> apartmentImages) throws IOException {
        boolean imagesWereUploaded = false;
        if (apartmentImages.size()>0) {
            var uploadDir = "apartment-images/" + apartment.getId();
            for (ApartmentImage extraImage : apartmentImages) {

                var filename = extraImage.name();
                AmazonS3Util.uploadFile(uploadDir, filename, extraImage.data(), extraImage.contentType());

                apartment.getImages().add(AmazonS3Constants.S3_BASE_URI + "/" + uploadDir + "/" + filename);

                imagesWereUploaded = true;
                LOGGER.info("Image {} uploaded to S3 for apartment {}", filename, apartment.getName());
            }

        }
        return imagesWereUploaded;
    }

    private void generateImageMetadata(Apartment apartment) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        boolean imagesWereModified = false;

        for (String apartmentImages : apartment.getImages()) {
            URL url = new URL(apartmentImages);
            try (InputStream inputStream = url.openStream()) {
                ChatClient chatClient = ChatClient.builder(chatModel).build();
                ChatClient.ChatClientRequestSpec chatClientRequest = chatClient.prompt();

                imagesWereModified = true;
                URLConnection connection = url.openConnection();
                Resource imageResource = new InputStreamResource(inputStream);

                Message userMessage = new UserMessage(
                        userPictureMetadataGeneration.getContentAsString(Charset.defaultCharset()),
                        List.of(new Media(MimeTypeUtils.parseMimeType(connection.getContentType()), imageResource))
                );
                Message systemMessage = new SystemMessage(systemPictureMetadataGeneration.getContentAsString(Charset.defaultCharset()));
                chatClientRequest.messages(List.of(systemMessage, userMessage));
                Map<String, Object> result = chatClientRequest.call().entity(new ParameterizedTypeReference<>() {});
                stringBuilder.append(result.get("description"));

                LOGGER.info("Image Metatada generated : {}", result.get("description"));
            }
        }
        if (imagesWereModified) {
            apartment.setImagesGeneratedDescription(stringBuilder.toString());
        }

    }

    public List<ApartmentImage> processImages(MultipartFile[] apartmentImages) {
        List<ApartmentImage> processedImages = new ArrayList<>();
        if (Objects.nonNull(apartmentImages)) {
            for (MultipartFile extraImage : apartmentImages) {
                if (extraImage.isEmpty()) continue;

                var filename = StringUtils.cleanPath(extraImage.getOriginalFilename()).replace(" ", "-");
                var contentType = extraImage.getContentType();
                try {
                    processedImages.add(new ApartmentImage(filename, contentType, extraImage.getInputStream()));
                } catch (IOException ex) {
                    LOGGER.error("File {} could not be read, hence it will be skipped", filename);
                }
            }
        }
        return processedImages;
    }
}
