package com.testehan.springai.immobiliare.service;

import com.testehan.springai.immobiliare.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class UserSseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSseService.class);

    // Map to hold session ID TO user SSE UUID pairs
    private final Map<String, String> activeSseIdBySessionId = new ConcurrentHashMap<>();

    // sse connections where key is UUID
    private final Map<String, Sinks.Many<Event>> sseConnectionSinks = new ConcurrentHashMap<>();

    public Sinks.Many<Event> getUserSseConnection(String sessionId) {
        LOGGER.info("Getting user SSE connection for {}", sessionId );
        var sseUuid = activeSseIdBySessionId.get(sessionId);
        if (Objects.isNull(sseUuid)){
            sseUuid = addUserSseId(sessionId);
        }
        LOGGER.info("SSE uuid = {} and connection exists = {}", sseUuid, sseConnectionSinks.containsKey(sseUuid) );
        return sseConnectionSinks.get(sseUuid);
    }

    /**
     * Retrieves the Sink for the *currently active* SSE connection associated with a user session.
     * Used by services that need to send targeted events.
     *
     * @param sessionId The user's HTTP session ID.
     * @return The active Sink, or null if the user has no active connection registered.
     */
    public Sinks.Many<Event> getActiveUserSseConnection(String sessionId) {
        String activeSseId = activeSseIdBySessionId.get(sessionId);
        if (activeSseId == null) {
            LOGGER.warn("No active SSE connection ID found for session {}", sessionId);
            return null;
        }
        Sinks.Many<Event> sink = sseConnectionSinks.get(activeSseId);
        if (sink == null) {
            LOGGER.error("CRITICAL: Active SSE ID {} found for session {}, but no corresponding Sink exists in map!", activeSseId, sessionId);
            // Maybe clean up the inconsistent state?
            activeSseIdBySessionId.remove(sessionId, activeSseId); // Remove the dangling reference
        }
        return sink;
    }

    public String addUserSseId(String userSessionId) {
        if (!activeSseIdBySessionId.containsKey(userSessionId)) {
            LOGGER.info("User session {} does not contain a SSE id", userSessionId );
            UUID userSseId = UUID.randomUUID();
            activeSseIdBySessionId.put(userSessionId, userSseId.toString());
            Sinks.Many<Event> sink =  Sinks.many().multicast().onBackpressureBuffer();
            sseConnectionSinks.put(userSseId.toString(), sink);
            return userSseId.toString();
        } else {
            LOGGER.info("User session {} already contains a SSE id", userSessionId );
            return activeSseIdBySessionId.get(userSessionId);
        }
    }

    /**
     * Registers a new SSE connection or updates the existing one for a given session.
     * Should be called by the controller handling the /api/apartments/stream/{sseId} endpoint.
     *
     * @param sessionId       The user's HTTP session ID.
     * @param clientProvidedSseId The unique ID provided by the client for this specific connection attempt.
     * @return The Sink associated with the newly established/updated connection.
     */
    public Sinks.Many<Event> registerOrUpdateConnection(String sessionId, String clientProvidedSseId) {
        LOGGER.info("Registering/Updating SSE connection. SessionId: {}, ClientSseId: {}", sessionId, clientProvidedSseId);

        // 1. Clean up any *old* connection associated with this session
        String oldSseId = activeSseIdBySessionId.put(sessionId, clientProvidedSseId); // Atomically update and get the old value
        if (oldSseId != null && !oldSseId.equals(clientProvidedSseId)) {
            Sinks.Many<Event> oldSink = sseConnectionSinks.remove(oldSseId);
            if (oldSink != null) {
                LOGGER.warn("Session {} established new connection {}. Closing old connection {}.", sessionId, clientProvidedSseId, oldSseId);
                oldSink.tryEmitComplete(); // Complete the old Sink
            } else {
                LOGGER.warn("Session {} established new connection {}. Old connection {} sink was already removed.", sessionId, clientProvidedSseId, oldSseId);
            }
        } else if (oldSseId != null) {
            LOGGER.info("Session {} reconnected with the same SSE ID {}. Reusing existing sink.", sessionId, clientProvidedSseId);
            // If the ID is the same, we might not need to create a new sink, depends on whether the old connection truly died
            // For simplicity and robustness against edge cases, let's always create/replace the sink mapping below.
        }


        // 2. Create and store the Sink for the *new* clientProvidedSseId
        // Use computeIfAbsent to ensure only one sink is created even under concurrent requests for the same ID
        return sseConnectionSinks.computeIfAbsent(clientProvidedSseId, id -> {
            LOGGER.info("Creating new Sink for SSE connection ID: {}", id);
            return Sinks.many().multicast().onBackpressureBuffer();
        });
    }

    public void removeUserSseId(String userSessionId) {
        String sseUuid = activeSseIdBySessionId.remove(userSessionId);
        if (sseUuid != null) {
            var sink = sseConnectionSinks.remove(sseUuid);
            if (sink != null) {
                // Stop emitting further events
                sink.tryEmitComplete();
                LOGGER.info("Removed SSE connection for session {}", userSessionId);
            }
        }


    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    public void cleanupUnusedConnections() {
        LOGGER.info("Starting cleanup of unused SSE connections...");
        for (String sseUuid : sseConnectionSinks.keySet()) {
            var sink = sseConnectionSinks.get(sseUuid);
            if (sink != null && sink.currentSubscriberCount() == 0) {
                // If there are no subscribers, clean up this connection
                LOGGER.info("No subscribers found for SSE connection with UUID {}. Cleaning up.", sseUuid);
                sseConnectionSinks.remove(sseUuid);
                // Stop emitting further events
                sink.tryEmitComplete();

                // we also need to clean up the userSseUuid of the removed sseUuid
                for (Map.Entry<String, String> me : activeSseIdBySessionId.entrySet()){
                    if (me.getValue().equalsIgnoreCase(sseUuid)){
                        activeSseIdBySessionId.remove(me.getKey());
                        LOGGER.info("Cleaning up userSseUuids {} for session id {}", sseUuid, me.getKey());
                    }
                }
            }
        }
    }

    public boolean isUserLoggedIn(String userSessionId) {
        return activeSseIdBySessionId.containsKey(userSessionId);
    }
}
