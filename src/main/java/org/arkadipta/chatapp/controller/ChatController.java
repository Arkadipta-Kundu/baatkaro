package org.arkadipta.chatapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.dto.ApiResponse;
import org.arkadipta.chatapp.dto.chat.*;
import org.arkadipta.chatapp.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for chat operations
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Management", description = "Chat room and messaging endpoints")
public class ChatController {

    private final ChatService chatService;

    /**
     * Create a new chat room
     */
    @PostMapping("/rooms")
    @Operation(summary = "Create chat room", description = "Create a new chat room (direct or group)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Chat room created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
            @Valid @RequestBody CreateChatRoomRequest request) {
        try {
            log.info("Creating chat room: {}", request.getName());

            ChatRoomResponse response = chatService.createChatRoom(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Chat room created successfully"));

        } catch (Exception e) {
            log.error("Failed to create chat room", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get user's chat rooms
     */
    @GetMapping("/rooms")
    @Operation(summary = "Get user chat rooms", description = "Get all chat rooms for the current user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chat rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getUserChatRooms() {
        try {
            log.info("Getting user chat rooms");

            List<ChatRoomResponse> chatRooms = chatService.getUserChatRooms();

            return ResponseEntity.ok(ApiResponse.success(chatRooms,
                    "Found " + chatRooms.size() + " chat rooms"));

        } catch (Exception e) {
            log.error("Failed to get user chat rooms", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Send a message
     */
    @PostMapping("/messages")
    @Operation(summary = "Send message", description = "Send a message to a chat room")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized to send message to this chat room")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        try {
            log.info("Sending message to chat room: {}", request.getChatRoomId());

            MessageResponse response = chatService.sendMessage(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Message sent successfully"));

        } catch (Exception e) {
            log.error("Failed to send message", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get chat history
     */
    @GetMapping("/rooms/{chatRoomId}/messages")
    @Operation(summary = "Get chat history", description = "Get message history for a chat room")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chat history retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized to view this chat room"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Chat room not found")
    })
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getChatHistory(
            @PathVariable Long chatRoomId,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            log.info("Getting chat history for room: {}", chatRoomId);

            Page<MessageResponse> messages = chatService.getChatHistory(chatRoomId, pageable);

            return ResponseEntity.ok(ApiResponse.success(messages,
                    "Retrieved " + messages.getNumberOfElements() + " messages"));

        } catch (Exception e) {
            log.error("Failed to get chat history for room: {}", chatRoomId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Search messages in a chat room
     */
    @GetMapping("/rooms/{chatRoomId}/messages/search")
    @Operation(summary = "Search messages", description = "Search for messages in a chat room")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized to search this chat room"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Chat room not found")
    })
    public ResponseEntity<ApiResponse<List<MessageResponse>>> searchMessages(
            @PathVariable Long chatRoomId,
            @RequestParam String q) {
        try {
            log.info("Searching messages in room: {} with query: {}", chatRoomId, q);

            List<MessageResponse> messages = chatService.searchMessages(chatRoomId, q);

            return ResponseEntity.ok(ApiResponse.success(messages,
                    "Found " + messages.size() + " messages"));

        } catch (Exception e) {
            log.error("Failed to search messages in room: {}", chatRoomId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Add participant to group chat
     */
    @PostMapping("/rooms/{chatRoomId}/participants/{userId}")
    @Operation(summary = "Add participant", description = "Add a participant to a group chat room")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Participant added successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid operation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized to add participants"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Chat room or user not found")
    })
    public ResponseEntity<ApiResponse<ChatRoomResponse>> addParticipant(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId) {
        try {
            log.info("Adding participant {} to chat room: {}", userId, chatRoomId);

            ChatRoomResponse response = chatService.addParticipant(chatRoomId, userId);

            return ResponseEntity.ok(ApiResponse.success(response, "Participant added successfully"));

        } catch (Exception e) {
            log.error("Failed to add participant {} to room: {}", userId, chatRoomId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}