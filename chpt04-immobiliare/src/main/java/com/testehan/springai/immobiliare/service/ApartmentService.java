package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.util.ContactValidator;
import com.testehan.springai.immobiliare.util.FormattingUtil;
import com.testehan.springai.immobiliare.util.ListingUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Service
public class ApartmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentService.class);

    private final ApartmentsRepository apartmentsRepository;
    private final ApartmentCrudService apartmentCrudService;
    private final ListingEmbeddingService listingEmbeddingService;
    private final ListingImageService listingImageService;
    private final ListingNotificationService listingNotificationService;
    private final ListingAmenitiesService listingAmenitiesService;
    private final SequenceGeneratorService sequenceGeneratorService;


    private final UserService userService;
    private final LLMCacheService llmCacheService;

    private final ListingUtil listingUtil;
    private final FormattingUtil formattingUtil;

    public ApartmentService(ApartmentsRepository apartmentsRepository, ApartmentCrudService apartmentCrudService, ListingEmbeddingService listingEmbeddingService, ListingImageService listingImageService,
                            UserService userService, ListingNotificationService listingNotificationService, LLMCacheService llmCacheService,
                            ListingAmenitiesService listingAmenitiesService, SequenceGeneratorService sequenceGeneratorService, ListingUtil listingUtil, FormattingUtil formattingUtil) {
        this.apartmentsRepository = apartmentsRepository;
        this.apartmentCrudService = apartmentCrudService;
        this.listingEmbeddingService = listingEmbeddingService;
        this.listingImageService = listingImageService;
        this.userService = userService;
        this.listingNotificationService = listingNotificationService;
        this.llmCacheService = llmCacheService;
        this.listingAmenitiesService = listingAmenitiesService;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.listingUtil = listingUtil;
        this.formattingUtil = formattingUtil;
    }

    public List<Apartment> getApartmentsSemanticSearch(PropertyType propertyType, String city, ApartmentDescription apartment, List<Double> apartmentDescriptionEmbedding) {
        var listings = apartmentsRepository.findApartmentsByVector(propertyType, city, apartment, apartmentDescriptionEmbedding);

        // later in the call, these results are further filtered by the llm...so assuming that the maxContacted
        // listing or maxFavourite listing are filtered out by the llm, then those badges will not be displayed
        // for the results...no harm as these are supposed to be 100% accurate
        listingUtil.setIsMostFavouriteAndContacted(listings);
        return listings;
    }

    public boolean isPhoneValid(String phoneNumber) {
        return apartmentsRepository.isPhoneValid(phoneNumber);
    }

    @Async
    public void saveApartmentAndImages(Apartment apartment, List<ApartmentImage> apartmentImages, Optional<ImmobiliareUser> userOptional, boolean isBatchSave) throws IOException {
        apartment.setShortDescription(apartment.getShortDescription().replace("\n", " ")); // no newlines in description
        if (Objects.isNull(apartment.getContactEmail()) && userOptional.isPresent()) {
            apartment.setContactEmail(userOptional.get().getEmail());
        }

        apartment.setContact(ContactValidator.internationalizePhoneNumber(apartment.getContact()));

        handleNewOrUpdatedProperty(apartment);
        var isPropertyNew = isPropertyNew(apartment);
        if (isPropertyNew){
           handleNewProperty(apartment);
        } else {
            var optionalApartment = apartmentCrudService.findApartmentById(apartment.getIdString());
            if (optionalApartment.isPresent()){
                var apartmentCurrentlySaved = optionalApartment.get();
                var hasAddressChange = !apartmentCurrentlySaved.getArea().equalsIgnoreCase(apartment.getArea());
                if (hasAddressChange || apartmentCurrentlySaved.getNearbyAmenities().isEmpty()){
                    // means that address changed and we need to set amenities again
                    listingAmenitiesService.getAmenitiesAndSetInApartment(apartment);
                } else {
                    apartment.setNearbyAmenities(apartmentCurrentlySaved.getNearbyAmenities());
                }

                apartment.setImagesGeneratedDescription(apartmentCurrentlySaved.getImagesGeneratedDescription());
                apartment.setSocialId(apartmentCurrentlySaved.getSocialId());

            }
        }

        saveImagesAndMetadata(apartment, apartmentImages);
        updateEmbeddings(apartment);

        var savedListing = apartmentCrudService.saveApartment(apartment);

        updateUserPhoneIfChanged(apartment, userOptional);
        if (isPropertyNew) {
            updateUserInfo(apartment, userOptional);
            if(!isBatchSave && userOptional.isPresent()) {
                listingNotificationService.sendListingAddedEmail(savedListing, userOptional.get());
            }
        }

        llmCacheService.removeCachedEntries(apartment.getCity(), apartment.getPropertyType());

        LOGGER.info("Apartment was added with success!");
    }

    public void deleteListingAndImages(Apartment apartment, ImmobiliareUser user){
        var listingId = apartment.getId().toString();
        listingImageService.deleteUploadedImages(apartment);
        apartmentCrudService.deleteApartmentsByIds(List.of(listingId));
        user.getListedProperties().remove(listingId);
        if (!user.isAdmin()) {
            user.setMaxNumberOfListedProperties(1);
        }
        userService.updateUser(user);
        llmCacheService.removeCachedEntries(apartment.getCity(), apartment.getPropertyType());
    }

    private void updateUserPhoneIfChanged(Apartment apartment, Optional<ImmobiliareUser> userOptional) {
        var contact = apartment.getContact();
        if (ContactValidator.isValidPhoneNumber(contact,null) && userOptional.isPresent()) {
            var user = userOptional.get();
            if ( !contact.equalsIgnoreCase(user.getPhoneNumber())) {
                user.setPhoneNumber(contact);
                userService.updateUser(user);
            }
        }
    }

    private void updateEmbeddings(Apartment apartment) {
        apartment.setPlot_embedding(listingEmbeddingService.createEmbedding(apartment));
        if (Objects.isNull(apartment.getPlot_embedding()) || apartment.getPlot_embedding().isEmpty()) {
            // todo having plot embedding empty means that vector search will not work as expected... need to figure out why this happens..
            // probably when trying to create multiple listings quickly ?
            LOGGER.error("###################### apartment {} has no plot embedding ", apartment.getName());
        }
    }

    private void saveImagesAndMetadata(Apartment apartment, List<ApartmentImage> apartmentImages) throws IOException {
        var imagesWereUploaded = listingImageService.saveUploadedImages(apartment, apartmentImages);
        var imagesWereDeleted = listingImageService.deleteUploadedImages(apartment);
        if (imagesWereUploaded || imagesWereDeleted || StringUtils.isEmpty(apartment.getImagesGeneratedDescription())) {
            listingImageService.generateImageMetadata(apartment);
        }
    }


    private void updateUserInfo(Apartment apartment, Optional<ImmobiliareUser> userOptional) {
        if (userOptional.isPresent()) {
            var user = userOptional.get();
            user.getListedProperties().add(apartment.getId().toString());
            user.setMaxNumberOfListedProperties(user.getMaxNumberOfListedProperties() - 1);
            userService.updateUser(user);
        }
    }

    private void handleNewProperty(Apartment apartment) {
        apartment.setCreationDateTime(formattingUtil.getFormattedDateCustom(LocalDateTime.now()));
        apartment.setSocialId(String.valueOf(sequenceGeneratorService.generateSequence(Apartment.class.getSimpleName())));
        listingAmenitiesService.getAmenitiesAndSetInApartment(apartment);
        apartmentCrudService.saveApartment(apartment);
    }

    private void handleNewOrUpdatedProperty(Apartment apartment){
        apartment.setLastUpdateDateTime(formattingUtil.getFormattedDateCustom(LocalDateTime.now()));
        apartment.setActivationToken(UUID.randomUUID().toString());
    }

    public boolean isPropertyNew(Apartment apartment) {
       return Objects.isNull(apartment.getId());

    }
}
