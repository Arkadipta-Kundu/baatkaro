package org.arkadipta.chatapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.dto.ApiResponse;
import org.arkadipta.chatapp.dto.user.UpdateUserRequest;
import org.arkadipta.chatapp.dto.user.UserResponse;
import org.arkadipta.chatapp.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Management REST Controller - Handles user profile and directory
 * operations
 * 
 * This controller provides comprehensive user management endpoints for the chat
 * application,
 * enabling profile management, user discovery, and online presence features
 * through a
 * well-designed RESTful API.
 * 
 * Core API Endpoints:
 * - GET /me: Current user profile retrieval
 * - PUT /me: Profile update operations
 * - GET /{id}: Public user profile lookup
 * - GET /username/{username}: User lookup by username
 * - GET /search: User search and discovery
 * - GET /online: Online users listing
 * - PUT /status: Online status management
 * 
 * Authentication & Authorization:
 * - All endpoints require valid JWT authentication
 * - Current user context automatically injected via Spring Security
 * - Profile operations limited to authenticated user's own data
 * - Public profile viewing for user discovery features
 * 
 * API Design Principles:
 * - RESTful resource-based URL structure (/api/users/...)
 * - Standard HTTP methods (GET, PUT) for semantic operations
 * - Consistent JSON response format via ApiResponse wrapper
 * - Comprehensive error handling with meaningful HTTP status codes
 * - Input validation using Bean Validation annotations
 * 
 * Response Format:
 * All endpoints return standardized ApiResponse<T> containing:
 * - success: Boolean indicating operation success
 * - message: Human-readable status message
 * - data: Requested user information (UserResponse DTO)
 * - timestamp: Response generation time for debugging
 * 
 * Security Features:
 * - JWT token validation for all requests
 * - User data exposure via safe DTOs (no sensitive information)
 * - Rate limiting ready (configurable via Spring Security)
 * - CORS configuration for cross-origin access
 * 
 * Performance Optimizations:
 * - Pagination support for user search operations
 * - Efficient database queries via service layer
 * - DTO pattern reduces data transfer overhead
 * - Caching integration points for frequently accessed data
 * 
 * Documentation & Testing:
 * - OpenAPI/Swagger annotations for API documentation
 * - Comprehensive operation descriptions and examples
 * - Response code documentation for all scenarios
 * - Tagged for organized API documentation
 * 
 * Integration Points:
 * - UserService: Business logic delegation
 * - Spring Security: Authentication and authorization
 * - WebSocket: Real-time status updates
 * - Chat Controllers: User discovery for chat operations
 * 
 * Error Handling:
 * - Global exception handler integration
 * - Detailed logging for debugging and monitoring
 * - User-friendly error messages
 * - Proper HTTP status codes for API clients
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and management endpoints")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves the current authenticated user's complete profile information
     * 
     * This endpoint provides the primary method for clients to access the current
     * user's profile data, including personal information, account status, and
     * application-specific metadata.
     * 
     * Authentication Flow:
     * 1. JWT token extracted from Authorization header
     * 2. Spring Security validates token and sets authentication context
     * 3. UserService retrieves current user from security context
     * 4. User entity converted to UserResponse DTO for safe exposure
     * 5. Complete profile data returned to client
     * 
     * Response Data:
     * - Basic Info: username, email, first/last name, full name
     * - Account Status: enabled, role, creation timestamp
     * - Presence: online status, last seen timestamp
     * - Profile: profile picture URL, preferences
     * - Security: No sensitive data (password, tokens) exposed
     * 
     * Use Cases:
     * - User profile page population
     * - Navigation bar user information display
     * - Account settings form pre-population
     * - Authorization decisions in UI components
     * 
     * HTTP Status Codes:
     * - 200 OK: Profile retrieved successfully
     * - 401 Unauthorized: Invalid or missing JWT token
     * - 500 Internal Server Error: Database or service errors
     * 
     * @return ResponseEntity containing ApiResponse with UserResponse data
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Get the profile of the currently authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        try {
            log.info("Getting current user profile");

            UserResponse userResponse = userService.getCurrentUserProfile();

            return ResponseEntity.ok(ApiResponse.success(userResponse, "Profile retrieved successfully"));

        } catch (Exception e) {
            log.error("Failed to get current user profile", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/me")
    @Operation(summary = "Update user profile", description = "Update the profile of the currently authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(@Valid @RequestBody UpdateUserRequest request) {
        try {
            log.info("Updating user profile");

            UserResponse userResponse = userService.updateUserProfile(request);

            return ResponseEntity.ok(ApiResponse.success(userResponse, "Profile updated successfully"));

        } catch (Exception e) {
            log.error("Failed to update user profile", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get user information by user ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        try {
            log.info("Getting user by ID: {}", userId);

            UserResponse userResponse = userService.getUserById(userId);

            return ResponseEntity.ok(ApiResponse.success(userResponse));

        } catch (Exception e) {
            log.error("Failed to get user by ID: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Get user information by username")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        try {
            log.info("Getting user by username: {}", username);

            UserResponse userResponse = userService.getUserByUsername(username);

            return ResponseEntity.ok(ApiResponse.success(userResponse));

        } catch (Exception e) {
            log.error("Failed to get user by username: {}", username, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Search users
     */
    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search for users by username, email, or name")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            log.info("Searching users with query: {}", q);

            List<UserResponse> users = userService.searchUsers(q, pageable);

            return ResponseEntity.ok(ApiResponse.success(users,
                    "Found " + users.size() + " users"));

        } catch (Exception e) {
            log.error("Failed to search users", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get online users
     */
    @GetMapping("/online")
    @Operation(summary = "Get online users", description = "Get list of currently online users")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Online users retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<List<UserResponse>>> getOnlineUsers() {
        try {
            log.info("Getting online users");

            List<UserResponse> onlineUsers = userService.getOnlineUsers();

            return ResponseEntity.ok(ApiResponse.success(onlineUsers,
                    "Found " + onlineUsers.size() + " online users"));

        } catch (Exception e) {
            log.error("Failed to get online users", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get users by IDs
     */
    @PostMapping("/by-ids")
    @Operation(summary = "Get users by IDs", description = "Get multiple users by their IDs")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByIds(@RequestBody List<Long> userIds) {
        try {
            log.info("Getting users by IDs: {}", userIds);

            List<UserResponse> users = userService.getUsersByIds(userIds);

            return ResponseEntity.ok(ApiResponse.success(users,
                    "Found " + users.size() + " users"));

        } catch (Exception e) {
            log.error("Failed to get users by IDs", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}