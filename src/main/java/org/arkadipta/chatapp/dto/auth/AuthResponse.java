package org.arkadipta.chatapp.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response")
public class AuthResponse {

    @Schema(description = "Access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @JsonProperty("refresh_token")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Token expiration time in milliseconds", example = "86400000")
    @JsonProperty("expires_in")
    private Long expiresIn;

    @Schema(description = "User information")
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User information")
    public static class UserInfo {

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
    }
}