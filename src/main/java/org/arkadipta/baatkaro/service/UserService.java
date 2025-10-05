package org.arkadipta.baatkaro.service;

import org.arkadipta.baatkaro.dto.UserRegistrationRequest;
import org.arkadipta.baatkaro.dto.UserResponse;
import org.arkadipta.baatkaro.entity.User;
import org.arkadipta.baatkaro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebSocketEventListener webSocketEventListener;

    /**
     * Register a new user
     */
    public UserResponse registerUser(UserRegistrationRequest request) {
        // Validate passwords match
        if (!request.passwordsMatch()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Save user
        User savedUser = userRepository.save(user);

        return UserResponse.fromUser(savedUser);
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Get online users from active WebSocket sessions
     */
    public List<UserResponse> getOnlineUsers() {
        // Get usernames from active WebSocket sessions
        List<String> activeUsernames = webSocketEventListener.getActiveUsers().values()
                .stream()
                .distinct()
                .toList();

        // Get user details for active users
        return userRepository.findByUsernameIn(activeUsernames)
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * Get all users
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * Search users by username
     */
    public List<UserResponse> searchUsers(String searchTerm) {
        return userRepository.findByUsernameContaining(searchTerm)
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * Check if user exists
     */
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Authenticate user with username and password
     */
    public UserResponse authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Check if the provided password matches the stored password
            if (passwordEncoder.matches(password, user.getPassword())) {
                // Authentication successful
                return UserResponse.fromUser(user);
            }
        }

        // Authentication failed
        return null;
    }
}