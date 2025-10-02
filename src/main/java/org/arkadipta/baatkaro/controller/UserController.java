package org.arkadipta.baatkaro.controller;

import org.arkadipta.baatkaro.dto.UserResponse;
import org.arkadipta.baatkaro.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get all online users
     */
    @GetMapping("/online")
    public ResponseEntity<List<UserResponse>> getOnlineUsers() {
        List<UserResponse> onlineUsers = userService.getOnlineUsers();
        return ResponseEntity.ok(onlineUsers);
    }

    /**
     * Get all users
     */
    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(allUsers);
    }

    /**
     * Search users by username
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String query) {
        List<UserResponse> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    /**
     * Get current authenticated user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(UserResponse.fromUser(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update user online status
     */
    @PostMapping("/status")
    public ResponseEntity<?> updateUserStatus(@RequestBody Map<String, Boolean> statusUpdate,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        Boolean online = statusUpdate.get("online");

        if (online != null) {
            userService.setUserOnline(username, online);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Status updated successfully");
            response.put("username", username);
            response.put("online", online);

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body("Invalid status update request");
    }
}