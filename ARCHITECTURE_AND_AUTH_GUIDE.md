# Spring Boot Chat Application - Architecture & Authentication Guide

## Table of Contents

1. [High-Level Architecture](#high-level-architecture)
2. [Database Schema & Entity Relationships](#database-schema--entity-relationships)
3. [Authentication & Authorization System](#authentication--authorization-system)
4. [Security Flow Deep Dive](#security-flow-deep-dive)
5. [Integration Patterns](#integration-patterns)
6. [API Security Architecture](#api-security-architecture)

---

## High-Level Architecture

### 🏗️ System Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Load Balancer  │    │   Frontend      │
│   (React/Vue)   │    │   (nginx/ALB)   │    │   (Mobile)      │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │     API Gateway         │
                    │   (Spring Security)     │
                    └────────────┬────────────┘
                                 │
    ┌────────────────────────────┼────────────────────────────┐
    │                            │                            │
    ▼                            ▼                            ▼
┌─────────────┐           ┌─────────────┐           ┌─────────────┐
│Spring Boot  │           │Spring Boot  │           │Spring Boot  │
│Instance 1   │           │Instance 2   │           │Instance N   │
│             │           │             │           │             │
│┌───────────┐│           │┌───────────┐│           │┌───────────┐│
││Controllers││           ││Controllers││           ││Controllers││
│└───────────┘│           │└───────────┘│           │└───────────┘│
│┌───────────┐│           │┌───────────┐│           │┌───────────┐│
││Services   ││           ││Services   ││           ││Services   ││
│└───────────┘│           │└───────────┘│           │└───────────┘│
│┌───────────┐│           │┌───────────┐│           │┌───────────┐│
││WebSocket  ││           ││WebSocket  ││           ││WebSocket  ││
│└───────────┘│           │└───────────┘│           │└───────────┘│
└─────────────┘           └─────────────┘           └─────────────┘
      │                           │                           │
      └───────────────────────────┼───────────────────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │     Redis Pub-Sub         │
                    │  (Real-time Messaging)    │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │    PostgreSQL Database    │
                    │   (Persistent Storage)    │
                    └───────────────────────────┘
```

### 🔧 Technology Stack

| **Layer**           | **Technology**                | **Purpose**                              |
| ------------------- | ----------------------------- | ---------------------------------------- |
| **Frontend**        | React/Vue/Angular + WebSocket | User Interface & Real-time Communication |
| **Backend**         | Spring Boot 3.5.6 + Java 21   | REST API & Business Logic                |
| **Security**        | Spring Security 6 + JWT       | Authentication & Authorization           |
| **Real-time**       | WebSocket + STOMP             | Live Chat & Notifications                |
| **Database**        | PostgreSQL 15+                | Primary Data Storage                     |
| **Cache/Messaging** | Redis 7.0+                    | Pub-Sub & Session Storage                |
| **Documentation**   | Swagger/OpenAPI 3             | API Documentation                        |
| **Build**           | Maven                         | Dependency Management & Build            |

### 📁 Project Structure

```
src/main/java/org/arkadipta/chatapp/
├── config/                     # Configuration Classes
│   ├── SecurityConfig.java     # Spring Security Configuration
│   ├── WebSocketConfig.java    # WebSocket & STOMP Configuration
│   ├── RedisConfig.java        # Redis Pub-Sub Configuration
│   ├── OpenApiConfig.java      # Swagger/OpenAPI Configuration
│   └── GlobalExceptionHandler.java # Global Error Handling
├── controller/                 # REST & WebSocket Controllers
│   ├── AuthController.java     # Authentication Endpoints
│   ├── UserController.java     # User Management Endpoints
│   ├── ChatController.java     # Chat Operations Endpoints
│   └── WebSocketChatController.java # Real-time Message Handling
├── dto/                        # Data Transfer Objects
│   ├── auth/                   # Authentication DTOs
│   ├── user/                   # User Management DTOs
│   ├── chat/                   # Chat Operation DTOs
│   └── ApiResponse.java        # Standard API Response Wrapper
├── model/                      # JPA Entity Classes
│   ├── User.java              # User Entity (implements UserDetails)
│   ├── ChatRoom.java          # Chat Room Entity
│   ├── Message.java           # Message Entity
│   ├── Role.java              # User Role Enumeration
│   ├── ChatRoomType.java      # Chat Room Type Enumeration
│   └── MessageType.java       # Message Type Enumeration
├── repository/                 # Data Access Layer
│   ├── UserRepository.java    # User Database Operations
│   ├── ChatRoomRepository.java # Chat Room Database Operations
│   └── MessageRepository.java  # Message Database Operations
├── security/                   # Security Components
│   ├── JwtUtils.java          # JWT Token Operations
│   ├── JwtAuthenticationFilter.java # JWT Request Filter
│   ├── JwtAuthenticationEntryPoint.java # Auth Error Handler
│   └── CustomUserDetailsService.java # User Loading Service
├── service/                    # Business Logic Layer
│   ├── AuthService.java       # Authentication Business Logic
│   ├── UserService.java       # User Management Business Logic
│   ├── ChatService.java       # Chat Operations Business Logic
│   └── RedisMessagePublisher.java # Cross-instance Messaging
└── ChatappApplication.java    # Spring Boot Main Class
```

---

## Database Schema & Entity Relationships

### 📊 Entity Relationship Diagram

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│      User       │         │   ChatRoom      │         │     Message     │
├─────────────────┤         ├─────────────────┤         ├─────────────────┤
│ id (PK)         │◄─────┐  │ id (PK)         │         │ id (PK)         │
│ username (UK)   │      │  │ name            │         │ content         │
│ email (UK)      │      │  │ description     │         │ type            │
│ password        │      │  │ type            │◄────────┤ sender_id (FK)  │
│ first_name      │      │  │ created_by (FK) │         │ chat_room_id(FK)│
│ last_name       │      │  │ created_at      │         │ timestamp       │
│ role            │      │  │ updated_at      │         │ is_edited       │
│ is_online       │      │  │ is_active       │         │ edited_at       │
│ last_seen       │      │  │ room_image_url  │         │ is_deleted      │
│ is_enabled      │      └──┤                 │         │ file_url        │
│ profile_pic_url │         │                 │         │ file_name       │
│ created_at      │         │                 │         │ file_size       │
│ updated_at      │         │                 │         │ file_type       │
└─────────────────┘         └─────────────────┘         │ reply_to_msg(FK)│
         │                           │                   └─────────────────┘
         │                           │                            │
         │                           │                            │
         └───────────────────────────┼────────────────────────────┘
                                     │
                                     ▼
                    ┌─────────────────────────────────┐
                    │  chat_room_participants (Join) │
                    ├─────────────────────────────────┤
                    │ chat_room_id (FK)               │
                    │ user_id (FK)                    │
                    └─────────────────────────────────┘
```

### 🗄️ Database Tables

#### **users** Table

```sql
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    username           VARCHAR(50) NOT NULL UNIQUE,
    email              VARCHAR(100) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    first_name         VARCHAR(50),
    last_name          VARCHAR(50),
    role               VARCHAR(10) NOT NULL DEFAULT 'USER',
    is_online          BOOLEAN DEFAULT FALSE,
    last_seen          TIMESTAMP,
    is_enabled         BOOLEAN DEFAULT TRUE,
    profile_picture_url TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_online ON users(is_online);
```

#### **chat_rooms** Table

```sql
CREATE TABLE chat_rooms (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    type            VARCHAR(10) NOT NULL, -- 'DIRECT' or 'GROUP'
    created_by      BIGINT NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active       BOOLEAN DEFAULT TRUE,
    room_image_url  TEXT
);

-- Indexes for performance
CREATE INDEX idx_chat_rooms_created_by ON chat_rooms(created_by);
CREATE INDEX idx_chat_rooms_type ON chat_rooms(type);
CREATE INDEX idx_chat_rooms_updated_at ON chat_rooms(updated_at);
```

#### **messages** Table

```sql
CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    content         TEXT NOT NULL,
    type            VARCHAR(10) NOT NULL DEFAULT 'TEXT', -- 'TEXT', 'IMAGE', 'FILE', 'SYSTEM'
    sender_id       BIGINT NOT NULL REFERENCES users(id),
    chat_room_id    BIGINT NOT NULL REFERENCES chat_rooms(id),
    timestamp       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_edited       BOOLEAN DEFAULT FALSE,
    edited_at       TIMESTAMP,
    is_deleted      BOOLEAN DEFAULT FALSE,
    file_url        TEXT,
    file_name       VARCHAR(255),
    file_size       BIGINT,
    file_type       VARCHAR(100),
    reply_to_message_id BIGINT REFERENCES messages(id)
);

-- Indexes for performance
CREATE INDEX idx_messages_chat_room_id ON messages(chat_room_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);
CREATE INDEX idx_messages_is_deleted ON messages(is_deleted);
```

#### **chat_room_participants** Table (Join Table)

```sql
CREATE TABLE chat_room_participants (
    chat_room_id    BIGINT NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (chat_room_id, user_id)
);

-- Indexes for performance
CREATE INDEX idx_chat_room_participants_user_id ON chat_room_participants(user_id);
CREATE INDEX idx_chat_room_participants_chat_room_id ON chat_room_participants(chat_room_id);
```

### 🔗 JPA Entity Relationships

#### User Entity

```java
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt hashed

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = false;

    // Many-to-Many relationship with ChatRoom
    @ManyToMany(mappedBy = "participants", fetch = FetchType.LAZY)
    private Set<ChatRoom> chatRooms = new HashSet<>();

    // One-to-Many relationship with Messages
    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Message> sentMessages = new HashSet<>();

    // Spring Security UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
```

#### ChatRoom Entity

```java
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type; // DIRECT or GROUP

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Many-to-Many relationship with Users
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "chat_room_participants",
        joinColumns = @JoinColumn(name = "chat_room_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    // One-to-Many relationship with Messages
    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Message> messages = new HashSet<>();

    // Helper methods
    public boolean isParticipant(User user) {
        return participants.contains(user);
    }

    public boolean isDirectChat() {
        return type == ChatRoomType.DIRECT && participants.size() == 2;
    }
}
```

#### Message Entity

```java
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    // Many-to-One relationship with User (sender)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Many-to-One relationship with ChatRoom
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    // Self-referencing relationship for replies
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Builder.Default
    private Boolean isDeleted = false; // Soft deletion

    // File attachment fields
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
}
```

### 📈 Data Flow Patterns

#### **Chat Room Participation**

```java
// Adding a user to a chat room (Many-to-Many relationship)
public void addParticipantToChatRoom(Long chatRoomId, Long userId) {
    ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
        .orElseThrow(() -> new RuntimeException("Chat room not found"));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    // JPA handles the join table automatically
    chatRoom.getParticipants().add(user);
    user.getChatRooms().add(chatRoom);

    chatRoomRepository.save(chatRoom); // Cascades to join table
}
```

#### **Message Retrieval with Relationships**

```java
// Fetch messages with sender and chat room information
@Query("SELECT m FROM Message m " +
       "JOIN FETCH m.sender " +
       "JOIN FETCH m.chatRoom " +
       "WHERE m.chatRoom.id = :chatRoomId " +
       "AND m.isDeleted = false " +
       "ORDER BY m.timestamp DESC")
Page<Message> findChatRoomMessages(@Param("chatRoomId") Long chatRoomId, Pageable pageable);
```

---

## Authentication & Authorization System

### 🔐 Security Architecture Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│     Client      │    │   Spring Boot    │    │    Database     │
│   (Frontend)    │    │   Application    │    │  (PostgreSQL)   │
└─────────┬───────┘    └─────────┬────────┘    └─────────┬───────┘
          │                      │                       │
          │ 1. POST /auth/login  │                       │
          ├─────────────────────►│                       │
          │ {username, password} │                       │
          │                      │ 2. Load User          │
          │                      ├──────────────────────►│
          │                      │ by username/email     │
          │                      │                       │
          │                      │ 3. User Entity        │
          │                      │◄──────────────────────┤
          │                      │ with hashed password  │
          │                      │                       │
          │ 4. JWT Tokens        │                       │
          │◄─────────────────────┤                       │
          │ {access, refresh}    │                       │
          │                      │                       │
          │ 5. API Requests      │                       │
          ├─────────────────────►│                       │
          │ Authorization: Bearer │                       │
          │                      │                       │
          │ 6. Protected Data    │                       │
          │◄─────────────────────┤                       │
```

### 🔧 Authentication Components

#### **1. SecurityConfig.java - Main Security Configuration**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS Configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF (not needed for stateless JWT)
            .csrf(AbstractHttpConfigurer::disable)

            // Exception handling
            .exceptionHandling(exception ->
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

            // Stateless session management
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                .requestMatchers("/ws/**").permitAll() // WebSocket handled separately

                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated())

            // Add JWT filter
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### **2. JwtAuthenticationFilter - Request Processing Filter**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Extract JWT token from Authorization header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token (remove "Bearer " prefix)
        jwt = authHeader.substring(7);
        username = jwtUtils.extractUsername(jwt);

        // If username exists and no authentication is set in context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load user details from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate token against user details
            if (jwtUtils.validateToken(jwt, userDetails)) {

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                // Set authentication in security context
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}
```

#### **3. JwtUtils - Token Operations**

```java
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    // Generate access token
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return generateToken(claims, userDetails.getUsername());
    }

    // Generate token with custom claims
    public String generateToken(Map<String, Object> extraClaims, String username) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(username)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    // Generate refresh token
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return Jwts.builder()
            .claims(claims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Check if token is expired
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

#### **4. User Entity - UserDetails Implementation**

```java
@Entity
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt hashed

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private Boolean isEnabled = true;

    // Spring Security UserDetails methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}
```

#### **5. AuthService - Business Logic**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    // User Registration
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Create and save user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .role(Role.USER)
            .isEnabled(true)
            .isOnline(false)
            .build();

        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtUtils.getExpirationTime())
            .user(mapToUserInfo(user))
            .build();
    }

    // User Login
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            // Get authenticated user
            User user = (User) authentication.getPrincipal();

            // Update online status
            user.setIsOnline(true);
            userRepository.save(user);

            // Generate tokens
            String accessToken = jwtUtils.generateToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationTime())
                .user(mapToUserInfo(user))
                .build();

        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid username or password");
        }
    }

    // Token Refresh
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtUtils.isRefreshToken(refreshToken) || jwtUtils.isTokenExpired(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        // Extract username and get user
        String username = jwtUtils.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate new access token
        String newAccessToken = jwtUtils.generateToken(user);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken) // Keep same refresh token
            .tokenType("Bearer")
            .expiresIn(jwtUtils.getExpirationTime())
            .user(mapToUserInfo(user))
            .build();
    }
}
```

---

## Security Flow Deep Dive

### 🔄 Complete Authentication Flow

#### **1. User Registration Flow**

```
1. POST /api/auth/register
   ┌─────────────────────────────────────────┐
   │ {                                       │
   │   "username": "john_doe",               │
   │   "email": "john@example.com",          │
   │   "password": "securePassword123",      │
   │   "firstName": "John",                  │
   │   "lastName": "Doe"                     │
   │ }                                       │
   └─────────────────────────────────────────┘
                      ↓
2. AuthController.register()
                      ↓
3. AuthService.register()
   ├─ Check username uniqueness
   ├─ Check email uniqueness
   ├─ Hash password with BCrypt
   ├─ Create User entity
   ├─ Save to database
   ├─ Generate JWT access token
   ├─ Generate JWT refresh token
   └─ Return AuthResponse
                      ↓
4. Client receives tokens
   ┌─────────────────────────────────────────┐
   │ {                                       │
   │   "access_token": "eyJhbGci...",        │
   │   "refresh_token": "eyJhbGci...",       │
   │   "token_type": "Bearer",               │
   │   "expires_in": 86400000,               │
   │   "user": {                             │
   │     "id": 1,                            │
   │     "username": "john_doe",             │
   │     "email": "john@example.com",        │
   │     "role": "USER"                      │
   │   }                                     │
   │ }                                       │
   └─────────────────────────────────────────┘
```

#### **2. User Login Flow**

```
1. POST /api/auth/login
   ┌─────────────────────────────────────────┐
   │ {                                       │
   │   "username": "john_doe",               │
   │   "password": "securePassword123"       │
   │ }                                       │
   └─────────────────────────────────────────┘
                      ↓
2. AuthController.login()
                      ↓
3. AuthService.login()
   ├─ Create UsernamePasswordAuthenticationToken
   ├─ AuthenticationManager.authenticate()
   │  ├─ DaoAuthenticationProvider
   │  ├─ CustomUserDetailsService.loadUserByUsername()
   │  ├─ PasswordEncoder.matches(rawPassword, hashedPassword)
   │  └─ Return Authentication object
   ├─ Update user online status
   ├─ Generate JWT tokens
   └─ Return AuthResponse with tokens
                      ↓
4. Client stores tokens in localStorage/sessionStorage
```

#### **3. Protected Request Flow**

```
1. Client sends request with JWT token
   GET /api/chat/rooms
   Headers: {
     "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   }
                      ↓
2. JwtAuthenticationFilter.doFilterInternal()
   ├─ Extract "Authorization" header
   ├─ Validate "Bearer " prefix
   ├─ Extract token (remove "Bearer ")
   ├─ Extract username from token using JwtUtils
   ├─ Load UserDetails from database
   ├─ Validate token signature and expiration
   ├─ Create UsernamePasswordAuthenticationToken
   ├─ Set authentication in SecurityContext
   └─ Continue filter chain
                      ↓
3. Controller method executes
   ├─ SecurityContext contains authenticated user
   ├─ @PreAuthorize annotations are evaluated
   ├─ Method-level security is enforced
   └─ Business logic executes
                      ↓
4. Response sent back to client
```

#### **4. WebSocket Authentication Flow**

```
1. Client connects to WebSocket
   const socket = new SockJS('/ws');
   const stompClient = Stomp.over(socket);

   stompClient.connect({
     'Authorization': 'Bearer ' + accessToken
   }, callback);
                      ↓
2. WebSocketConfig.configureClientInboundChannel()
   ├─ Intercept STOMP CONNECT command
   ├─ Extract JWT token from native headers
   ├─ Validate token using JwtUtils
   ├─ Load UserDetails from database
   ├─ Create Authentication object
   ├─ Set in StompHeaderAccessor
   └─ Continue connection process
                      ↓
3. WebSocket connection established
   ├─ Principal is available in @MessageMapping methods
   ├─ User can subscribe to authorized topics
   └─ Real-time messaging is enabled
```

### 🛡️ Security Validation Points

#### **Password Security**

```java
@Component
public class PasswordSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strong hashing with cost factor 12
    }

    // Password validation (can be added to RegisterRequest)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least 8 characters, including uppercase, lowercase, number, and special character"
    )
    private String password;
}
```

#### **JWT Token Security**

```properties
# application.properties
app.jwt.secret=mySecretKeyThatShouldBeAtLeast256BitsLong
app.jwt.expiration=86400000      # 24 hours in milliseconds
app.jwt.refresh-expiration=604800000  # 7 days in milliseconds
```

#### **CORS Security**

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Production: Use specific origins
    configuration.setAllowedOriginPatterns(List.of("https://yourdomain.com"));

    // Development: Allow localhost
    configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(List.of("Authorization"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## Integration Patterns

### 🔗 Service Layer Integration

#### **Current User Access Pattern**

```java
@Service
public class ChatService {

    public MessageResponse sendMessage(SendMessageRequest request) {
        // Get current authenticated user
        User sender = getCurrentUser();

        // Validate user has permission to send to chat room
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
            .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatRoom.isParticipant(sender)) {
            throw new RuntimeException("User is not a participant in this chat room");
        }

        // Continue with message creation...
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
}
```

#### **Method-Level Security**

```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Class-level security
public class AdminController {

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')") // Method-level security
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') and #id != authentication.principal.id") // Prevent self-deletion
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### **Chat Room Authorization Pattern**

```java
@Service
public class ChatRoomAuthorizationService {

    public void validateUserCanAccessChatRoom(Long chatRoomId, String username) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!chatRoom.isParticipant(user)) {
            throw new AccessDeniedException("User does not have access to this chat room");
        }
    }

    public void validateUserCanModifyChatRoom(Long chatRoomId, String username) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Only creator or admin can modify chat room
        if (!chatRoom.getCreatedBy().equals(user) && !user.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("User does not have permission to modify this chat room");
        }
    }
}
```

### 🔄 Error Handling Integration

#### **Global Exception Handler**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("Access denied: " + e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntime(RuntimeException e) {
        if (e.getMessage().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

---

## API Security Architecture

### 🛡️ Security Headers & Best Practices

#### **Security Configuration**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Security headers
        .headers(headers -> headers
            .frameOptions().deny()
            .contentTypeOptions().and()
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubdomains(true))
            .and())

        // Content Security Policy
        .headers(headers -> headers
            .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'"))

        // Rest of configuration...
        ;

    return http.build();
}
```

#### **Rate Limiting (Future Enhancement)**

```java
@Component
public class RateLimitingFilter implements Filter {

    private final Map<String, List<LocalDateTime>> requestCounts = new ConcurrentHashMap<>();
    private final int MAX_REQUESTS_PER_MINUTE = 60;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientId = getClientIdentifier(httpRequest);

        if (isRateLimited(clientId)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);

        List<LocalDateTime> requests = requestCounts.computeIfAbsent(clientId, k -> new ArrayList<>());

        // Remove old requests
        requests.removeIf(time -> time.isBefore(oneMinuteAgo));

        // Check limit
        if (requests.size() >= MAX_REQUESTS_PER_MINUTE) {
            return true;
        }

        // Add current request
        requests.add(now);
        return false;
    }
}
```

### 🔍 Security Monitoring & Logging

#### **Security Event Logging**

```java
@Component
@Slf4j
public class SecurityEventLogger {

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String clientIp = getClientIp();
        log.info("Successful login: user={}, ip={}, timestamp={}",
                 username, clientIp, LocalDateTime.now());
    }

    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String clientIp = getClientIp();
        String reason = event.getException().getMessage();
        log.warn("Failed login attempt: user={}, ip={}, reason={}, timestamp={}",
                 username, clientIp, reason, LocalDateTime.now());
    }

    @EventListener
    public void handleAccessDenied(AuthorizationDeniedEvent event) {
        String username = event.getAuthentication().getName();
        String resource = event.getAuthorizationDecision().toString();
        log.warn("Access denied: user={}, resource={}, timestamp={}",
                 username, resource, LocalDateTime.now());
    }
}
```

---

## Summary

This Spring Boot chat application implements a comprehensive authentication and authorization system with:

### **🔑 Key Security Features**

- **JWT-based Authentication**: Stateless token-based auth perfect for microservices
- **Role-based Authorization**: USER and ADMIN roles with method-level security
- **Password Security**: BCrypt hashing with configurable strength
- **WebSocket Security**: JWT validation for real-time connections
- **CORS Protection**: Configurable cross-origin request handling

### **🏗️ Architecture Highlights**

- **Layered Architecture**: Clear separation between controllers, services, and repositories
- **Entity Relationships**: Proper JPA mappings with optimized queries
- **Real-time Integration**: WebSocket authentication integrated with Spring Security
- **Scalable Design**: Redis pub-sub for multi-instance deployments

### **🛡️ Security Best Practices**

- **Input Validation**: Bean validation on all DTOs
- **Error Handling**: Consistent error responses without information leakage
- **Transaction Management**: Atomic operations for data consistency
- **Audit Logging**: Comprehensive security event logging

This architecture provides a solid foundation for a production-ready chat application that can handle thousands of concurrent users while maintaining security and scalability.
