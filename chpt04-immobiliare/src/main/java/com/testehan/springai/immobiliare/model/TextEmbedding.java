package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TextEmbedding {

    @Id
    private String id;  // Hash of text
    private String text;
    private List<Double> embedding;

}
