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
 * Redis message publisher for pub-sub messaging
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "messaging.type", havingValue = "redis", matchIfMissing = true)
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish chat message to Redis
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
     * Publish user status update to Redis
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
     * DTO for user status messages
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