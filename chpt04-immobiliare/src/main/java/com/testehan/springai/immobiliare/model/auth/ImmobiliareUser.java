package com.testehan.springai.immobiliare.model.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class ImmobiliareUser {

    private ObjectId id;

    private String email;

    private String name;

    private String password;

    private AuthenticationType authenticationType;

    private List<String> favouriteProperties;

    private int maxNumberOfListedProperties;

    private List<String> listedProperties;

    private String city;

    private String propertyType;

    private String lastPropertyDescription;

    private Integer searchesAvailable;      // a way to limit user usage of the app

    public ImmobiliareUser() {
        this.favouriteProperties = new ArrayList<>();
        this.maxNumberOfListedProperties = 1;
        this.listedProperties = new ArrayList<>();
        this.city = "";
        this.propertyType = "";
        this.lastPropertyDescription = "";
        this.searchesAvailable = 100;           // Todo this is for now ..in the future it will be a smaller number
    }
}
