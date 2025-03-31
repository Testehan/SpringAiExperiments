package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.Apartment;
import com.testehan.springai.immobiliare.repository.ApartmentsRepository;
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

    public void deactivateApartments(LocalDateTime date) {
        apartmentsRepository.deactivateApartments(date);
    }

    public List<Apartment> findAll(){
        return apartmentsRepository.findAll();
    }

    public Optional<Apartment> findApartmentById(String apartmentId) {
        return apartmentsRepository.findApartmentById(apartmentId);
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
}
