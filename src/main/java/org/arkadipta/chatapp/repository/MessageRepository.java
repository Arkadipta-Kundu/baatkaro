package org.arkadipta.chatapp.repository;

import org.arkadipta.chatapp.model.Message;
import org.arkadipta.chatapp.model.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Message entity
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find messages by chat room with pagination
     */
    Page<Message> findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(Long chatRoomId, Pageable pageable);

    /**
     * Find messages by chat room (non-deleted)
     */
    List<Message> findByChatRoomIdAndIsDeletedFalseOrderByTimestampAsc(Long chatRoomId);

    /**
     * Find latest messages by chat room
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false ORDER BY m.timestamp DESC")
    List<Message> findLatestMessagesByChatRoom(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    /**
     * Find messages by sender
     */
    List<Message> findBySenderIdAndIsDeletedFalseOrderByTimestampDesc(Long senderId);

    /**
     * Find messages by type
     */
    List<Message> findByTypeAndIsDeletedFalseOrderByTimestampDesc(MessageType type);

    /**
     * Find messages in chat room by content containing (search)
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY m.timestamp DESC")
    List<Message> searchMessagesInChatRoom(@Param("chatRoomId") Long chatRoomId, @Param("searchTerm") String searchTerm);

    /**
     * Find messages in chat room after a specific timestamp
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false AND m.timestamp > :timestamp ORDER BY m.timestamp ASC")
    List<Message> findMessagesAfterTimestamp(@Param("chatRoomId") Long chatRoomId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find messages in chat room between timestamps
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false AND m.timestamp BETWEEN :startTime AND :endTime ORDER BY m.timestamp ASC")
    List<Message> findMessagesBetweenTimestamps(@Param("chatRoomId") Long chatRoomId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Count messages in chat room
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false")
    Long countMessagesByChatRoom(@Param("chatRoomId") Long chatRoomId);

    /**
     * Count unread messages for user in chat room (after their last seen timestamp)
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false AND m.sender.id != :userId AND m.timestamp > :lastSeenTimestamp")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId, @Param("lastSeenTimestamp") LocalDateTime lastSeenTimestamp);

    /**
     * Find latest message in each chat room for a user
     */
    @Query("SELECT m FROM Message m WHERE m.id IN (SELECT MAX(m2.id) FROM Message m2 WHERE m2.chatRoom.id IN (SELECT cr.id FROM ChatRoom cr JOIN cr.participants p WHERE p.id = :userId) AND m2.isDeleted = false GROUP BY m2.chatRoom.id)")
    List<Message> findLatestMessagesForUserChatRooms(@Param("userId") Long userId);

    /**
     * Find file messages in chat room
     */
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false AND m.type IN ('FILE', 'IMAGE') ORDER BY m.timestamp DESC")
    List<Message> findFileMessagesByChatRoom(@Param("chatRoomId") Long chatRoomId);

    /**
     * Find messages that are replies to a specific message
     */
    List<Message> findByReplyToMessageIdAndIsDeletedFalseOrderByTimestampAsc(Long replyToMessageId);
}