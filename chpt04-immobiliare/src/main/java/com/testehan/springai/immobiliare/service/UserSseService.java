package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSseService.class);

    // Map to hold session ID TO user SSE UUID pairs
    private final Map<String, String> userSseUuids = new ConcurrentHashMap<>();

    // sse connections where key is UUID
    private final Map<String, Sinks.Many<Event>> userSseConnections = new ConcurrentHashMap<>();

    public Sinks.Many<Event> getUserSseConnection(String sessionId) {
        LOGGER.info("Getting user SSE connection for {}", sessionId );
        var sseUuid = userSseUuids.get(sessionId);
        if (Objects.isNull(sseUuid)){
            sseUuid = addUserSseId(sessionId);
        }
        LOGGER.info("SSE uuid = {} and connection exists = {}", sseUuid, userSseConnections.containsKey(sseUuid) );
        return userSseConnections.get(sseUuid);
    }

    public String addUserSseId(String userSessionId) {
        if (!userSseUuids.containsKey(userSessionId)) {
            LOGGER.info("User session {} does not contain a SSE id", userSessionId );
            UUID userSseId = UUID.randomUUID();
            userSseUuids.put(userSessionId, userSseId.toString());
            Sinks.Many<Event> sink =  Sinks.many().multicast().onBackpressureBuffer();
            userSseConnections.put(userSseId.toString(), sink);
            return userSseId.toString();
        } else {
            LOGGER.info("User session {} already contains a SSE id", userSessionId );
            return userSseUuids.get(userSessionId);
        }
    }

    public void removeUserSseId(String userSessionId) {
        String sseUuid = userSseUuids.remove(userSessionId);
        if (sseUuid != null) {
            var sink = userSseConnections.remove(sseUuid);
        }


    }

    public boolean isUserLoggedIn(String userSessionId) {
        return userSseUuids.containsKey(userSessionId);
    }
}
