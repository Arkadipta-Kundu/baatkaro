package org.arkadipta.baatkaro.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

public class UsernameHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        String query = request.getURI().getQuery();
        if (query != null && query.contains("username=")) {
            String username = extractUsername(query);
            if (username != null && !username.trim().isEmpty()) {
                attributes.put("username", username);
                return true;
            }
        }

        // Reject connection if no valid username
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Nothing to do after handshake
    }

    private String extractUsername(String query) {
        return Arrays.stream(query.split("&"))
                .filter(param -> param.startsWith("username="))
                .map(param -> param.substring(9)) // Remove "username="
                .findFirst()
                .orElse(null);
    }
}