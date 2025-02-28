package com.testehan.springai.immobiliare.controller;

import com.testehan.springai.immobiliare.service.UserSseService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SSEController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSEController.class);

    private final UserSseService userSseService;

    public SSEController(UserSseService userSseService) {
        this.userSseService = userSseService;
    }

    @GetMapping("/sse-id")
    public ResponseEntity<Map<String, String>> getSseId(HttpSession session){
        var sseId = userSseService.addUserSseId(session.getId());
        LOGGER.info("Obtained new SSE id {} for session {}", sseId, session.getId());
        return ResponseEntity.ok(Map.of("sseId",sseId));
    }

}
