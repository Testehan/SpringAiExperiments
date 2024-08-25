package com.testehan.springai.immobiliare.model.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ImmobiliareUser {

    private ObjectId id;

    private String email;

    private String name;

    private String password;

    private AuthenticationType authenticationType;

    private List<String> favourites;

    private int maxNumberOfListedApartments;

}
