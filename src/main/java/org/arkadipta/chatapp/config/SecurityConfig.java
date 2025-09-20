package org.arkadipta.chatapp.config;

import lombok.RequiredArgsConstructor;
import org.arkadipta.chatapp.security.JwtAuthenticationEntryPoint;
import org.arkadipta.chatapp.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration - Comprehensive Spring Security setup for JWT-based
 * authentication
 * 
 * This configuration class sets up the complete security framework for the chat
 * application:
 * 
 * Key Security Features:
 * - JWT-based stateless authentication (no server-side sessions)
 * - BCrypt password hashing for secure password storage
 * - CORS configuration for cross-origin requests (frontend integration)
 * - Method-level security annotations support (@PreAuthorize, @Secured, etc.)
 * - Custom JWT authentication filter for token validation
 * - Exception handling for authentication failures
 * 
 * Authentication Flow:
 * 1. User submits credentials to /api/auth/login
 * 2. DaoAuthenticationProvider validates credentials against database
 * 3. JWT token is generated and returned to client
 * 4. Client includes JWT token in Authorization header for subsequent requests
 * 5. JwtAuthenticationFilter validates token and sets SecurityContext
 * 6. Spring Security authorizes requests based on user roles/permissions
 * 
 * Security Architecture:
 * - Stateless: No server-side sessions (perfect for microservices)
 * - Filter Chain: Custom JWT filter + Spring Security default filters
 * - Exception Handling: Custom entry point for authentication errors
 * - CORS Support: Configured for frontend application integration
 * 
 * @author Chat App Development Team
 * @version 1.0
 * @since 2025-09-20
 */
@Configuration // Marks this as a Spring configuration class
@EnableWebSecurity // Enables Spring Security web security support
@EnableMethodSecurity // Enables method-level security annotations (@PreAuthorize, etc.)
@RequiredArgsConstructor // Lombok: Generates constructor for final fields
public class SecurityConfig {

    // ===== INJECTED DEPENDENCIES =====

    /**
     * Custom entry point for handling authentication failures
     * Returns JSON error responses instead of default HTML error pages
     * Provides consistent API error format for frontend consumption
     */
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Custom filter for JWT token validation
     * Runs before UsernamePasswordAuthenticationFilter in the filter chain
     * Extracts JWT from request, validates it, and sets authentication context
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Service for loading user details from database
     * Used by DaoAuthenticationProvider for credential validation
     * Implemented by UserService which loads User entities
     */
    private final UserDetailsService userDetailsService;

    // ===== SECURITY BEANS CONFIGURATION =====

    /**
     * Password Encoder Bean - BCrypt for secure password hashing
     * 
     * BCrypt is a slow, adaptive hash function designed for password hashing:
     * - Uses salt to prevent rainbow table attacks
     * - Adaptive: can be made slower as computers get faster
     * - Industry standard for password hashing
     * - Default strength of 10 rounds (2^10 iterations)
     * 
     * Usage:
     * - Encoding: passwordEncoder.encode(rawPassword) during registration
     * - Matching: passwordEncoder.matches(rawPassword, encodedPassword) during
     * login
     * 
     * @return BCryptPasswordEncoder instance for password operations
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Provider Bean - Configures database-based authentication
     * 
     * DaoAuthenticationProvider performs authentication against database:
     * - Loads user details from UserDetailsService (our UserService)
     * - Compares submitted password with stored hash using PasswordEncoder
     * - Returns authenticated Authentication object on success
     * - Throws exceptions on authentication failure
     * 
     * Flow:
     * 1. User submits username/password
     * 2. UserDetailsService loads User from database
     * 3. PasswordEncoder compares submitted password with stored hash
     * 4. Authentication succeeds/fails based on comparison
     * 
     * Note: Using deprecated constructor/methods but they still work
     * Future versions may require different configuration approach
     * 
     * @return configured DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication Manager Bean - Central authentication coordinator
     * 
     * AuthenticationManager is the main interface for authentication:
     * - Delegates to configured AuthenticationProvider(s)
     * - Used by JWT authentication logic in AuthService
     * - Returns authenticated Authentication object or throws exception
     * - Required for manual authentication in login endpoint
     * 
     * @param config Spring's authentication configuration
     * @return configured AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ===== CORS CONFIGURATION =====

    /**
     * CORS Configuration Bean - Enables cross-origin requests from frontend
     * 
     * CORS (Cross-Origin Resource Sharing) allows frontend applications
     * running on different domains/ports to access this API:
     * 
     * Configured Permissions:
     * - Origins: http://localhost:3000 (React), http://localhost:8080 (same origin)
     * - Methods: GET, POST, PUT, DELETE, OPTIONS (covers all REST operations)
     * - Headers: Authorization, Content-Type, Accept (supports JWT + JSON)
     * - Credentials: true (allows cookies/auth headers in cross-origin requests)
     * 
     * Security Notes:
     * - Production should specify exact frontend domains, not wildcards
     * - This allows JWT tokens in Authorization headers from frontend
     * - Preflight OPTIONS requests are automatically handled
     * 
     * @return configured CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow all origins with patterns (more flexible than specific origins)
        configuration.setAllowedOriginPatterns(List.of("*"));
        // Allow standard HTTP methods needed for REST API
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow all headers (including Authorization for JWT tokens)
        configuration.setAllowedHeaders(List.of("*"));
        // Allow credentials (needed for JWT tokens in Authorization header)
        configuration.setAllowCredentials(true);
        // Expose Authorization header to frontend (for new JWT tokens)
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS configuration to all endpoints
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ===== MAIN SECURITY FILTER CHAIN CONFIGURATION =====

    /**
     * Security Filter Chain Bean - Core security configuration
     * 
     * This method configures the complete security filter chain that processes
     * every HTTP request to the application. It defines:
     * 
     * 1. CORS Configuration: Cross-origin request handling
     * 2. CSRF Protection: Disabled (not needed for stateless JWT auth)
     * 3. Exception Handling: Custom error responses for auth failures
     * 4. Session Management: Stateless (no server-side sessions)
     * 5. Authorization Rules: Public vs protected endpoints
     * 6. JWT Filter: Custom filter for token validation
     * 
     * Authentication Flow Through Filter Chain:
     * 1. CORS filter handles cross-origin requests
     * 2. JWT filter extracts and validates token from Authorization header
     * 3. If valid token: SecurityContext is populated with user details
     * 4. If invalid/missing token: request continues but user is anonymous
     * 5. Authorization rules check if anonymous/authenticated user can access
     * endpoint
     * 6. If authorized: request proceeds to controller
     * 7. If unauthorized: JwtAuthenticationEntryPoint returns 401 error
     * 
     * @param http HttpSecurity configuration object
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS with our custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF protection (not needed for stateless JWT authentication)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure custom exception handling for authentication failures
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // Configure stateless session management (no server-side sessions)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ===== AUTHORIZATION RULES =====
                // Define which endpoints are public vs protected
                .authorizeHttpRequests(authz -> authz
                        // ===== PUBLIC ENDPOINTS (no authentication required) =====

                        // Authentication endpoints - must be public for login/registration
                        .requestMatchers("/api/auth/**").permitAll()

                        // General public API endpoints
                        .requestMatchers("/api/public/**").permitAll()

                        // ===== DEVELOPMENT/DOCUMENTATION ENDPOINTS =====

                        // Swagger/OpenAPI documentation - public for development
                        // Production deployments may want to restrict these
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**").permitAll()

                        // ===== WEBSOCKET ENDPOINTS =====

                        // WebSocket connection endpoints - authentication handled within WebSocket
                        // JWT token is validated in WebSocket interceptors
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/chat/**").permitAll()

                        // ===== MONITORING/OPERATIONS ENDPOINTS =====

                        // Health check and monitoring endpoints
                        // Production may want to restrict to internal networks
                        .requestMatchers("/actuator/**").permitAll()

                        // ===== STATIC RESOURCES =====

                        // Static web resources (CSS, JS, images)
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()

                        // ===== PROTECTED ENDPOINTS WITH SPECIFIC ROLES =====

                        // Admin-only endpoints - require ADMIN role
                        // Used for user management, system configuration, etc.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ===== DEFAULT PROTECTION =====

                        // All other endpoints require authentication (any valid JWT token)
                        // This includes: /api/users/**, /api/chat/**, etc.
                        .anyRequest().authenticated())

                // ===== AUTHENTICATION PROVIDER =====
                // Register our custom authentication provider for database validation
                .authenticationProvider(authenticationProvider())

                // ===== JWT FILTER REGISTRATION =====
                // Add our JWT filter before the standard username/password filter
                // This ensures JWT tokens are processed before any other authentication
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}