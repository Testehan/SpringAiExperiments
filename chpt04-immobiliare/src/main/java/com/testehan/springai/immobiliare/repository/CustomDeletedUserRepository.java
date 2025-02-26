package com.testehan.springai.immobiliare.repository;

import java.time.LocalDateTime;

public interface CustomDeletedUserRepository {

    void deleteDeletedUsers(LocalDateTime date);
}
