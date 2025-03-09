package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.CachedResponse;
import com.testehan.springai.immobiliare.repository.CachedResponseRepository;
import com.testehan.springai.immobiliare.util.ListingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LLMCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMCacheService.class);

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

    public void saveToCache(String city, String propertyType, String key, String response) {
        String hash = listingUtil.hashText(key);
        CachedResponse cached = new CachedResponse(hash, response, System.currentTimeMillis(), city, propertyType);
        cachedResponseRepository.save(cached);
    }

    public void removeCachedEntries(String city, String propertyType) {
        long deletedCount = cachedResponseRepository.deleteByCityAndPropertyType(city, propertyType) ;
        LOGGER.info("Deleted {} cached entries because a new listing was added in {} for {}.", deletedCount ,city, propertyType);
    }
}
