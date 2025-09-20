# Developer Guide - Real-Time Chat Application

## 📋 Overview

This comprehensive guide helps new developers understand the codebase structure, architecture, and key concepts used in the Real-Time Chat Application.

## 🏗️ Architecture Overview

### Technology Stack

- **Backend**: Spring Boot 3.5.6 with Java 21
- **Database**: PostgreSQL 15+ (primary data storage)
- **Cache/Messaging**: Redis 7.0+ (pub-sub messaging, session storage)
- **Security**: Spring Security 6 + JWT tokens
- **Real-time**: WebSocket with STOMP protocol
- **Documentation**: Swagger/OpenAPI 3
- **Build**: Maven

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Load Balancer │    │   API Gateway   │
│   (React/Vue)   │◄──►│   (Nginx)       │◄──►│   (Optional)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                       ┌─────────────────────────────────┼─────────────────┐
                       │                                 ▼                 │
                       │            Spring Boot Application                 │
                       │  ┌─────────────────────────────────────────────┐  │
                       │  │              Controllers                     │  │
                       │  │  ┌─────────┐ ┌─────────┐ ┌─────────────┐   │  │
                       │  │  │  Auth   │ │  Chat   │ │  WebSocket  │   │  │
                       │  │  └─────────┘ └─────────┘ └─────────────┘   │  │
                       │  └─────────────────────────────────────────────┘  │
                       │  ┌─────────────────────────────────────────────┐  │
                       │  │                Services                     │  │
                       │  │  ┌─────────┐ ┌─────────┐ ┌─────────────┐   │  │
                       │  │  │ AuthSvc │ │ ChatSvc │ │  UserSvc    │   │  │
                       │  │  └─────────┘ └─────────┘ └─────────────┘   │  │
                       │  └─────────────────────────────────────────────┘  │
                       │  ┌─────────────────────────────────────────────┐  │
                       │  │             Repositories                    │  │
                       │  │  ┌─────────┐ ┌─────────┐ ┌─────────────┐   │  │
                       │  │  │UserRepo │ │MsgRepo  │ │ ChatRepo    │   │  │
                       │  │  └─────────┘ └─────────┘ └─────────────┘   │  │
                       │  └─────────────────────────────────────────────┘  │
                       └─────────────────────────────────────────────────────┘
                                                        │
                       ┌─────────────────┐             │            ┌─────────────────┐
                       │   PostgreSQL    │◄────────────┼───────────►│     Redis       │
                       │   (Primary DB)  │             │            │  (Cache/PubSub) │
                       └─────────────────┘             │            └─────────────────┘
```

## 📁 Project Structure Deep Dive

### Source Code Organization

```
src/main/java/org/arkadipta/chatapp/
├── config/                 # Configuration classes
│   ├── SecurityConfig.java      # Spring Security + JWT setup
│   ├── WebSocketConfig.java     # WebSocket configuration
│   ├── RedisConfig.java         # Redis pub-sub configuration
│   └── SwaggerConfig.java       # API documentation setup
├── controller/             # REST and WebSocket controllers
│   ├── AuthController.java      # Authentication endpoints
│   ├── UserController.java      # User management endpoints
│   ├── ChatController.java      # Chat room endpoints
│   └── WebSocketChatController.java # Real-time messaging
├── dto/                    # Data Transfer Objects
│   ├── auth/                    # Authentication DTOs
│   ├── user/                    # User management DTOs
│   ├── chat/                    # Chat operation DTOs
│   └── ApiResponse.java         # Standard API response wrapper
├── exception/              # Exception handling
│   ├── GlobalExceptionHandler.java # Global error handling
│   ├── UserNotFoundException.java  # Custom exceptions
│   └── ChatRoomNotFoundException.java
├── model/                  # JPA Entity classes
│   ├── User.java               # User entity + Spring Security integration
│   ├── Message.java            # Message entity with file support
│   ├── ChatRoom.java           # Chat room entity
│   ├── Role.java               # User role enumeration
│   └── MessageType.java        # Message type enumeration
├── repository/             # Data Access Layer
│   ├── UserRepository.java     # User database operations
│   ├── MessageRepository.java  # Message database operations
│   └── ChatRoomRepository.java # Chat room database operations
├── security/               # Security components
│   ├── JwtUtils.java           # JWT token generation/validation
│   ├── JwtAuthenticationFilter.java # JWT filter for requests
│   └── JwtAuthenticationEntryPoint.java # Auth error handling
└── service/                # Business Logic Layer
    ├── AuthService.java        # Authentication business logic
    ├── UserService.java        # User management business logic
    ├── ChatService.java        # Chat operations business logic
    └── RedisMessagePublisher.java # Redis pub-sub messaging
```

## 🔐 Security Architecture

### JWT Authentication Flow

```
1. User Login Request
   POST /api/auth/login
   { "username": "user", "password": "pass" }

2. AuthController → AuthService → AuthenticationManager

3. Database Credential Validation
   UserService loads User → PasswordEncoder checks hash

4. JWT Token Generation
   JwtUtils.generateToken(user) → Returns access + refresh tokens

5. Client Stores Tokens
   localStorage.setItem('accessToken', token)

6. Subsequent Requests
   Authorization: Bearer <access-token>

7. Request Processing
   JwtAuthenticationFilter → JwtUtils.validateToken()
   → SecurityContext.setAuthentication()

8. Controller Access
   @PreAuthorize("hasRole('USER')") or SecurityContext.getAuthentication()
```

### Security Configuration Breakdown

**SecurityConfig.java** sets up:

- **CORS**: Cross-origin requests from frontend
- **CSRF**: Disabled (stateless JWT doesn't need CSRF protection)
- **Session Management**: Stateless (no server-side sessions)
- **Authentication Provider**: Database-backed user validation
- **JWT Filter**: Custom filter for token processing
- **Authorization Rules**: Endpoint access control

## 💬 Real-time Messaging Architecture

### WebSocket + STOMP Protocol

```
WebSocket Connection Flow:
1. Client connects: new WebSocket('/ws')
2. STOMP handshake with JWT token in headers
3. Authentication in WebSocketConfig.configureClientInboundChannel()
4. Client subscribes: SUBSCRIBE /topic/chatroom/{roomId}
5. Client sends: SEND /app/chat.sendMessage
6. Server processes in WebSocketChatController
7. Server publishes to Redis: redisTemplate.convertAndSend()
8. Redis notifies all app instances: @RedisMessageListener
9. All instances broadcast: simpMessagingTemplate.convertAndSend()
10. All connected clients receive message
```

### Message Flow Architecture

```
User A (Instance 1)     User B (Instance 2)     User C (Instance 1)
       │                       │                       │
       ▼                       ▼                       ▼
   WebSocket                WebSocket              WebSocket
   Connection              Connection             Connection
       │                       │                       │
       ▼                       ▼                       ▼
 Spring Boot              Spring Boot            Spring Boot
 Instance 1               Instance 2             Instance 1
       │                       │                       │
       └─────────┬─────────────┴──────────┬────────────┘
                 ▼                        ▼
             Redis Pub-Sub           PostgreSQL
           (Real-time sync)        (Persistence)
```

## 🗄️ Database Design

### Entity Relationships

```
User (1) ────────── (N) Message
  │                     │
  │                     │
  │ (N)             (1) │
  │                     │
  └── ChatRoom ─────────┘
      (M:N via          (1:N)
    participants)
```

### Key Database Tables

**users table:**

- Primary authentication and profile data
- Implements Spring Security UserDetails
- Tracks online status and last seen timestamps
- Supports soft deletion and audit trails

**messages table:**

- Stores all chat messages with full history
- Supports different message types (TEXT, FILE, IMAGE, SYSTEM)
- Implements soft deletion for "unsend" functionality
- Includes threading support for replies

**chat_rooms table:**

- Defines chat spaces (one-to-one or group chats)
- Tracks metadata like creation time, last activity
- Supports room types and privacy settings

## 🔄 Service Layer Patterns

### Transaction Management

```java
@Transactional  // Ensures data consistency
public AuthResponse register(RegisterRequest request) {
    // 1. Validate uniqueness
    // 2. Hash password
    // 3. Create user entity
    // 4. Save to database
    // 5. Generate JWT tokens
    // If any step fails, entire operation rolls back
}
```

### Business Logic Organization

- **AuthService**: User registration, login, JWT management
- **UserService**: Profile management, user search, online status
- **ChatService**: Room creation, message sending, participant management
- **RedisMessagePublisher**: Cross-instance message broadcasting

## 🌐 API Design Patterns

### Consistent Response Format

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2025-09-20T10:30:00Z"
}
```

### Error Handling Strategy

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Converts all exceptions to consistent JSON format
    // Maps Spring validation errors to user-friendly messages
    // Logs technical details while returning safe public messages
}
```

### Validation Approach

```java
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @Email(message = "Valid email is required")
    private String email;
    // Bean Validation annotations provide automatic validation
}
```

## 🔧 Configuration Management

### Environment-Specific Properties

**Development** (`application.properties`):

```properties
spring.jpa.hibernate.ddl-auto=update  # Auto-update schema
spring.jpa.show-sql=true              # Show SQL for debugging
logging.level.org.arkadipta.chatapp=DEBUG
```

**Production** (environment variables):

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/chatapp
SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
APP_JWT_SECRET=${JWT_SECRET_KEY}
```

## 🧪 Testing Strategy

### Testing Pyramid

```
    /\         Unit Tests
   /  \        - Service layer logic
  /____\       - Repository queries
 /      \      - Utility functions
/__________\

    /\         Integration Tests
   /  \        - Controller endpoints
  /____\       - Database operations
 /      \      - Security configuration
/__________\

    /\         End-to-End Tests
   /  \        - Full user workflows
  /____\       - WebSocket messaging
/__________\    - Authentication flows
```

### Test Examples

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthServiceTest {
    // Tests business logic with in-memory database
}

@WebMvcTest(AuthController.class)
class AuthControllerTest {
    // Tests HTTP endpoints with mocked services
}
```

## 🚀 Deployment Considerations

### Docker Deployment

The application includes complete Docker support:

```yaml
# docker-compose.yml provides:
services:
  app: # Spring Boot application
  postgres: # PostgreSQL database
  redis: # Redis cache/pub-sub
  pgadmin: # Database management (optional)
  redis-commander: # Redis management (optional)
```

### Production Checklist

- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Configure production database with connection pooling
- [ ] Set up Redis cluster for high availability
- [ ] Configure proper logging levels and log aggregation
- [ ] Set up monitoring (actuator endpoints + Micrometer)
- [ ] Configure rate limiting for API endpoints
- [ ] Set up SSL/TLS termination at load balancer
- [ ] Implement proper secret management (HashiCorp Vault, AWS Secrets Manager)

## 🔍 Troubleshooting Guide

### Common Issues

**WebSocket Connection Fails:**

- Check CORS configuration in WebSocketConfig
- Verify JWT token is included in WebSocket headers
- Ensure Redis is running for multi-instance deployments

**Authentication Issues:**

- Verify JWT secret is consistent across instances
- Check password encoding (BCrypt) configuration
- Validate database user credentials and permissions

**Performance Issues:**

- Check database connection pool settings
- Monitor Redis memory usage and eviction policies
- Review JPA query performance (enable SQL logging)
- Verify WebSocket message broadcasting efficiency

### Debugging Tips

```java
// Enable debug logging for specific components
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web.socket=DEBUG
logging.level.org.arkadipta.chatapp=DEBUG

// Monitor SQL queries
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

// WebSocket debugging
logging.level.org.springframework.messaging=DEBUG
```

## 📚 Further Reading

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring WebSocket Guide](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [PostgreSQL Performance Tuning](https://www.postgresql.org/docs/current/performance-tips.html)
- [Redis Best Practices](https://redis.io/docs/manual/patterns/)

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed contribution guidelines including:

- Code style conventions
- Pull request process
- Testing requirements
- Documentation standards

---

**Happy Coding! 🚀**

This guide should give you everything needed to understand and contribute to the Real-Time Chat Application codebase.
