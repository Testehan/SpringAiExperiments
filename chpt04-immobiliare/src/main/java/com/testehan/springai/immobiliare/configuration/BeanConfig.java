package com.testehan.springai.immobiliare.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
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

    @Bean
    @Primary
    public ChatModel getGeminiChatModel(VertexAiGeminiChatModel vertexAiGeminiChatModel){
        return vertexAiGeminiChatModel;
    }

//    @Bean
//    @Primary
//    public ChatModel getGeminiChatModel(OpenAiChatModel openAiChatModel){
//        return openAiChatModel;
//    }

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
}
