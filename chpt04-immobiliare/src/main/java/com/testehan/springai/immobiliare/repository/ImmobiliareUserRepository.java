package com.testehan.springai.immobiliare.repository;

import com.testehan.springai.immobiliare.model.auth.AuthenticationType;
import com.testehan.springai.immobiliare.model.auth.ImmobiliareUser;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface ImmobiliareUserRepository {
    Optional<ImmobiliareUser> findUserByEmail(String email);

    Optional<ImmobiliareUser> findUserByInviteUuid(String inviteUuid);

    void updateAuthenticationType(ObjectId id, AuthenticationType authenticationType);

    void save(ImmobiliareUser user);

    ImmobiliareUser update(ImmobiliareUser user);

    void deleteById(final ObjectId id);

    void resetSearchesAvailable();

    List<ImmobiliareUser> findAdminUsers();
}
