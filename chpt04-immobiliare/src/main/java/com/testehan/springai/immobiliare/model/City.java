package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document(collection = "cities")
public class City {

    private ObjectId id;
    private String name;        // shown to users Bucuresti
    private String slug;        // internal name bucharest
    private boolean isEnabled;

    private int requestCount;

    public City(String name, String slug, boolean isEnabled, int requestCount) {
        this.name = name;
        this.slug = slug;
        this.isEnabled = isEnabled;
        this.requestCount = requestCount;
    }

}
