package org.arkadipta.baatkaro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private MessageType type;
    private String content;
    private String sender;
    private String receiver; // For private messages
    private UUID roomId; // For room messages
    private String roomName; // For display purposes

    // Static factory methods
    public static ChatMessage createPrivateMessage(String sender, String receiver, String content) {
        ChatMessage message = new ChatMessage();
        message.setType(MessageType.PRIVATE);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        return message;
    }

    public static ChatMessage createRoomMessage(String sender, UUID roomId, String roomName, String content) {
        ChatMessage message = new ChatMessage();
        message.setType(MessageType.ROOM);
        message.setSender(sender);
        message.setRoomId(roomId);
        message.setRoomName(roomName);
        message.setContent(content);
        return message;
    }

    public static ChatMessage createJoinMessage(String sender, UUID roomId, String roomName) {
        ChatMessage message = new ChatMessage();
        message.setType(MessageType.JOIN);
        message.setSender(sender);
        message.setRoomId(roomId);
        message.setRoomName(roomName);
        message.setContent(sender + " joined the room");
        return message;
    }

    public static ChatMessage createLeaveMessage(String sender, UUID roomId, String roomName) {
        ChatMessage message = new ChatMessage();
        message.setType(MessageType.LEAVE);
        message.setSender(sender);
        message.setRoomId(roomId);
        message.setRoomName(roomName);
        message.setContent(sender + " left the room");
        return message;
    }

    // Utility methods
    public boolean isPrivateMessage() {
        return type == MessageType.PRIVATE && receiver != null;
    }

    public boolean isRoomMessage() {
        return (type == MessageType.ROOM || type == MessageType.JOIN || type == MessageType.LEAVE)
                && roomId != null;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type=" + type +
                ", content='" + content + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", roomId=" + roomId +
                ", roomName='" + roomName + '\'' +
                '}';
    }
}
