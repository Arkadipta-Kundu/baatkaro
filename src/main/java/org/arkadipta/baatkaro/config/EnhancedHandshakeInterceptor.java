package org.arkadipta.baatkaro.config;

import org.arkadipta.baatkaro.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * Enhanced WebSocket handshake interceptor that supports multiple
 * authentication methods:
 * 1. Username query parameter (for direct username-based authentication)
 * 2. JWT token in query parameter (for token-based authentication)
 * 3. JWT token in Authorization header (fallback)
 */
public class EnhancedHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedHandshakeInterceptor.class);

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        try {
            URI uri = request.getURI();
            String query = uri.getQuery();

            logger.debug("WebSocket handshake attempt. URI: {}, Query: {}", uri, query);

            // Method 1: Try to get username directly from query parameter
            if (query != null && query.contains("username=")) {
                String username = extractUsernameFromQuery(query);
                if (username != null && !username.trim().isEmpty()) {
                    attributes.put("username", username);
                    logger.info("WebSocket handshake successful with username: {}", username);
                    return true;
                }
            }

            // Method 2: Try to get JWT token from query parameter and extract username
            if (query != null && query.contains("token=")) {
                String token = extractTokenFromQuery(query);
                if (token != null && !token.trim().isEmpty()) {
                    try {
                        String username = jwtService.extractUsername(token);
                        if (username != null && jwtService.validateToken(token)) {
                            attributes.put("username", username);
                            attributes.put("jwt_token", token);
                            logger.info("WebSocket handshake successful with JWT token for user: {}", username);
                            return true;
                        }
                    } catch (Exception e) {
                        logger.warn("Invalid JWT token in WebSocket handshake: {}", e.getMessage());
                    }
                }
            }

            // Method 3: Try to get JWT token from Authorization header
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String username = jwtService.extractUsername(token);
                    if (username != null && jwtService.validateToken(token)) {
                        attributes.put("username", username);
                        attributes.put("jwt_token", token);
                        logger.info("WebSocket handshake successful with Authorization header for user: {}", username);
                        return true;
                    }
                } catch (Exception e) {
                    logger.warn("Invalid JWT token in Authorization header: {}", e.getMessage());
                }
            }

            logger.warn("WebSocket handshake rejected: No valid authentication found");
            return false;

        } catch (Exception e) {
            logger.error("Error during WebSocket handshake: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        if (exception != null) {
            logger.error("WebSocket handshake completed with exception: {}", exception.getMessage());
        } else {
            logger.debug("WebSocket handshake completed successfully");
        }
    }

    private String extractUsernameFromQuery(String query) {
        try {
            return Arrays.stream(query.split("&"))
                    .filter(param -> param.startsWith("username="))
                    .map(param -> URLDecoder.decode(param.substring(9), StandardCharsets.UTF_8))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error extracting username from query: {}", e.getMessage());
            return null;
        }
    }

    private String extractTokenFromQuery(String query) {
        try {
            return Arrays.stream(query.split("&"))
                    .filter(param -> param.startsWith("token="))
                    .map(param -> URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error extracting token from query: {}", e.getMessage());
            return null;
        }
    }
}