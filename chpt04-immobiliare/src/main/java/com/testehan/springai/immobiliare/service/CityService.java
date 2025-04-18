package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.City;
import com.testehan.springai.immobiliare.repository.CityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityService {

    private final CityRepository cityRepository;


    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<String> getEnabledCityNames(){
        List<City> cities = cityRepository.findByIsEnabledTrue();
        return cities.stream().map(City::getName).toList();
    }

    public boolean isEnabled(String cityName){
        return getEnabledCityNames().contains(cityName);
    }

    public void requestCity(String cityName) {
        var cityOptional = cityRepository.findByName(cityName);
        if (cityOptional.isPresent()){
            var city = cityOptional.get();
            city.setRequestCount(city.getRequestCount()+1);
            cityRepository.save(city);
        } else {
            var city = new City(cityName, cityName, false, 1);
//            city.setName(cityName);
//            city.setSlug(cityName);
//            city.setEnabled(false);
//            city.setRequestCount(1);
            cityRepository.save(city);
        }
    }
}
