package org.arkadipta.baatkaro.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // Enable CORS
                .csrf(csrf -> csrf.disable()) // Disable CSRF for REST API
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // Allow sessions for WebSocket
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/", "/api/info").permitAll()
                        .requestMatchers("/api/auth/**").permitAll() // Registration and auth endpoints
                        .requestMatchers("/ws/**").permitAll() // WebSocket endpoint
                        .requestMatchers("/pages/**").permitAll() // Thymeleaf pages
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**").permitAll() // Static
                                                                                                      // resources
                        // Protected API endpoints
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic
                        .realmName("BaatKaro Chat API")
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                    {
                                        "timestamp": "%s",
                                        "status": 401,
                                        "error": "Unauthorized",
                                        "message": "Authentication required",
                                        "path": "%s"
                                    }
                                    """.formatted(java.time.LocalDateTime.now(), request.getRequestURI()));
                        }));

        return http.build();
    }
}