package com.testehan.springai.immobiliare.service;

import jakarta.servlet.http.HttpSession;

public interface ApiService {

    String getChatResponse(HttpSession session, String message);
}
