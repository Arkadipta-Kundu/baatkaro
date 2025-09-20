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
 * Service for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    /**
     * Get user by ID
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
     * Update user profile
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
     * Search users by search term
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
     * Update user online status
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
     * Map User entity to UserResponse DTO
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