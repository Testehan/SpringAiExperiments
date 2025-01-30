package com.testehan.springai.immobiliare.util;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PlacesApi;
import com.google.maps.model.*;
import com.testehan.springai.immobiliare.configuration.BeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GoogleMapsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleMapsUtil.class);

    // Search radius in meters...doesn't seem very accurate so we'll put a smaller number
    public static final int RADIUS = 500;
    // The maximum number of results you want per category
    public static final int MAX_RESULTS = 3;

    public static final List<PlaceType> PLACE_TYPES_TO_SEARCH = List.of(PlaceType.TRANSIT_STATION, PlaceType.GROCERY_OR_SUPERMARKET,
            PlaceType.SCHOOL, PlaceType.UNIVERSITY, PlaceType.GYM, PlaceType.PARK);

    private final BeanConfig beanConfig;
    private final LocaleUtils localeUtils;

    public GoogleMapsUtil(BeanConfig beanConfig, LocaleUtils localeUtils){
        this.beanConfig = beanConfig;
        this.localeUtils = localeUtils;
    }

    // todo we need to also calculate the distance to each point of interest, and add that to the result
    //  of this method ..see example in GeocodingExample on how to calculate distance between 2 points using coordinates..

    public String getPointsOfInterest(final String address){
        var optionalCoordinates = getCoordinatesOfAddress(address);
        StringBuilder sb = new StringBuilder();
        if (optionalCoordinates.isPresent()){
            Map<PlaceType, List<PlacesSearchResult>> placesMap = getNearbyPointsOfInterest(optionalCoordinates.get());
            for (PlaceType placeType : PLACE_TYPES_TO_SEARCH){
                sb.append(placeType.toString() + ": ");
                for (PlacesSearchResult placeResult : placesMap.get(placeType)){
                    sb.append(placeResult.name + ", ");
                }
                sb.append("\n");
            }

        } else {
            LOGGER.warn("No coordinates found for the address: {}", address);
        }
        return sb.toString();
    }

    public Optional<LatLng> getCoordinatesOfAddress(final String address){
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(beanConfig.getGoogleMapsApiKey())
                .build();

        var geocodingApiRequest = GeocodingApi.newRequest(context);
        try {
            GeocodingResult[] results = geocodingApiRequest.address(address).await(); // Use .await() to get the results

            if (results.length > 0) {
                GeocodingResult result = results[0];
                LatLng location = result.geometry.location;
                return Optional.of(location);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        finally {
            // It's good practice to shutdown the context when you're done
            context.shutdown();
        }
        return Optional.empty();
    }

    public Map<PlaceType, List<PlacesSearchResult>> getNearbyPointsOfInterest(LatLng addressCoordinates){

        var result = new HashMap<PlaceType, List<PlacesSearchResult>>();

        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(beanConfig.getGoogleMapsApiKey())
                .build();
        try {
            for (PlaceType placeType : PLACE_TYPES_TO_SEARCH) {

                PlacesSearchResponse response = PlacesApi.nearbySearchQuery(context, addressCoordinates)
                        .radius(RADIUS)
                        .type(placeType)
                        .rankby(RankBy.PROMINENCE)
                        .language(localeUtils.getCurrentLocale().getLanguage())
                        .await();

                if (response.results != null) {
                    int numResultsToProcess = Math.min(response.results.length, MAX_RESULTS);
                    Arrays.sort(response.results, (o1, o2) -> {
                        // 1. Compare by userRatingsTotal (descending)
                        int totalComparison = Integer.compare(o2.userRatingsTotal, o1.userRatingsTotal); // Note the order for descending sort

                        if (totalComparison != 0) {
                            return totalComparison; // If totals are different, sort by totals
                        } else {
                            // 2. If userRatingsTotal is the same, compare by rating (descending)
                            return Double.compare(o2.rating, o1.rating); // Note the order for descending sort
                        }
                    });

                    for (int i = 0; i < numResultsToProcess; i++) {
                        PlacesSearchResult placesSearchResult = response.results[i];
                        if (!placesSearchResult.permanentlyClosed){
                            if (!result.containsKey(placeType)){
                                result.put(placeType, new ArrayList<>());
                            }
                            result.get(placeType).add(placesSearchResult);
                        }
                        // Process the result (e.g., print details, add to a list)
//                        System.out.println("Name: " + placesSearchResult.name);
//                        System.out.println("Address: " + placesSearchResult.formattedAddress);
//                        System.out.println("Latitude: " + placesSearchResult.geometry.location.lat);
//                        System.out.println("Longitude: " + placesSearchResult.geometry.location.lng);
//                        System.out.println("Rating: " + placesSearchResult.rating); // Print rating if available
//                        System.out.println("Permanently closed: " + placesSearchResult.permanentlyClosed); // Print rating if available
//                        System.out.println("Business status: " + placesSearchResult.businessStatus); // Print rating if available
//                        System.out.println("--------------------");
                    }

                } else {
                    LOGGER.warn("No {} found for given coordinates.",placeType.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            context.shutdown();
        }

        return result;
    }

}
