package org.arkadipta.baatkaro.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserSessionManager {

    // Track user sessions: username -> Set of session IDs
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        //Extracts STOMP headers (metadata) from the event.
        //From this, you can access user info, session ID, and more.
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();
            String sessionId = headerAccessor.getSessionId();

            // Add session for user
            //If no entry for the user exists, create one.
            //Add the new session ID to that user’s set.
            userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet())
                    .add(sessionId);

            log.info("👤 User connected: {} (session: {})", username, sessionId);
            log.info("📊 Online users: {}", getOnlineUsers());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();
            String sessionId = headerAccessor.getSessionId();

            // Remove session
            Set<String> sessions = userSessions.get(username);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(username);
                }
            }

            log.info("👤 User disconnected: {} (session: {})", username, sessionId);
            log.info("📊 Online users: {}", getOnlineUsers());
        }
    }

    /**
     * Check if a user is currently online
     */
    public boolean isUserOnline(String username) {
        return userSessions.containsKey(username) &&
                !userSessions.get(username).isEmpty();
    }

    /**
     * Get all currently online users
     */
    public Set<String> getOnlineUsers() {
        return userSessions.keySet();
    }

    /**
     * Get session count for a user
     */
    public int getUserSessionCount(String username) {
        Set<String> sessions = userSessions.get(username);
        return sessions != null ? sessions.size() : 0;
    }
}