package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.util.AmazonS3Util;
import com.testehan.springai.immobiliare.util.ContactValidator;
import com.testehan.springai.immobiliare.util.ImageConverter;
import com.testehan.springai.immobiliare.util.LocaleUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class ApartmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentService.class);

    private final ApartmentsRepository apartmentsRepository;
    private final OpenAiService embedder;
    private final ChatModel chatModel ;
    private final UserService userService;
    private final LocaleUtils localeUtils;
    private final AmazonS3Util amazonS3Util;
    private final ImageConverter imageConverter;

    public ApartmentService(ApartmentsRepository apartmentsRepository, OpenAiService embedder, ChatModel chatModel,
                            UserService userService, LocaleUtils localeUtils, AmazonS3Util amazonS3Util,
                            ImageConverter imageConverter) {
        this.apartmentsRepository = apartmentsRepository;
        this.embedder = embedder;
        this.chatModel = chatModel;
        this.userService = userService;
        this.localeUtils = localeUtils;
        this.amazonS3Util = amazonS3Util;
        this.imageConverter = imageConverter;
    }

    public List<Apartment> getApartmentsSemanticSearch(PropertyType propertyType, String city, ApartmentDescription apartment, String apartmentDescription) {
        var embedding = embedder.createEmbedding(apartmentDescription).block();
        var listings = apartmentsRepository.findApartmentsByVector(propertyType, city, apartment, embedding);

        // later in the call, these results are further filtered by the llm...so assuming that the maxContacted
        // listing or maxFavourite listing are filtered out by the llm, then those badges will not be displayed
        // for the results...no harm as these are supposed to be 100% accurate
        setIsMostFavouriteAndContacted(listings);
        return listings;
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
    public List<Apartment> findByLastUpdateDateTimeBefore(LocalDateTime date){
        return apartmentsRepository.findByLastUpdateDateTimeBefore(date);
    }

    public void deactivateApartments(LocalDateTime date) {
        apartmentsRepository.deactivateApartments(date);
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

        var contact = apartment.getContact();
        if (ContactValidator.isValidPhoneNumber(contact,"RO") && !contact.equalsIgnoreCase(user.getPhoneNumber())){
            user.setPhoneNumber(contact);
            userService.updateUser(user);
        }
        if (isPropertyNew) {
            updateUserInfo(apartment, user);
        }

        LOGGER.info("Apartment was added with success!");
    }

    private void updateUserInfo(Apartment apartment, ImmobiliareUser user) {
        user.getListedProperties().add(apartment.getId().toString());
        user.setMaxNumberOfListedProperties(user.getMaxNumberOfListedProperties() - 1);
        userService.updateUser(user);
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

    public boolean deleteUploadedImages(Apartment apartment) {
        boolean imagesWereDeleted = false;
        var uploadDir = "apartment-images/" + apartment.getId().toString();

        List<String> objectKeys = amazonS3Util.listFolder(uploadDir);
        for (String key : objectKeys){
            int lastIndexOfSlash = key.lastIndexOf("/");
            var filename = key.substring(lastIndexOfSlash + 1);
            if (apartment.getImages().stream().filter(imageURL -> {
                int lastSlash = imageURL.lastIndexOf("/");
                return imageURL.substring(lastSlash+1).equals(filename);
            }).count()<1){
                amazonS3Util.deleteFile(key);
                imagesWereDeleted = true;
            }
        }

        return imagesWereDeleted;
    }

    private boolean saveUploadedImages(Apartment apartment, List<ApartmentImage> apartmentImages) throws IOException {
        boolean imagesWereUploaded = false;
        if (apartmentImages.size()>0) {
            var uploadDir = "apartment-images/" + apartment.getId();
            var amazonS3BaseUri = amazonS3Util.getS3_BASE_URI();
            for (ApartmentImage extraImage : apartmentImages) {

                var filename = extraImage.name().replaceFirst("[.][^.]+$", "") + ".webp";
                var contentType = "image/webp";

                try {
                    InputStream webPImageInputStream = imageConverter.convertToWebPInputStream(extraImage.data());
                    amazonS3Util.uploadFile(uploadDir, filename, webPImageInputStream, contentType);

                    apartment.getImages().add(amazonS3BaseUri + "/" + uploadDir + "/" + filename);

                    imagesWereUploaded = true;
                    LOGGER.info("Image {} uploaded to S3 for apartment {}", filename, apartment.getName());
                } catch (Exception e){
                    LOGGER.info("Image {} could not be uploaded to S3 for apartment {}. Error: {}", filename, apartment.getName(), e.getMessage());
                }
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

                var userPictureMetadataPrompt = localeUtils.getLocalizedPrompt("UserPictureMetadataGeneration");
                var systemPictureMetadataPrompt = localeUtils.getLocalizedPrompt("SystemPictureMetadataGeneration");
                Message userMessage = new UserMessage(
                        userPictureMetadataPrompt,
                        List.of(new Media(MimeTypeUtils.parseMimeType(connection.getContentType()), imageResource))
                );
                Message systemMessage = new SystemMessage(systemPictureMetadataPrompt);
                chatClientRequest.messages(List.of(systemMessage, userMessage));
                Map<String, Object> result = chatClientRequest.call().entity(new ParameterizedTypeReference<>() {});
                stringBuilder.append(result.get("description"));

                LOGGER.info("Image Metatada generated : {}", result.get("description"));
            }
        }
        if (imagesWereModified) {
            apartment.setImagesGeneratedDescription(stringBuilder.toString());
        }
        if (apartment.getImages().size()==0){   // means that user either removed images or didn't add any
            apartment.setImagesGeneratedDescription(Strings.EMPTY);
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

    private void setIsMostFavouriteAndContacted(List<Apartment> listings){
        final int[] maxFavourite = {0};
        final int[] maxContacted = {0};
        listings.stream().forEach(listing -> {
            if (listing.getNoOfFavourite()> maxFavourite[0]){
                maxFavourite[0] = listing.getNoOfFavourite();
            }
            if (listing.getNoOfContact()> maxContacted[0]){
                maxContacted[0] = listing.getNoOfContact();
            }
        });

        listings.stream().forEach(listing -> {
            if (listing.getNoOfFavourite()==maxFavourite[0]){
                listing.setMostFavourite(true);
            } else {
                listing.setMostFavourite(false);
            }

            if (listing.getNoOfContact()==maxContacted[0]){
                listing.setMostContacted(true);
            } else {
                listing.setMostContacted(false);
            }
        });
    }
}
