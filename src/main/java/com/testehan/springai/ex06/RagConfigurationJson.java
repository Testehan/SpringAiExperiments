package com.testehan.springai.ex06;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
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
    SimpleVectorStore immobiliareVectorStore(EmbeddingClient embeddingClient) {
        // (from the documentation) SimpleVectorStore - A simple implementation of persistent vector storage,
        // good for educational purposes.
        var simpleVectorStore = new SimpleVectorStore(embeddingClient);
        var vectorStoreFile = new File(vectorStorePath);
        if (vectorStoreFile.exists()) {
            System.out.println("Vector Store File Exists,");
            simpleVectorStore.load(vectorStoreFile);
        } else {
            System.out.println("Vector Store File Does Not Exist, load documents");
            JsonReader jsonReader = new JsonReader(immobiliareData);
            List<Document> documents = jsonReader.get();
            // TODO I think these lines of code are only needed when you are dealing with a large text file, not with json
//            TextSplitter textSplitter = new TokenTextSplitter();
//            List<Document> splitDocuments = textSplitter.apply(documents);
//            simpleVectorStore.add(splitDocuments);
            simpleVectorStore.add(documents);
            simpleVectorStore.save(vectorStoreFile);
        }
        return simpleVectorStore;
    }
}
