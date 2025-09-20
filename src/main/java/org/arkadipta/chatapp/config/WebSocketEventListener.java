package org.arkadipta.chatapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket event listener for handling user connections and disconnections
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final UserService userService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("WebSocket connection established");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null) {
            log.info("User {} disconnected from WebSocket", username);

            try {
                // Update user online status
                userService.updateOnlineStatus(username, false);

                // Broadcast user disconnection
                UserLeaveEvent leaveEvent = new UserLeaveEvent();
                leaveEvent.setUsername(username);
                leaveEvent.setType("LEAVE");

                messagingTemplate.convertAndSend("/topic/public", leaveEvent);

                log.info("User {} status updated to offline", username);

            } catch (Exception e) {
                log.error("Failed to handle user disconnection for: {}", username, e);
            }
        }
    }

    /**
     * DTO for user leave events
     */
    public static class UserLeaveEvent {
        private String type;
        private String username;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}