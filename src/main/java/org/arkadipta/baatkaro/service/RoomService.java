package org.arkadipta.baatkaro.service;

import org.arkadipta.baatkaro.dto.RoomRequest;
import org.arkadipta.baatkaro.dto.RoomResponse;
import org.arkadipta.baatkaro.dto.UserResponse;
import org.arkadipta.baatkaro.entity.Room;
import org.arkadipta.baatkaro.entity.User;
import org.arkadipta.baatkaro.repository.RoomRepository;
import org.arkadipta.baatkaro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new room
     */
    public RoomResponse createRoom(RoomRequest request, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found: " + creatorUsername));

        Room room = new Room(request.getName(), creator, request.getIsPrivate());
        Room savedRoom = roomRepository.save(room);

        return RoomResponse.fromRoomWithParticipants(savedRoom);
    }

    /**
     * Join an existing room
     */
    public RoomResponse joinRoom(UUID roomId, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Check if user is already a participant
        if (room.getParticipants().contains(user)) {
            throw new IllegalArgumentException("User is already a participant in this room");
        }

        room.addParticipant(user);
        Room savedRoom = roomRepository.save(room);

        return RoomResponse.fromRoomWithParticipants(savedRoom);
    }

    /**
     * Leave a room
     */
    public RoomResponse leaveRoom(UUID roomId, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Check if user is a participant
        if (!room.getParticipants().contains(user)) {
            throw new IllegalArgumentException("User is not a participant in this room");
        }

        room.removeParticipant(user);
        Room savedRoom = roomRepository.save(room);

        return RoomResponse.fromRoomWithParticipants(savedRoom);
    }

    /**
     * Get all public rooms (private rooms are only visible to their creators)
     */
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(room -> !room.getIsPrivate()) // Only show public rooms
                .map(RoomResponse::fromRoom)
                .collect(Collectors.toList());
    }

    /**
     * Get all rooms visible to a specific user (public + private rooms they
     * created)
     */
    public List<RoomResponse> getAllRoomsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return roomRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(room -> !room.getIsPrivate() || room.getCreatedBy().equals(user))
                .map(RoomResponse::fromRoom)
                .collect(Collectors.toList());
    }

    /**
     * Get rooms where user is a participant
     */
    public List<RoomResponse> getUserRooms(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return roomRepository.findRoomsByParticipantId(user.getId())
                .stream()
                .map(RoomResponse::fromRoom)
                .collect(Collectors.toList());
    }

    /**
     * Get room by ID
     */
    public Optional<RoomResponse> getRoomById(UUID roomId) {
        return roomRepository.findById(roomId)
                .map(RoomResponse::fromRoomWithParticipants);
    }

    /**
     * Get room participants
     */
    public List<UserResponse> getRoomParticipants(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        return room.getParticipants()
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * Search rooms by name
     */
    public List<RoomResponse> searchRooms(String searchTerm) {
        return roomRepository.findByNameContaining(searchTerm)
                .stream()
                .map(RoomResponse::fromRoom)
                .collect(Collectors.toList());
    }

    /**
     * Check if user is participant in room
     */
    public boolean isParticipant(UUID roomId, String username) {
        User user = userRepository.findByUsername(username)
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