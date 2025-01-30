package com.testehan.springai.immobiliare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AmenityCategory {
    private String category;
    private List<String> items;

    public AmenityCategory copy() {  // Copy constructor or clone method in your object class
        return new AmenityCategory(this.category, this.items);
    }
}
