// Chat page functionality with WebSocket
let stompClient = null;
let currentChatType = null; // 'private' or 'room'
let currentTarget = null; // username or roomId
let currentRoomName = null;

document.addEventListener('DOMContentLoaded', async function () {
    // Check authentication
    const auth = getAuth();
    if (!auth) {
        window.location.href = '/pages/login';
        return;
    }

    // Get chat parameters
    currentChatType = localStorage.getItem('chat_type');
    currentTarget = localStorage.getItem('chat_target');
    currentRoomName = localStorage.getItem('chat_name');

    if (!currentChatType || !currentTarget) {
        alert('Invalid chat session');
        window.location.href = '/pages/dashboard';
        return;
    }

    // Initialize page
    initializeChatPage();

    // Load message history
    await loadMessageHistory();

    // Connect WebSocket
    connectWebSocket();

    // Set up event listeners
    setupEventListeners();
});

function getAuth() {
    const authData = localStorage.getItem('baatkaro_auth');
    if (authData) {
        return JSON.parse(authData);
    }
    return null;
}

function getAuthHeaders() {
    const auth = getAuth();
    return {
        'Content-Type': 'application/json',
        'Authorization': 'Basic ' + auth.basicAuth
    };
}

function initializeChatPage() {
    const auth = getAuth();

    // Set chat title
    if (currentChatType === 'private') {
        document.getElementById('chatTitle').textContent = currentTarget;
        document.getElementById('chatSubtitle').textContent = 'Private Chat';
        document.getElementById('leaveBtn').style.display = 'none';
    } else {
        document.getElementById('chatTitle').textContent = currentRoomName || 'Room Chat';
        document.getElementById('chatSubtitle').textContent = 'Room Chat';
        document.getElementById('leaveBtn').style.display = 'inline-block';
    }
}

function setupEventListeners() {
    // Message form
    document.getElementById('messageForm').addEventListener('submit', sendMessage);

    // Info button
    document.getElementById('showInfoBtn').addEventListener('click', showChatInfo);

    // Leave button (for rooms)
    const leaveBtn = document.getElementById('leaveBtn');
    if (leaveBtn) {
        leaveBtn.addEventListener('click', leaveRoom);
    }

    // Modal close
    document.querySelectorAll('.modal .close').forEach(btn => {
        btn.addEventListener('click', closeModals);
    });

    window.addEventListener('click', function (event) {
        if (event.target.classList.contains('modal')) {
            closeModals();
        }
    });
}

async function loadMessageHistory() {
    const messagesLoading = document.getElementById('messagesLoading');
    const messagesError = document.getElementById('messagesError');
    const messagesList = document.getElementById('messagesList');

    messagesLoading.style.display = 'block';
    messagesError.style.display = 'none';
    messagesList.innerHTML = '';

    try {
        let endpoint;
        if (currentChatType === 'private') {
            endpoint = `/api/messages/private/${encodeURIComponent(currentTarget)}?limit=50`;
        } else {
            endpoint = `/api/messages/room/${currentTarget}?limit=50`;
        }

        const response = await fetch(endpoint, {
            headers: getAuthHeaders()
        });

        if (!response.ok) throw new Error('Failed to load messages');

        const messages = await response.json();

        messagesLoading.style.display = 'none';

        if (messages.length === 0) {
            messagesList.innerHTML = '<div class="text-center" style="padding: 40px; color: #999;">No messages yet. Start the conversation!</div>';
        } else {
            messages.forEach(msg => displayMessage(msg, false));
            scrollToBottom();
        }
    } catch (error) {
        console.error('Error loading messages:', error);
        messagesLoading.style.display = 'none';
        messagesError.textContent = 'Failed to load message history. Please try again.';
        messagesError.style.display = 'block';
    }
}

function connectWebSocket() {
    const auth = getAuth();
    updateConnectionStatus('connecting');

    // Connect to WebSocket with username parameter
    const socket = new SockJS(`/ws?username=${encodeURIComponent(auth.username)}`);
    stompClient = Stomp.over(socket);

    // Disable debug output
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log('WebSocket Connected:', frame);
        updateConnectionStatus('connected');

        // Subscribe based on chat type
        if (currentChatType === 'private') {
            // Subscribe to private messages
            stompClient.subscribe('/user/queue/messages', function (message) {
                const chatMessage = JSON.parse(message.body);
                // Only show if from current chat partner (not our own messages)
                if (chatMessage.sender === currentTarget) {
                    displayMessage(chatMessage, true);
                }
            });
        } else {
            // Subscribe to room messages
            stompClient.subscribe(`/topic/${currentTarget}`, function (message) {
                const chatMessage = JSON.parse(message.body);
                displayMessage(chatMessage, true);
            });

            // Send join notification
            sendJoinNotification();
        }
    }, function (error) {
        console.error('WebSocket Error:', error);
        updateConnectionStatus('disconnected');

        // Try to reconnect after 5 seconds
        setTimeout(connectWebSocket, 5000);
    });
}

function sendJoinNotification() {
    if (stompClient && stompClient.connected && currentChatType === 'room') {
        const auth = getAuth();
        stompClient.send('/app/chat.addUser', {}, JSON.stringify({
            sender: auth.username,
            type: 'JOIN',
            roomId: currentTarget
        }));
    }
}

function sendMessage(e) {
    e.preventDefault();

    const messageInput = document.getElementById('messageInput');
    const content = messageInput.value.trim();

    if (!content || !stompClient || !stompClient.connected) {
        return;
    }

    const auth = getAuth();

    const chatMessage = {
        sender: auth.username,
        content: content,
        type: 'CHAT',
        sentAt: new Date().toISOString()
    };

    if (currentChatType === 'private') {
        chatMessage.receiver = currentTarget;

        // Send via WebSocket
        stompClient.send('/app/chat.private', {}, JSON.stringify(chatMessage));

        // Immediately display your own message locally
        displayMessage(chatMessage, true);
    } else {
        chatMessage.roomId = currentTarget;
        stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(chatMessage));

        // Room messages will come back through the subscription automatically
    }

    // Clear input
    messageInput.value = '';
    messageInput.focus();
}

function displayMessage(message, animate = false) {
    const messagesList = document.getElementById('messagesList');
    const auth = getAuth();

    const messageDiv = document.createElement('div');
    const isSent = message.sender === auth.username;

    messageDiv.className = `message ${isSent ? 'sent' : 'received'}`;

    const timeStr = formatTime(message.sentAt || message.timestamp || new Date());

    if (message.type === 'JOIN' || message.type === 'LEAVE') {
        messageDiv.className = 'message system';
        messageDiv.innerHTML = `
            <div class="system-message">
                ${escapeHtml(message.sender)} ${message.type === 'JOIN' ? 'joined' : 'left'} the room
                <span class="message-time">${timeStr}</span>
            </div>
        `;
    } else {
        messageDiv.innerHTML = `
            ${!isSent ? `<div class="message-header">
                <span class="message-sender">${escapeHtml(message.sender)}</span>
                <span class="message-time">${timeStr}</span>
            </div>` : ''}
            <div class="message-bubble">${escapeHtml(message.content)}</div>
            ${isSent ? `<div class="message-header" style="text-align: right;">
                <span class="message-time">${timeStr}</span>
            </div>` : ''}
        `;
    }

    messagesList.appendChild(messageDiv);

    if (animate) {
        scrollToBottom(true);
    }
}

function scrollToBottom(smooth = false) {
    const messagesContainer = document.getElementById('messagesContainer');
    messagesContainer.scrollTo({
        top: messagesContainer.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
    });
}

function updateConnectionStatus(status) {
    const statusIndicator = document.getElementById('statusIndicator');
    const statusText = document.getElementById('statusText');

    statusIndicator.className = `status-indicator ${status}`;

    switch (status) {
        case 'connecting':
            statusText.textContent = 'Connecting...';
            break;
        case 'connected':
            statusText.textContent = 'Connected';
            break;
        case 'disconnected':
            statusText.textContent = 'Disconnected';
            break;
    }
}

async function showChatInfo() {
    const modal = document.getElementById('infoModal');
    const infoContent = document.getElementById('infoContent');

    if (currentChatType === 'private') {
        infoContent.innerHTML = `
            <div class="info-item">
                <label>Chat Type</label>
                <value>Private Chat</value>
            </div>
            <div class="info-item">
                <label>Chatting With</label>
                <value>${escapeHtml(currentTarget)}</value>
            </div>
        `;
    } else {
        try {
            const response = await fetch(`/api/rooms/${currentTarget}`, {
                headers: getAuthHeaders()
            });

            if (response.ok) {
                const room = await response.json();
                infoContent.innerHTML = `
                    <div class="info-item">
                        <label>Room Name</label>
                        <value>${escapeHtml(room.name)}</value>
                    </div>
                    <div class="info-item">
                        <label>Room ID</label>
                        <value>
                            ${room.id}
                            <button class="copy-id-btn" onclick="copyToClipboard('${room.id}')">Copy</button>
                        </value>
                    </div>
                    <div class="info-item">
                        <label>Type</label>
                        <value>${room.isPrivate ? '🔒 Private' : '🌐 Public'}</value>
                    </div>
                    <div class="info-item">
                        <label>Created By</label>
                        <value>${escapeHtml(room.createdBy)}</value>
                    </div>
                    <div class="info-item">
                        <label>Participants (${room.participantCount || 0})</label>
                        <div class="info-participants">
                            ${(room.participants || []).map(p =>
                    `<span class="participant-chip">${escapeHtml(p)}</span>`
                ).join('')}
                        </div>
                    </div>
                `;
            } else {
                infoContent.innerHTML = '<p>Failed to load room information.</p>';
            }
        } catch (error) {
            console.error('Error loading room info:', error);
            infoContent.innerHTML = '<p>Failed to load room information.</p>';
        }
    }

    modal.classList.add('show');
}

async function leaveRoom() {
    if (!confirm('Are you sure you want to leave this room?')) {
        return;
    }

    try {
        // Send leave notification via WebSocket
        if (stompClient && stompClient.connected) {
            const auth = getAuth();
            stompClient.send('/app/chat.removeUser', {}, JSON.stringify({
                sender: auth.username,
                type: 'LEAVE',
                roomId: currentTarget
            }));

            // Disconnect WebSocket
            stompClient.disconnect();
        }

        // Leave room via API
        const response = await fetch(`/api/rooms/${currentTarget}/leave`, {
            method: 'POST',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to leave room');
        }

        // Redirect to dashboard
        window.location.href = '/pages/dashboard';
    } catch (error) {
        console.error('Error leaving room:', error);
        alert('Failed to leave room: ' + error.message);
    }
}

function closeModals() {
    document.querySelectorAll('.modal').forEach(modal => {
        modal.classList.remove('show');
    });
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        alert('Copied to clipboard!');
    }).catch(err => {
        console.error('Failed to copy:', err);
        prompt('Copy this:', text);
    });
}

function formatTime(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Disconnect WebSocket on page unload
window.addEventListener('beforeunload', function () {
    if (stompClient && stompClient.connected) {
        if (currentChatType === 'room') {
            const auth = getAuth();
            stompClient.send('/app/chat.removeUser', {}, JSON.stringify({
                sender: auth.username,
                type: 'LEAVE',
                roomId: currentTarget
            }));
        }
        stompClient.disconnect();
    }
});
