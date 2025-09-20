package org.arkadipta.chatapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User Entity - Core user model for the chat application
 * 
 * This class serves multiple purposes:
 * 1. JPA Entity for database persistence
 * 2. Spring Security UserDetails implementation for authentication
 * 3. Contains user profile information and chat relationships
 * 
 * Key Features:
 * - Implements UserDetails for Spring Security integration
 * - Uses Lombok for automatic getter/setter generation
 * - Builder pattern for easy object construction
 * - Tracks online status and last seen timestamps
 * - Maintains relationships with chat rooms and messages
 * 
 * Database Design:
 * - Uses IDENTITY strategy for auto-incrementing primary key
 * - Enforces unique constraints on username and email
 * - Uses optimized column lengths for better performance
 * 
 * Security Integration:
 * - Password field stores BCrypt hashed passwords
 * - Role-based authorization with USER/ADMIN roles
 * - Account status tracking (enabled/disabled)
 * 
 * @author Chat App Development Team
 * @version 1.0
 * @since 2025-09-20
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"), // Ensures username uniqueness across all users
        @UniqueConstraint(columnNames = "email") // Ensures email uniqueness across all users
})
@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@Builder // Lombok: Generates builder pattern methods
@NoArgsConstructor // Lombok: Generates no-argument constructor (required by JPA)
@AllArgsConstructor // Lombok: Generates all-argument constructor (used by builder)
public class User implements UserDetails {

    // ===== PRIMARY KEY =====
    /**
     * Primary key for the user entity
     * Uses IDENTITY strategy which relies on database auto-increment feature
     * PostgreSQL will automatically generate sequential IDs starting from 1
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== AUTHENTICATION FIELDS =====
    /**
     * Unique username for login and identification
     * - Cannot be null (required for login)
     * - Must be unique across all users
     * - Limited to 50 characters for performance
     * - Used as primary login identifier
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * User's email address
     * - Cannot be null (required for account recovery)
     * - Must be unique across all users
     * - Limited to 100 characters (standard email length)
     * - Used for password reset and notifications
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * BCrypt hashed password for authentication
     * - Cannot be null (required for login)
     * - Stored as BCrypt hash (never plain text)
     * - Hash includes salt for security
     * - Minimum 8 characters enforced at service layer
     */
    @Column(nullable = false)
    private String password;

    // ===== PROFILE INFORMATION =====
    /**
     * User's first name (optional)
     * - Can be null (profile completion is optional)
     * - Limited to 50 characters
     * - Used for display purposes and full name generation
     */
    @Column(name = "first_name", length = 50)
    private String firstName;

    /**
     * User's last name (optional)
     * - Can be null (profile completion is optional)
     * - Limited to 50 characters
     * - Used for display purposes and full name generation
     */
    @Column(name = "last_name", length = 50)
    private String lastName;

    // ===== AUTHORIZATION & STATUS =====
    /**
     * User's role in the system for authorization
     * - Stored as STRING enum in database
     * - Default value is USER (basic user permissions)
     * - ADMIN role has additional management permissions
     * - Used by Spring Security for role-based access control
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Real-time online status indicator
     * - Default false (user starts offline)
     * - Updated when user connects/disconnects via WebSocket
     * - Used to show online/offline status to other users
     * - Automatically set to false on logout
     */
    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = false;

    /**
     * Timestamp of user's last activity
     * - Updated whenever user performs any action
     * - Used for "last seen" display to other users
     * - Helps with cleaning up inactive sessions
     * - Set to current time when user goes offline
     */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    // ===== AUDIT FIELDS =====
    /**
     * Account creation timestamp
     * - Set automatically when user registers
     * - Cannot be updated after creation (updatable = false)
     * - Used for analytics and account age tracking
     * - Set via @PrePersist lifecycle callback
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last profile update timestamp
     * - Updated automatically on any profile change
     * - Set via @PreUpdate lifecycle callback
     * - Used for tracking profile modification history
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Account status flag for enabling/disabling users
     * - Default true (account enabled by default)
     * - Used by Spring Security for account validation
     * - Admin can disable accounts without deletion
     * - Disabled accounts cannot login
     */
    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * URL to user's profile picture (optional)
     * - Can be null (default avatar will be used)
     * - Stores full URL to image resource
     * - Could be local file path or external URL
     * - Future: integrate with file upload service
     */
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    // ===== RELATIONSHIP MAPPINGS =====
    /**
     * Many-to-many relationship with chat rooms
     * - A user can participate in multiple chat rooms
     * - A chat room can have multiple participants
     * - LAZY loading to avoid performance issues
     * - Mapped by "participants" field in ChatRoom entity
     * - Uses Set to avoid duplicates and improve performance
     * - Default empty HashSet initialized by builder
     */
    @ManyToMany(mappedBy = "participants", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ChatRoom> chatRooms = new HashSet<>();

    /**
     * One-to-many relationship with sent messages
     * - A user can send multiple messages
     * - Each message has exactly one sender
     * - LAZY loading to avoid loading all user messages
     * - CASCADE.ALL ensures messages are deleted when user is deleted
     * - Mapped by "sender" field in Message entity
     * - Uses Set to avoid duplicates and improve performance
     */
    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Message> sentMessages = new HashSet<>();

    // ===== JPA LIFECYCLE CALLBACKS =====
    /**
     * Automatically set creation and update timestamps when entity is first
     * persisted
     * Called automatically by JPA before INSERT operations
     * Ensures every user has creation and update timestamps
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Automatically update the timestamp when entity is modified
     * Called automatically by JPA before UPDATE operations
     * Tracks when user profile was last modified
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== SPRING SECURITY USERDETAILS IMPLEMENTATION =====
    /**
     * Returns the authorities granted to the user
     * Converts the user's role into Spring Security GrantedAuthority
     * Prefixes role with "ROLE_" as required by Spring Security convention
     * 
     * @return Collection of authorities (roles) for this user
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Returns the password used to authenticate the user
     * Required by Spring Security UserDetails interface
     * Returns the BCrypt hashed password
     * 
     * @return the user's hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the username used to authenticate the user
     * Required by Spring Security UserDetails interface
     * This is the primary identifier for login
     * 
     * @return the username for authentication
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Indicates whether the user's account has expired
     * For this application, accounts never expire
     * Could be extended to implement account expiration logic
     * 
     * @return true (accounts never expire in this implementation)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked
     * For this application, accounts are never locked
     * Could be extended to implement account locking after failed login attempts
     * 
     * @return true (accounts are never locked in this implementation)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired
     * For this application, passwords never expire
     * Could be extended to implement password expiration policy
     * 
     * @return true (passwords never expire in this implementation)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled
     * Maps to the isEnabled field in database
     * Disabled users cannot authenticate
     * 
     * @return true if user is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    // ===== BUSINESS LOGIC HELPER METHODS =====
    /**
     * Generates user's full display name
     * Priority: "FirstName LastName" > "FirstName" > "LastName" > "username"
     * Used in UI to display user-friendly names
     * 
     * @return formatted full name or username as fallback
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }

    /**
     * Updates the user's last seen timestamp to current time
     * Called whenever user performs any activity in the application
     * Also updates the general updatedAt timestamp for audit purposes
     * Used for displaying "last seen" information to other users
     */
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Sets user's online status and updates relevant timestamps
     * When going online: updates lastSeen to current time
     * When going offline: lastSeen keeps the timestamp when they went offline
     * Always updates the general updatedAt timestamp
     * 
     * @param online true to set user online, false for offline
     */
    public void setOnline(boolean online) {
        this.isOnline = online;
        if (online) {
            this.lastSeen = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Convenience method that delegates to setOnline()
     * Provides consistent API for updating online status
     * Used by service layer to maintain clean abstraction
     * 
     * @param isOnline true to set user online, false for offline
     */
    public void updateOnlineStatus(boolean isOnline) {
        setOnline(isOnline);
    }
}