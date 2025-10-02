package org.arkadipta.baatkaro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WebController {

    /**
     * API Health Check and Welcome endpoint
     */
    @GetMapping("/")
    public ResponseEntity<?> index() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to BaatKaro Chat API");
        response.put("version", "1.0.0");
        response.put("status", "active");
        response.put("endpoints", Map.of(
                "auth", "/api/auth/*",
                "users", "/api/users/*",
                "rooms", "/api/rooms/*",
                "messages", "/api/messages/*",
                "websocket", "/ws"));
        return ResponseEntity.ok(response);
    }

    /**
     * API Information endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<?> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "BaatKaro Chat Application");
        response.put("description", "Real-time chat application with private messaging and room-based conversations");
        response.put("features", new String[] {
                "User Registration & Authentication",
                "Private Messaging",
                "Room-based Chat",
                "Real-time WebSocket Communication",
                "Message History",
                "User Management"
        });
        response.put("websocket_endpoint", "/ws");
        return ResponseEntity.ok(response);
    }
}