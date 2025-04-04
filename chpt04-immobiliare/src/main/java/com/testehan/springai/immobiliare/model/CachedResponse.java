package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document(collection = "llm_cache")
public class CachedResponse {
    @Id
    private String inputHash;
    private String userInput;
    private String response;
    private long createdAt;

    private String city;
    private String propertyType;

}
