package org.arkadipta.baatkaro.controller;

import org.arkadipta.baatkaro.dto.MessageHistoryResponse;
import org.arkadipta.baatkaro.dto.MessageRequest;
import org.arkadipta.baatkaro.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * Send a private message
     */
    @PostMapping("/private")
    public ResponseEntity<?> sendPrivateMessage(@Valid @RequestBody MessageRequest request,
            Authentication authentication) {
        try {
            String senderUsername = authentication.getName();

            // Save the message using the message service
            messageService.savePrivateMessage(senderUsername, request.getReceiver(), request.getContent());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message sent successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get private message history between current user and another user
     */
    @GetMapping("/private/{username}")
    public ResponseEntity<?> getPrivateMessageHistory(@PathVariable String username,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        try {
            String currentUser = authentication.getName();

            // Validate access
            if (!messageService.canAccessPrivateHistory(currentUser, currentUser, username)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Access denied");
                return ResponseEntity.status(403).body(error);
            }

            List<MessageHistoryResponse> messages;
            if (limit > 0 && limit < 1000) {
                messages = messageService.getRecentPrivateMessages(currentUser, username, limit);
            } else {
                messages = messageService.getPrivateMessageHistory(currentUser, username);
            }

            return ResponseEntity.ok(messages);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get room message history
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomMessageHistory(@PathVariable UUID roomId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        try {
            String currentUser = authentication.getName();

            // Validate access
            if (!messageService.canAccessRoomHistory(currentUser, roomId)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You are not a participant in this room");
                return ResponseEntity.status(403).body(error);
            }

            List<MessageHistoryResponse> messages;
            if (limit > 0 && limit < 1000) {
                messages = messageService.getRecentRoomMessages(roomId, limit);
            } else {
                messages = messageService.getRoomMessageHistory(roomId);
            }

            return ResponseEntity.ok(messages);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all messages sent by current user
     */
    @GetMapping("/my-messages")
    public ResponseEntity<?> getMyMessages(Authentication authentication) {
        try {
            String currentUser = authentication.getName();
            List<MessageHistoryResponse> messages = messageService.getUserMessages(currentUser);
            return ResponseEntity.ok(messages);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}