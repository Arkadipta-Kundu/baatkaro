package org.arkadipta.baatkaro.dto;

public enum MessageType {
    CHAT, // Regular chat message (backward compatibility)
    JOIN, // User joined (backward compatibility)
    LEAVE, // User left (backward compatibility)
    PRIVATE, // Private message between two users
    ROOM // Message in a room/group
}
