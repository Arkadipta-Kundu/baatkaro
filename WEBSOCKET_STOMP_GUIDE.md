# WebSockets and STOMP Implementation Guide for Spring Boot Chat Application

## Table of Contents

1. [WebSocket Fundamentals](#websocket-fundamentals)
2. [WebSocket Configuration](#websocket-configuration)
3. [Message Handling Controller](#message-handling-controller)
4. [End-to-End Message Flow](#end-to-end-message-flow)
5. [Integration with Redis and Scalability](#integration-with-redis-and-scalability)
6. [Debugging and Troubleshooting](#debugging-and-troubleshooting)
7. [Hackathon-Ready Features](#hackathon-ready-features)

---

## WebSocket Fundamentals

### What are WebSockets?

**Think of WebSockets like a phone call vs HTTP which is like sending letters:**

- **HTTP (REST APIs)**: Client sends request → Server responds → Connection closes (like sending a letter)
- **WebSocket**: Client connects once → Both can send messages anytime → Connection stays open (like a phone call)

### What is STOMP?

**STOMP (Simple Text Oriented Messaging Protocol)** is like having a postal system for WebSockets:

- **Raw WebSocket**: Like shouting messages in a room - everyone hears everything
- **STOMP**: Like having organized mailboxes - messages go to specific destinations

```javascript
// Raw WebSocket (hard to organize)
websocket.send("Hello everyone!");

// STOMP (organized messaging)
stompClient.send(
  "/app/chat.sendMessage",
  {},
  JSON.stringify({
    chatRoomId: 1,
    content: "Hello room 1!",
  })
);
```

### Why Use STOMP with WebSockets?

1. **Message Routing**: Send messages to specific rooms/users
2. **Subscriptions**: Users only receive messages they care about
3. **Authentication**: Integrate with Spring Security
4. **Error Handling**: Better error management than raw WebSockets

---

## WebSocket Configuration

### 1. Main Configuration Class

**Location**: `src/main/java/org/arkadipta/chatapp/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker  // Enables STOMP over WebSocket
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Value("${app.websocket.allowed-origins}")
    private String allowedOrigins;
```

**Key Points:**

- `@EnableWebSocketMessageBroker` enables STOMP protocol support
- `WebSocketMessageBrokerConfigurer` provides configuration methods
- JWT and UserDetailsService are injected for authentication

### 2. Endpoint Registration (`registerStompEndpoints`)

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Main WebSocket endpoint with SockJS fallback
    registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins.split(","))
            .withSockJS();

    // Direct WebSocket endpoint (no SockJS)
    registry.addEndpoint("/chat")
            .setAllowedOriginPatterns(allowedOrigins.split(","));
}
```

**Explanation:**

#### What is a WebSocket Endpoint?

- **Endpoint = Entry Point**: Like a door to your building
- `/ws` is the URL where clients first connect to establish WebSocket connection

#### Why `/ws`?

- **Convention**: Common pattern (like `/api` for REST)
- **Short & Clear**: Easy to remember and type
- **You can change it**: Could be `/websocket`, `/realtime`, etc.

#### What does `withSockJS()` do?

**SockJS = WebSocket Fallback System**

```
Modern Browser: Uses native WebSocket
Older Browser: Falls back to HTTP polling/streaming
Corporate Firewall: Automatically switches to HTTP tunneling
```

**Real-world example:**

```javascript
// Client connecting to /ws endpoint
const socket = new SockJS("/ws");
const stompClient = Stomp.over(socket);
```

### 3. Message Broker Configuration (`configureMessageBroker`)

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    // Enable simple in-memory broker for these prefixes
    config.enableSimpleBroker("/topic", "/queue");

    // Messages starting with /app go to @MessageMapping methods
    config.setApplicationDestinationPrefixes("/app");

    // Enable user-specific destinations
    config.setUserDestinationPrefix("/user");
}
```

**Line-by-Line Breakdown:**

#### What is a Message Broker?

**Think of it like a mail sorting office:**

- Receives messages from senders
- Sorts them by destination
- Delivers to correct recipients

#### `config.enableSimpleBroker("/topic", "/queue")`

**Creates two "mailbox areas":**

- `/topic`: Public channels (like chat rooms) - one-to-many messaging
- `/queue`: Private messages - one-to-one messaging

**Examples:**

```javascript
// Public room (everyone subscribed receives message)
stompClient.subscribe("/topic/chatroom/1", callback);

// Private queue (only specific user receives)
stompClient.subscribe("/user/queue/private", callback);
```

#### `config.setApplicationDestinationPrefixes("/app")`

**Routes client messages to your controller methods:**

```
Client sends to: /app/chat.sendMessage
↓
Spring routes to: @MessageMapping("/chat.sendMessage") method
```

**Difference from broker prefixes:**

- **Application prefixes** (`/app`): Messages TO your application
- **Broker prefixes** (`/topic`, `/queue`): Messages FROM your application

#### `config.setUserDestinationPrefix("/user")`

**Enables personal message destinations:**

```javascript
// Send private message to specific user
stompClient.send('/app/private', {}, message); // You send here
↓
// They receive here:
stompClient.subscribe('/user/queue/private', callback);
```

### 4. How to Modify Configuration

#### Change Application Prefix

```java
// Instead of /app, use /api
config.setApplicationDestinationPrefixes("/api");

// Now clients send to: /api/chat.sendMessage
```

#### Add CORS Origins

```java
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("http://localhost:3000", "https://myapp.com")
    .withSockJS();
```

#### Use External Broker (Advanced)

```java
// Instead of simple broker, use RabbitMQ/ActiveMQ
config.enableStompBrokerRelay("/topic", "/queue")
    .setRelayHost("rabbitmq-server")
    .setRelayPort(61613);
```

### 5. Authentication Integration

```java
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // Extract JWT from WebSocket handshake headers
                List<String> authorization = accessor.getNativeHeader("Authorization");

                if (authorization != null && !authorization.isEmpty()) {
                    String token = authorization.get(0);
                    if (token.startsWith("Bearer ")) {
                        token = token.substring(7);

                        // Validate JWT token
                        String username = jwtUtils.extractUsername(token);
                        if (username != null && !jwtUtils.isTokenExpired(token)) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                            if (jwtUtils.validateToken(token, userDetails)) {
                                // Create authentication and set in context
                                UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                                accessor.setUser(authentication);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("Missing authorization header");
                }
            }
            return message;
        }
    });
}
```

**This intercepts EVERY WebSocket message and:**

1. Checks if it's a CONNECT command (initial connection)
2. Extracts JWT token from headers
3. Validates the token using your existing JWT utilities
4. Sets the authenticated user in Spring Security context
5. Makes `Principal principal` available in your controller methods

---

## Message Handling Controller

### 1. Controller Overview

**Location**: `src/main/java/org/arkadipta/chatapp/controller/WebSocketChatController.java`

```java
@Controller  // Not @RestController - WebSocket uses different response mechanism
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
```

### 2. Understanding `@MessageMapping`

#### Basic Message Mapping

```java
@MessageMapping("/chat.sendMessage")
@SendTo("/topic/public")
public MessageResponse sendMessage(@Payload SendMessageRequest messageRequest, Principal principal) {
    // Process message
    MessageResponse response = chatService.sendMessage(messageRequest);
    return response; // Automatically sent to /topic/public
}
```

**Full STOMP Destination Calculation:**

- **Application Prefix**: `/app` (from configuration)
- **Method Mapping**: `/chat.sendMessage`
- **Full Client Destination**: `/app/chat.sendMessage`

**Client Usage:**

```javascript
stompClient.send(
  "/app/chat.sendMessage",
  {},
  JSON.stringify({
    chatRoomId: 1,
    content: "Hello World!",
    type: "TEXT",
  })
);
```

#### Room-Specific Message Mapping

```java
@MessageMapping("/chat.sendMessage.{roomId}")
public void sendMessageToRoom(
    @DestinationVariable String roomId,
    @Payload SendMessageRequest messageRequest,
    Principal principal) {

    // Process message for specific room
    MessageResponse response = chatService.sendMessage(messageRequest);

    // Send to room-specific topic
    String destination = "/topic/chatroom/" + roomId;
    messagingTemplate.convertAndSend(destination, response);
}
```

**Client sends to**: `/app/chat.sendMessage.123` (for room 123)

### 3. Understanding `@SendTo` vs `@SendToUser`

#### `@SendTo` - Broadcast to Everyone

```java
@MessageMapping("/chat.sendMessage")
@SendTo("/topic/public")  // Everyone subscribed to /topic/public receives this
public MessageResponse sendMessage(@Payload SendMessageRequest request, Principal principal) {
    return chatService.sendMessage(request);
}
```

#### Manual Sending with `SimpMessagingTemplate`

```java
@MessageMapping("/chat.sendMessage.{roomId}")
public void sendToSpecificRoom(@DestinationVariable String roomId,
                               @Payload SendMessageRequest request,
                               Principal principal) {

    MessageResponse response = chatService.sendMessage(request);

    // Send to specific room topic
    String destination = "/topic/chatroom/" + roomId;
    messagingTemplate.convertAndSend(destination, response);
}
```

#### `@SendToUser` - Send to Specific User

```java
@MessageMapping("/private.message")
@SendToUser("/queue/private")  // Only the sender receives this response
public String sendPrivateMessage(@Payload String message, Principal principal) {
    return "ACK: " + message;
}
```

### 4. Message DTOs Analysis

#### SendMessageRequest (Input DTO)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "Chat room ID is required")
    @JsonProperty("chat_room_id")
    private Long chatRoomId;

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Message content must not exceed 5000 characters")
    private String content;

    @Builder.Default
    private String type = "TEXT";  // TEXT, IMAGE, FILE, SYSTEM

    @JsonProperty("reply_to_message_id")
    private Long replyToMessageId;

    // File attachment fields
    @JsonProperty("file_url")
    private String fileUrl;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("file_type")
    private String fileType;
}
```

**Why these fields are needed:**

- `chatRoomId`: Identifies which room/conversation the message belongs to
- `content`: The actual message text
- `type`: Supports different message types (text, images, files, system messages)
- `replyToMessageId`: For threaded conversations (replying to specific messages)
- File fields: For sending attachments with messages

#### MessageResponse (Output DTO)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;              // Database ID of saved message
    private String content;       // Message text
    private String type;          // Message type
    private SenderInfo sender;    // Who sent the message
    private Long chatRoomId;      // Which room it belongs to
    private LocalDateTime timestamp;  // When it was sent

    private Boolean isEdited;     // Has the message been edited?
    private LocalDateTime editedAt;  // When was it edited?

    // File attachment info
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;

    private ReplyToMessage replyTo;  // If replying to another message

    @Data
    @Builder
    public static class SenderInfo {
        private Long id;
        private String username;
        private String fullName;
        private String profilePictureUrl;
        private Boolean isOnline;
    }

    @Data
    @Builder
    public static class ReplyToMessage {
        private Long id;
        private String content;
        private SenderInfo sender;
        private LocalDateTime timestamp;
    }
}
```

---

## End-to-End Message Flow

Let's walk through what happens when **UserA sends "Hello" to chat room 1**:

### Step 1: Connection

**Client connects to WebSocket endpoint first:**

```javascript
// 1. Connect to WebSocket endpoint
const socket = new SockJS("/ws");
const stompClient = Stomp.over(socket);

// 2. Connect with JWT token in headers
stompClient.connect(
  {
    Authorization: "Bearer " + jwtToken,
  },
  function (frame) {
    console.log("Connected: " + frame);

    // 3. After connection, subscribe to topics
    subscribeToRooms();
  }
);
```

**Server side:** `WebSocketConfig.configureClientInboundChannel()` validates the JWT token.

### Step 2: Subscription

**Client subscribes to receive messages for room 1:**

```javascript
// Subscribe to room 1's messages
stompClient.subscribe("/topic/chatroom/1", function (message) {
  const messageData = JSON.parse(message.body);
  displayMessage(messageData);
});
```

**What happens:**

- Client tells server "I want to receive messages sent to `/topic/chatroom/1`"
- Server registers this subscription
- Any message sent to `/topic/chatroom/1` will be delivered to this client

### Step 3: Sending Message

**UserA sends message:**

```javascript
// Send message to room 1
const messageRequest = {
  chat_room_id: 1,
  content: "Hello",
  type: "TEXT",
};

stompClient.send("/app/chat.sendMessage.1", {}, JSON.stringify(messageRequest));
```

### Step 4: Backend - Controller Processing

**Message arrives at controller:**

```java
@MessageMapping("/chat.sendMessage.{roomId}")
public void sendMessageToRoom(@DestinationVariable String roomId,
                             @Payload SendMessageRequest messageRequest,
                             Principal principal) {
    try {
        Long chatRoomId = Long.parseLong(roomId); // "1" → 1
        messageRequest.setChatRoomId(chatRoomId);

        log.info("WebSocket room message from user: {} to room: {}",
                 principal.getName(), roomId);

        // Process message through business logic
        MessageResponse response = chatService.sendMessage(messageRequest);

        // Send to specific room topic
        String destination = "/topic/chatroom/" + roomId; // "/topic/chatroom/1"
        messagingTemplate.convertAndSend(destination, response);

    } catch (Exception e) {
        // Send error back to sender
        String errorDestination = "/user/" + principal.getName() + "/queue/errors";
        messagingTemplate.convertAndSend(errorDestination, "Failed: " + e.getMessage());
    }
}
```

**What the controller does:**

1. Receives message destined for `/app/chat.sendMessage.1`
2. Extracts room ID (`1`) from URL path
3. Gets authenticated user from `Principal` (set during JWT validation)
4. Calls business logic (`chatService.sendMessage()`)
5. Broadcasts result to room topic (`/topic/chatroom/1`)

### Step 5: Backend - Business Logic (ChatService)

```java
@Transactional
public MessageResponse sendMessage(SendMessageRequest request) {
    User sender = userService.getCurrentUser();

    // 1. Validate chat room exists and user has access
    ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
        .orElseThrow(() -> new RuntimeException("Chat room not found"));

    if (!chatRoom.isParticipant(sender)) {
        throw new RuntimeException("User is not a participant in this chat room");
    }

    // 2. Create and save message to database
    Message message = Message.builder()
        .content(request.getContent())
        .type(MessageType.valueOf(request.getType().toUpperCase()))
        .sender(sender)
        .chatRoom(chatRoom)
        .timestamp(LocalDateTime.now())
        .build();

    message = messageRepository.save(message);

    // 3. Convert to response DTO
    MessageResponse messageResponse = mapToMessageResponse(message);

    // 4. Send real-time message to participants (calls Redis publisher)
    sendRealTimeMessage(chatRoom, messageResponse);

    return messageResponse;
}
```

### Step 6: Backend - Redis Publishing (Scalability)

```java
private void sendRealTimeMessage(ChatRoom chatRoom, MessageResponse messageResponse) {
    String destination = "/topic/chatroom/" + chatRoom.getId();

    // Send to local WebSocket clients
    messagingTemplate.convertAndSend(destination, messageResponse);

    // ALSO publish to Redis for other application instances
    redisMessagePublisher.publishChatMessage(messageResponse);

    log.debug("Real-time message sent to destination: {}", destination);
}
```

**Redis Publisher:**

```java
public void publishChatMessage(MessageResponse message) {
    try {
        String messageJson = objectMapper.writeValueAsString(message);
        redisTemplate.convertAndSend(RedisConfig.CHAT_TOPIC, messageJson);

        log.debug("Published chat message to Redis: {}", message.getId());

    } catch (Exception e) {
        log.error("Failed to publish chat message to Redis", e);
    }
}
```

### Step 7: Backend - Redis Subscriber (Other Instances)

**On ALL application instances (including the sender):**

```java
@Override
public void onMessage(Message message, byte[] pattern) {
    try {
        String channel = new String(message.getChannel());
        String messageBody = new String(message.getBody());

        if (RedisConfig.CHAT_TOPIC.equals(channel)) {
            handleChatMessage(messageBody);
        }
    } catch (Exception e) {
        log.error("Failed to process Redis message", e);
    }
}

private void handleChatMessage(String messageBody) {
    try {
        MessageResponse messageResponse = objectMapper.readValue(messageBody, MessageResponse.class);

        // Broadcast to THIS instance's WebSocket clients
        String destination = "/topic/chatroom/" + messageResponse.getChatRoomId();
        messagingTemplate.convertAndSend(destination, messageResponse);

        log.debug("Chat message broadcasted to: {}", destination);

    } catch (Exception e) {
        log.error("Failed to handle chat message", e);
    }
}
```

### Step 8: Delivery to All Subscribed Clients

**Every client subscribed to `/topic/chatroom/1` receives the message:**

```javascript
stompClient.subscribe("/topic/chatroom/1", function (message) {
  const messageData = JSON.parse(message.body);
  /*
    messageData = {
        id: 123,
        content: "Hello",
        type: "TEXT",
        sender: {
            id: 1,
            username: "userA",
            fullName: "User A",
            isOnline: true
        },
        chatRoomId: 1,
        timestamp: "2025-09-29T10:15:30"
    }
    */

  displayMessage(messageData);
});
```

### Complete Flow Summary

```
1. UserA connects: WebSocket('/ws') + JWT authentication
2. UserA subscribes: SUBSCRIBE /topic/chatroom/1
3. UserA sends: SEND /app/chat.sendMessage.1 {content: "Hello"}
4. Server Controller: @MessageMapping("/chat.sendMessage.{roomId}")
5. Business Logic: Save to DB, validate permissions
6. Local Broadcast: messagingTemplate.convertAndSend("/topic/chatroom/1", response)
7. Redis Publish: Notify other application instances
8. Redis Subscribe: Other instances also broadcast to their clients
9. All Clients Receive: Everyone subscribed to /topic/chatroom/1 gets the message
```

---

## Integration with Redis and Scalability

### Why Redis is Needed

**Problem without Redis:**

```
Instance 1: UserA connected     Instance 2: UserB connected
     ↓                                ↓
   UserA sends message              UserB never receives it!
   (only goes to Instance 1)
```

**Solution with Redis:**

```
Instance 1: UserA ──→ Redis ←── Instance 2: UserB
                        ↓
            Both instances receive message
            Both broadcast to their clients
```

### Redis Integration Flow

#### 1. Message Publisher

```java
@Service
@ConditionalOnProperty(name = "messaging.type", havingValue = "redis", matchIfMissing = true)
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishChatMessage(MessageResponse message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisConfig.CHAT_TOPIC, messageJson);
        } catch (Exception e) {
            log.error("Failed to publish chat message to Redis", e);
        }
    }

    public void publishUserStatus(String username, boolean isOnline) {
        try {
            UserStatusMessage statusMessage = new UserStatusMessage(username, isOnline);
            String messageJson = objectMapper.writeValueAsString(statusMessage);
            redisTemplate.convertAndSend(RedisConfig.USER_STATUS_TOPIC, messageJson);
        } catch (Exception e) {
            log.error("Failed to publish user status to Redis", e);
        }
    }
}
```

#### 2. Redis Subscriber

```java
@Component
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String messageBody = new String(message.getBody());

        if (RedisConfig.CHAT_TOPIC.equals(channel)) {
            handleChatMessage(messageBody);
        } else if (RedisConfig.USER_STATUS_TOPIC.equals(channel)) {
            handleUserStatusMessage(messageBody);
        }
    }

    private void handleChatMessage(String messageBody) {
        MessageResponse messageResponse = objectMapper.readValue(messageBody, MessageResponse.class);
        String destination = "/topic/chatroom/" + messageResponse.getChatRoomId();
        messagingTemplate.convertAndSend(destination, messageResponse);
    }

    private void handleUserStatusMessage(String messageBody) {
        messagingTemplate.convertAndSend("/topic/user-status", messageBody);
    }
}
```

#### 3. Redis Configuration

```java
@Configuration
@ConditionalOnProperty(name = "messaging.type", havingValue = "redis", matchIfMissing = true)
public class RedisConfig {

    public static final String CHAT_TOPIC = "chat_messages";
    public static final String USER_STATUS_TOPIC = "user_status";

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, chatTopic());
        container.addMessageListener(listenerAdapter, userStatusTopic());
        return container;
    }

    @Bean
    public ChannelTopic chatTopic() {
        return new ChannelTopic(CHAT_TOPIC);
    }

    @Bean
    public ChannelTopic userStatusTopic() {
        return new ChannelTopic(USER_STATUS_TOPIC);
    }
}
```

### How Redis Helps with Online Presence

```java
// When user connects to WebSocket
@EventListener
public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String username = headerAccessor.getUser().getName();

    // Update local status
    userService.updateOnlineStatus(username, true);

    // Notify other instances via Redis
    redisMessagePublisher.publishUserStatus(username, true);
}

// When user disconnects
@EventListener
public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String username = (String) headerAccessor.getSessionAttributes().get("username");

    if (username != null) {
        userService.updateOnlineStatus(username, false);
        redisMessagePublisher.publishUserStatus(username, false);
    }
}
```

### Kafka Integration (Optional Advanced Feature)

**Your project is prepared for Kafka but currently uses Redis**. Here's how Kafka would work:

```java
// Kafka Producer (instead of Redis)
@Service
public class KafkaMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendChatMessage(MessageResponse message) {
        String messageJson = objectMapper.writeValueAsString(message);
        kafkaTemplate.send("chat-messages", message.getChatRoomId().toString(), messageJson);
    }
}

// Kafka Consumer
@Component
public class KafkaMessageConsumer {

    @KafkaListener(topics = "chat-messages")
    public void handleChatMessage(String messageJson) {
        MessageResponse message = objectMapper.readValue(messageJson, MessageResponse.class);
        String destination = "/topic/chatroom/" + message.getChatRoomId();
        messagingTemplate.convertAndSend(destination, message);
    }
}
```

**Kafka vs Redis for Chat:**

- **Redis**: Faster, simpler, perfect for real-time chat
- **Kafka**: More scalable, persistent, better for analytics and complex event streaming

---

## Debugging and Troubleshooting

### 1. Enable WebSocket Logging

**In `application.properties`:**

```properties
# Enable WebSocket debug logging
logging.level.org.springframework.messaging=DEBUG
logging.level.org.springframework.web.socket=DEBUG
logging.level.org.arkadipta.chatapp.controller.WebSocketChatController=DEBUG
```

### 2. Common Issues and Solutions

#### Issue: "WebSocket connection failed"

**Symptoms:** Client can't connect to WebSocket
**Solutions:**

```java
// Check CORS configuration
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("*")  // For development - be more specific in production
    .withSockJS();
```

#### Issue: "Missing authorization header"

**Symptoms:** Connection fails during handshake
**Client Side Fix:**

```javascript
stompClient.connect(
  {
    Authorization: "Bearer " + jwtToken, // Make sure token is included
  },
  connectCallback,
  errorCallback
);
```

#### Issue: Messages not being delivered

**Check subscription destinations:**

```javascript
// Wrong - missing room ID
stompClient.subscribe("/topic/chatroom", callback);

// Correct - specific room
stompClient.subscribe("/topic/chatroom/1", callback);
```

### 3. WebSocket Session Monitoring

**Add to your controller for debugging:**

```java
@Controller
public class WebSocketChatController {

    @MessageMapping("/debug.info")
    public void getDebugInfo(Principal principal) {
        log.info("Debug info request from: {}", principal.getName());

        String destination = "/user/" + principal.getName() + "/queue/debug";
        Map<String, Object> debugInfo = Map.of(
            "username", principal.getName(),
            "timestamp", LocalDateTime.now(),
            "authorities", principal instanceof Authentication
                ? ((Authentication) principal).getAuthorities() : "N/A"
        );

        messagingTemplate.convertAndSend(destination, debugInfo);
    }
}
```

### 4. Redis Connection Issues

**Check Redis connectivity:**

```java
@Component
public class RedisHealthCheck {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void checkRedisConnection() {
        try {
            redisTemplate.opsForValue().set("health-check", "OK");
            String value = (String) redisTemplate.opsForValue().get("health-check");
            log.info("Redis connection healthy: {}", value);
        } catch (Exception e) {
            log.error("Redis connection failed", e);
        }
    }
}
```

---

## Hackathon-Ready Features

### 1. Private Messaging (Direct Messages)

**Backend Controller:**

```java
@MessageMapping("/private.message")
@SendToUser("/queue/private")
public MessageResponse sendPrivateMessage(@Payload PrivateMessageRequest request, Principal principal) {
    // Create direct chat room if doesn't exist
    ChatRoom directRoom = chatService.getOrCreateDirectChat(principal.getName(), request.getRecipient());

    // Send message
    SendMessageRequest messageRequest = SendMessageRequest.builder()
        .chatRoomId(directRoom.getId())
        .content(request.getContent())
        .type("TEXT")
        .build();

    return chatService.sendMessage(messageRequest);
}
```

**Client Usage:**

```javascript
// Send private message
stompClient.send(
  "/app/private.message",
  {},
  JSON.stringify({
    recipient: "userB",
    content: "Hey, this is a private message!",
  })
);

// Subscribe to private messages
stompClient.subscribe("/user/queue/private", function (message) {
  const privateMsg = JSON.parse(message.body);
  displayPrivateMessage(privateMsg);
});
```

### 2. Typing Indicators

**Backend:**

```java
@MessageMapping("/typing.start.{roomId}")
public void startTyping(@DestinationVariable String roomId, Principal principal) {
    TypingIndicator indicator = new TypingIndicator();
    indicator.setUsername(principal.getName());
    indicator.setTyping(true);
    indicator.setChatRoomId(roomId);

    // Broadcast to room (excluding sender)
    messagingTemplate.convertAndSend("/topic/chatroom/" + roomId + "/typing", indicator);
}

@MessageMapping("/typing.stop.{roomId}")
public void stopTyping(@DestinationVariable String roomId, Principal principal) {
    TypingIndicator indicator = new TypingIndicator();
    indicator.setUsername(principal.getName());
    indicator.setTyping(false);
    indicator.setChatRoomId(roomId);

    messagingTemplate.convertAndSend("/topic/chatroom/" + roomId + "/typing", indicator);
}
```

**Client Usage:**

```javascript
let typingTimer;

// When user starts typing
messageInput.addEventListener("input", function () {
  stompClient.send("/app/typing.start.1", {}, {});

  clearTimeout(typingTimer);
  typingTimer = setTimeout(() => {
    stompClient.send("/app/typing.stop.1", {}, {});
  }, 2000); // Stop after 2 seconds of inactivity
});

// Subscribe to typing indicators
stompClient.subscribe("/topic/chatroom/1/typing", function (message) {
  const typing = JSON.parse(message.body);
  if (typing.isTyping) {
    showTypingIndicator(typing.username + " is typing...");
  } else {
    hideTypingIndicator(typing.username);
  }
});
```

### 3. Online Presence System

**Backend:**

```java
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = headerAccessor.getUser().getName();

        userService.updateOnlineStatus(username, true);

        // Broadcast online status
        messagingTemplate.convertAndSend("/topic/user-status",
            Map.of("username", username, "online", true));
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            userService.updateOnlineStatus(username, false);
            messagingTemplate.convertAndSend("/topic/user-status",
                Map.of("username", username, "online", false));
        }
    }
}
```

**Client Usage:**

```javascript
// Subscribe to online status updates
stompClient.subscribe("/topic/user-status", function (message) {
  const status = JSON.parse(message.body);
  updateUserOnlineStatus(status.username, status.online);
});
```

### 4. Group Chat Features

**Create Group Chat:**

```java
@MessageMapping("/group.create")
@SendToUser("/queue/group-created")
public ChatRoomResponse createGroup(@Payload CreateGroupRequest request, Principal principal) {
    return chatService.createGroupChat(request, principal.getName());
}
```

**Add/Remove Participants:**

```java
@MessageMapping("/group.{roomId}.add-participant")
public void addParticipant(@DestinationVariable String roomId,
                          @Payload AddParticipantRequest request,
                          Principal principal) {

    chatService.addParticipant(Long.parseLong(roomId), request.getUserId(), principal.getName());

    // Notify all participants
    messagingTemplate.convertAndSend("/topic/chatroom/" + roomId + "/participants",
        Map.of("action", "added", "user", request.getUserId()));
}
```

### 5. File Sharing

**Backend:**

```java
@MessageMapping("/file.share.{roomId}")
public void shareFile(@DestinationVariable String roomId,
                     @Payload FileShareRequest request,
                     Principal principal) {

    SendMessageRequest messageRequest = SendMessageRequest.builder()
        .chatRoomId(Long.parseLong(roomId))
        .content(request.getDescription())
        .type("FILE")
        .fileUrl(request.getFileUrl())
        .fileName(request.getFileName())
        .fileSize(request.getFileSize())
        .fileType(request.getFileType())
        .build();

    MessageResponse response = chatService.sendMessage(messageRequest);
    messagingTemplate.convertAndSend("/topic/chatroom/" + roomId, response);
}
```

### 6. Message Reactions/Emojis

**Backend:**

```java
@MessageMapping("/message.{messageId}.react")
public void reactToMessage(@DestinationVariable String messageId,
                          @Payload EmojiReaction reaction,
                          Principal principal) {

    MessageResponse messageWithReaction = chatService.addReaction(
        Long.parseLong(messageId),
        reaction.getEmoji(),
        principal.getName()
    );

    // Broadcast updated message with reactions
    messagingTemplate.convertAndSend(
        "/topic/chatroom/" + messageWithReaction.getChatRoomId() + "/reactions",
        messageWithReaction
    );
}
```

### 7. Quick Setup Script for Hackathon

**Create this as a startup configuration:**

```java
@Component
public class HackathonSetup {

    @EventListener(ApplicationReadyEvent.class)
    public void setupHackathonData() {
        if (isHackathonMode()) {
            // Create sample users
            createSampleUsers();

            // Create sample rooms
            createSampleRooms();

            // Add sample messages
            addSampleMessages();

            log.info("Hackathon setup complete! Ready to demo.");
        }
    }

    private boolean isHackathonMode() {
        return environment.getProperty("app.hackathon.mode", Boolean.class, false);
    }
}
```

**Add to `application.properties` for hackathon:**

```properties
# Hackathon mode
app.hackathon.mode=true

# Relaxed CORS for demo
app.websocket.allowed-origins=*

# More verbose logging
logging.level.org.arkadipta.chatapp=DEBUG
```

### 8. Performance Tips for Hackathon

1. **Connection Pooling**: Use connection pooling for database
2. **Caching**: Cache frequently accessed chat rooms and user data
3. **Async Processing**: Use `@Async` for non-critical operations
4. **Rate Limiting**: Implement rate limiting for message sending
5. **Memory Management**: Monitor WebSocket connections and clean up inactive ones

---

## Summary

You now have a comprehensive understanding of:

1. **WebSocket Basics**: Real-time bidirectional communication
2. **STOMP Protocol**: Organized messaging with subscriptions and destinations
3. **Spring Integration**: Configuration, authentication, and message handling
4. **Scalability**: Redis pub-sub for multi-instance deployment
5. **Debugging**: Tools and techniques for troubleshooting
6. **Advanced Features**: Private messaging, typing indicators, presence, file sharing

**Key Takeaways for Hackathon:**

- WebSockets provide real-time communication (like phone calls vs letters)
- STOMP adds structure with topics and queues (like organized mailboxes)
- Redis enables horizontal scaling across multiple server instances
- The architecture supports thousands of concurrent users
- Rich features like typing indicators and file sharing are easily implementable

**Next Steps:**

1. Set up a frontend client using the provided JavaScript examples
2. Test the various message flows with multiple browser tabs
3. Experiment with the advanced features like typing indicators
4. Consider adding Kafka for even more scalability if needed

This chat system is production-ready and can handle real-world usage while being perfect for hackathon demonstrations!
