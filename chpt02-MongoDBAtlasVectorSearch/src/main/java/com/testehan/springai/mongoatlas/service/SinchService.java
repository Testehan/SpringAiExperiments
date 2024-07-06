package com.testehan.springai.mongoatlas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class SinchService {
    @Value("${SINCH_KEY}")
    private String SINCH_KEY;
    @Value("${SINCH_SECRET}")
    private String SINCH_SECRET;
    @Value("${SINCH_FROM_NUMBER}")
    private String SINCH_FROM_NUMBER ;
    @Value("${SINCH_TO_NUMBER}")
    private String SINCH_TO_NUMBER;
    private static final String locale = "ro-RO";

    public void sendMessage(String message) throws Exception {
        var httpClient = HttpClient.newBuilder().build();

        var payload = """
               {
                    "method": "ttsCallout",
                    "ttsCallout": {
                        "cli": "%fromNumber%",
                        "destination": {
                            "type": "number",
                            "endpoint": "%to%"
                        },
                        "locale": "%locale%",
                        "text": "%message%"
                    }
               }
               """; //.formatted(fromNumber, to, locale, message);

        payload = payload
                .replace("%fromNumber%", SINCH_FROM_NUMBER)
                .replace("%to%", SINCH_TO_NUMBER)
                .replace("%locale%", locale)
                .replace("%message%", message);

        var host = "https://calling.api.sinch.com";
        var pathname = "/calling/v1/callouts";
        var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(host + pathname))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((SINCH_KEY + ":" + SINCH_SECRET).getBytes()))
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }
}