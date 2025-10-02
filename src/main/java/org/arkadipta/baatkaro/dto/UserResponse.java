package org.arkadipta.baatkaro.dto;

import org.arkadipta.baatkaro.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private Boolean online;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.online = user.getOnline();
        this.lastSeen = user.getLastSeen();
        this.createdAt = user.getCreatedAt();
    }

    // Static factory method
    public static UserResponse fromUser(User user) {
        return new UserResponse(user);
    }

    @Override
    public String toString() {
        return "UserResponse{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", online=" + online +
                ", lastSeen=" + lastSeen +
                '}';
    }
}