package com.testehan.springai.immobiliare.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CacheMetrics {

    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    public CacheMetrics(MeterRegistry meterRegistry) {
        this.cacheHitCounter = Counter.builder("llm_cache_hits")
                .description("Number of LLM cache hits from MongoDB")
                .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("llm_cache_misses")
                .description("Number of LLM cache misses (required LLM call)")
                .register(meterRegistry);
    }

    public void incrementHit() {
        cacheHitCounter.increment();
    }

    public void incrementMiss() {
        cacheMissCounter.increment();
    }

}
