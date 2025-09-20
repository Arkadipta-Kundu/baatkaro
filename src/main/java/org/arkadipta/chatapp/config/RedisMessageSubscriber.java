package org.arkadipta.chatapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.dto.chat.MessageResponse;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis message subscriber for handling pub-sub messages
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());

            log.debug("Received Redis message on channel: {}", channel);

            if (RedisConfig.CHAT_TOPIC.equals(channel)) {
                handleChatMessage(messageBody);
            } else if (RedisConfig.USER_STATUS_TOPIC.equals(channel)) {
                handleUserStatusMessage(messageBody);
            }

        } catch (Exception e) {
            log.error("Failed to process Redis message", e);
        }
    }

    public void receiveMessage(String message) {
        log.debug("Received message via adapter: {}", message);
        // This method is called by MessageListenerAdapter
    }

    private void handleChatMessage(String messageBody) {
        try {
            MessageResponse messageResponse = objectMapper.readValue(messageBody, MessageResponse.class);

            // Broadcast to WebSocket clients
            String destination = "/topic/chatroom/" + messageResponse.getChatRoomId();
            messagingTemplate.convertAndSend(destination, messageResponse);

            log.debug("Chat message broadcasted to: {}", destination);

        } catch (Exception e) {
            log.error("Failed to handle chat message", e);
        }
    }

    private void handleUserStatusMessage(String messageBody) {
        try {
            // Handle user status updates (online/offline)
            messagingTemplate.convertAndSend("/topic/user-status", messageBody);

            log.debug("User status message broadcasted");

        } catch (Exception e) {
            log.error("Failed to handle user status message", e);
        }
    }
}