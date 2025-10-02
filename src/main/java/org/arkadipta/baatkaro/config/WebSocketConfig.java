package org.arkadipta.baatkaro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // endpoint for websocket handshake
                .setHandshakeHandler(new UserPrincipalHandshakeHandler()) // Add Principal support
                .addInterceptors(new UsernameHandshakeInterceptor()) // Extract username from URL
                .setAllowedOriginPatterns("*")
                .withSockJS(); // fallback
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app"); // where messages are sent from client
        registry.enableSimpleBroker("/topic", "/queue", "/user"); // where server broadcasts messages
        registry.setUserDestinationPrefix("/user"); // Private messaging
    }
}
