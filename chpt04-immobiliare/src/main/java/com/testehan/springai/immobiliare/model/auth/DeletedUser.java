package com.testehan.springai.immobiliare.model.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@Getter
@Setter
@Document(collection = "users_deleted")
public class DeletedUser {

    @Id
    private String email;
    private Integer searchesAvailable;
    private String deletionDate;
}
