package org.arkadipta.baatkaro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.arkadipta.baatkaro.dto.MessageType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = true)
    private User receiver; // For private messages

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    private Room room; // For room messages

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Constructor for private message
    public Message(User sender, User receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.messageType = MessageType.PRIVATE;
    }

    // Constructor for room message
    public Message(User sender, Room room, String content) {
        this.sender = sender;
        this.room = room;
        this.content = content;
        this.messageType = MessageType.ROOM;
    }

    // Utility methods
    public boolean isPrivateMessage() {
        return messageType == MessageType.PRIVATE && receiver != null;
    }

    public boolean isRoomMessage() {
        return messageType == MessageType.ROOM && room != null;
    }

    // Override toString to include useful info without circular references
    public String toStringWithInfo() {
        return "Message{" +
                "id=" + id +
                ", sender=" + (sender != null ? sender.getUsername() : null) +
                ", receiver=" + (receiver != null ? receiver.getUsername() : null) +
                ", room=" + (room != null ? room.getName() : null) +
                ", content='" + content + '\'' +
                ", messageType=" + messageType +
                ", timestamp=" + timestamp +
                '}';
    }
}