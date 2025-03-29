package com.testehan.springai.immobiliare.util;

import com.testehan.springai.immobiliare.model.Amenity;
import com.testehan.springai.immobiliare.model.AmenityCategory;
import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

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
            String messageCode = category.getCategory();
            var translatedCategory = messageSource.getMessage("listing.nearby.amenities." + messageCode, null, localeUtils.getCurrentLocale());
            sb.append(translatedCategory).append(": ");
            for (Amenity amenity : category.getItems()){
                sb.append(amenity.getName()).append(" ").append(amenity.getDistance()).append(", ");
            }
            sb.append("\n");
        }

        return sb.toString();
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

}
