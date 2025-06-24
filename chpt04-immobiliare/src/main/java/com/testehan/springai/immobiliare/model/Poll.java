package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document("polls")
public class Poll {
    @Id
    private String id;
    private String question;
    private Map<String, Integer> options;
}
