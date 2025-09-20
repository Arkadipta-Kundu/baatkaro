package org.arkadipta.chatapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.dto.ApiResponse;
import org.arkadipta.chatapp.dto.auth.*;
import org.arkadipta.chatapp.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST Controller - Handles all authentication-related HTTP
 * endpoints
 * 
 * This controller provides the public API for user authentication operations
 * including:
 * - User registration (creating new accounts)
 * - User login (credential validation and JWT token generation)
 * - JWT token refresh (maintaining session continuity)
 * - User logout (session cleanup)
 * 
 * API Design Principles:
 * - RESTful endpoints following standard HTTP conventions
 * - Consistent JSON request/response format
 * - Comprehensive error handling with meaningful HTTP status codes
 * - Input validation using Bean Validation annotations
 * - Swagger/OpenAPI documentation for all endpoints
 * 
 * Security Considerations:
 * - All endpoints are public (no authentication required)
 * - Password validation and hashing handled by service layer
 * - JWT tokens returned in response body (not cookies for SPA compatibility)
 * - Error responses don't leak sensitive information
 * 
 * HTTP Status Codes Used:
 * - 200 OK: Successful login/refresh/logout
 * - 201 CREATED: Successful registration
 * - 400 BAD REQUEST: Invalid input data or business logic errors
 * - 401 UNAUTHORIZED: Invalid credentials
 * - 409 CONFLICT: Username/email already exists
 * - 500 INTERNAL SERVER ERROR: Unexpected system errors
 * 
 * Request/Response Format:
 * - All requests accept JSON with Content-Type: application/json
 * - All responses return JSON with consistent ApiResponse wrapper
 * - Error responses include error codes and user-friendly messages
 * - Success responses include relevant data (tokens, user info)
 * 
 * Integration Points:
 * - AuthService: Business logic for authentication operations
 * - Spring Security: Integration with security context and filters
 * - Bean Validation: Automatic request validation
 * - Swagger: Automatic API documentation generation
 * 
 * @author Chat App Development Team
 * @version 1.0
 * @since 2025-09-20
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Registration request for username: {}", request.getUsername());

            AuthResponse response = authService.register(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "User registered successfully"));

        } catch (Exception e) {
            log.error("Registration failed for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Login user
     */
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return JWT tokens")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login request for username: {}", request.getUsername());

            AuthResponse response = authService.login(request);

            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));

        } catch (Exception e) {
            log.error("Login failed for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get a new access token using refresh token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.info("Token refresh request");

            AuthResponse response = authService.refreshToken(request);

            return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Logout user
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logout current user and update online status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<Void>> logout() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Logout request for username: {}", username);

            authService.logout(username);

            return ResponseEntity.ok(ApiResponse.success("Logout successful"));

        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Check if username is available
     */
    @GetMapping("/check-username")
    @Operation(summary = "Check username availability", description = "Check if a username is available for registration")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Username availability checked")
    })
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        log.info("Checking username availability: {}", username);

        boolean available = authService.isUsernameAvailable(username);

        return ResponseEntity.ok(ApiResponse.success(available,
                available ? "Username is available" : "Username is already taken"));
    }

    /**
     * Check if email is available
     */
    @GetMapping("/check-email")
    @Operation(summary = "Check email availability", description = "Check if an email is available for registration")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email availability checked")
    })
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        log.info("Checking email availability: {}", email);

        boolean available = authService.isEmailAvailable(email);

        return ResponseEntity.ok(ApiResponse.success(available,
                available ? "Email is available" : "Email is already registered"));
    }
}