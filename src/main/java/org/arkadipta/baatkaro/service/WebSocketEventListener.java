package org.arkadipta.baatkaro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    // Track active sessions
    private final Map<String, String> activeUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Get username from session attributes or from user principal
        String username = null;

        // Try to get username from session attributes first
        if (headerAccessor.getSessionAttributes() != null) {
            username = (String) headerAccessor.getSessionAttributes().get("username");
        }

        // If not found, try to get from user principal
        if (username == null && headerAccessor.getUser() != null) {
            username = headerAccessor.getUser().getName();
        }

        if (username != null) {
            activeUsers.put(sessionId, username);

            logger.info("User connected: {} (Session: {})", username, sessionId);

            // Broadcast user online status
            messagingTemplate.convertAndSend("/topic/user.status",
                    Map.of("username", username, "online", true));
        } else {
            logger.warn("WebSocket connection without username (Session: {})", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String username = activeUsers.remove(sessionId);

        if (username != null) {
            logger.info("User disconnected: {} (Session: {})", username, sessionId);

            // Broadcast user offline status
            messagingTemplate.convertAndSend("/topic/user.status",
                    Map.of("username", username, "online", false));
        }
    }

    public Map<String, String> getActiveUsers() {
        return new ConcurrentHashMap<>(activeUsers);
    }
}