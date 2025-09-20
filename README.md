# Real-Time Chat Application API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0+-red.svg)](https://redis.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A comprehensive real-time chat application API built with Spring Boot 3, featuring JWT authentication, WebSocket messaging, and Redis pub-sub for scalable multi-instance deployment.

## 🚀 Features

### Core Features

- **Real-time Messaging**: WebSocket-based instant messaging with STOMP protocol
- **User Authentication**: JWT-based authentication and authorization
- **User Management**: Complete user registration, profile management, and online status tracking
- **Chat Rooms**: Support for both one-to-one and group chat conversations
- **Message Persistence**: All messages stored in PostgreSQL with full history
- **File Sharing**: Support for file and image sharing in chats
- **Message Search**: Full-text search across chat messages
- **Unread Message Tracking**: Track and count unread messages per user

### Technical Features

- **Scalable Architecture**: Redis pub-sub for multi-instance deployment
- **Security**: Spring Security 6 with JWT tokens and CORS support
- **API Documentation**: Interactive Swagger/OpenAPI 3 documentation
- **Data Validation**: Comprehensive request/response validation
- **Error Handling**: Global exception handling with meaningful error responses
- **Audit Trail**: Automatic timestamp tracking for all entities

## 🛠️ Tech Stack

- **Backend Framework**: Spring Boot 3.5.6
- **Language**: Java 21
- **Database**: PostgreSQL 15+
- **Cache/Messaging**: Redis 7.0+
- **Security**: Spring Security 6 + JWT
- **WebSocket**: Spring WebSocket + STOMP
- **Documentation**: Swagger/OpenAPI 3
- **Build Tool**: Maven
- **ORM**: Spring Data JPA (Hibernate)

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 15+
- Redis 7.0+

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/chatapp.git
cd chatapp
```

### 2. Database Setup

#### PostgreSQL

```sql
-- Create database
CREATE DATABASE chatapp;

-- Create user (optional)
CREATE USER chatapp_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE chatapp TO chatapp_user;
```

#### Redis

Make sure Redis is running on default port 6379:

```bash
redis-server
```

### 3. Configuration

Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/chatapp
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT Configuration
app.jwt.secret=YourSuperSecretJWTKeyThatShouldBeAtLeast256BitsLongForHS256Algorithm
app.jwt.expiration=86400000
app.jwt.refresh-expiration=604800000
```

### 4. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 🔗 API Endpoints

### Authentication

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh-token` - Refresh JWT token
- `POST /api/auth/logout` - User logout

### User Management

- `GET /api/users/profile` - Get current user profile
- `PUT /api/users/profile` - Update user profile
- `GET /api/users/search` - Search users
- `PUT /api/users/online-status` - Update online status

### Chat Management

- `POST /api/chat/rooms` - Create chat room
- `GET /api/chat/rooms` - Get user's chat rooms
- `GET /api/chat/rooms/{roomId}/messages` - Get chat messages
- `POST /api/chat/rooms/{roomId}/messages` - Send message
- `POST /api/chat/rooms/{roomId}/join` - Join chat room
- `DELETE /api/chat/rooms/{roomId}/leave` - Leave chat room

### WebSocket Endpoints

- `/ws` - WebSocket connection endpoint
- `/app/chat.sendMessage` - Send message via WebSocket
- `/app/chat.addUser` - Add user to chat
- `/topic/public` - Public message topic
- `/topic/chatroom/{roomId}` - Room-specific messages

## 🧪 Testing

### Run Tests

```bash
mvn test
```

### Test with Postman

Import the API collection and test the endpoints:

1. Register a new user
2. Login to get JWT token
3. Create a chat room
4. Send messages
5. Test WebSocket connection

## 🐳 Docker Support

### Using Docker Compose

```bash
# Start all services (PostgreSQL + Redis + Application)
docker-compose up -d

# Stop all services
docker-compose down
```

### Manual Docker Build

```bash
# Build the image
docker build -t chatapp:latest .

# Run the container
docker run -p 8080:8080 chatapp:latest
```

## 🏗️ Architecture

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── org/arkadipta/chatapp/
│   │       ├── config/           # Configuration classes
│   │       ├── controller/       # REST & WebSocket controllers
│   │       ├── dto/             # Data Transfer Objects
│   │       ├── exception/       # Exception handling
│   │       ├── model/           # JPA entities
│   │       ├── repository/      # Data access layer
│   │       ├── security/        # Security components
│   │       └── service/         # Business logic
│   └── resources/
│       ├── application.properties
│       ├── static/              # Static resources
│       └── templates/           # Templates (if any)
```

### Key Components

1. **Security Layer**: JWT-based authentication with Spring Security
2. **WebSocket Layer**: Real-time messaging with STOMP protocol
3. **Service Layer**: Business logic and transaction management
4. **Repository Layer**: Data access with Spring Data JPA
5. **Redis Integration**: Pub-sub for scalable messaging

## 🔧 Configuration

### Environment Variables

You can override configuration using environment variables:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=chatapp
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=your_secret_key
```

### Application Profiles

- `default`: Development configuration
- `prod`: Production configuration
- `test`: Testing configuration

## 🚀 Deployment

### Production Deployment

1. Build the JAR file: `mvn clean package`
2. Set production environment variables
3. Run: `java -jar target/chatapp-*.jar`

### Cloud Deployment

The application is ready for deployment on:

- AWS (EC2, ECS, Lambda)
- Google Cloud Platform
- Microsoft Azure
- Heroku
- DigitalOcean

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Issues

Found a bug or have a feature request? Please open an issue on GitHub.

## 📞 Support

For support and questions:

- Open an issue on GitHub
- Check the API documentation
- Review the code examples

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Spring Security for robust security features
- Redis team for the powerful in-memory data structure store
- PostgreSQL team for the reliable database system

---

**Built with ❤️ using Spring Boot 3 and Java 21**
# baatkaro
