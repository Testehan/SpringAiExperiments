package com.testehan.springai.immobiliare.util;

import com.testehan.springai.immobiliare.model.Amenity;
import com.testehan.springai.immobiliare.model.AmenityCategory;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.ListingStatistics;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import com.testehan.springai.immobiliare.repository.ListingStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ListingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingUtil.class);

    private final ListingStatisticsRepository listingStatisticsRepository;
    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;
    private final FormattingUtil formattingUtil;

    public ListingUtil(ListingStatisticsRepository listingStatisticsRepository, MessageSource messageSource, LocaleUtils localeUtils, FormattingUtil formattingUtil) {
        this.listingStatisticsRepository = listingStatisticsRepository;
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
        this.formattingUtil = formattingUtil;
    }

    public boolean isApartmentAlreadyFavourite(final String listingId, final ImmobiliareUser immobiliareUser) {
        if (immobiliareUser.getFavouriteProperties().contains(listingId)) {
            return true;
        }
        return false;
    }

    public String getFavouritesText(final boolean isFavourite) {
        if (isFavourite) {
            var heartSymbol = "♥";
            return heartSymbol;
        } else {
            return "listing.favourites";
        }
    }

    public String getApartmentInfoToEmbedd(final Apartment apartment) {
        return messageSource.getMessage("listing.embedded.data", new Object[]{
                apartment.getName(),
                apartment.getCity(),
                apartment.getArea(),
                apartment.getShortDescription(),
                apartment.getSurface(),
                apartment.getPrice(),
                apartment.getAvailableFrom(),
                apartment.getNoOfRooms(),
                apartment.getFloor(),
                apartment.getTags(),
                apartment.getImagesGeneratedDescription(),
                translateNearbyAmenities(apartment)
        }, localeUtils.getCurrentLocale());

    }

    public String getApartmentInfo(final Apartment apartment) {

        return messageSource.getMessage("listing.info", new Object[]{
                apartment.getName(),
                apartment.getCity(),
                apartment.getArea(),
                apartment.getShortDescription(),
                apartment.getSurface(),
                apartment.getPrice(),
                apartment.getAvailableFrom(),
                apartment.getNoOfRooms(),
                apartment.getFloor(),
                apartment.getTags(),
                apartment.getImagesGeneratedDescription(),
                translateNearbyAmenities(apartment)
        }, localeUtils.getCurrentLocale());
    }

    private String translateNearbyAmenities(final Apartment apartment) {
        StringBuilder sb = new StringBuilder();

        for (AmenityCategory category : apartment.getNearbyAmenities()) {
            translateAmenityCategory(category, sb);
        }

        return sb.toString();
    }

    private void translateAmenityCategory(AmenityCategory category, StringBuilder sb) {
        String messageCode = category.getCategory();
        var translatedCategory = messageSource.getMessage("listing.nearby.amenities." + messageCode, null, localeUtils.getCurrentLocale());
        sb.append(translatedCategory).append(": ");
        for (Amenity amenity : category.getItems()){
            sb.append(amenity.getName()).append(" ").append(amenity.getDistance()).append(", ");
        }
        sb.append("\n");
    }

    public void setIsMostFavouriteAndContacted(List<Apartment> listings){

        if (!listings.isEmpty()){
            var first = listings.get(0);
            var statisticsList = listingStatisticsRepository.findByCityAndPropertyType(first.getCity(),first.getPropertyType());

            for (Apartment listing : listings){
                for (ListingStatistics statistics : statisticsList.get()){
                    if (listing.getNoOfRooms() == statistics.getNoOfRooms() ) {
                        if (Objects.nonNull(statistics.getContactThreshold())
                                && statistics.getContactThreshold() > 0
                                && listing.getNoOfContact() >= statistics.getContactThreshold()) {
                            listing.setMostContacted(true);
                        }
                        if (Objects.nonNull(statistics.getFavoriteThreshold())
                                && statistics.getFavoriteThreshold() > 0
                                && listing.getNoOfFavourite() >= statistics.getFavoriteThreshold()) {
                            listing.setMostFavourite(true);
                        }
                    }
                }
            }
        }
    }

    public List<Map<String, Object>> getListingDataByFields(List<Apartment> listings, List<String> fields) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Apartment apartment : listings) {
            Map<String, Object> apartmentData = new HashMap<>();
            for (String fieldName : fields) {
                try {
                    if (fieldName.startsWith("nearby.amenities-")){
                        String[] parts = fieldName.split("-");
                        Field field = Apartment.class.getDeclaredField("nearbyAmenities");
                        field.setAccessible(true);
                        List<AmenityCategory> nearbyAmenities = (List<AmenityCategory>) field.get(apartment);
                        var category = nearbyAmenities.stream()
                                .filter(amenityCategory -> amenityCategory.getCategory().equalsIgnoreCase(parts[1]))
                                .findFirst();

                        if (category.isPresent()) {
                            StringBuilder sb = new StringBuilder();
                            translateAmenityCategory(category.get(), sb);
                            apartmentData.put(fieldName.replace("-", "."), sb.toString());
                        }

                    } else {
                        Field field = Apartment.class.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        var translatedFieldName = messageSource.getMessage("listing." + fieldName, null, localeUtils.getCurrentLocale());
                        String value = translatedFieldName + " : " + field.get(apartment).toString();
                        apartmentData.put(fieldName, value);
                    }
                } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
                    apartmentData.put(fieldName, null); // Handle missing fields
                }
            }
            result.add(apartmentData);
        }
        return result;
    }

    public String apartmentFieldDataToString(List<Map<String, Object>> apartmentDataList) {
        if (apartmentDataList == null || apartmentDataList.isEmpty()) {
            return "";
        }

        return apartmentDataList.stream()
                .map(map -> map.entrySet().stream()
                        .map(entry -> entry.getValue() != null ? entry.getValue().toString() : "")
                        .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

    public boolean isListingNew(Apartment apartment){
        if (apartment.getCreationDateTime() == null || apartment.getCreationDateTime() .isEmpty()) {
            return false;
        }

        try {
            LocalDateTime creationTime = formattingUtil.getFormattedDateCustom(apartment.getCreationDateTime());
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

            return creationTime.isAfter(threeDaysAgo);

        } catch (DateTimeParseException e) {
            LOGGER.error("Error parsing date string: {} - {}", apartment.getCreationDateTime(), e.getMessage());
            return false;
        }
    }

}
