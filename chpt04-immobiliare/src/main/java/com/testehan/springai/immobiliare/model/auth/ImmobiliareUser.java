package com.testehan.springai.immobiliare.model.auth;

import com.testehan.springai.immobiliare.model.PropertyType;
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

    private String phoneNumber;

    private String name;

    private String password;

    private String refreshToken;

    private AuthenticationType authenticationType;

    private List<String> favouriteProperties;

    private int maxNumberOfListedProperties;

    private List<String> listedProperties;

    private String city;

    private String propertyType;

    private String lastPropertyDescription;

    private Integer searchesAvailable;      // a way to limit user usage of the app
    private Integer maxSearchesAvailable;
    private String inviteUuid;

    private String isAdmin;

    private Boolean gdprConsent;
    private String gdprTimestamp;

    public ImmobiliareUser() {
        this.favouriteProperties = new ArrayList<>();
        this.maxNumberOfListedProperties = 1;
        this.listedProperties = new ArrayList<>();
        this.city = "Cluj-Napoca";
        this.propertyType = PropertyType.rent.name();
        this.lastPropertyDescription = "";
        this.searchesAvailable = 10;
        this.maxSearchesAvailable = 10;
    }

    public boolean isAdmin(){
        return isAdmin.equalsIgnoreCase("true");
    }
}
