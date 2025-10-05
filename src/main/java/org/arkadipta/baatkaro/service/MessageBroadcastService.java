package org.arkadipta.baatkaro.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.baatkaro.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for broadcasting messages across multiple server instances using
 * Redis pub-sub
 * This enables horizontal scalability by allowing users on different servers to
 * communicate
 */
@Slf4j
@Service
public class MessageBroadcastService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private RedisMessageListenerContainer messageListenerContainer;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper;
    private boolean redisAvailable = false;

    // Initialize ObjectMapper with proper configuration
    public MessageBroadcastService() {
        this.objectMapper = new ObjectMapper();
        // Configure to ignore unknown properties (redundant with annotation but
        // explicit)
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        // Handle missing properties gracefully
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                false);
    }

    // Redis channels for different message types
    private static final String ROOM_CHANNEL = "room_messages";
    private static final String PRIVATE_CHANNEL = "private_messages";
    private static final String USER_ACTIVITY_CHANNEL = "user_activity";

    /**
     * Subscribe to Redis channels when service starts
     * This runs automatically after Spring creates this service bean
     * Falls back to local-only mode if Redis is not available
     */
    @PostConstruct
    public void subscribeToChannels() {
        if (redisTemplate != null && messageListenerContainer != null) {
            try {
                log.info("Subscribing to Redis channels for cross-server messaging");

                // Subscribe to room messages
                messageListenerContainer.addMessageListener(
                        (message, pattern) -> handleRoomMessage(new String(message.getBody())),
                        new ChannelTopic(ROOM_CHANNEL));

                // Subscribe to private messages
                messageListenerContainer.addMessageListener(
                        (message, pattern) -> handlePrivateMessage(new String(message.getBody())),
                        new ChannelTopic(PRIVATE_CHANNEL));

                // Subscribe to user activity (join/leave events)
                messageListenerContainer.addMessageListener(
                        (message, pattern) -> handleUserActivity(new String(message.getBody())),
                        new ChannelTopic(USER_ACTIVITY_CHANNEL));

                redisAvailable = true;
                log.info("Successfully subscribed to Redis channels: {}, {}, {}",
                        ROOM_CHANNEL, PRIVATE_CHANNEL, USER_ACTIVITY_CHANNEL);
            } catch (Exception e) {
                log.warn("Failed to subscribe to Redis channels, falling back to local-only mode: {}", e.getMessage());
                redisAvailable = false;
            }
        } else {
            log.warn("Redis not available, running in local-only mode");
            redisAvailable = false;
        }
    }

    /**
     * Publish room message to Redis for cross-server broadcasting
     * Falls back to local WebSocket if Redis is not available
     */
    public void publishRoomMessage(ChatMessage chatMessage) {
        if (redisAvailable && redisTemplate != null) {
            try {
                String messageJson = objectMapper.writeValueAsString(chatMessage);
                redisTemplate.convertAndSend(ROOM_CHANNEL, messageJson);

                log.debug("Published room message to Redis: roomId={}, sender={}",
                        chatMessage.getRoomId(), chatMessage.getSender());
            } catch (Exception e) {
                log.error("Failed to publish room message to Redis, sending locally: {}", e.getMessage());
                sendRoomMessageLocally(chatMessage);
            }
        } else {
            log.debug("Redis not available, sending room message locally only");
            sendRoomMessageLocally(chatMessage);
        }
    }

    /**
     * Publish private message to Redis for cross-server delivery
     * Falls back to local WebSocket if Redis is not available
     */
    public void publishPrivateMessage(ChatMessage chatMessage) {
        if (redisAvailable && redisTemplate != null) {
            try {
                String messageJson = objectMapper.writeValueAsString(chatMessage);
                redisTemplate.convertAndSend(PRIVATE_CHANNEL, messageJson);

                log.debug("Published private message to Redis: from={} to={}",
                        chatMessage.getSender(), chatMessage.getReceiver());
            } catch (Exception e) {
                log.error("Failed to publish private message to Redis, sending locally: {}", e.getMessage());
                sendPrivateMessageLocally(chatMessage);
            }
        } else {
            log.debug("Redis not available, sending private message locally only");
            sendPrivateMessageLocally(chatMessage);
        }
    }

    /**
     * Publish user activity (join/leave) to Redis for cross-server broadcasting
     * Falls back to local WebSocket if Redis is not available
     */
    public void publishUserActivity(ChatMessage activityMessage) {
        if (redisAvailable && redisTemplate != null) {
            try {
                String messageJson = objectMapper.writeValueAsString(activityMessage);
                redisTemplate.convertAndSend(USER_ACTIVITY_CHANNEL, messageJson);

                log.debug("Published user activity to Redis: type={}, user={}, room={}",
                        activityMessage.getType(), activityMessage.getSender(), activityMessage.getRoomId());
            } catch (Exception e) {
                log.error("Failed to publish user activity to Redis, sending locally: {}", e.getMessage());
                sendUserActivityLocally(activityMessage);
            }
        } else {
            log.debug("Redis not available, sending user activity locally only");
            sendUserActivityLocally(activityMessage);
        }
    }

    /**
     * Handle incoming room messages from Redis
     * Forwards to local WebSocket connections
     */
    private void handleRoomMessage(String messageJson) {
        try {
            log.debug("Attempting to deserialize room message: {}", messageJson);
            ChatMessage message = objectMapper.readValue(messageJson, ChatMessage.class);

            // Validate that we have required fields
            if (message.getRoomId() == null) {
                log.warn("Received room message without roomId, skipping: {}", messageJson);
                return;
            }

            // Broadcast to all users in the room via local WebSocket
            messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);

            log.debug("Broadcasted Redis room message to local WebSocket: roomId={}, sender={}",
                    message.getRoomId(), message.getSender());
        } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException e) {
            log.error("JSON deserialization failed for room message. JSON: {}, Error: {}",
                    messageJson, e.getMessage());
            // Don't let Redis failures break local messaging
        } catch (Exception e) {
            log.error("Failed to handle room message from Redis: {}", messageJson, e);
            // Don't let Redis failures break local messaging
        }
    }

    /**
     * Handle incoming private messages from Redis - NEW IMPLEMENTATION
     * Uses dedicated conversation topics instead of user queues for better
     * real-time delivery
     */
    private void handlePrivateMessage(String messageJson) {
        try {
            log.debug("Attempting to deserialize private message: {}", messageJson);
            ChatMessage message = objectMapper.readValue(messageJson, ChatMessage.class);
            String sender = message.getSender();
            String recipient = message.getReceiver();

            // Validate that we have required fields
            if (recipient == null || recipient.trim().isEmpty() || sender == null || sender.trim().isEmpty()) {
                log.warn("Received private message without valid sender/receiver, skipping: {}", messageJson);
                return;
            }

            // NEW APPROACH: Use consistent conversation ID and broadcast to dedicated topic
            String conversationId = createConversationId(sender, recipient);
            String conversationTopic = "/topic/private/" + conversationId;

            // Broadcast to conversation topic - all subscribers to this conversation will
            // receive it
            messagingTemplate.convertAndSend(conversationTopic, message);

            log.debug("Delivered Redis private message to conversation topic {}: from={} to={}",
                    conversationTopic, sender, recipient);
        } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException e) {
            log.error("JSON deserialization failed for private message. JSON: {}, Error: {}",
                    messageJson, e.getMessage());
            // Don't let Redis failures break local messaging
        } catch (Exception e) {
            log.error("Failed to handle private message from Redis: {}", messageJson, e);
        }
    }

    /**
     * Helper method to create consistent conversation IDs for private chats
     * Ensures both users get the same conversation ID regardless of who initiated
     */
    private String createConversationId(String user1, String user2) {
        // Sort usernames alphabetically to ensure consistent conversation ID
        if (user1.compareTo(user2) <= 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }

    /**
     * Handle incoming user activity from Redis
     * Broadcasts join/leave events to room participants
     */
    private void handleUserActivity(String messageJson) {
        try {
            log.debug("Attempting to deserialize user activity message: {}", messageJson);
            ChatMessage message = objectMapper.readValue(messageJson, ChatMessage.class);

            // Validate that we have required fields
            if (message.getRoomId() == null) {
                log.warn("Received user activity message without roomId, skipping: {}", messageJson);
                return;
            }

            // Broadcast activity to room participants via local WebSocket
            messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);

            log.debug("Broadcasted Redis user activity to local WebSocket: type={}, user={}, room={}",
                    message.getType(), message.getSender(), message.getRoomId());
        } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException e) {
            log.error("JSON deserialization failed for user activity message. JSON: {}, Error: {}",
                    messageJson, e.getMessage());
            // Don't let Redis failures break local messaging
        } catch (Exception e) {
            log.error("Failed to handle user activity from Redis: {}", messageJson, e);
            // Don't let Redis failures break local messaging
        }
    }

    // Local fallback methods when Redis is not available

    private void sendRoomMessageLocally(ChatMessage message) {
        try {
            if (message.getRoomId() != null) {
                messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);
                log.debug("Sent room message locally: room={}, sender={}",
                        message.getRoomId(), message.getSender());
            }
        } catch (Exception e) {
            log.error("Failed to send room message locally: {}", e.getMessage());
        }
    }

    private void sendPrivateMessageLocally(ChatMessage message) {
        try {
            String sender = message.getSender();
            String receiver = message.getReceiver();

            // NEW APPROACH: Use conversation topic for local delivery too
            if (sender != null && receiver != null && !sender.trim().isEmpty() && !receiver.trim().isEmpty()) {
                String conversationId = createConversationId(sender, receiver);
                String conversationTopic = "/topic/private/" + conversationId;

                messagingTemplate.convertAndSend(conversationTopic, message);
                log.debug("Sent private message locally to conversation topic {}: from={} to={}",
                        conversationTopic, sender, receiver);
            } else {
                log.warn("Cannot send private message locally - missing sender or receiver info");
            }
        } catch (Exception e) {
            log.error("Failed to send private message locally: {}", e.getMessage());
        }
    }

    private void sendUserActivityLocally(ChatMessage message) {
        try {
            if (message.getRoomId() != null) {
                messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);
                log.debug("Sent user activity locally: type={}, user={}, room={}",
                        message.getType(), message.getSender(), message.getRoomId());
            }
        } catch (Exception e) {
            log.error("Failed to send user activity locally: {}", e.getMessage());
        }
    }
}