package org.arkadipta.chatapp.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for message response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Message information")
public class MessageResponse {

    @Schema(description = "Message ID", example = "1")
    private Long id;

    @Schema(description = "Message content", example = "Hello, how are you?")
    private String content;

    @Schema(description = "Message type", example = "TEXT")
    private String type;

    @Schema(description = "Sender information")
    private SenderInfo sender;

    @Schema(description = "Chat room ID", example = "1")
    @JsonProperty("chat_room_id")
    private Long chatRoomId;

    @Schema(description = "Message timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "Whether message was edited", example = "false")
    @JsonProperty("is_edited")
    private Boolean isEdited;

    @Schema(description = "When message was edited")
    @JsonProperty("edited_at")
    private LocalDateTime editedAt;

    @Schema(description = "File URL for file/image messages")
    @JsonProperty("file_url")
    private String fileUrl;

    @Schema(description = "File name for file/image messages")
    @JsonProperty("file_name")
    private String fileName;

    @Schema(description = "File size in bytes for file/image messages")
    @JsonProperty("file_size")
    private Long fileSize;

    @Schema(description = "File MIME type for file/image messages")
    @JsonProperty("file_type")
    private String fileType;

    @Schema(description = "Reply to message information")
    @JsonProperty("reply_to")
    private ReplyToMessage replyTo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Sender information")
    public static class SenderInfo {

        @Schema(description = "User ID", example = "1")
        private Long id;

        @Schema(description = "Username", example = "john_doe")
        private String username;

        @Schema(description = "Full name", example = "John Doe")
        @JsonProperty("full_name")
        private String fullName;

        @Schema(description = "Profile picture URL")
        @JsonProperty("profile_picture_url")
        private String profilePictureUrl;

        @Schema(description = "Online status", example = "true")
        @JsonProperty("is_online")
        private Boolean isOnline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Reply to message information")
    public static class ReplyToMessage {

        @Schema(description = "Original message ID", example = "123")
        private Long id;

        @Schema(description = "Original message content", example = "Original message content")
        private String content;

        @Schema(description = "Original message sender")
        private SenderInfo sender;

        @Schema(description = "Original message timestamp")
        private LocalDateTime timestamp;
    }
}