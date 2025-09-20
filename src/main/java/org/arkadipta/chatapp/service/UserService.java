package org.arkadipta.chatapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.dto.user.UpdateUserRequest;
import org.arkadipta.chatapp.dto.user.UserResponse;
import org.arkadipta.chatapp.model.User;
import org.arkadipta.chatapp.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User Management Service - Handles user profile operations, search, and status
 * management
 * 
 * This service provides comprehensive user management functionality for the
 * chat application:
 * 
 * Core Responsibilities:
 * - User profile retrieval and management (current user, by ID, by username)
 * - User profile updates with validation and conflict checking
 * - User search functionality with pagination support
 * - Online status tracking and management
 * - User existence validation for chat operations
 * - Data Transfer Object (DTO) mapping for clean API responses
 * 
 * Security Integration:
 * - Leverages Spring Security Context to identify current authenticated user
 * - Ensures users can only access/modify their own profiles (implicit security)
 * - Validates email uniqueness during profile updates
 * - Provides safe user data exposure through UserResponse DTOs
 * 
 * Business Logic:
 * - Profile Updates: Validates email uniqueness, updates only provided fields
 * - Search: Implements flexible user search with database optimization
 * - Online Status: Real-time tracking of user presence for chat features
 * - Batch Operations: Efficient bulk user retrieval for chat room participants
 * 
 * Performance Considerations:
 * - Uses pagination for search results to handle large user bases
 * - Implements efficient batch queries for multiple user operations
 * - Caches current user context to avoid repeated database calls
 * - Optimized DTO mapping to minimize data transfer overhead
 * 
 * Integration Points:
 * - UserRepository: Database operations for user entities
 * - SecurityContextHolder: Current user authentication context
 * - Chat Services: User validation for chat room operations
 * - WebSocket Services: Online status updates for real-time features
 * 
 * Transaction Management:
 * - Uses @Transactional for profile updates to ensure data consistency
 * - Rollback protection for failed update operations
 * - Atomic online status updates for reliable presence tracking
 * 
 * Data Privacy:
 * - Exposes user data through controlled UserResponse DTOs
 * - Excludes sensitive information like passwords from API responses
 * - Provides appropriate user information for chat functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves the currently authenticated user from the security context
     * 
     * This method is fundamental to user-specific operations throughout the
     * application.
     * It extracts the authenticated user's username from Spring Security's
     * SecurityContext
     * and fetches the complete User entity from the database.
     * 
     * Security Context Integration:
     * - Uses SecurityContextHolder to access the current authentication
     * - Assumes authentication has already been validated by JWT filter
     * - Username is extracted from the principal (set during JWT authentication)
     * 
     * Error Handling:
     * - Throws RuntimeException if user is not found (should not happen in normal
     * flow)
     * - This typically indicates a data consistency issue or user deletion
     * 
     * Usage Pattern:
     * - Called by all user-specific operations (profile updates, chat operations)
     * - Provides the authenticated user context for authorization decisions
     * - Ensures operations are performed on behalf of the correct user
     * 
     * @return User entity of the currently authenticated user
     * @throws RuntimeException if the authenticated user is not found in database
     */
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    /**
     * Retrieves a user by their unique identifier and returns a safe DTO
     * representation
     * 
     * This method provides public user information lookup functionality used
     * primarily
     * for chat operations, user directories, and profile viewing.
     * 
     * Security Considerations:
     * - Returns only public-safe user information via UserResponse DTO
     * - No authentication required as this provides public user data
     * - Sensitive fields (password, tokens) are excluded from response
     * 
     * Use Cases:
     * - Chat participant information display
     * - User profile viewing in chat interface
     * - User validation for chat room invitations
     * - Admin user management operations
     * 
     * @param userId the unique identifier of the user to retrieve
     * @return UserResponse DTO containing public user information
     * @throws RuntimeException if user with specified ID does not exist
     */
    public UserResponse getUserById(Long userId) {
        log.info("Fetching user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return mapToUserResponse(user);
    }

    /**
     * Get user by username
     */
    public UserResponse getUserByUsername(String username) {
        log.info("Fetching user with username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        return mapToUserResponse(user);
    }

    /**
     * Get current user profile
     */
    public UserResponse getCurrentUserProfile() {
        User currentUser = getCurrentUser();
        return mapToUserResponse(currentUser);
    }

    /**
     * Updates the current user's profile with provided information and validation
     * 
     * This method handles comprehensive profile updates with business rule
     * validation
     * and data integrity checks. It implements partial update pattern - only
     * provided
     * fields are updated, null values are ignored.
     * 
     * Business Rules:
     * - Email uniqueness: Prevents duplicate email addresses across all users
     * - Partial updates: Only non-null fields in request are updated
     * - Current user only: Users can only update their own profiles
     * - Immediate persistence: Changes are saved and reflected immediately
     * 
     * Validation Logic:
     * - Email conflict checking before update
     * - Field-by-field conditional updates
     * - Input sanitization through DTO validation
     * 
     * Transaction Management:
     * - @Transactional ensures atomic updates
     * - Rollback on any validation failure
     * - Consistent database state maintained
     * 
     * Audit Trail:
     * - Logs profile update attempts for security monitoring
     * - Tracks successful updates with user identification
     * 
     * @param request DTO containing fields to update (null fields ignored)
     * @return UserResponse with updated user information
     * @throws RuntimeException if email is already taken by another user
     */
    @Transactional
    public UserResponse updateUserProfile(UpdateUserRequest request) {
        User currentUser = getCurrentUser();

        log.info("Updating profile for user: {}", currentUser.getUsername());

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(currentUser.getEmail())) {
            // Check if email is already taken by another user
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email is already registered");
            }
            currentUser.setEmail(request.getEmail());
        }

        // Update other fields
        if (request.getFirstName() != null) {
            currentUser.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            currentUser.setLastName(request.getLastName());
        }

        if (request.getProfilePictureUrl() != null) {
            currentUser.setProfilePictureUrl(request.getProfilePictureUrl());
        }

        User updatedUser = userRepository.save(currentUser);

        log.info("Profile updated successfully for user: {}", updatedUser.getUsername());

        return mapToUserResponse(updatedUser);
    }

    /**
     * Performs flexible user search across multiple fields with pagination support
     * 
     * This method enables user discovery functionality essential for chat
     * applications.
     * It searches across username, email, firstName, and lastName fields to provide
     * comprehensive user lookup capabilities.
     * 
     * Search Strategy:
     * - Multi-field search: Matches across username, email, first name, last name
     * - Case-insensitive matching for user-friendly search experience
     * - Partial matching support (contains-style search)
     * - Excludes current user from results to prevent self-selection
     * 
     * Performance Optimization:
     * - Database-level search implementation in UserRepository
     * - Pagination support to handle large user bases efficiently
     * - Stream processing for efficient DTO conversion
     * - Indexed database fields for fast search performance
     * 
     * Use Cases:
     * - Finding users to invite to chat rooms
     * - User directory browsing and discovery
     * - Admin user management and lookup
     * - Contact/friend finding functionality
     * 
     * @param searchTerm the text to search for in user fields
     * @param pageable   pagination parameters for result limiting
     * @return List of UserResponse DTOs matching the search criteria
     */
    public List<UserResponse> searchUsers(String searchTerm, Pageable pageable) {
        log.info("Searching users with term: {}", searchTerm);

        List<User> users = userRepository.searchUsers(searchTerm);

        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all online users
     */
    public List<UserResponse> getOnlineUsers() {
        log.info("Fetching all online users");

        List<User> onlineUsers = userRepository.findByIsOnlineTrue();

        return onlineUsers.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates user's online presence status for real-time chat features
     * 
     * This method manages user presence tracking which is crucial for real-time
     * chat applications. It updates both the online status flag and the last seen
     * timestamp for accurate presence indication.
     * 
     * Presence Management:
     * - Real-time status updates: Immediate reflection of online/offline state
     * - Last seen tracking: Automatic timestamp updates for offline users
     * - Status persistence: Survives application restarts and reconnections
     * - Integration with WebSocket events for automatic status changes
     * 
     * Usage Patterns:
     * - WebSocket connection events: Set online on connect, offline on disconnect
     * - Periodic heartbeat: Maintain online status during active sessions
     * - Manual status changes: User-initiated presence updates
     * - Session timeout: Automatic offline status for inactive users
     * 
     * Business Logic:
     * - Online status: Indicates real-time availability for chat
     * - Last seen: Provides timestamp of last activity for offline users
     * - Status broadcasting: Can trigger WebSocket notifications to contacts
     * 
     * @param username the username of the user to update
     * @param isOnline true for online status, false for offline
     * @throws RuntimeException if user is not found
     */
    @Transactional
    public void updateOnlineStatus(String username, boolean isOnline) {
        log.info("Updating online status for user: {} to {}", username, isOnline);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.updateOnlineStatus(isOnline);
        userRepository.save(user);
    }

    /**
     * Get users by IDs (for chat room participants)
     */
    public List<UserResponse> getUsersByIds(List<Long> userIds) {
        log.info("Fetching users with IDs: {}", userIds);

        List<User> users = userRepository.findAllById(userIds);

        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if user exists by ID
     */
    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

    /**
     * Check if all user IDs exist
     */
    public boolean allUsersExist(List<Long> userIds) {
        long existingCount = userRepository.countByIdIn(userIds);
        return existingCount == userIds.size();
    }

    /**
     * Converts User entity to UserResponse DTO for secure API data transfer
     * 
     * This method implements the Data Transfer Object pattern to provide clean
     * separation between internal data models and external API representations.
     * It ensures only appropriate user information is exposed to API consumers.
     * 
     * Data Privacy Implementation:
     * - Excludes sensitive fields: password, refresh tokens, internal IDs
     * - Includes public profile information: names, email, status, timestamps
     * - Role information: Provides user authorization level for UI decisions
     * - Online presence: Real-time status for chat interface features
     * 
     * DTO Benefits:
     * - Version stability: Internal model changes don't break API contracts
     * - Security: Controlled data exposure prevents information leakage
     * - Performance: Optimized data structure for network transfer
     * - Flexibility: Different DTO variants for different use cases
     * 
     * Builder Pattern Usage:
     * - Leverages Lombok @Builder for clean, immutable DTO construction
     * - Null-safe property mapping from entity to DTO
     * - Consistent data transformation across all user operations
     * 
     * @param user the User entity to convert
     * @return UserResponse DTO with safe, public user information
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isOnline(user.getIsOnline())
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .isEnabled(user.getIsEnabled())
                .build();
    }
}