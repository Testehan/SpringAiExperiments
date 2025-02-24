package com.testehan.springai.immobiliare.security;

import com.testehan.springai.immobiliare.service.UserSseService;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class SessionCleanupListener implements HttpSessionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCleanupListener.class);

    private static final Sinks.Many<String> sessionEvents = Sinks.many().multicast().onBackpressureBuffer();

    private final UserSseService userSseService;

    public SessionCleanupListener(UserSseService userSseService) {
        this.userSseService = userSseService;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        String sessionId = event.getSession().getId();
        LOGGER.info("Session expired or destroyed: {}", sessionId);

        // Remove SSE connection when session expires
        userSseService.removeUserSseId(sessionId);

        sessionEvents.tryEmitNext(sessionId); // Notify when session is destroyed
    }

    public static Flux<String> getSessionDestroyedFlux() {
        return sessionEvents.asFlux();
    }
}
