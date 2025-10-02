package org.arkadipta.baatkaro.repository;

import org.arkadipta.baatkaro.dto.MessageType;
import org.arkadipta.baatkaro.entity.Message;
import org.arkadipta.baatkaro.entity.Room;
import org.arkadipta.baatkaro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Find messages in a specific room, ordered by timestamp
    List<Message> findByRoomOrderByTimestampAsc(Room room);

    // Find private messages between two users
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
            "(m.sender.id = :user2Id AND m.receiver.id = :user1Id) " +
            "ORDER BY m.timestamp ASC")
    List<Message> findPrivateMessagesBetweenUsers(UUID user1Id, UUID user2Id);

    // Find all messages sent by a user
    List<Message> findBySenderOrderByTimestampDesc(User sender);

    // Find messages by type
    List<Message> findByMessageTypeOrderByTimestampDesc(MessageType messageType);

    // Find recent messages in a room (limit to last N messages)
    @Query("SELECT m FROM Message m WHERE m.room.id = :roomId " +
            "ORDER BY m.timestamp DESC LIMIT :limit")
    List<Message> findRecentRoomMessages(UUID roomId, int limit);

    // Find recent private messages between users (limit to last N messages)
    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
            "(m.sender.id = :user2Id AND m.receiver.id = :user1Id)) " +
            "ORDER BY m.timestamp DESC LIMIT :limit")
    List<Message> findRecentPrivateMessages(UUID user1Id, UUID user2Id, int limit);
}