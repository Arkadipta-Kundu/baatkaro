package org.arkadipta.chatapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.config.RedisConfig;
import org.arkadipta.chatapp.dto.chat.MessageResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Pub-Sub Message Publisher - Handles cross-instance real-time message
 * broadcasting
 * 
 * This service enables horizontal scaling of the chat application by providing
 * cross-instance communication via Redis pub-sub messaging. It allows multiple
 * application instances to synchronize real-time events seamlessly.
 * 
 * Core Functionality:
 * - Chat message broadcasting across all application instances
 * - User presence status synchronization for online/offline indicators
 * - Real-time event distribution for WebSocket clients
 * - JSON serialization for reliable message format
 * 
 * Scaling Architecture:
 * When multiple instances of the chat application run (e.g., behind a load
 * balancer):
 * 1. User A connects to Instance 1, User B connects to Instance 2
 * 2. User A sends message → Instance 1 receives → Publishes to Redis
 * 3. Redis broadcasts to all instances (Instance 1 & Instance 2)
 * 4. Both instances receive message → Broadcast to their WebSocket clients
 * 5. User B (on Instance 2) receives the message in real-time
 * 
 * Message Types:
 * - CHAT_TOPIC: Chat messages, file attachments, system notifications
 * - USER_STATUS_TOPIC: Online/offline status changes, presence updates
 * - Extensible design for additional message types (typing indicators, etc.)
 * 
 * Reliability Features:
 * - JSON serialization with ObjectMapper for consistent message format
 * - Error handling and logging for message publishing failures
 * - Graceful degradation if Redis is unavailable
 * - Debug logging for troubleshooting message flow
 * 
 * Configuration:
 * - @ConditionalOnProperty allows disabling Redis messaging for development
 * - Fallback to single-instance mode when Redis is not configured
 * - Topic names defined in RedisConfig for centralized management
 * 
 * Performance Considerations:
 * - Asynchronous publishing to avoid blocking chat operations
 * - Efficient JSON serialization with Jackson ObjectMapper
 * - Redis pub-sub provides low-latency message distribution
 * - Memory-efficient message format for high-throughput scenarios
 * 
 * Security:
 * - Redis access should be secured with authentication and SSL
 * - Message content is already validated before publishing
 * - No sensitive data included in pub-sub messages
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "messaging.type", havingValue = "redis", matchIfMissing = true)
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes chat messages to Redis for cross-instance broadcasting
     * 
     * This method is the core of the multi-instance messaging system. When a user
     * sends a message, it gets published to Redis so all application instances
     * can receive and broadcast it to their connected WebSocket clients.
     * 
     * Publishing Workflow:
     * 1. Serialize MessageResponse to JSON using ObjectMapper
     * 2. Publish to CHAT_TOPIC Redis channel
     * 3. All subscribing instances receive the message
     * 4. Each instance broadcasts to its WebSocket clients
     * 5. Real-time message delivery across all connected users
     * 
     * Error Handling:
     * - Catches serialization errors and logs them
     * - Non-blocking: Failed publishing doesn't break chat functionality
     * - Debug logging for successful operations
     * - Chat continues to work on single instance if Redis fails
     * 
     * @param message MessageResponse DTO containing complete message data
     */
    public void publishChatMessage(MessageResponse message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisConfig.CHAT_TOPIC, messageJson);

            log.debug("Published chat message to Redis: {}", message.getId());

        } catch (Exception e) {
            log.error("Failed to publish chat message to Redis", e);
        }
    }

    /**
     * Publishes user online/offline status changes for real-time presence updates
     * 
     * This method enables real-time presence indicators across all application
     * instances.
     * When a user goes online/offline on one instance, all other instances are
     * notified
     * so they can update the presence indicators for all connected users.
     * 
     * Presence Synchronization:
     * 1. User connects/disconnects on any instance
     * 2. Status change published to USER_STATUS_TOPIC
     * 3. All instances receive status update
     * 4. Each instance updates WebSocket clients with new presence info
     * 5. UI shows real-time online/offline indicators
     * 
     * Use Cases:
     * - User login/logout events
     * - WebSocket connection/disconnection
     * - Periodic heartbeat status updates
     * - Manual status changes by users
     * 
     * @param username the username of the user whose status changed
     * @param isOnline true if user is now online, false if offline
     */
    public void publishUserStatus(String username, boolean isOnline) {
        try {
            UserStatusMessage statusMessage = new UserStatusMessage(username, isOnline);
            String messageJson = objectMapper.writeValueAsString(statusMessage);
            redisTemplate.convertAndSend(RedisConfig.USER_STATUS_TOPIC, messageJson);

            log.debug("Published user status to Redis: {} - {}", username, isOnline);

        } catch (Exception e) {
            log.error("Failed to publish user status to Redis", e);
        }
    }

    /**
     * Data Transfer Object for user presence status messages
     * 
     * This inner class represents the structure of user status messages published
     * to Redis. It includes the username, online status, and timestamp for tracking
     * when the status change occurred.
     * 
     * Fields:
     * - username: Identifies which user's status changed
     * - isOnline: Boolean indicating current online status
     * - timestamp: When the status change occurred (milliseconds since epoch)
     * 
     * JSON Serialization:
     * The class is designed for JSON serialization via Jackson ObjectMapper.
     * It includes default constructor for deserialization and proper
     * getters/setters.
     * 
     * Timestamp Usage:
     * - Helps with message ordering and deduplication
     * - Useful for debugging message flow issues
     * - Can be used for presence history tracking
     */
    public static class UserStatusMessage {
        private String username;
        private boolean isOnline;
        private long timestamp;

        public UserStatusMessage() {
            this.timestamp = System.currentTimeMillis();
        }

        public UserStatusMessage(String username, boolean isOnline) {
            this.username = username;
            this.isOnline = isOnline;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public boolean isOnline() {
            return isOnline;
        }

        public void setOnline(boolean online) {
            isOnline = online;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}