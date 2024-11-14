package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.model.EmbeddingResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {
    private static final String OPENAI_API_URL = "https://api.openai.com";

    @Value("${OPEN_API_KEY}")
    private String OPENAI_API_KEY;

    private WebClient webClient;

    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    public OpenAiService(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel){
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
    }

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                .build();
    }


    public Mono<List<Double>> createEmbedding(String text) {
        Map<String, Object> body = Map.of(
                "model", "text-embedding-ada-002",
                "input", text
        );

        return webClient.post()
                .uri("/v1/embeddings")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .map(EmbeddingResponse::getEmbedding);
    }

    public String transcribeAudioMessage(Resource audioFile){
        var responseFormat = OpenAiAudioApi.TranscriptResponseFormat.TEXT;

        var transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .withLanguage("en")
                .withTemperature(0f)
                .withResponseFormat(responseFormat)
                .build();
        var transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        var response = openAiAudioTranscriptionModel.call(transcriptionRequest);

        var transcribedAudioMessage = response.getResult().getOutput();
        return transcribedAudioMessage;
    }
}
