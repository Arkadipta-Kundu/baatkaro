package org.arkadipta.baatkaro.dto;

import org.arkadipta.baatkaro.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryResponse {

    private UUID id;
    private String sender;
    private String receiver; // null for room messages
    private UUID roomId; // null for private messages
    private String roomName; // null for private messages
    private String content;
    private MessageType messageType;
    private LocalDateTime timestamp;

    public MessageHistoryResponse(Message message) {
        this.id = message.getId();
        this.sender = message.getSender().getUsername();
        this.receiver = message.getReceiver() != null ? message.getReceiver().getUsername() : null;
        this.roomId = message.getRoom() != null ? message.getRoom().getId() : null;
        this.roomName = message.getRoom() != null ? message.getRoom().getName() : null;
        this.content = message.getContent();
        this.messageType = message.getMessageType();
        this.timestamp = message.getTimestamp();
    }

    // Static factory method
    public static MessageHistoryResponse fromMessage(Message message) {
        return new MessageHistoryResponse(message);
    }

    @Override
    public String toString() {
        return "MessageHistoryResponse{" +
                "id=" + id +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", roomName='" + roomName + '\'' +
                ", content='" + content + '\'' +
                ", messageType=" + messageType +
                ", timestamp=" + timestamp +
                '}';
    }
}