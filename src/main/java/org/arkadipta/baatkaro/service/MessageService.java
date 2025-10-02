package org.arkadipta.baatkaro.service;

import org.arkadipta.baatkaro.dto.MessageHistoryResponse;
import org.arkadipta.baatkaro.entity.Message;
import org.arkadipta.baatkaro.entity.Room;
import org.arkadipta.baatkaro.entity.User;
import org.arkadipta.baatkaro.repository.MessageRepository;
import org.arkadipta.baatkaro.repository.RoomRepository;
import org.arkadipta.baatkaro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Save a private message
     */
    public Message savePrivateMessage(String senderUsername, String receiverUsername, String content) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + senderUsername));

        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found: " + receiverUsername));

        Message message = new Message(sender, receiver, content);
        return messageRepository.save(message);
    }

    /**
     * Save a room message
     */
    public Message saveRoomMessage(String senderUsername, UUID roomId, String content) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + senderUsername));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        // Verify sender is a participant in the room
        if (!room.getParticipants().contains(sender)) {
            throw new IllegalArgumentException("User is not a participant in this room");
        }

        Message message = new Message(sender, room, content);
        return messageRepository.save(message);
    }

    /**
     * Get private message history between two users
     */
    public List<MessageHistoryResponse> getPrivateMessageHistory(String username1, String username2) {
        User user1 = userRepository.findByUsername(username1)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username1));

        User user2 = userRepository.findByUsername(username2)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username2));

        return messageRepository.findPrivateMessagesBetweenUsers(user1.getId(), user2.getId())
                .stream()
                .map(MessageHistoryResponse::fromMessage)
                .collect(Collectors.toList());
    }

    /**
     * Get room message history
     */
    public List<MessageHistoryResponse> getRoomMessageHistory(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        return messageRepository.findByRoomOrderByTimestampAsc(room)
                .stream()
                .map(MessageHistoryResponse::fromMessage)
                .collect(Collectors.toList());
    }

    /**
     * Get recent private messages between users (limited)
     */
    public List<MessageHistoryResponse> getRecentPrivateMessages(String username1, String username2, int limit) {
        User user1 = userRepository.findByUsername(username1)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username1));

        User user2 = userRepository.findByUsername(username2)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username2));

        List<Message> messages = messageRepository.findRecentPrivateMessages(user1.getId(), user2.getId(), limit);

        // Reverse the list to get chronological order (oldest first)
        return messages.stream()
                .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .map(MessageHistoryResponse::fromMessage)
                .collect(Collectors.toList());
    }

    /**
     * Get recent room messages (limited)
     */
    public List<MessageHistoryResponse> getRecentRoomMessages(UUID roomId, int limit) {
        List<Message> messages = messageRepository.findRecentRoomMessages(roomId, limit);

        // Reverse the list to get chronological order (oldest first)
        return messages.stream()
                .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .map(MessageHistoryResponse::fromMessage)
                .collect(Collectors.toList());
    }

    /**
     * Get all messages sent by a user
     */
    public List<MessageHistoryResponse> getUserMessages(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return messageRepository.findBySenderOrderByTimestampDesc(user)
                .stream()
                .map(MessageHistoryResponse::fromMessage)
                .collect(Collectors.toList());
    }

    /**
     * Validate if user can access message history
     */
    public boolean canAccessPrivateHistory(String requestingUser, String user1, String user2) {
        return requestingUser.equals(user1) || requestingUser.equals(user2);
    }

    /**
     * Validate if user can access room history
     */
    public boolean canAccessRoomHistory(String requestingUser, UUID roomId) {
        User user = userRepository.findByUsername(requestingUser)
                .orElse(null);

        if (user == null) {
            return false;
        }

        Room room = roomRepository.findById(roomId)
                .orElse(null);

        if (room == null) {
            return false;
        }

        return room.getParticipants().contains(user);
    }
}