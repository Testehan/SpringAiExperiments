package com.testehan.springai.immobiliare.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


// since i don't have a verified meta business, i have to use short duration 1 hour tokens
// which can be a pain...so the purpose of this service is to fetch a new short duration token
// every 50 mins
@Service
public class WhatsAppTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppTokenService.class);

    @Value("${whatsapp.api.app.id}")
    private String appId;

    @Value("${whatsapp.api.app.secret}")
    private String appSecret;

    @Value("${whatsapp.api.short.token}")
    private String shortLivedToken;

    @Value("${whatsapp.api.token.url}")
    private String tokenUrl;

    private String currentToken;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        this.currentToken = this.shortLivedToken;
        refreshToken();
    }

    public String getToken() {
        return currentToken;
    }

    // todo for now i use another service
//    @Scheduled(fixedDelay = 50 * 60 * 1000) // refresh every 50 min
    public void scheduledRefresh() {
        refreshToken();
    }

    private void refreshToken() {
//        try {
//            String url = UriComponentsBuilder.fromHttpUrl(tokenUrl)
//                    .queryParam("grant_type", "fb_exchange_token")
//                    .queryParam("client_id", appId)
//                    .queryParam("client_secret", appSecret)
//                    .queryParam("fb_exchange_token", currentToken)
//                    .toUriString();
//
//            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
//            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                currentToken = (String) response.getBody().get("access_token");
//                LOGGER.info("Refreshed WhatsApp token: {}", currentToken);
//            } else {
//                LOGGER.error("Failed to refresh token: {}", response.getStatusCode());
//            }
//        } catch (Exception e) {
//            LOGGER.error("Exception refreshing WhatsApp token: {}" , e.getMessage());
//        }
    }
}