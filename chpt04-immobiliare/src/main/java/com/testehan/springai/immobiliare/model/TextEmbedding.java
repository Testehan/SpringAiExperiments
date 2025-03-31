package com.testehan.springai.immobiliare.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.List;

// if a user input already has an embedding calculated, we use it to speed things up
@NoArgsConstructor
@Getter
@Setter
public class TextEmbedding {

    @Id
    private String id;  // Hash of text
    private String text;
    private long usageCount;
    private List<Double> embedding;

    public TextEmbedding(String textHash, String text, List<Double> newEmbedding) {
        this.id = textHash;
        this.text = text;
        this.usageCount = 1;
        this.embedding = newEmbedding;
    }
}
