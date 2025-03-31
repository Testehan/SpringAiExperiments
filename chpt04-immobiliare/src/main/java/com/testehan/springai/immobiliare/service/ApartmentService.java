package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ApartmentDescription;
import com.testehan.springai.immobiliare.model.ApartmentImage;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
import com.testehan.springai.immobiliare.security.UserService;
import com.testehan.springai.immobiliare.util.ContactValidator;
import com.testehan.springai.immobiliare.util.ListingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class ApartmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApartmentService.class);

    private final ApartmentsRepository apartmentsRepository;
    private final ListingEmbeddingService listingEmbeddingService;
    private final ListingImageService listingImageService;
    private final ListingNotificationService listingNotificationService;
    private final ListingAmenitiesService listingAmenitiesService;

    private final UserService userService;
    private final LLMCacheService llmCacheService;

    private final ListingUtil listingUtil;

    public ApartmentService(ApartmentsRepository apartmentsRepository, ListingEmbeddingService listingEmbeddingService, ListingImageService listingImageService,
                            UserService userService, ListingNotificationService listingNotificationService, LLMCacheService llmCacheService,
                            ListingAmenitiesService listingAmenitiesService, ListingUtil listingUtil) {
        this.apartmentsRepository = apartmentsRepository;
        this.listingEmbeddingService = listingEmbeddingService;
        this.listingImageService = listingImageService;
        this.userService = userService;
        this.listingNotificationService = listingNotificationService;
        this.llmCacheService = llmCacheService;
        this.listingAmenitiesService = listingAmenitiesService;
        this.listingUtil = listingUtil;
    }

    public List<Apartment> getApartmentsSemanticSearch(PropertyType propertyType, String city, ApartmentDescription apartment, List<Double> apartmentDescriptionEmbedding) {
        var listings = apartmentsRepository.findApartmentsByVector(propertyType, city, apartment, apartmentDescriptionEmbedding);

        // later in the call, these results are further filtered by the llm...so assuming that the maxContacted
        // listing or maxFavourite listing are filtered out by the llm, then those badges will not be displayed
        // for the results...no harm as these are supposed to be 100% accurate
        listingUtil.setIsMostFavouriteAndContacted(listings);
        return listings;
    }

    public Optional<Apartment> findApartmentById(String apartmentId) {
        return apartmentsRepository.findApartmentById(apartmentId);
    }

    public boolean isPhoneValid(String phoneNumber) {
        return apartmentsRepository.isPhoneValid(phoneNumber);
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

    public Apartment saveApartment(Apartment apartment){
        return apartmentsRepository.saveApartment(apartment);
    }

    public void deleteApartmentsByIds(List<String> apartmentIds){
        apartmentsRepository.deleteApartmentsByIds(apartmentIds);
    }

    @Async
    public void saveApartmentAndImages(Apartment apartment,  List<ApartmentImage> apartmentImages, ImmobiliareUser user) throws IOException {
        apartment.setShortDescription(apartment.getShortDescription().replace("\n", " ")); // no newlines in description
        if (Objects.isNull(apartment.getContactEmail())) {
            apartment.setContactEmail(user.getEmail());
        }

        var isPropertyNew = isPropertyNew(apartment);
        if (isPropertyNew){
            listingAmenitiesService.getAmenitiesAndSetInApartment(apartment);
            saveApartment(apartment);
        } else {
            var optionalApartment = findApartmentById(apartment.getIdString());
            if (optionalApartment.isPresent()){
                var apartmentCurrentlySaved = optionalApartment.get();
                var hasAddressChange = !apartmentCurrentlySaved.getArea().equalsIgnoreCase(apartment.getArea());
                if (hasAddressChange || apartmentCurrentlySaved.getNearbyAmenities().isEmpty()){
                    // means that address changed and we need to set amenities again
                    listingAmenitiesService.getAmenitiesAndSetInApartment(apartment);
                } else {
                    apartment.setNearbyAmenities(apartmentCurrentlySaved.getNearbyAmenities());
                }
            }
        }

        var imagesWereUploaded = listingImageService.saveUploadedImages(apartment, apartmentImages);
        var imagesWereDeleted = listingImageService.deleteUploadedImages(apartment);
        if (imagesWereUploaded || imagesWereDeleted) {
            listingImageService.generateImageMetadata(apartment);
        }

        apartment.setPlot_embedding(listingEmbeddingService.createEmbedding(apartment));
        if (Objects.isNull(apartment.getPlot_embedding()) || apartment.getPlot_embedding().isEmpty()) {
            // todo having plot embedding empty means that vector search will not work as expected... need to figure out why this happens..
            // probably when trying to create multiple listings quickly ?
            LOGGER.error("###################### apartment {} has no plot embedding ", apartment.getName());
        }

        var savedListing = saveApartment(apartment);

        var contact = apartment.getContact();
        if (ContactValidator.isValidPhoneNumber(contact,"RO") && !contact.equalsIgnoreCase(user.getPhoneNumber())){
            user.setPhoneNumber(contact);
            userService.updateUser(user);
        }
        if (isPropertyNew) {
            updateUserInfo(apartment, user);
            listingNotificationService.sendListingAddedEmail(savedListing, user);
        }

        llmCacheService.removeCachedEntries(apartment.getCity(), apartment.getPropertyType());

        LOGGER.info("Apartment was added with success!");
    }


    private void updateUserInfo(Apartment apartment, ImmobiliareUser user) {
        user.getListedProperties().add(apartment.getId().toString());
        user.setMaxNumberOfListedProperties(user.getMaxNumberOfListedProperties() - 1);
        userService.updateUser(user);
    }

    private boolean isPropertyNew(Apartment apartment) {
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

    public Optional<String> findApartmentIdBySocialId(String socialId) {
        return apartmentsRepository.findApartmentIdBySocialId(socialId);
    }
}
