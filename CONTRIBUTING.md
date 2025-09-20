# Contributing to Chat Application API

Thank you for your interest in contributing to the Chat Application API! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 15+
- Redis 7.0+
- Git

### Development Setup

1. **Fork and Clone**

   ```bash
   git clone https://github.com/yourusername/chatapp.git
   cd chatapp
   ```

2. **Database Setup**

   ```bash
   # Start PostgreSQL and Redis
   docker-compose up postgres redis -d
   ```

3. **Build and Test**

   ```bash
   mvn clean install
   ```

4. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

## 📝 Development Guidelines

### Code Style

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods and classes
- Keep methods focused and concise
- Use Spring Boot best practices

### Project Structure

```
src/main/java/org/arkadipta/chatapp/
├── config/          # Configuration classes
├── controller/      # REST and WebSocket controllers
├── dto/            # Data Transfer Objects
├── exception/      # Custom exceptions
├── model/          # JPA entities
├── repository/     # Data access layer
├── security/       # Security components
└── service/        # Business logic
```

### Commit Message Format

Use conventional commits:

```
type(scope): description

Types: feat, fix, docs, style, refactor, test, chore
Scope: auth, chat, user, websocket, security, etc.

Examples:
feat(auth): add JWT refresh token functionality
fix(chat): resolve message ordering issue
docs: update API documentation
```

## 🛠️ Development Workflow

### Branch Strategy

- `main`: Production-ready code
- `develop`: Integration branch for features
- `feature/feature-name`: Feature development
- `bugfix/bug-name`: Bug fixes
- `hotfix/issue-name`: Critical fixes

### Pull Request Process

1. **Create Feature Branch**

   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make Changes**

   - Write tests for new functionality
   - Ensure all tests pass
   - Update documentation if needed

3. **Test Your Changes**

   ```bash
   mvn clean test
   mvn clean package
   ```

4. **Submit Pull Request**
   - Create PR against `develop` branch
   - Add clear description of changes
   - Reference related issues
   - Ensure CI checks pass

### Code Review Guidelines

- Be respectful and constructive
- Focus on code quality and maintainability
- Suggest improvements and alternatives
- Test the changes locally

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run integration tests
mvn test -Dtest=*IT
```

### Test Categories

- **Unit Tests**: Test individual components
- **Integration Tests**: Test component interactions
- **API Tests**: Test REST endpoints
- **WebSocket Tests**: Test real-time messaging

### Writing Tests

- Use JUnit 5 and Mockito
- Follow AAA pattern (Arrange, Act, Assert)
- Use meaningful test names
- Test both positive and negative scenarios

## 📋 Issue Guidelines

### Bug Reports

Include:

- Clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Environment details
- Log files if applicable

### Feature Requests

Include:

- Clear description of the feature
- Use case and benefits
- Possible implementation approach
- Screenshots or mockups if applicable

## 🔍 API Guidelines

### REST API

- Follow RESTful principles
- Use appropriate HTTP methods
- Return meaningful HTTP status codes
- Include proper error responses
- Document all endpoints

### WebSocket API

- Use STOMP protocol standards
- Handle connection failures gracefully
- Implement proper authentication
- Document message formats

## 📚 Documentation

### API Documentation

- Use Swagger/OpenAPI annotations
- Include request/response examples
- Document authentication requirements
- Explain error responses

### Code Documentation

- Add JavaDoc for public APIs
- Include inline comments for complex logic
- Update README for significant changes
- Maintain changelog

## 🚦 CI/CD Pipeline

### Automated Checks

- Code compilation
- Unit and integration tests
- Code quality analysis
- Security vulnerability scan
- Docker image build

### Quality Gates

- All tests must pass
- Code coverage > 80%
- No critical security vulnerabilities
- No code quality issues

## 🌟 Best Practices

### Security

- Never commit secrets or passwords
- Use environment variables for configuration
- Validate all user inputs
- Follow OWASP guidelines
- Keep dependencies updated

### Performance

- Use appropriate caching strategies
- Optimize database queries
- Monitor application metrics
- Handle large datasets efficiently

### Scalability

- Design for horizontal scaling
- Use stateless components
- Implement proper error handling
- Consider async processing

## 📞 Getting Help

- **Documentation**: Check README and API docs
- **Issues**: Search existing issues first
- **Discussions**: Use GitHub Discussions for questions
- **Code Review**: Ask for feedback on PRs

## 📜 License

By contributing, you agree that your contributions will be licensed under the MIT License.

## 🙏 Recognition

Contributors will be acknowledged in:

- CONTRIBUTORS.md file
- Release notes
- Project documentation

Thank you for contributing to making this project better! 🎉
