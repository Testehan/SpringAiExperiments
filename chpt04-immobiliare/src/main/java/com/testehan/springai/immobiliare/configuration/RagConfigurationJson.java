package com.testehan.springai.immobiliare.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.List;

@Configuration
public class RagConfigurationJson {

    @Value("/tmp/vectorstoreImmobiliare.json")
    private String vectorStorePath;

    @Value("classpath:/docs/immobiliare.json")
    private Resource immobiliareData;

    @Bean
    SimpleVectorStore immobiliareVectorStore(EmbeddingModel embeddingModel) {
        // (from the documentation) SimpleVectorStore - A simple implementation of persistent vector storage,
        // good for educational purposes.
        var simpleVectorStore = new SimpleVectorStore(embeddingModel);
        var vectorStoreFile = new File(vectorStorePath);
        if (vectorStoreFile.exists()) {
            System.out.println("Vector Store File Exists,");
            simpleVectorStore.load(vectorStoreFile);
        } else {
            System.out.println("Vector Store File Does Not Exist, load documents");
            JsonReader jsonReader = new JsonReader(immobiliareData,new ImmobiliareMetadataGenerator(),"id","name","location","shortDescription","price","surface","noOfRooms","floor","tags");
            List<Document> documents = jsonReader.get();

            simpleVectorStore.add(documents);
            simpleVectorStore.save(vectorStoreFile);
        }
        return simpleVectorStore;
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder){
        return builder.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }
}
