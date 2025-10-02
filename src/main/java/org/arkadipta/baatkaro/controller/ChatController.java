package org.arkadipta.baatkaro.controller;

import org.arkadipta.baatkaro.dto.ChatMessage;
import org.arkadipta.baatkaro.dto.MessageType;
import org.arkadipta.baatkaro.entity.Message;
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
 * Note: This remains as @Controller (not @RestController) because it handles
 * WebSocket messages,
 * not HTTP REST requests
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

    /**
     * Handle private messages
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

            // Send to receiver
            messagingTemplate.convertAndSendToUser(receiverUsername, "/queue/messages", responseMessage);

            // Send confirmation to sender (optional)
            messagingTemplate.convertAndSendToUser(senderUsername, "/queue/messages", responseMessage);

            logger.info("Private message saved and sent successfully. Message ID: {}", savedMessage.getId());

        } catch (Exception e) {
            logger.error("Error handling private message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle room messages
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

            // Broadcast to all room participants
            messagingTemplate.convertAndSend("/topic/" + roomId, responseMessage);

            logger.info("Room message saved and broadcast successfully. Message ID: {}", savedMessage.getId());

        } catch (Exception e) {
            logger.error("Error handling room message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user joining a room
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

                // Broadcast join notification to room
                messagingTemplate.convertAndSend("/topic/" + roomId, joinMessage);

                logger.info("User {} successfully joined room {} ({})", username, roomId, roomName);
            }

        } catch (Exception e) {
            logger.error("Error adding user to room: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user leaving a room
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

                // Broadcast leave notification to room
                messagingTemplate.convertAndSend("/topic/" + roomId, leaveMessage);

                logger.info("User {} left room {} ({})", username, roomId, roomName);
            }

        } catch (Exception e) {
            logger.error("Error removing user from room: {}", e.getMessage(), e);
        }
    }
}
