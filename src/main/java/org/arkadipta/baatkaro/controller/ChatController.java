package org.arkadipta.baatkaro.controller;

import org.arkadipta.baatkaro.dto.ChatMessage;
import org.arkadipta.baatkaro.dto.MessageType;
import org.arkadipta.baatkaro.entity.Message;
import org.arkadipta.baatkaro.service.MessageBroadcastService;
import org.arkadipta.baatkaro.service.MessageService;
import org.arkadipta.baatkaro.service.RoomService;
import org.arkadipta.baatkaro.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * WebSocket Controller for real-time chat messaging
 * Now with Redis pub-sub integration for multi-server scalability
 * 
 * Note: This remains as @Controller (not @RestController) because it handles
 * WebSocket messages, not HTTP REST requests
 */
@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private MessageBroadcastService broadcastService;

    /**
     * Handle private messages - NEW IMPLEMENTATION
     * Uses dedicated conversation topics instead of user queues for better
     * real-time delivery
     * Endpoint: /chat.private (unchanged from original)
     * Now publishes to Redis for cross-server delivery
     */
    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        try {
            String senderUsername = principal.getName();
            String receiverUsername = chatMessage.getReceiver();
            String content = chatMessage.getContent();

            logger.info("Private message from {} to {}: {}", senderUsername, receiverUsername, content);

            // Validate sender
            if (!senderUsername.equals(chatMessage.getSender())) {
                logger.warn("Sender mismatch: authenticated as {} but claims to be {}",
                        senderUsername, chatMessage.getSender());
                return;
            }

            // Validate receiver exists
            if (!userService.userExists(receiverUsername)) {
                logger.warn("Receiver does not exist: {}", receiverUsername);
                return;
            }

            // Save message to database
            Message savedMessage = messageService.savePrivateMessage(senderUsername, receiverUsername, content);

            // Create response message
            ChatMessage responseMessage = ChatMessage.createPrivateMessage(senderUsername, receiverUsername, content);
            responseMessage.setType(MessageType.PRIVATE);

            // NEW APPROACH: Create consistent conversation ID and broadcast to dedicated
            // topic
            String conversationId = createConversationId(senderUsername, receiverUsername);
            String conversationTopic = "/topic/private/" + conversationId;

            // IMMEDIATE LOCAL DELIVERY: Broadcast to conversation topic for instant
            // feedback
            messagingTemplate.convertAndSend(conversationTopic, responseMessage);

            // CROSS-SERVER DELIVERY: Publish to Redis for other servers
            broadcastService.publishPrivateMessage(responseMessage);

            logger.info("Private message saved and published to conversation topic {}. Message ID: {}",
                    conversationTopic, savedMessage.getId());

        } catch (Exception e) {
            logger.error("Error handling private message: {}", e.getMessage(), e);
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
     * Handle room messages
     * Endpoint: /chat.sendMessage (unchanged from original)
     * Now publishes to Redis for cross-server broadcasting
     */
    @MessageMapping("/chat.sendMessage")
    public void sendRoomMessage(@Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        try {
            String senderUsername = principal.getName();
            UUID roomId = chatMessage.getRoomId();
            String content = chatMessage.getContent();

            logger.info("Room message from {} to room {}: {}", senderUsername, roomId, content);

            // Validate sender
            if (!senderUsername.equals(chatMessage.getSender())) {
                logger.warn("Sender mismatch: authenticated as {} but claims to be {}",
                        senderUsername, chatMessage.getSender());
                return;
            }

            // Validate room exists and user is participant
            if (!roomService.isParticipant(roomId, senderUsername)) {
                logger.warn("User {} is not a participant in room {}", senderUsername, roomId);
                return;
            }

            // Save message to database
            Message savedMessage = messageService.saveRoomMessage(senderUsername, roomId, content);

            // Get room details for response
            String roomName = roomService.getRoomById(roomId)
                    .map(room -> room.getName())
                    .orElse("Unknown Room");

            // Create response message
            ChatMessage responseMessage = ChatMessage.createRoomMessage(senderUsername, roomId, roomName, content);

            // IMMEDIATE LOCAL DELIVERY: Broadcast locally first for instant feedback
            messagingTemplate.convertAndSend("/topic/room/" + roomId, responseMessage);

            // CROSS-SERVER DELIVERY: Publish to Redis for other servers
            broadcastService.publishRoomMessage(responseMessage);

            logger.info("Room message saved and published to Redis. Message ID: {}", savedMessage.getId());

        } catch (Exception e) {
            logger.error("Error handling room message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user joining a room
     * Endpoint: /chat.addUser (unchanged from original)
     * Now publishes to Redis for cross-server notifications
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        try {
            String username = principal.getName();
            UUID roomId = chatMessage.getRoomId();

            logger.info("User {} joining room {}", username, roomId);

            // Store username in websocket session
            headerAccessor.getSessionAttributes().put("username", username);

            if (roomId != null) {
                // Get room details
                String roomName = roomService.getRoomById(roomId)
                        .map(room -> room.getName())
                        .orElse("Unknown Room");

                // Create join message
                ChatMessage joinMessage = ChatMessage.createJoinMessage(username, roomId, roomName);

                // IMMEDIATE LOCAL DELIVERY: Broadcast locally first for instant feedback
                messagingTemplate.convertAndSend("/topic/room/" + roomId, joinMessage);

                // CROSS-SERVER DELIVERY: Publish to Redis for other servers
                broadcastService.publishUserActivity(joinMessage);

                logger.info("User {} join event published to Redis for room {} ({})", username, roomId, roomName);
            }

        } catch (Exception e) {
            logger.error("Error adding user to room: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user leaving a room
     * Endpoint: /chat.removeUser (unchanged from original)
     * Now publishes to Redis for cross-server notifications
     */
    @MessageMapping("/chat.removeUser")
    public void removeUser(@Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        try {
            String username = principal.getName();
            UUID roomId = chatMessage.getRoomId();

            logger.info("User {} leaving room {}", username, roomId);

            if (roomId != null) {
                // Get room details
                String roomName = roomService.getRoomById(roomId)
                        .map(room -> room.getName())
                        .orElse("Unknown Room");

                // Create leave message
                ChatMessage leaveMessage = ChatMessage.createLeaveMessage(username, roomId, roomName);

                // IMMEDIATE LOCAL DELIVERY: Broadcast locally first for instant feedback
                messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMessage);

                // CROSS-SERVER DELIVERY: Publish to Redis for other servers
                broadcastService.publishUserActivity(leaveMessage);

                logger.info("User {} leave event published to Redis for room {} ({})", username, roomId, roomName);
            }

        } catch (Exception e) {
            logger.error("Error removing user from room: {}", e.getMessage(), e);
        }
    }
}
