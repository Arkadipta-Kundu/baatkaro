package org.arkadipta.baatkaro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessage {

    @NotBlank(message = "Sender cannot be blank")
    private String sender; // Who sent the message

    @NotBlank(message = "Receiver cannot be blank")
    private String receiver; // Who should receive it

    @NotBlank(message = "Content cannot be blank")
    @Size(max = 1000, message = "Message too long")
    private String content; // Message text

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    private String messageId = UUID.randomUUID().toString();
}