package org.arkadipta.baatkaro.controller;

import org.arkadipta.baatkaro.dto.UserRegistrationRequest;
import org.arkadipta.baatkaro.dto.UserLoginRequest;
import org.arkadipta.baatkaro.dto.UserResponse;
import org.arkadipta.baatkaro.service.UserService;
import org.arkadipta.baatkaro.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request,
            BindingResult bindingResult) {

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            UserResponse userResponse = userService.registerUser(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("user", userResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Check if username is available
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        boolean exists = userService.userExists(username);

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("available", !exists);

        return ResponseEntity.ok(response);
    }

    /**
     * Login user with JWT authentication
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginRequest request,
            BindingResult bindingResult,
            HttpServletResponse httpResponse) {

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            // Authenticate user using Spring Security AuthenticationManager
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            // If authentication is successful, get user details
            UserResponse userResponse = userService.authenticateUser(request.getUsername(), request.getPassword());

            if (userResponse != null) {
                // Generate JWT token
                String token = jwtService.generateToken(request.getUsername());

                // Set JWT token as HTTP-only cookie
                Cookie jwtCookie = new Cookie("jwt_token", token);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setSecure(false); // Set to true in production with HTTPS
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge((int) (jwtService.getExpirationTime() / 1000)); // Convert to seconds
                httpResponse.addCookie(jwtCookie);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("token", token);
                response.put("tokenType", "Bearer");
                response.put("expiresIn", jwtService.getExpirationTime());
                response.put("user", userResponse);

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid username or password");
                return ResponseEntity.status(401).body(response);
            }

        } catch (AuthenticationException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Logout user by clearing JWT token cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse httpResponse) {
        // Clear JWT token cookie
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setMaxAge(0);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        httpResponse.addCookie(jwtCookie);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");

        return ResponseEntity.ok(response);
    }
}