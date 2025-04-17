package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.CachedResponse;
import com.testehan.springai.immobiliare.observability.CacheMetrics;
import com.testehan.springai.immobiliare.repository.CachedResponseRepository;
import com.testehan.springai.immobiliare.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LLMCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMCacheService.class);

    private final CacheMetrics cacheMetrics;
    private final CachedResponseRepository cachedResponseRepository;
    private final HashUtil hashUtil;

    public LLMCacheService(CacheMetrics cacheMetrics, CachedResponseRepository cachedResponseRepository, HashUtil hashUtil) {
        this.cacheMetrics = cacheMetrics;
        this.cachedResponseRepository = cachedResponseRepository;
        this.hashUtil = hashUtil;
    }

    public Optional<String> getCachedResponse(String input) {
        String hash = hashUtil.hashText(input);
        Optional<CachedResponse> cached = cachedResponseRepository.findByInputHash(hash);
        if (cached.isPresent()){
            cacheMetrics.incrementHit();
            return Optional.ofNullable(cached.get().getResponse());
        } else {
            cacheMetrics.incrementMiss();
            return Optional.empty();
        }
    }

    public void saveToCache(String city, String propertyType, String userInput, String response) {
        String hash = hashUtil.hashText(userInput);
        CachedResponse cached = new CachedResponse(hash,userInput, response, System.currentTimeMillis(), city, propertyType, 0);
        cachedResponseRepository.save(cached);
    }

    public void removeCachedEntries(String city, String propertyType) {
        long deletedCount = cachedResponseRepository.deleteByCityAndPropertyType(city, propertyType) ;
        LOGGER.info("Deleted {} cached entries because a new listing was added in {} for {}.", deletedCount ,city, propertyType);
    }

    public void reportInaccurateResponse(String input){
        cachedResponseRepository.decreaseFieldByOne(hashUtil.hashText(input), "inaccurateResponseCount");
    }
}
