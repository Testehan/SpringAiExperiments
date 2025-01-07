package com.testehan.springai.immobiliare.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

@Configuration
@EnableAsync
public class BeanConfig {

    @Value("${OPEN_API_KEY}")
    private String OPENAI_API_KEY;
// TODO These properties should all be moved in 1 config class i guess...
    @Value("${AWS_IMOBIL_BUCKET_NAME}")
    private String bucketName;
    @Value("${AWS_REGION}")
    private String regionName;
    @Value("${AWS_IMOBIL_ACCESS_KEY_ID}")
    private String awsAccessKeyId;
    @Value("${AWS_IMOBIL_SECRET_ACCESS_KEY}")
    private String awsAccessSecret;
    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String googleAppCredentials;
    @Value("${google.maps.api-key}")
    private String googleMapsApiKey;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder){
        return builder.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    @Bean
    public ChatMemory chatMemory(){
        return new InMemoryChatMemory();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        // Can be any other EmbeddingModel implementation.
        return new OpenAiEmbeddingModel(new OpenAiApi(OPENAI_API_KEY));
    }

//    @Bean         // this doesn't work in a docker container see Spring AI Notes.txt
//    @Primary
//    public ChatModel getGeminiChatModel(VertexAiGeminiChatModel vertexAiGeminiChatModel){
//        return vertexAiGeminiChatModel;
//    }

    @Bean
    @Primary
    public ChatModel getOpenAiChatModel(OpenAiChatModel openAiChatModel){
        return openAiChatModel;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }


    @Bean
    public SpringWebFluxTemplateEngine thymeleafTemplateEngine(SpringResourceTemplateResolver templateResolver) {
        SpringWebFluxTemplateEngine templateEngine = new SpringWebFluxTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    public String getAwsAccessSecret() {
        return awsAccessSecret;
    }

    public String getGoogleMapsApiKey() {
        return googleMapsApiKey;
    }
}
