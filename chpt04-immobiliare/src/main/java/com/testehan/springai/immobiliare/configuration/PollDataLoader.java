package com.testehan.springai.immobiliare.configuration;

import com.testehan.springai.immobiliare.model.Poll;
import com.testehan.springai.immobiliare.repository.PollRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class PollDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollDataLoader.class);

    @Bean
    CommandLineRunner initPolls(PollRepository pollRepository) {
        return args -> {
            if (pollRepository.count() == 0) {
                pollRepository.saveAll(List.of(
                        new Poll(
                                "poll1",
                                "Care e cea mai grea parte la găsirea unei chirii?",
                                Map.of(
                                        "Contactarea proprietarilor", 0,
                                        "Anunturi false sau expirate", 0,
                                        "Filtrarea dupa buget sau locatie", 0
                                )
                        ),
                        new Poll(
                                "poll2",
                                "De cât timp cauți o chirie nouă?",
                                Map.of(
                                        "Mai puțin de o săptămână", 0,
                                        "1-4 săptămâni", 0,
                                        "Peste o lună", 0
                                )
                        ),
                        new Poll(
                                "poll3",
                                "Ce alte platforme folosesti pentru a gasi o chirie?",
                                Map.of(
                                        "Agentii imobiliare", 0,
                                        "Facebook", 0,
                                        "Storia_ro", 0,
                                        "Imobiliare_ro", 0,
                                        "Olx_ro", 0
                                )
                        )
                ));

                LOGGER.info("Default polls inserted with fixed IDs.");
            } else {
                LOGGER.info("Polls already exist, skipping initialization.");
            }
        };
    }
}
