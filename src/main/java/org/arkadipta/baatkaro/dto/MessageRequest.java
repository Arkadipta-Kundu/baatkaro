package org.arkadipta.baatkaro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;

    private String receiver; // For private messages
    private UUID roomId; // For room messages

    public MessageRequest(String content) {
        this.content = content;
    }

    public MessageRequest(String content, String receiver) {
        this.content = content;
        this.receiver = receiver;
    }

    public MessageRequest(String content, UUID roomId) {
        this.content = content;
        this.roomId = roomId;
    }

    // Utility methods
    public boolean isPrivateMessage() {
        return receiver != null && !receiver.trim().isEmpty();
    }

    public boolean isRoomMessage() {
        return roomId != null;
    }

    @Override
    public String toString() {
        return "MessageRequest{" +
                "content='" + content + '\'' +
                ", receiver='" + receiver + '\'' +
                ", roomId=" + roomId +
                '}';
    }
}