package org.arkadipta.baatkaro.repository;

import org.arkadipta.baatkaro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Find user by username
    Optional<User> findByUsername(String username);

    // Check if username exists
    boolean existsByUsername(String username);

    // Find users by list of usernames (for online status)
    List<User> findByUsernameIn(List<String> usernames);

    // Custom query to find users with username containing search term
    @Query("SELECT u FROM User u WHERE u.username LIKE %:searchTerm%")
    List<User> findByUsernameContaining(String searchTerm);

    // Find users who are participants in a specific room
    @Query("SELECT u FROM User u JOIN u.rooms r WHERE r.id = :roomId")
    List<User> findParticipantsByRoomId(UUID roomId);
}