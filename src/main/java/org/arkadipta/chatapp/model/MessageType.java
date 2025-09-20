package org.arkadipta.chatapp.model;

/**
 * Enum representing different types of messages
 */
public enum MessageType {
    TEXT, // Plain text message
    IMAGE, // Image attachment
    FILE, // File attachment
    SYSTEM // System-generated message (user joined/left, etc.)
}