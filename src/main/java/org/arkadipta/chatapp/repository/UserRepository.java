package org.arkadipta.chatapp.repository;

import org.arkadipta.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository Interface - Data Access Layer for User Entity Operations
 * 
 * This repository provides comprehensive database operations for user
 * management,
 * including authentication, search, and presence tracking functionality. It
 * extends
 * JpaRepository to leverage Spring Data JPA's automatic query generation and
 * custom query capabilities.
 * 
 * Core Database Operations:
 * - User authentication: Find by username/email for login validation
 * - Uniqueness validation: Check username/email existence for registration
 * - User search: Multi-field search across username, email, names
 * - Presence tracking: Online status management and queries
 * - Bulk operations: Batch user retrieval for chat operations
 * 
 * Custom Query Features:
 * - Case-insensitive search for better user experience
 * - Multi-field search using JPQL for flexible user discovery
 * - Optimized queries for frequently used operations
 * - Named parameter binding for SQL injection prevention
 * 
 * Performance Optimizations:
 * - Database indexes on username and email fields
 * - Efficient counting queries for existence checks
 * - Batch operations for multiple user operations
 * - Optimized search queries with LIKE operations
 * 
 * Security Considerations:
 * - Parameterized queries prevent SQL injection
 * - Username and email uniqueness enforcement
 * - Proper case handling for authentication
 * - Safe search operations without exposing sensitive data
 * 
 * Integration Points:
 * - Spring Security: User authentication and authorization
 * - UserService: Business logic and transaction management
 * - Chat Services: User validation for chat operations
 * - WebSocket: Online presence tracking
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by username containing (case-insensitive search)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> findByUsernameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Find users by email containing (case-insensitive search)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> findByEmailContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Find all online users
     */
    List<User> findByIsOnlineTrue();

    /**
     * Find users by first name or last name containing search term
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Find users by any field containing search term (username, email, firstName,
     * lastName)
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    /**
     * Find enabled users only
     */
    List<User> findByIsEnabledTrue();

    /**
     * Count users by IDs
     */
    long countByIdIn(List<Long> ids);
}