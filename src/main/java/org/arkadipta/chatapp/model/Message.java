package org.arkadipta.chatapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message Entity - Represents messages sent in chat rooms
 * 
 * This entity stores all messages exchanged in the chat application including:
 * - Text messages
 * - File attachments
 * - Image uploads
 * - System notifications
 * 
 * Key Features:
 * - Supports different message types (TEXT, FILE, IMAGE, SYSTEM)
 * - Tracks message relationships (replies to other messages)
 * - Implements soft deletion for message history preservation
 * - Maintains full audit trail with timestamps
 * - Links to sender and chat room through foreign keys
 * 
 * Database Design:
 * - Uses TEXT column type for content to support long messages
 * - Indexes on chat_room_id and sender_id for query performance
 * - Soft deletion preserves message history for compliance
 * - Timestamp tracking for message ordering and audit
 * 
 * Business Logic:
 * - Messages belong to exactly one chat room and one sender
 * - Messages can be replies to other messages (threaded conversations)
 * - Different message types support various content formats
 * - Soft deletion allows "unsending" messages without data loss
 * 
 * @author Chat App Development Team
 * @version 1.0
 * @since 2025-09-20
 */
@Entity
@Table(name = "messages")
@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@Builder // Lombok: Generates builder pattern methods
@NoArgsConstructor // Lombok: Generates no-argument constructor (required by JPA)
@AllArgsConstructor // Lombok: Generates all-argument constructor (used by builder)
public class Message {

    // ===== PRIMARY KEY =====
    /**
     * Primary key for the message entity
     * Auto-generated using database IDENTITY strategy
     * Used for message identification and foreign key references
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== MESSAGE CONTENT =====
    /**
     * The actual message content
     * - Cannot be null (every message must have content)
     * - Uses TEXT column type to support long messages (up to 65,535 characters)
     * - For file/image messages, contains file metadata or URLs
     * - For system messages, contains formatted notification text
     * - HTML/Markdown formatting can be stored here if implemented
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Type of message for different content handling
     * - TEXT: Regular text message from user
     * - FILE: File attachment with metadata in content
     * - IMAGE: Image upload with URL/metadata in content
     * - SYSTEM: Automated system notifications (user joined, left, etc.)
     * - Stored as STRING enum for readability in database
     * - Default value is TEXT for regular messages
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    // ===== RELATIONSHIPS =====
    /**
     * The user who sent this message
     * - Many-to-One relationship: many messages can have same sender
     * - Cannot be null: every message must have a sender
     * - LAZY loading to avoid loading user data unnecessarily
     * - Foreign key stored in 'sender_id' column
     * - Used for displaying sender information and authorization
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The chat room where this message was sent
     * - Many-to-One relationship: many messages belong to same chat room
     * - Cannot be null: every message must belong to a chat room
     * - LAZY loading to avoid loading chat room data unnecessarily
     * - Foreign key stored in 'chat_room_id' column
     * - Used for message filtering and chat room displays
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    // ===== TIMESTAMP FIELDS =====
    /**
     * When the message was originally sent
     * - Cannot be null: every message must have a timestamp
     * - Cannot be updated: original send time is immutable
     * - Used for message ordering and display
     * - Set automatically via @PrePersist lifecycle callback
     * - Critical for maintaining chronological message order
     */
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // ===== MESSAGE EDITING TRACKING =====
    /**
     * Flag indicating if message has been edited after sending
     * - Default false: messages start as unedited
     * - Set to true when message content is modified
     * - Used to show "edited" indicator in UI
     * - Maintains transparency about message modifications
     */
    @Column(name = "is_edited")
    @Builder.Default
    private Boolean isEdited = false;

    /**
     * Timestamp of the last edit operation
     * - Can be null: only set when message is actually edited
     * - Updated each time message is modified
     * - Used to show "last edited" time to users
     * - Helps with edit history tracking
     */
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    // ===== SOFT DELETION SUPPORT =====
    /**
     * Flag for soft deletion of messages
     * - Default false: messages start as not deleted
     * - Set to true when user "deletes" a message
     * - Soft deletion preserves data for audit/compliance
     * - Deleted messages are hidden from normal queries
     * - Allows for potential message recovery features
     */
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Timestamp when message was soft deleted
     * - Can be null: only set when message is deleted
     * - Records when deletion occurred for audit purposes
     * - Used for cleanup policies and data retention
     * - Supports compliance with data retention requirements
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== FILE ATTACHMENT SUPPORT =====
    /**
     * URL or path to attached file (for FILE/IMAGE message types)
     * - Can be null: only used for file/image messages
     * - Could be local file path or cloud storage URL
     * - Used to retrieve and display file attachments
     * - Security: should validate file access permissions
     */
    @Column(name = "file_url")
    private String fileUrl;

    /**
     * Original filename of attached file
     * - Can be null: only used for file attachments
     * - Preserves original filename for user display
     * - Used for download filename and file type detection
     * - Important for user experience with attachments
     */
    @Column(name = "file_name")
    private String fileName;

    /**
     * Size of attached file in bytes
     * - Can be null: only used for file attachments
     * - Used for storage quota management
     * - Displayed to users for file information
     * - Helps with bandwidth and storage optimization
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * MIME type of attached file
     * - Can be null: only used for file attachments
     * - Used for proper file handling and display
     * - Helps browsers handle file downloads correctly
     * - Security: validates file types allowed in system
     */
    @Column(name = "file_type")
    private String fileType;

    // ===== MESSAGE THREADING SUPPORT =====
    /**
     * Reference to original message for threaded replies
     * - Can be null: only set for reply messages
     * - Many-to-One relationship: many replies to one original message
     * - LAZY loading to avoid recursive loading of message threads
     * - Enables threaded conversation support
     * - Foreign key stored in 'reply_to_message_id' column
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    // ===== JPA LIFECYCLE CALLBACKS =====
    /**
     * Automatically set timestamp when message is first persisted
     * Called automatically by JPA before INSERT operations
     * Ensures every message has a creation timestamp
     * Only sets timestamp if not already provided
     */
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // ===== BUSINESS LOGIC HELPER METHODS =====
    /**
     * Marks message as edited and sets edit timestamp
     * Called when message content is modified after creation
     * Updates both edit flag and timestamp atomically
     * Used by service layer when handling message edits
     */
    public void markAsEdited() {
        this.isEdited = true;
        this.editedAt = LocalDateTime.now();
    }

    /**
     * Soft deletes the message by setting deletion flags
     * Preserves original message data for audit/compliance
     * Sets both deletion flag and timestamp
     * Message will be hidden from normal queries but data is preserved
     */
    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Checks if this message contains a file attachment
     * Returns true only if message type is FILE and file URL exists
     * Used for conditional rendering of file attachment UI
     * 
     * @return true if message has file attachment, false otherwise
     */
    public boolean isFileMessage() {
        return type == MessageType.FILE && fileUrl != null;
    }

    /**
     * Checks if this message contains an image attachment
     * Returns true only if message type is IMAGE and file URL exists
     * Used for conditional rendering of image display UI
     * 
     * @return true if message has image attachment, false otherwise
     */
    public boolean isImageMessage() {
        return type == MessageType.IMAGE && fileUrl != null;
    }

    /**
     * Checks if this message is a reply to another message
     * Returns true if replyToMessage reference is set
     * Used for threaded conversation display logic
     * 
     * @return true if message is a reply, false otherwise
     */
    public boolean isReply() {
        return replyToMessage != null;
    }

    /**
     * Gets appropriate content for display based on message state
     * Returns deletion notice for deleted messages
     * Returns actual content for normal messages
     * Provides safe content display handling
     * 
     * @return display-appropriate content string
     */
    public String getDisplayContent() {
        if (isDeleted) {
            return "This message has been deleted";
        }
        return content;
    }
}