package com.testehan.springai.immobiliare.util;

import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;

public class ListingUtil {

    public static boolean isApartmentAlreadyFavourite(String listingId, ImmobiliareUser immobiliareUser) {
        if (immobiliareUser.getFavouriteProperties().contains(listingId)){
            return true;
        }
        return false;
    }


    public static String getFavouritesText(boolean isFavourite) {
        if (isFavourite){
            var heartSymbol = "♥";
            return heartSymbol;
        } else {
            return "save.favourites";
        }
    }

}
