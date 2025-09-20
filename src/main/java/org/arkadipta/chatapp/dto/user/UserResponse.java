package org.arkadipta.chatapp.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user information response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information")
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "First name", example = "John")
    @JsonProperty("first_name")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    @JsonProperty("last_name")
    private String lastName;

    @Schema(description = "Full name", example = "John Doe")
    @JsonProperty("full_name")
    private String fullName;

    @Schema(description = "User role", example = "USER")
    private String role;

    @Schema(description = "Profile picture URL")
    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;

    @Schema(description = "Online status", example = "true")
    @JsonProperty("is_online")
    private Boolean isOnline;

    @Schema(description = "Last seen timestamp")
    @JsonProperty("last_seen")
    private LocalDateTime lastSeen;

    @Schema(description = "Account creation timestamp")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "Account enabled status", example = "true")
    @JsonProperty("is_enabled")
    private Boolean isEnabled;
}