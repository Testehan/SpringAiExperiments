package com.testehan.springai.immobiliare.util;

import com.testehan.springai.immobiliare.model.Amenity;
import com.testehan.springai.immobiliare.model.AmenityCategory;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ListingUtil {

    private final MessageSource messageSource;
    private final LocaleUtils localeUtils;

    public ListingUtil(MessageSource messageSource, LocaleUtils localeUtils) {
        this.messageSource = messageSource;
        this.localeUtils = localeUtils;
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

    public String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing text", e);
        }
    }

    public void setIsMostFavouriteAndContacted(List<Apartment> listings){
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
                        Object value = field.get(apartment);
                        apartmentData.put(fieldName, value);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
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
                        .map(entry -> messageSource.getMessage("listing." + entry.getKey(), null, localeUtils.getCurrentLocale()) + ": " + entry.getValue())
                        .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

}
