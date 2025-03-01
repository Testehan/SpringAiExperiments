package com.testehan.springai.immobiliare.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// In the future, if needed, this list will be stored in the DB.
public enum SupportedCity {
    CLUJ_NAPOCA("Cluj-Napoca"),
//    BUCHAREST("Bucharest"),
    UNSUPPORTED("Unsupported city");

    private final String name;

    SupportedCity(String city) {
        this.name = city;
    }

    public static SupportedCity getByName(String cityName) {
        for (SupportedCity city : SupportedCity.values()) {
            if (city.getName().equalsIgnoreCase(cityName)) {
                return city;
            }
        }
        return UNSUPPORTED;
    }

    public String getName() {
        return name;
    }

    public static List<String> getSupportedCities(){
        return Arrays.stream(SupportedCity.values())
                .filter(city -> 0 != city.compareTo(SupportedCity.UNSUPPORTED))
                .map(city -> city.getName()).collect(Collectors.toList());
    }

}
