package org.arkadipta.chatapp.repository;

import org.arkadipta.chatapp.model.ChatRoom;
import org.arkadipta.chatapp.model.ChatRoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ChatRoom entity
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * Find chat rooms by type
     */
    List<ChatRoom> findByType(ChatRoomType type);

    /**
     * Find active chat rooms by type
     */
    List<ChatRoom> findByTypeAndIsActiveTrue(ChatRoomType type);

    /**
     * Find chat rooms where user is a participant
     */
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p WHERE p.id = :userId AND cr.isActive = true")
    List<ChatRoom> findByParticipantId(@Param("userId") Long userId);

    /**
     * Find chat rooms where user is a participant with pagination
     */
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p WHERE p.id = :userId AND cr.isActive = true")
    Page<ChatRoom> findByParticipantId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find direct chat between two users
     */
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p1 JOIN cr.participants p2 " +
            "WHERE cr.type = 'DIRECT' AND p1.id = :userId1 AND p2.id = :userId2 AND cr.isActive = true")
    Optional<ChatRoom> findDirectChatBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Find group chats created by user
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.createdBy.id = :userId AND cr.type = 'GROUP' AND cr.isActive = true")
    List<ChatRoom> findGroupChatsByCreator(@Param("userId") Long userId);

    /**
     * Find chat rooms by name containing (case-insensitive)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE LOWER(cr.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND cr.isActive = true")
    List<ChatRoom> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Find public group chats (excluding direct chats)
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = 'GROUP' AND cr.isActive = true")
    List<ChatRoom> findAllGroupChats();

    /**
     * Find chat rooms where user is participant and matches search term
     */
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p " +
            "WHERE p.id = :userId AND cr.isActive = true AND " +
            "LOWER(cr.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ChatRoom> searchUserChatRooms(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);

    /**
     * Count participants in a chat room
     */
    @Query("SELECT COUNT(p) FROM ChatRoom cr JOIN cr.participants p WHERE cr.id = :chatRoomId")
    Long countParticipants(@Param("chatRoomId") Long chatRoomId);

    /**
     * Check if user is participant in chat room
     */
    @Query("SELECT COUNT(p) > 0 FROM ChatRoom cr JOIN cr.participants p WHERE cr.id = :chatRoomId AND p.id = :userId")
    boolean isUserParticipant(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}