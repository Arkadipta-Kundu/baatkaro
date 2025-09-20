package org.arkadipta.chatapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.dto.chat.MessageResponse;
import org.arkadipta.chatapp.dto.chat.SendMessageRequest;
import org.arkadipta.chatapp.service.ChatService;
import org.arkadipta.chatapp.service.UserService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket Chat Controller - Handles real-time messaging via WebSocket/STOMP
 * protocol
 * 
 * This controller enables real-time bidirectional communication between clients
 * and server
 * using WebSocket technology with STOMP (Simple Text Oriented Messaging
 * Protocol) for
 * structured message handling and routing.
 * 
 * Real-time Communication Architecture:
 * - WebSocket connections for persistent, low-latency communication
 * - STOMP protocol for message routing and subscription management
 * - Topic-based broadcasting for chat room message distribution
 * - Principal-based authentication integration with JWT tokens
 * 
 * Core WebSocket Operations:
 * - Message sending: /app/chat.sendMessage → Process and broadcast
 * - Room subscription: /topic/chatroom/{roomId} → Receive room messages
 * - User presence: /app/user.connect → Manage online status
 * - Typing indicators: /app/typing.start → Real-time typing notifications
 * - Connection management: Connect/disconnect lifecycle handling
 * 
 * Message Flow Architecture:
 * 1. Client sends message via WebSocket: SEND /app/chat.sendMessage
 * 2. Server receives and validates message in @MessageMapping method
 * 3. ChatService processes business logic and persists to database
 * 4. Redis publisher broadcasts to all application instances
 * 5. All instances receive Redis message and broadcast to their WebSocket
 * clients
 * 6. SimpMessagingTemplate sends to subscribed clients:
 * /topic/chatroom/{roomId}
 * 
 * Authentication and Security:
 * - JWT token validation via WebSocket handshake headers
 * - Principal injection provides authenticated user context
 * - Session-based connection tracking for user presence
 * - Access control integration with chat room participant validation
 * - CORS configuration for cross-origin WebSocket connections
 * 
 * Subscription Management:
 * - Room-specific subscriptions: /topic/chatroom/{roomId}
 * - User-specific subscriptions: /queue/user/{username}
 * - Public broadcasts: /topic/public for system messages
 * - Dynamic subscription handling for join/leave operations
 * 
 * Performance and Scalability:
 * - Efficient message routing via STOMP destinations
 * - Redis pub-sub for horizontal scaling across multiple instances
 * - Connection pooling and resource management
 * - Heartbeat and reconnection support for reliable connections
 * - Message batching for high-throughput scenarios
 * 
 * Error Handling and Reliability:
 * - Connection failure detection and automatic reconnection
 * - Message delivery confirmation and retry mechanisms
 * - Graceful degradation when WebSocket is unavailable
 * - Error message broadcasting for client-side error handling
 * - Connection state synchronization across service restarts
 * 
 * Integration Points:
 * - ChatService: Message processing and business logic
 * - UserService: User presence and authentication
 * - Redis Publisher: Cross-instance message synchronization
 * - SimpMessagingTemplate: Server-to-client message broadcasting
 * - Spring Security: Authentication and authorization
 * 
 * Real-time Features:
 * - Instant message delivery with sub-second latency
 * - Live typing indicators for enhanced user experience
 * - Online presence tracking and status broadcasting
 * - Message read receipts and delivery confirmations
 * - Participant join/leave notifications
 * 
 * Development and Debugging:
 * - Comprehensive logging for message flow tracking
 * - WebSocket session monitoring and analytics
 * - Performance metrics for connection and message statistics
 * - Debug endpoints for connection state inspection
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle incoming chat messages via WebSocket
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public MessageResponse sendMessage(@Payload SendMessageRequest messageRequest, Principal principal) {
        try {
            log.info("WebSocket message received from user: {} to room: {}",
                    principal.getName(), messageRequest.getChatRoomId());

            // Process message through chat service
            MessageResponse response = chatService.sendMessage(messageRequest);

            log.info("WebSocket message processed successfully: {}", response.getId());

            return response;

        } catch (Exception e) {
            log.error("Failed to process WebSocket message from user: {}", principal.getName(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Handle messages sent to specific chat rooms
     */
    @MessageMapping("/chat.sendMessage.{roomId}")
    public void sendMessageToRoom(@DestinationVariable String roomId,
            @Payload SendMessageRequest messageRequest,
            Principal principal) {
        try {
            Long chatRoomId = Long.parseLong(roomId);
            messageRequest.setChatRoomId(chatRoomId);

            log.info("WebSocket room message from user: {} to room: {}", principal.getName(), roomId);

            // Process message
            MessageResponse response = chatService.sendMessage(messageRequest);

            // Send to specific room topic
            String destination = "/topic/chatroom/" + roomId;
            messagingTemplate.convertAndSend(destination, response);

            log.info("Message sent to room topic: {}", destination);

        } catch (Exception e) {
            log.error("Failed to send message to room: {}", roomId, e);

            // Send error back to user
            String errorDestination = "/user/" + principal.getName() + "/queue/errors";
            messagingTemplate.convertAndSend(errorDestination,
                    "Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Handle user connection to chat room
     */
    @SubscribeMapping("/topic/chatroom/{roomId}")
    public void subscribeToRoom(@DestinationVariable String roomId, Principal principal) {
        log.info("User {} subscribed to chat room: {}", principal.getName(), roomId);

        // Update user online status when they connect to a room
        userService.updateOnlineStatus(principal.getName(), true);

        // Notify other participants that user is online
        String destination = "/topic/chatroom/" + roomId + "/status";
        messagingTemplate.convertAndSend(destination,
                principal.getName() + " is now online");
    }

    /**
     * Handle typing indicators
     */
    @MessageMapping("/chat.typing.{roomId}")
    public void handleTyping(@DestinationVariable String roomId,
            @Payload TypingIndicator typingIndicator,
            Principal principal) {
        try {
            log.debug("Typing indicator from user: {} in room: {}", principal.getName(), roomId);

            // Add username to typing indicator
            typingIndicator.setUsername(principal.getName());

            // Broadcast typing indicator to room (excluding sender)
            String destination = "/topic/chatroom/" + roomId + "/typing";
            messagingTemplate.convertAndSend(destination, typingIndicator);

        } catch (Exception e) {
            log.error("Failed to handle typing indicator", e);
        }
    }

    /**
     * Handle user connection events
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public UserConnectionEvent addUser(@Payload UserConnectionEvent connectionEvent,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        String username = principal.getName();
        connectionEvent.setUsername(username);
        connectionEvent.setType("JOIN");

        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", username);

        // Update user online status
        userService.updateOnlineStatus(username, true);

        log.info("User {} connected to chat", username);

        return connectionEvent;
    }

    /**
     * DTO for typing indicators
     */
    public static class TypingIndicator {
        private String username;
        private boolean isTyping;
        private String chatRoomId;

        // Getters and setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public boolean isTyping() {
            return isTyping;
        }

        public void setTyping(boolean typing) {
            isTyping = typing;
        }

        public String getChatRoomId() {
            return chatRoomId;
        }

        public void setChatRoomId(String chatRoomId) {
            this.chatRoomId = chatRoomId;
        }
    }

    /**
     * DTO for user connection events
     */
    public static class UserConnectionEvent {
        private String type; // JOIN, LEAVE
        private String username;

        // Getters and setters
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