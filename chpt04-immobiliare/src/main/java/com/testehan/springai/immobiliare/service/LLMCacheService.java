package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.CachedResponse;
import com.testehan.springai.immobiliare.model.PropertyType;
import com.testehan.springai.immobiliare.repository.CachedResponseRepository;
import com.testehan.springai.immobiliare.util.ListingUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LLMCacheService {

    private final CachedResponseRepository cachedResponseRepository;
    private final ListingUtil listingUtil;

    public LLMCacheService(CachedResponseRepository cachedResponseRepository, ListingUtil listingUtil) {
        this.cachedResponseRepository = cachedResponseRepository;
        this.listingUtil = listingUtil;
    }

    public Optional<String> getCachedResponse(String input) {
        String hash = listingUtil.hashText(input);
        Optional<CachedResponse> cached = cachedResponseRepository.findByInputHash(hash);
        if (cached.isPresent()){
            return Optional.ofNullable(cached.get().getResponse());
        } else {
            return Optional.empty();
        }
    }

    public void saveToCache(String city, PropertyType propertyType, String key, String response) {
        String hash = listingUtil.hashText(key);
        CachedResponse cached = new CachedResponse(hash, response, System.currentTimeMillis(), city, propertyType);
        cachedResponseRepository.save(cached);
    }
}
