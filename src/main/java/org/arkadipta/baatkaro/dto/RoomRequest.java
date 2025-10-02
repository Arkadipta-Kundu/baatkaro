package org.arkadipta.baatkaro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotBlank(message = "Room name is required")
    @Size(min = 1, max = 100, message = "Room name must be between 1 and 100 characters")
    private String name;

    private Boolean isPrivate = false;

    @Override
    public String toString() {
        return "RoomRequest{" +
                "name='" + name + '\'' +
                ", isPrivate=" + isPrivate +
                '}';
    }
}