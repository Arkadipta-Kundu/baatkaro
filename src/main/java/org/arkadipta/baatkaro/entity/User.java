package org.arkadipta.baatkaro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Boolean online = false;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many-to-Many relationship with rooms
    @ManyToMany(mappedBy = "participants", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Room> rooms = new HashSet<>();

    // Custom constructor for username and password
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.online = false;
        this.rooms = new HashSet<>();
    }

    // Utility methods
    public void goOnline() {
        this.online = true;
        this.lastSeen = null; // Clear last seen when online
    }

    public void goOffline() {
        this.online = false;
        this.lastSeen = LocalDateTime.now();
    }

    // Override toString to include useful info without sensitive data
    public String toStringWithInfo() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", online=" + online +
                ", lastSeen=" + lastSeen +
                '}';
    }
}