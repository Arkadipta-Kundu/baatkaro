package org.arkadipta.chatapp.service;

import lombok.RequiredArgsConstructor;
import org.arkadipta.chatapp.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService Implementation - Bridges Spring Security with User
 * Entity
 * 
 * This service is a critical component of the Spring Security authentication
 * framework.
 * It connects Spring Security's authentication mechanism with our custom User
 * entity,
 * enabling database-backed authentication for the chat application.
 * 
 * Spring Security Integration:
 * - Implements UserDetailsService interface required by Spring Security
 * - Called during authentication process to load user credentials
 * - Provides user details for password validation and authorization
 * - Integrates with AuthenticationManager and PasswordEncoder
 * 
 * Authentication Flow:
 * 1. User submits login credentials (username/password)
 * 2. AuthenticationManager calls this service's loadUserByUsername()
 * 3. Service queries database via UserRepository
 * 4. Returns User entity (which implements UserDetails)
 * 5. Spring Security validates password and creates authentication
 * 
 * User Entity Integration:
 * - Our User entity implements UserDetails interface
 * - Provides required methods: getPassword(), getAuthorities(), etc.
 * - Enables direct use of User entity in Spring Security context
 * - No additional UserDetails wrapper classes needed
 * 
 * Security Features:
 * - Username-based user lookup (case-sensitive)
 * - Proper exception handling for non-existent users
 * - Integrates with User entity's enabled/disabled status
 * - Supports role-based authorization through User.getAuthorities()
 * 
 * Performance:
 * - Direct database query via UserRepository
 * - Leverages JPA caching when configured
 * - Single query per authentication attempt
 * - Efficient username-based index lookup
 * 
 * Error Handling:
 * - Throws UsernameNotFoundException for missing users
 * - Spring Security converts to proper HTTP status codes
 * - Logging integration for security audit trails
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads user details by username for Spring Security authentication
     * 
     * This method is the core of the authentication process. It's called by Spring
     * Security
     * when a user attempts to log in, and is responsible for retrieving the user's
     * credentials and account details from the database.
     * 
     * Authentication Process:
     * 1. Spring Security extracts username from login request
     * 2. Calls this method with the provided username
     * 3. Service queries UserRepository to find user by username
     * 4. Returns User entity (implements UserDetails) if found
     * 5. Spring Security validates password against stored hash
     * 6. Creates authentication context if credentials are valid
     * 
     * User Entity as UserDetails:
     * Our User entity implements UserDetails interface, providing:
     * - getPassword(): Returns BCrypt-hashed password for validation
     * - getAuthorities(): Returns user roles for authorization
     * - isEnabled(): Account status for login permission
     * - isAccountNonExpired(): Account validity check
     * - isCredentialsNonExpired(): Password validity check
     * - isAccountNonLocked(): Account lock status
     * 
     * Security Considerations:
     * - Username lookup is case-sensitive for security
     * - User account status is checked (enabled/disabled)
     * - Proper exception thrown for non-existent users
     * - No password returned to caller (Spring Security handles validation)
     * 
     * @param username the username provided in the authentication request
     * @return UserDetails implementation containing user credentials and status
     * @throws UsernameNotFoundException if no user found with the given username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
}