package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ApartmentCrudService {

    private final ApartmentsRepository apartmentsRepository;

    public ApartmentCrudService(ApartmentsRepository apartmentsRepository) {
        this.apartmentsRepository = apartmentsRepository;
    }

    public Apartment saveApartment(Apartment apartment){
        return apartmentsRepository.saveApartment(apartment);
    }

    public void deleteApartmentsByIds(List<String> apartmentIds){
        apartmentsRepository.deleteApartmentsByIds(apartmentIds);
    }

    public List<Apartment> findByLastUpdateDateTimeBefore(LocalDateTime date){
        return apartmentsRepository.findByLastUpdateDateTimeBefore(date);
    }

    public List<String> deactivateApartments(LocalDateTime date) {
        return apartmentsRepository.deactivateApartments(date);
    }

    public List<Apartment> findAll(){
        return apartmentsRepository.findAll();
    }

    public Optional<Apartment> findApartmentById(String apartmentId) {
        return apartmentsRepository.findApartmentById(apartmentId);
    }

    public Optional<Apartment> findApartmentByContact(String phoneNumber) {
        return apartmentsRepository.findApartmentByContact(phoneNumber);
    }

    public List<Apartment> findApartmentsByIds(List<String> apartmentIds){
        List<Apartment> apartments = new ArrayList<>();
        for (String apartmentId : apartmentIds){
            findApartmentById(apartmentId).ifPresent(apartment -> apartments.add(apartment));
        }
        return apartments;
    }

    public Optional<String> findApartmentIdBySocialId(String socialId) {
        return apartmentsRepository.findApartmentIdBySocialId(socialId);
    }

    public Page<Apartment> searchApartment(String search, String cityFilter, String propertyTypeFilter,
                                           Integer minPrice, Integer maxPrice, boolean showOnlyActive, String sortBy,
                                           String sortDir, int page, int size) {

        return apartmentsRepository.searchApartment(search, cityFilter, propertyTypeFilter, minPrice, maxPrice,showOnlyActive, sortBy, sortDir, page, size);

    }

}
