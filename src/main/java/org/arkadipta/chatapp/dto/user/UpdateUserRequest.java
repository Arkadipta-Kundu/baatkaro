package org.arkadipta.chatapp.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update user profile request")
public class UpdateUserRequest {

    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    @Schema(description = "First name", example = "John")
    @JsonProperty("first_name")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    @Schema(description = "Last name", example = "Doe")
    @JsonProperty("last_name")
    private String lastName;

    @Schema(description = "Profile picture URL")
    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;
}