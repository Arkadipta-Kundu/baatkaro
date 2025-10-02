package org.arkadipta.baatkaro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Room name is required")
    @Size(min = 1, max = 100, message = "Room name must be between 1 and 100 characters")
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Many-to-Many relationship with users
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "room_participants", joinColumns = @JoinColumn(name = "room_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<User> participants = new HashSet<>();

    // One-to-Many relationship with messages
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Message> messages = new HashSet<>();

    // Custom constructor for name and creator
    public Room(String name, User createdBy) {
        this.name = name;
        this.createdBy = createdBy;
        this.isPrivate = false;
        this.participants = new HashSet<>();
        this.messages = new HashSet<>();
        this.participants.add(createdBy); // Creator automatically joins
    }

    // Custom constructor for name, creator and privacy
    public Room(String name, User createdBy, Boolean isPrivate) {
        this.name = name;
        this.createdBy = createdBy;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        this.participants = new HashSet<>();
        this.messages = new HashSet<>();
        this.participants.add(createdBy); // Creator automatically joins
    }

    // Utility methods
    public void addParticipant(User user) {
        this.participants.add(user);
        user.getRooms().add(this);
    }

    public void removeParticipant(User user) {
        this.participants.remove(user);
        user.getRooms().remove(this);
    }

    public int getParticipantCount() {
        return participants.size();
    }

    // Override toString to include useful info without circular references
    public String toStringWithInfo() {
        return "Room{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdBy=" + (createdBy != null ? createdBy.getUsername() : null) +
                ", participantCount=" + getParticipantCount() +
                ", createdAt=" + createdAt +
                '}';
    }
}