package com.testehan.springai.immobiliare.repository;

public interface CustomCachedResponseRepository {

    void decreaseFieldByOne(String id, String fieldName);
}
