package org.arkadipta.baatkaro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)  // Enables .toBuilder() for copying
public class MessageResponse {

    private String messageId;     // Unique identifier
    private String sender;        // Who sent it
    private String content;       // Message text
    private MessageType type;     // Type of message

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private MessageStatus status; // SENT, DELIVERED, READ
}