package org.arkadipta.baatkaro.controller;

import org.arkadipta.baatkaro.dto.RoomRequest;
import org.arkadipta.baatkaro.dto.RoomResponse;
import org.arkadipta.baatkaro.dto.UserResponse;
import org.arkadipta.baatkaro.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    /**
     * Create a new room
     */
    @PostMapping
    public ResponseEntity<?> createRoom(@Valid @RequestBody RoomRequest request,
            BindingResult bindingResult,
            Authentication authentication) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            String username = authentication.getName();
            RoomResponse roomResponse = roomService.createRoom(request, username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Room created successfully");
            response.put("room", roomResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all public rooms (private rooms are not shown)
     */
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        // Only return public rooms for browsing
        List<RoomResponse> publicRooms = roomService.getAllPublicRooms();
        return ResponseEntity.ok(publicRooms);
    }

    /**
     * Get all rooms for a user (public + user's private rooms)
     */
    @GetMapping("/for-user")
    public ResponseEntity<List<RoomResponse>> getAllRoomsForUser(Authentication authentication) {
        List<RoomResponse> rooms = roomService.getAllRoomsForUser(authentication.getName());
        return ResponseEntity.ok(rooms);
    }

    /**
     * Get rooms where current user is a participant
     */
    @GetMapping("/my-rooms")
    public ResponseEntity<List<RoomResponse>> getMyRooms(Authentication authentication) {
        String username = authentication.getName();
        List<RoomResponse> rooms = roomService.getUserRooms(username);
        return ResponseEntity.ok(rooms);
    }

    /**
     * Get room by ID
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable UUID roomId) {
        return roomService.getRoomById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Join a room
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable UUID roomId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            RoomResponse roomResponse = roomService.joinRoom(roomId, username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully joined room");
            response.put("room", roomResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Leave a room
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable UUID roomId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            RoomResponse roomResponse = roomService.leaveRoom(roomId, username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully left room");
            response.put("room", roomResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get room participants
     */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<?> getRoomParticipants(@PathVariable UUID roomId,
            Authentication authentication) {
        try {
            String username = authentication.getName();

            // Check if user is a participant in the room
            if (!roomService.isParticipant(roomId, username)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You are not a participant in this room");
                return ResponseEntity.status(403).body(error);
            }

            List<UserResponse> participants = roomService.getRoomParticipants(roomId);
            return ResponseEntity.ok(participants);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Join a room by room ID (for private rooms or direct links)
     */
    @PostMapping("/join-by-id")
    public ResponseEntity<?> joinRoomById(@RequestParam String roomId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            UUID parsedRoomId = UUID.fromString(roomId);
            RoomResponse roomResponse = roomService.joinRoom(parsedRoomId, username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully joined room");
            response.put("room", roomResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid room ID format");
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Search rooms by name (only public rooms)
     */
    @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>> searchRooms(@RequestParam String query) {
        List<RoomResponse> rooms = roomService.searchRooms(query);
        return ResponseEntity.ok(rooms);
    }
}