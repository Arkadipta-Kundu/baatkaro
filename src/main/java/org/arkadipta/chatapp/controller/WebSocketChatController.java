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
 * WebSocket controller for real-time chat messaging
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