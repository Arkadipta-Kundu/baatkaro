package org.arkadipta.baatkaro.controller;

import org.arkadipta.baatkaro.service.JwtService;
import org.arkadipta.baatkaro.service.UserService;
import org.arkadipta.baatkaro.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.util.Optional;

/**
 * Controller for serving Thymeleaf pages
 * Handles page navigation, JWT authentication, and initial data loading
 */
@Controller
public class PageController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    /**
     * Root path handler - redirect to API console
     */
    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/pages/api-console";
    }

    /**
     * Index page handler - redirect to API console
     */
    @GetMapping("/index")
    public String index() {
        return "redirect:/pages/api-console";
    }

    /**
     * Root path handler - redirect based on authentication status
     */
    @GetMapping("/pages")
    public String pagesRedirect(@CookieValue(value = "jwt_token", required = false) String jwtToken) {
        if (isValidToken(jwtToken)) {
            return "redirect:/pages/dashboard";
        }
        return "redirect:/pages/login";
    }

    /**
     * Serve the main Dashboard page
     * This page demonstrates user and room management with WebSocket integration
     */
    @GetMapping("/pages/dashboard")
    public String dashboard(@CookieValue(value = "jwt_token", required = false) String jwtToken, Model model) {
        UserResponse currentUser = validateTokenAndGetUser(jwtToken);
        if (currentUser == null) {
            return "redirect:/pages/login";
        }

        // Add model attributes
        model.addAttribute("pageTitle", "BaatKaro Dashboard");
        model.addAttribute("apiBaseUrl", "/api");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("jwtToken", jwtToken);
        return "dashboard";
    }

    /**
     * Serve the main chat page
     */
    @GetMapping("/pages/chat")
    public String chatPage(@CookieValue(value = "jwt_token", required = false) String jwtToken,
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) String username,
            Model model) {
        UserResponse currentUser = validateTokenAndGetUser(jwtToken);
        if (currentUser == null) {
            return "redirect:/pages/login";
        }

        model.addAttribute("pageTitle", "BaatKaro Chat");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("jwtToken", jwtToken);

        // Add chat-specific parameters
        if (roomId != null) {
            model.addAttribute("targetRoomId", roomId);
        }
        if (username != null) {
            model.addAttribute("targetUsername", username);
        }

        return "chat";
    }

    /**
     * Serve the login page
     */
    @GetMapping("/pages/login")
    public String loginPage(@CookieValue(value = "jwt_token", required = false) String jwtToken, Model model) {
        // If already authenticated, redirect to dashboard
        if (isValidToken(jwtToken)) {
            return "redirect:/pages/dashboard";
        }

        model.addAttribute("pageTitle", "Login - BaatKaro");
        return "login";
    }

    /**
     * Serve the API Testing Console page
     */
    @GetMapping("/pages/api-console")
    public String apiConsole(Model model) {
        model.addAttribute("pageTitle", "API Testing Console - BaatKaro");
        return "api-console";
    }

    /**
     * Serve the registration page
     */
    @GetMapping("/pages/register")
    public String registerPage(@CookieValue(value = "jwt_token", required = false) String jwtToken, Model model) {
        // If already authenticated, redirect to dashboard
        if (isValidToken(jwtToken)) {
            return "redirect:/pages/dashboard";
        }

        model.addAttribute("pageTitle", "Register - BaatKaro");
        return "register";
    }

    /**
     * Logout endpoint - clears JWT token cookie
     */
    @GetMapping("/pages/logout")
    public String logout(HttpServletResponse response) {
        // Clear JWT token cookie
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setMaxAge(0);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        response.addCookie(jwtCookie);

        return "redirect:/pages/login";
    }

    /**
     * Helper method to validate JWT token
     */
    private boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String username = jwtService.extractUsername(token);
            return username != null && jwtService.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Helper method to validate token and get user details
     */
    private UserResponse validateTokenAndGetUser(String token) {
        if (!isValidToken(token)) {
            return null;
        }

        try {
            String username = jwtService.extractUsername(token);
            Optional<UserResponse> userOpt = userService.findByUsername(username)
                    .map(UserResponse::fromUser);
            return userOpt.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}