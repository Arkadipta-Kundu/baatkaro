package org.arkadipta.chatapp.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending a message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Send message request")
public class SendMessageRequest {

    @NotNull(message = "Chat room ID is required")
    @Schema(description = "Chat room ID", example = "1", required = true)
    @JsonProperty("chat_room_id")
    private Long chatRoomId;

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Message content must not exceed 5000 characters")
    @Schema(description = "Message content", example = "Hello, how are you?", required = true)
    private String content;

    @Schema(description = "Message type", example = "TEXT", allowableValues = { "TEXT", "IMAGE", "FILE", "SYSTEM" })
    @Builder.Default
    private String type = "TEXT";

    @Schema(description = "Reply to message ID", example = "123")
    @JsonProperty("reply_to_message_id")
    private Long replyToMessageId;

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
}