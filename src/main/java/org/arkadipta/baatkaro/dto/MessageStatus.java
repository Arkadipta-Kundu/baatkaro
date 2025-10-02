package org.arkadipta.baatkaro.dto;

public enum MessageStatus {
    SENT,      // Message sent by sender
    DELIVERED, // Message received by server
    READ       // Message read by receiver (future feature)
}