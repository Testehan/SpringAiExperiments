package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.events.Event;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSseService {
    // Map to hold session ID TO user SSE UUID pairs
    private final Map<String, String> userSseUuids = new ConcurrentHashMap<>();

    // sse connections where key is UUID
    private final Map<String, Sinks.Many<Event>> userSseConnections = new ConcurrentHashMap<>();

    public Sinks.Many<Event> getUserSseConnection(String sessionId) {
        var sseUuid = userSseUuids.get(sessionId);
        return userSseConnections.get(sseUuid);
    }

    public String addUserSseId(String userSessionId) {
        if (!userSseUuids.containsKey(userSessionId)) {
            UUID userSseId = UUID.randomUUID();
            userSseUuids.put(userSessionId, userSseId.toString());
            Sinks.Many<Event> sink =  Sinks.many().multicast().onBackpressureBuffer();
            userSseConnections.put(userSseId.toString(), sink);
            return userSseId.toString();
        } else {
            return userSseUuids.get(userSessionId);
        }
    }

    public void removeUserSseId(String userSessionId) {
        userSseUuids.remove(userSessionId);
    }

    public boolean isUserLoggedIn(String userSessionId) {
        return userSseUuids.containsKey(userSessionId);
    }
}
