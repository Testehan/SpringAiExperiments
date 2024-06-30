package com.testehan.springai.ex06;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.List;

// RAG - Retrieval Augmented Generation.. see notes for more info

@Configuration
public class RagConfigurationText {

    // location on your machine where this vector store will be stored. The vectore store is used to send
    // better prompts (prompts with more context) to the LLM
    @Value("/tmp/vectorstore.json")
    private String vectorStorePath;

    @Value("classpath:/docs/olympic-faq.txt")
    private Resource faq;

    @Bean
    SimpleVectorStore olympicsVectorStore(EmbeddingClient embeddingClient) {
        // (from the documentation) SimpleVectorStore - A simple implementation of persistent vector storage,
        // good for educational purposes.
        var simpleVectorStore = new SimpleVectorStore(embeddingClient);
        var vectorStoreFile = new File(vectorStorePath);
        if (vectorStoreFile.exists()) {
            System.out.println("Vector Store File Exists,");
            simpleVectorStore.load(vectorStoreFile);
        } else {
            System.out.println("Vector Store File Does Not Exist, load documents");
            TextReader textReader = new TextReader(faq);
            textReader.getCustomMetadata().put("filename", "olympic-faq.txt");
            List<Document> documents = textReader.get();
            TextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(documents);
            simpleVectorStore.add(splitDocuments);
            simpleVectorStore.save(vectorStoreFile);
        }
        return simpleVectorStore;
    }

}
