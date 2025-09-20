package org.arkadipta.chatapp.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for chat room response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat room information")
public class ChatRoomResponse {

    @Schema(description = "Chat room ID", example = "1")
    private Long id;

    @Schema(description = "Chat room name", example = "Team Discussion")
    private String name;

    @Schema(description = "Chat room description", example = "A room for team discussions and updates")
    private String description;

    @Schema(description = "Chat room type", example = "GROUP")
    private String type;

    @Schema(description = "Creator information")
    @JsonProperty("created_by")
    private ParticipantInfo createdBy;

    @Schema(description = "Creation timestamp")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @Schema(description = "Whether chat room is active", example = "true")
    @JsonProperty("is_active")
    private Boolean isActive;

    @Schema(description = "Chat room image URL")
    @JsonProperty("room_image_url")
    private String roomImageUrl;

    @Schema(description = "List of participants")
    private List<ParticipantInfo> participants;

    @Schema(description = "Last message in the chat room")
    @JsonProperty("last_message")
    private MessageResponse lastMessage;

    @Schema(description = "Unread message count for current user")
    @JsonProperty("unread_count")
    private Long unreadCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Participant information")
    public static class ParticipantInfo {

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

        @Schema(description = "Last seen timestamp")
        @JsonProperty("last_seen")
        private LocalDateTime lastSeen;
    }
}