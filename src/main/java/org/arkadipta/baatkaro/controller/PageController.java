package org.arkadipta.baatkaro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving Thymeleaf pages
 * Handles page navigation and initial data loading
 */
@Controller
public class PageController {

    /**
     * Root path handler - redirect to dashboard
     */
    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/pages/dashboard";
    }

    /**
     * Serve the main Dashboard page
     * This page demonstrates user and room management with WebSocket integration
     */
    @GetMapping("/pages/dashboard")
    public String dashboard(Model model) {
        // Add any initial model attributes if needed
        model.addAttribute("pageTitle", "BaatKaro Dashboard");
        model.addAttribute("apiBaseUrl", "/api");
        return "dashboard";
    }

    /**
     * Serve the main chat page
     */
    @GetMapping("/pages/chat")
    public String chatPage(Model model) {
        model.addAttribute("pageTitle", "BaatKaro Chat");
        return "chat";
    }

    /**
     * Serve the login page
     */
    @GetMapping("/pages/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "Login - BaatKaro");
        return "login";
    }

    /**
     * Serve the registration page
     */
    @GetMapping("/pages/register")
    public String registerPage(Model model) {
        model.addAttribute("pageTitle", "Register - BaatKaro");
        return "register";
    }
}