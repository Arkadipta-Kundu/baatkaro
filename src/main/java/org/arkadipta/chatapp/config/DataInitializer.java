package org.arkadipta.chatapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.model.Role;
import org.arkadipta.chatapp.model.User;
import org.arkadipta.chatapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data initialization component to create default users and data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            createDefaultUsers();
        }
    }

    private void createDefaultUsers() {
        log.info("Creating default users...");

        // Create admin user
        User admin = User.builder()
                .username("admin")
                .email("admin@chatapp.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .isEnabled(true)
                .isOnline(false)
                .build();

        userRepository.save(admin);

        // Create demo users
        User alice = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Alice")
                .lastName("Johnson")
                .role(Role.USER)
                .isEnabled(true)
                .isOnline(false)
                .build();

        User bob = User.builder()
                .username("bob")
                .email("bob@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Bob")
                .lastName("Smith")
                .role(Role.USER)
                .isEnabled(true)
                .isOnline(false)
                .build();

        User charlie = User.builder()
                .username("charlie")
                .email("charlie@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Charlie")
                .lastName("Brown")
                .role(Role.USER)
                .isEnabled(true)
                .isOnline(false)
                .build();

        userRepository.save(alice);
        userRepository.save(bob);
        userRepository.save(charlie);

        log.info("Default users created successfully");
        log.info("Admin credentials: admin/admin123");
        log.info("Demo user credentials: alice/password123, bob/password123, charlie/password123");
    }
}