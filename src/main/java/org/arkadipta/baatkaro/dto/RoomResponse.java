package org.arkadipta.baatkaro.dto;

import org.arkadipta.baatkaro.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private UUID id;
    private String name;
    private String createdBy;
    private Boolean isPrivate;
    private LocalDateTime createdAt;
    private int participantCount;
    private List<UserResponse> participants;

    public RoomResponse(Room room) {
        this.id = room.getId();
        this.name = room.getName();
        this.createdBy = room.getCreatedBy().getUsername();
        this.isPrivate = room.getIsPrivate();
        this.createdAt = room.getCreatedAt();
        this.participantCount = room.getParticipantCount();
        this.participants = room.getParticipants().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    public RoomResponse(Room room, boolean includeParticipants) {
        this.id = room.getId();
        this.name = room.getName();
        this.createdBy = room.getCreatedBy().getUsername();
        this.isPrivate = room.getIsPrivate();
        this.createdAt = room.getCreatedAt();
        this.participantCount = room.getParticipantCount();

        if (includeParticipants) {
            this.participants = room.getParticipants().stream()
                    .map(UserResponse::fromUser)
                    .collect(Collectors.toList());
        }
    }

    // Static factory methods
    public static RoomResponse fromRoom(Room room) {
        return new RoomResponse(room, false);
    }

    public static RoomResponse fromRoomWithParticipants(Room room) {
        return new RoomResponse(room, true);
    }

    @Override
    public String toString() {
        return "RoomResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", participantCount=" + participantCount +
                '}';
    }
}