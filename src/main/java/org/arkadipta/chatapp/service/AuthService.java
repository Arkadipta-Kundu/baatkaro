package org.arkadipta.chatapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.dto.auth.AuthResponse;
import org.arkadipta.chatapp.dto.auth.LoginRequest;
import org.arkadipta.chatapp.dto.auth.RefreshTokenRequest;
import org.arkadipta.chatapp.dto.auth.RegisterRequest;
import org.arkadipta.chatapp.model.Role;
import org.arkadipta.chatapp.model.User;
import org.arkadipta.chatapp.repository.UserRepository;
import org.arkadipta.chatapp.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service - Handles user registration, login, and JWT token
 * management
 * 
 * This service provides the core authentication functionality for the chat
 * application:
 * 
 * Core Responsibilities:
 * - User registration with validation and password hashing
 * - User login with credential validation and JWT token generation
 * - JWT token refresh for maintaining session continuity
 * - User logout with proper cleanup
 * - Integration with Spring Security authentication framework
 * 
 * Security Features:
 * - BCrypt password hashing for secure password storage
 * - JWT token generation with configurable expiration
 * - Refresh token support for seamless user experience
 * - Proper authentication error handling
 * - Transaction management for data consistency
 * 
 * Authentication Flow:
 * 1. Registration: Validate input → Hash password → Save user → Generate tokens
 * 2. Login: Validate credentials → Authenticate → Generate tokens → Update
 * status
 * 3. Refresh: Validate refresh token → Generate new access token
 * 4. Logout: Update user status → Invalidate tokens (future enhancement)
 * 
 * Integration Points:
 * - UserRepository: Database operations for user management
 * - PasswordEncoder: Secure password hashing and verification
 * - JwtUtils: JWT token generation and validation
 * - AuthenticationManager: Spring Security authentication coordination
 * 
 * @author Chat App Development Team
 * @version 1.0
 * @since 2025-09-20
 */
@Service // Marks this as a Spring service component
@RequiredArgsConstructor // Lombok: Generates constructor for final fields
@Slf4j // Lombok: Provides logger instance
public class AuthService {

    // ===== INJECTED DEPENDENCIES =====

    /** Repository for user database operations */
    private final UserRepository userRepository;

    /** Encoder for secure password hashing and verification */
    private final PasswordEncoder passwordEncoder;

    /** Utility for JWT token generation and validation */
    private final JwtUtils jwtUtils;

    /** Spring Security authentication manager for credential validation */
    private final AuthenticationManager authenticationManager;

    // ===== USER REGISTRATION =====

    /**
     * Registers a new user in the system
     * 
     * Registration Process:
     * 1. Validates that username and email are unique
     * 2. Hashes the password using BCrypt
     * 3. Creates User entity with default settings (USER role, enabled)
     * 4. Saves user to database with transaction protection
     * 5. Generates JWT access and refresh tokens
     * 6. Returns tokens for immediate login
     * 
     * Validation:
     * - Username uniqueness (database constraint)
     * - Email uniqueness (database constraint)
     * - Password strength (should be enforced at DTO validation level)
     * 
     * Security:
     * - Password is immediately hashed and never stored in plain text
     * - User starts with basic USER role (not ADMIN)
     * - Account is enabled by default
     * 
     * @param request Registration request containing username, email, password, and
     *                optional profile info
     * @return AuthResponse containing JWT tokens and user information
     * @throws org.springframework.dao.DataIntegrityViolationException if
     *                                                                 username/email
     *                                                                 already
     *                                                                 exists
     */
    @Transactional // Ensures database consistency - rollback on any error
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with username: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Create new user
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

        // Save user
        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        log.info("User registered successfully with ID: {}", user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationTime())
                .user(mapToUserInfo(user))
                .build();
    }

    /**
     * Authenticate user and generate tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            // Get user details
            User user = (User) authentication.getPrincipal();

            // Update online status
            user.setIsOnline(true);
            userRepository.save(user);

            // Generate tokens
            String accessToken = jwtUtils.generateToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);

            log.info("User logged in successfully with ID: {}", user.getId());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtUtils.getExpirationTime())
                    .user(mapToUserInfo(user))
                    .build();

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Attempting to refresh token");

        try {
            String refreshToken = request.getRefreshToken();

            // Validate refresh token
            if (!jwtUtils.isRefreshToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            if (jwtUtils.isTokenExpired(refreshToken)) {
                throw new RuntimeException("Refresh token has expired");
            }

            // Extract username from refresh token
            String username = jwtUtils.extractUsername(refreshToken);

            // Get user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate new access token
            String newAccessToken = jwtUtils.generateToken(user);

            log.info("Token refreshed successfully for user: {}", username);

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // Keep the same refresh token
                    .tokenType("Bearer")
                    .expiresIn(jwtUtils.getExpirationTime())
                    .user(mapToUserInfo(user))
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired refresh token");
        }
    }

    /**
     * Logout user
     */
    @Transactional
    public void logout(String username) {
        log.info("Logging out user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update online status
        user.updateOnlineStatus(false);
        userRepository.save(user);

        log.info("User logged out successfully: {}", username);
    }

    /**
     * Validate if username is available
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Validate if email is available
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Map User entity to UserInfo DTO
     */
    private AuthResponse.UserInfo mapToUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isOnline(user.getIsOnline())
                .build();
    }
}