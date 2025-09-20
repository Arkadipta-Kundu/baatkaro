package org.arkadipta.chatapp.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating a chat room
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create chat room request")
public class CreateChatRoomRequest {

    @NotBlank(message = "Chat room name is required")
    @Size(max = 100, message = "Chat room name must not exceed 100 characters")
    @Schema(description = "Chat room name", example = "Team Discussion", required = true)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Chat room description", example = "A room for team discussions and updates")
    private String description;

    @Schema(description = "Chat room type", example = "GROUP", allowableValues = { "DIRECT", "GROUP" }, required = true)
    private String type;

    @NotEmpty(message = "At least one participant is required")
    @Schema(description = "List of participant user IDs", example = "[2, 3, 4]", required = true)
    @JsonProperty("participant_ids")
    private List<Long> participantIds;

    @Schema(description = "Chat room image URL")
    @JsonProperty("room_image_url")
    private String roomImageUrl;
}