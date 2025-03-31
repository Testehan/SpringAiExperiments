package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.util.AmazonS3Util;
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
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ListingImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingImageService.class);

    private final LocaleUtils localeUtils;
    private final AmazonS3Util amazonS3Util;
    private final ImageConverter imageConverter;
    private final ChatModel chatModel ;

    public ListingImageService(LocaleUtils localeUtils, AmazonS3Util amazonS3Util, ImageConverter imageConverter, ChatModel chatModel) {
        this.localeUtils = localeUtils;
        this.amazonS3Util = amazonS3Util;
        this.imageConverter = imageConverter;
        this.chatModel = chatModel;
    }

    public boolean saveUploadedImages(Apartment apartment, List<ApartmentImage> apartmentImages) throws IOException {
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

    public void generateImageMetadata(Apartment apartment) throws IOException {

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

}
