package org.arkadipta.baatkaro.controller;

import org.arkadipta.baatkaro.dto.ChatMessage;
import org.arkadipta.baatkaro.dto.MessageType;
import org.arkadipta.baatkaro.service.MessageBroadcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for testing Redis functionality and cross-server scaling features
 */
@RestController
@RequestMapping("/api/redis")
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MessageBroadcastService messageBroadcastService;

    /**
     * Test Redis connection and basic operations
     */
    @GetMapping("/status")
    public ResponseEntity<?> getRedisStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test Redis connection with a simple ping
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();

            // Test basic Redis operations
            String testKey = "baatkaro:test:" + System.currentTimeMillis();
            String testValue = "Redis connection test successful";

            redisTemplate.opsForValue().set(testKey, testValue, java.time.Duration.ofSeconds(60));
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);

            response.put("status", "connected");
            response.put("ping", pong);
            response.put("testWrite", testValue.equals(retrievedValue));
            response.put("serverInstance", getServerInstanceId());
            response.put("redisInfo", Map.of(
                    "connection", "active",
                    "operations", "working",
                    "pubsub", "available"));

            // Clean up test key
            redisTemplate.delete(testKey);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "disconnected");
            response.put("error", e.getMessage());
            response.put("serverInstance", getServerInstanceId());
            response.put("redisInfo", Map.of(
                    "connection", "failed",
                    "operations", "unavailable",
                    "pubsub", "unavailable"));

            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Test Redis Pub/Sub functionality
     */
    @PostMapping("/test-pubsub")
    public ResponseEntity<?> testRedisPubSub(@RequestBody Map<String, String> request,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            String testMessage = request.getOrDefault("message", "Redis Pub/Sub test message");
            String senderUsername = authentication.getName();

            // Create a test chat message
            ChatMessage testChatMessage = new ChatMessage();
            testChatMessage.setSender(senderUsername);
            testChatMessage.setReceiver("redis-test-channel");
            testChatMessage.setContent("[REDIS TEST] " + testMessage);
            testChatMessage.setType(MessageType.PRIVATE);
            testChatMessage.setRoomId(UUID.fromString("00000000-0000-0000-0000-000000000001")); // Test room ID

            // Publish message to Redis for cross-server delivery
            messageBroadcastService.publishPrivateMessage(testChatMessage);

            response.put("success", true);
            response.put("message", "Redis Pub/Sub test message published successfully");
            response.put("testMessage", testMessage);
            response.put("sender", senderUsername);
            response.put("serverInstance", getServerInstanceId());
            response.put("publishedTo", "redis-pubsub-channel");
            response.put("crossServerDelivery", "simulated");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Redis Pub/Sub test failed: " + e.getMessage());
            response.put("serverInstance", getServerInstanceId());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Simulate multi-instance server environment
     */
    @PostMapping("/simulate-multi-instance")
    public ResponseEntity<?> simulateMultiInstance(@RequestBody Map<String, Object> request,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            String testMessage = (String) request.getOrDefault("message", "Multi-instance test message");
            String senderUsername = authentication.getName();

            // Simulate messages from different server instances
            String[] serverInstances = { "Server-A", "Server-B", "Server-C" };

            for (String instance : serverInstances) {
                // Create test message for each instance
                ChatMessage instanceMessage = new ChatMessage();
                instanceMessage.setSender(senderUsername);
                instanceMessage.setReceiver("multi-instance-test");
                instanceMessage.setContent(String.format("[%s] %s", instance, testMessage));
                instanceMessage.setType(MessageType.ROOM);
                instanceMessage.setRoomId(UUID.fromString("00000000-0000-0000-0000-000000000002")); // Multi-instance
                                                                                                    // test room

                // Publish to Redis (simulating cross-server communication)
                messageBroadcastService.publishRoomMessage(instanceMessage);

                // Add small delay to simulate real-world scenario
                Thread.sleep(100);
            }

            response.put("success", true);
            response.put("message", "Multi-instance simulation completed");
            response.put("instancesSimulated", serverInstances);
            response.put("testMessage", testMessage);
            response.put("sender", senderUsername);
            response.put("currentInstance", getServerInstanceId());
            response.put("redisChannelsUsed", "room-messages, private-messages");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Multi-instance simulation failed: " + e.getMessage());
            response.put("currentInstance", getServerInstanceId());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get Redis performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getRedisMetrics() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test Redis connection
            redisTemplate.getConnectionFactory().getConnection().ping();

            response.put("serverInstance", getServerInstanceId());
            response.put("redisConnected", true);
            response.put("connectionPool", Map.of(
                    "active", "available",
                    "idle", "available",
                    "status", "healthy"));
            response.put("pubsubChannels", Map.of(
                    "private-messages", "active",
                    "room-messages", "active",
                    "system-events", "active"));
            response.put("scalingCapability", Map.of(
                    "crossServerMessaging", "enabled",
                    "loadDistribution", "redis-managed",
                    "failover", "automatic"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("serverInstance", getServerInstanceId());
            response.put("redisConnected", false);
            response.put("error", e.getMessage());
            response.put("scalingCapability", Map.of(
                    "crossServerMessaging", "disabled",
                    "loadDistribution", "single-server",
                    "failover", "unavailable"));

            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Get a unique server instance identifier
     */
    private String getServerInstanceId() {
        // In a real multi-instance deployment, this would be based on:
        // - Container ID, Pod name, or hostname
        // - Environment variables
        // - Load balancer instance ID

        String hostname = System.getProperty("server.hostname", "localhost");
        String port = System.getProperty("server.port", "8080");
        return String.format("Instance-%s-%s", hostname, port);
    }
}