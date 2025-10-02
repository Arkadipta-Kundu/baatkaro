package org.arkadipta.baatkaro.repository;

import org.arkadipta.baatkaro.entity.Room;
import org.arkadipta.baatkaro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    // Find rooms created by a specific user
    List<Room> findByCreatedBy(User createdBy);

    // Find rooms where user is a participant
    @Query("SELECT r FROM Room r JOIN r.participants p WHERE p.id = :userId")
    List<Room> findRoomsByParticipantId(UUID userId);

    // Find rooms by name containing search term
    @Query("SELECT r FROM Room r WHERE r.name LIKE %:searchTerm%")
    List<Room> findByNameContaining(String searchTerm);

    // Get all rooms ordered by creation date (newest first)
    List<Room> findAllByOrderByCreatedAtDesc();
}