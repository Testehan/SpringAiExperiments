package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import org.bson.types.ObjectId;

public interface ImmobiliareUserRepository {
    ImmobiliareUser findUserByEmail(String email);

    void updateAuthenticationType(ObjectId id, AuthenticationType authenticationType);

    void save(ImmobiliareUser user);
}
