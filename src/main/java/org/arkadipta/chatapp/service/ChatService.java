package org.arkadipta.chatapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arkadipta.chatapp.dto.chat.*;
import org.arkadipta.chatapp.model.*;
import org.arkadipta.chatapp.repository.ChatRoomRepository;
import org.arkadipta.chatapp.repository.MessageRepository;
import org.arkadipta.chatapp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for chat and messaging operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new chat room
     */
    @Transactional
    public ChatRoomResponse createChatRoom(CreateChatRoomRequest request) {
        User currentUser = userService.getCurrentUser();

        log.info("Creating {} chat room: {} by user: {}",
                request.getType(), request.getName(), currentUser.getUsername());

        // Validate chat room type
        ChatRoomType roomType;
        try {
            roomType = ChatRoomType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid chat room type: " + request.getType());
        }

        // For direct chats, ensure only 2 participants (including current user)
        if (roomType == ChatRoomType.DIRECT) {
            if (request.getParticipantIds().size() != 1) {
                throw new RuntimeException("Direct chat must have exactly one other participant");
            }

            Long otherUserId = request.getParticipantIds().get(0);

            // Check if direct chat already exists between these users
            var existingChat = chatRoomRepository.findDirectChatBetweenUsers(
                    currentUser.getId(), otherUserId);

            if (existingChat.isPresent()) {
                return mapToChatRoomResponse(existingChat.get(), currentUser);
            }
        }

        // Validate participant IDs
        List<Long> allParticipantIds = request.getParticipantIds();
        if (!allParticipantIds.contains(currentUser.getId())) {
            allParticipantIds.add(currentUser.getId());
        }

        List<User> participants = userRepository.findAllById(allParticipantIds);
        if (participants.size() != allParticipantIds.size()) {
            throw new RuntimeException("One or more participant IDs are invalid");
        }

        // Create chat room
        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomType == ChatRoomType.DIRECT ? generateDirectChatName(currentUser, participants)
                        : request.getName())
                .description(request.getDescription())
                .type(roomType)
                .createdBy(currentUser)
                .isActive(true)
                .roomImageUrl(request.getRoomImageUrl())
                .participants(new HashSet<>(participants))
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);

        // Send notification to participants
        notifyParticipantsAboutNewChatRoom(chatRoom, currentUser);

        log.info("Chat room created successfully with ID: {}", chatRoom.getId());

        return mapToChatRoomResponse(chatRoom, currentUser);
    }

    /**
     * Send a message to a chat room
     */
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        User sender = userService.getCurrentUser();

        log.info("Sending message to chat room: {} by user: {}",
                request.getChatRoomId(), sender.getUsername());

        // Get chat room and validate access
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatRoom.isParticipant(sender)) {
            throw new RuntimeException("User is not a participant in this chat room");
        }

        // Validate message type
        MessageType messageType;
        try {
            messageType = MessageType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid message type: " + request.getType());
        }

        // Handle reply to message
        Message replyToMessage = null;
        if (request.getReplyToMessageId() != null) {
            replyToMessage = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new RuntimeException("Reply to message not found"));

            if (!replyToMessage.getChatRoom().getId().equals(request.getChatRoomId())) {
                throw new RuntimeException("Reply to message must be in the same chat room");
            }
        }

        // Create message
        Message message = Message.builder()
                .content(request.getContent())
                .type(messageType)
                .sender(sender)
                .chatRoom(chatRoom)
                .timestamp(LocalDateTime.now())
                .replyToMessage(replyToMessage)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .fileType(request.getFileType())
                .build();

        message = messageRepository.save(message);

        // Update chat room's updated timestamp
        chatRoom.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // Convert to response DTO
        MessageResponse messageResponse = mapToMessageResponse(message);

        // Send real-time message to chat room participants
        sendRealTimeMessage(chatRoom, messageResponse);

        log.info("Message sent successfully with ID: {}", message.getId());

        return messageResponse;
    }

    /**
     * Get chat history for a chat room
     */
    public Page<MessageResponse> getChatHistory(Long chatRoomId, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        log.info("Fetching chat history for room: {} by user: {}", chatRoomId, currentUser.getUsername());

        // Validate chat room access
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatRoom.isParticipant(currentUser)) {
            throw new RuntimeException("User is not a participant in this chat room");
        }

        // Get messages with pagination
        Page<Message> messages = messageRepository.findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(
                chatRoomId, pageable);

        return messages.map(this::mapToMessageResponse);
    }

    /**
     * Get user's chat rooms
     */
    public List<ChatRoomResponse> getUserChatRooms() {
        User currentUser = userService.getCurrentUser();

        log.info("Fetching chat rooms for user: {}", currentUser.getUsername());

        List<ChatRoom> chatRooms = chatRoomRepository.findByParticipantId(currentUser.getId());

        return chatRooms.stream()
                .map(chatRoom -> mapToChatRoomResponse(chatRoom, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * Search messages in a chat room
     */
    public List<MessageResponse> searchMessages(Long chatRoomId, String searchTerm) {
        User currentUser = userService.getCurrentUser();

        log.info("Searching messages in room: {} with term: {}", chatRoomId, searchTerm);

        // Validate chat room access
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatRoom.isParticipant(currentUser)) {
            throw new RuntimeException("User is not a participant in this chat room");
        }

        List<Message> messages = messageRepository.searchMessagesInChatRoom(chatRoomId, searchTerm);

        return messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Add participant to group chat
     */
    @Transactional
    public ChatRoomResponse addParticipant(Long chatRoomId, Long userId) {
        User currentUser = userService.getCurrentUser();

        log.info("Adding user: {} to chat room: {} by: {}", userId, chatRoomId, currentUser.getUsername());

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new RuntimeException("Can only add participants to group chats");
        }

        if (!chatRoom.isParticipant(currentUser)) {
            throw new RuntimeException("User is not a participant in this chat room");
        }

        User newParticipant = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (chatRoom.isParticipant(newParticipant)) {
            throw new RuntimeException("User is already a participant");
        }

        chatRoom.addParticipant(newParticipant);
        chatRoom = chatRoomRepository.save(chatRoom);

        // Send system message
        sendSystemMessage(chatRoom,
                currentUser.getFullName() + " added " + newParticipant.getFullName() + " to the group");

        return mapToChatRoomResponse(chatRoom, currentUser);
    }

    /**
     * Send real-time message to chat room participants
     */
    private void sendRealTimeMessage(ChatRoom chatRoom, MessageResponse messageResponse) {
        String destination = "/topic/chatroom/" + chatRoom.getId();
        messagingTemplate.convertAndSend(destination, messageResponse);

        log.debug("Real-time message sent to destination: {}", destination);
    }

    /**
     * Send system message
     */
    private void sendSystemMessage(ChatRoom chatRoom, String content) {
        Message systemMessage = Message.builder()
                .content(content)
                .type(MessageType.SYSTEM)
                .sender(chatRoom.getCreatedBy()) // Use room creator as sender for system messages
                .chatRoom(chatRoom)
                .timestamp(LocalDateTime.now())
                .build();

        systemMessage = messageRepository.save(systemMessage);

        MessageResponse messageResponse = mapToMessageResponse(systemMessage);
        sendRealTimeMessage(chatRoom, messageResponse);
    }

    /**
     * Notify participants about new chat room
     */
    private void notifyParticipantsAboutNewChatRoom(ChatRoom chatRoom, User creator) {
        ChatRoomResponse chatRoomResponse = mapToChatRoomResponse(chatRoom, creator);

        for (User participant : chatRoom.getParticipants()) {
            if (!participant.getId().equals(creator.getId())) {
                String destination = "/user/" + participant.getUsername() + "/queue/chatroom";
                messagingTemplate.convertAndSend(destination, chatRoomResponse);
            }
        }
    }

    /**
     * Generate name for direct chat
     */
    private String generateDirectChatName(User currentUser, List<User> participants) {
        User otherUser = participants.stream()
                .filter(p -> !p.getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(currentUser);

        return currentUser.getFullName() + " & " + otherUser.getFullName();
    }

    /**
     * Map ChatRoom to ChatRoomResponse
     */
    private ChatRoomResponse mapToChatRoomResponse(ChatRoom chatRoom, User currentUser) {
        // Get last message
        List<Message> lastMessages = messageRepository.findLatestMessagesByChatRoom(
                chatRoom.getId(), PageRequest.of(0, 1));

        MessageResponse lastMessage = lastMessages.isEmpty() ? null : mapToMessageResponse(lastMessages.get(0));

        // Calculate unread count (simplified - in real app, you'd track user's last
        // read timestamp)
        Long unreadCount = 0L;

        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .description(chatRoom.getDescription())
                .type(chatRoom.getType().name())
                .createdBy(mapToParticipantInfo(chatRoom.getCreatedBy()))
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .isActive(chatRoom.getIsActive())
                .roomImageUrl(chatRoom.getRoomImageUrl())
                .participants(chatRoom.getParticipants().stream()
                        .map(this::mapToParticipantInfo)
                        .collect(Collectors.toList()))
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * Map Message to MessageResponse
     */
    private MessageResponse mapToMessageResponse(Message message) {
        MessageResponse.ReplyToMessage replyTo = null;
        if (message.getReplyToMessage() != null) {
            Message replyMsg = message.getReplyToMessage();
            replyTo = MessageResponse.ReplyToMessage.builder()
                    .id(replyMsg.getId())
                    .content(replyMsg.getDisplayContent())
                    .sender(mapToSenderInfo(replyMsg.getSender()))
                    .timestamp(replyMsg.getTimestamp())
                    .build();
        }

        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getDisplayContent())
                .type(message.getType().name())
                .sender(mapToSenderInfo(message.getSender()))
                .chatRoomId(message.getChatRoom().getId())
                .timestamp(message.getTimestamp())
                .isEdited(message.getIsEdited())
                .editedAt(message.getEditedAt())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .fileType(message.getFileType())
                .replyTo(replyTo)
                .build();
    }

    /**
     * Map User to SenderInfo
     */
    private MessageResponse.SenderInfo mapToSenderInfo(User user) {
        return MessageResponse.SenderInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isOnline(user.getIsOnline())
                .build();
    }

    /**
     * Map User to ParticipantInfo
     */
    private ChatRoomResponse.ParticipantInfo mapToParticipantInfo(User user) {
        return ChatRoomResponse.ParticipantInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isOnline(user.getIsOnline())
                .lastSeen(user.getLastSeen())
                .build();
    }
}