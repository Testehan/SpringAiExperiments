package com.testehan.springai.immobiliare.util;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PlacesApi;
import com.google.maps.model.*;
import com.testehan.springai.immobiliare.configuration.BeanConfig;
import com.testehan.springai.immobiliare.model.Amenity;
import com.testehan.springai.immobiliare.model.AmenityCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GoogleMapsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleMapsUtil.class);

    // Search radius in meters.
    public static final List<Integer> RADII = List.of(500, 1000, 2000);
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

    public List<AmenityCategory> getNearbyAmenities(final String address){
        var optionalCoordinates = getCoordinatesOfAddress(address);

        List<AmenityCategory> placeTypesToPlaces = new ArrayList<>();

        if (optionalCoordinates.isPresent()){
            var addressCoordinates = optionalCoordinates.get();
            Map<PlaceType, List<PlacesSearchResult>> placesMap = getNearbyAmenities(addressCoordinates);
            for (PlaceType placeType : PLACE_TYPES_TO_SEARCH){

                if (Objects.nonNull(placesMap.get(placeType)) && !placesMap.get(placeType).isEmpty()) {
                    var amenityCategory = new AmenityCategory();
                    amenityCategory.setCategory(placeType.toString());
                    amenityCategory.setItems(new ArrayList<>());

                    for (PlacesSearchResult placeResult : placesMap.get(placeType)) {
                        var distance = distanceBetweenCoordinates(addressCoordinates, placeResult.geometry.location);
                        var amenity = new Amenity();
                        amenity.setName(placeResult.name);
                        if (distance.isPresent()) {
                            amenity.setDistance(" (" + distance.get() + ") ");
                        }
                        amenityCategory.getItems().add(amenity);
                    }

                    placeTypesToPlaces.add(amenityCategory);
                }
            }

        } else {
            LOGGER.warn("No coordinates found for the address: {}", address);
        }
        return placeTypesToPlaces;
    }

    public Optional<LatLng> getCoordinatesOfAddress(final String address){
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(beanConfig.getGoogleMapsServerApiKey())
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

    public Map<PlaceType, List<PlacesSearchResult>> getNearbyAmenities(LatLng addressCoordinates){

        var result = new HashMap<PlaceType, List<PlacesSearchResult>>();

        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(beanConfig.getGoogleMapsServerApiKey())
                .build();
        try {
            for (PlaceType placeType : PLACE_TYPES_TO_SEARCH) {
                for (Integer radius : RADII) {
                    PlacesSearchResponse response = PlacesApi.nearbySearchQuery(context, addressCoordinates)
                            .radius(radius)
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
                            if (!placesSearchResult.permanentlyClosed) {
                                if (!result.containsKey(placeType)) {
                                    result.put(placeType, new ArrayList<>());
                                }
                                result.get(placeType).add(placesSearchResult);
                            }
                        }

                        if (numResultsToProcess>0){
                            break;  // means that for current place type we found something nearby so the search will not be extended
                        }
                    } else {
                        LOGGER.warn("No {} found for given coordinates. Will increase radius", placeType.toString());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            context.shutdown();
        }

        return result;
    }

    public Optional<String> distanceBetweenCoordinates(final LatLng origin, final LatLng destination) {
        Optional<String> result;

        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(beanConfig.getGoogleMapsServerApiKey())
                .build();

        try {
            var distanceMatrix = DistanceMatrixApi.getDistanceMatrix(context, new String[]{origin.toString()}, new String[]{destination.toString()})
                    .mode(TravelMode.WALKING)
                    .await();

            if (distanceMatrix.rows[0].elements[0].status.toString().equals("OK")) {
                result = distanceMatrix.rows[0].elements[0].distance.humanReadable.describeConstable();
            } else {
                LOGGER.error("Error calculating distance: {}", distanceMatrix.rows[0].elements[0].status);
                result = Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            result = Optional.empty();
        } finally {
            context.shutdown();
        }

        return  result;
    }

}
