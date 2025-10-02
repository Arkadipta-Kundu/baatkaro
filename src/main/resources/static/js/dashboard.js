// Dashboard page functionality
document.addEventListener('DOMContentLoaded', async function () {
    // Check authentication
    const auth = getAuth();
    if (!auth) {
        window.location.href = '/pages/login';
        return;
    }

    // Initialize page
    initializePage();

    // Load data
    loadUsers();
    loadRooms();

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

async function initializePage() {
    const auth = getAuth();

    // Set current username
    document.getElementById('currentUsername').textContent = auth.username;

    // Set user online status
    try {
        await fetch('/api/users/status', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ online: true })
        });
    } catch (error) {
        console.error('Failed to set online status:', error);
    }
}

function setupEventListeners() {
    // Logout button
    document.getElementById('logoutBtn').addEventListener('click', logout);

    // Refresh buttons
    document.getElementById('refreshUsersBtn').addEventListener('click', loadUsers);
    document.getElementById('refreshRoomsBtn').addEventListener('click', loadRooms);

    // Create room button and modal
    document.getElementById('createRoomBtn').addEventListener('click', openCreateRoomModal);
    document.getElementById('joinRoomBtn').addEventListener('click', openJoinRoomModal);

    // Modal close buttons
    document.querySelectorAll('.modal .close, .modal .cancel-btn').forEach(btn => {
        btn.addEventListener('click', closeModals);
    });

    // Click outside modal to close
    window.addEventListener('click', function (event) {
        if (event.target.classList.contains('modal')) {
            closeModals();
        }
    });

    // Form submissions
    document.getElementById('createRoomForm').addEventListener('submit', createRoom);
    document.getElementById('joinRoomForm').addEventListener('submit', joinRoomById);
}

async function logout() {
    const auth = getAuth();

    try {
        // Set user offline
        await fetch('/api/users/status', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ online: false })
        });
    } catch (error) {
        console.error('Failed to set offline status:', error);
    }

    // Clear auth
    localStorage.removeItem('baatkaro_auth');
    localStorage.removeItem('baatkaro_username');

    // Redirect to login
    window.location.href = '/pages/login';
}

async function loadUsers() {
    const usersLoading = document.getElementById('usersLoading');
    const usersError = document.getElementById('usersError');
    const usersTable = document.getElementById('usersTable');
    const usersTableBody = document.getElementById('usersTableBody');

    usersLoading.style.display = 'block';
    usersError.style.display = 'none';
    usersTable.style.display = 'none';

    try {
        const response = await fetch('/api/users/all', {
            headers: getAuthHeaders()
        });

        if (!response.ok) throw new Error('Failed to load users');

        const users = await response.json();
        const currentUsername = getAuth().username;

        // Filter out current user
        const otherUsers = users.filter(u => u.username !== currentUsername);

        if (otherUsers.length === 0) {
            usersTableBody.innerHTML = '<tr><td colspan="3" class="text-center">No other users found</td></tr>';
        } else {
            usersTableBody.innerHTML = otherUsers.map(user => `
                <tr>
                    <td><strong>${escapeHtml(user.username)}</strong></td>
                    <td>
                        <span class="status-badge ${user.online ? 'online' : 'offline'}">
                            ${user.online ? 'Online' : 'Offline'}
                        </span>
                    </td>
                    <td>
                        ${user.online ? `
                            <button class="table-btn table-btn-chat" onclick="startPrivateChat('${escapeHtml(user.username)}')">
                                💬 Chat
                            </button>
                        ` : '<span style="color: #999;">Offline</span>'}
                    </td>
                </tr>
            `).join('');
        }

        usersLoading.style.display = 'none';
        usersTable.style.display = 'table';
    } catch (error) {
        console.error('Error loading users:', error);
        usersLoading.style.display = 'none';
        usersError.textContent = 'Failed to load users. Please try again.';
        usersError.style.display = 'block';
    }
}

async function loadRooms() {
    const roomsLoading = document.getElementById('roomsLoading');
    const roomsError = document.getElementById('roomsError');
    const roomsTable = document.getElementById('roomsTable');
    const roomsTableBody = document.getElementById('roomsTableBody');

    roomsLoading.style.display = 'block';
    roomsError.style.display = 'none';
    roomsTable.style.display = 'none';

    try {
        const [allRoomsRes, myRoomsRes] = await Promise.all([
            fetch('/api/rooms', { headers: getAuthHeaders() }),
            fetch('/api/rooms/my-rooms', { headers: getAuthHeaders() })
        ]);

        if (!allRoomsRes.ok || !myRoomsRes.ok) throw new Error('Failed to load rooms');

        const allRooms = await allRoomsRes.json();
        const myRooms = await myRoomsRes.json();
        const myRoomIds = new Set(myRooms.map(r => r.id));

        if (allRooms.length === 0) {
            roomsTableBody.innerHTML = '<tr><td colspan="5" class="text-center">No rooms available. Create one!</td></tr>';
        } else {
            roomsTableBody.innerHTML = allRooms.map(room => {
                const isMember = myRoomIds.has(room.id);
                return `
                    <tr>
                        <td><strong>${escapeHtml(room.name)}</strong></td>
                        <td>
                            <span class="room-type-badge ${room.isPrivate ? 'private' : 'public'}">
                                ${room.isPrivate ? '🔒 Private' : '🌐 Public'}
                            </span>
                        </td>
                        <td>
                            <span class="participant-count">
                                👥 ${room.participantCount || 0}
                            </span>
                        </td>
                        <td>${escapeHtml(room.createdBy)}</td>
                        <td>
                            ${isMember ? `
                                <button class="table-btn table-btn-chat" onclick="openRoomChat('${room.id}', '${escapeHtml(room.name)}')">
                                    💬 Chat
                                </button>
                            ` : `
                                <button class="table-btn table-btn-join" onclick="joinRoom('${room.id}')">
                                    ➕ Join
                                </button>
                            `}
                            <button class="table-btn table-btn-copy" onclick="copyRoomId('${room.id}')" title="Copy Room ID">
                                📋
                            </button>
                        </td>
                    </tr>
                `;
            }).join('');
        }

        roomsLoading.style.display = 'none';
        roomsTable.style.display = 'table';
    } catch (error) {
        console.error('Error loading rooms:', error);
        roomsLoading.style.display = 'none';
        roomsError.textContent = 'Failed to load rooms. Please try again.';
        roomsError.style.display = 'block';
    }
}

function startPrivateChat(username) {
    localStorage.setItem('chat_type', 'private');
    localStorage.setItem('chat_target', username);
    window.location.href = '/pages/chat';
}

function openRoomChat(roomId, roomName) {
    localStorage.setItem('chat_type', 'room');
    localStorage.setItem('chat_target', roomId);
    localStorage.setItem('chat_name', roomName);
    window.location.href = '/pages/chat';
}

async function joinRoom(roomId) {
    if (!confirm('Do you want to join this room?')) return;

    try {
        const response = await fetch(`/api/rooms/${roomId}/join`, {
            method: 'POST',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to join room');
        }

        alert('Successfully joined the room!');
        loadRooms(); // Reload rooms
    } catch (error) {
        console.error('Error joining room:', error);
        alert('Failed to join room: ' + error.message);
    }
}

function openCreateRoomModal() {
    document.getElementById('createRoomModal').classList.add('show');
}

function openJoinRoomModal() {
    document.getElementById('joinRoomModal').classList.add('show');
}

function closeModals() {
    document.querySelectorAll('.modal').forEach(modal => {
        modal.classList.remove('show');
    });
    // Reset forms
    document.getElementById('createRoomForm').reset();
    document.getElementById('joinRoomForm').reset();
}

async function createRoom(e) {
    e.preventDefault();

    const roomName = document.getElementById('roomName').value.trim();
    const isPrivate = document.getElementById('roomIsPrivate').checked;

    if (!roomName) {
        alert('Please enter a room name');
        return;
    }

    try {
        const response = await fetch('/api/rooms', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({
                name: roomName,
                isPrivate: isPrivate
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to create room');
        }

        const data = await response.json();
        alert('Room created successfully!');

        closeModals();
        loadRooms(); // Reload rooms

        // Optionally open the room
        if (confirm('Do you want to open the room now?')) {
            openRoomChat(data.room.id, data.room.name);
        }
    } catch (error) {
        console.error('Error creating room:', error);
        alert('Failed to create room: ' + error.message);
    }
}

async function joinRoomById(e) {
    e.preventDefault();

    const roomId = document.getElementById('roomId').value.trim();

    if (!roomId) {
        alert('Please enter a room ID');
        return;
    }

    // Validate UUID format
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    if (!uuidRegex.test(roomId)) {
        alert('Invalid room ID format. Please enter a valid UUID.');
        return;
    }

    try {
        const response = await fetch(`/api/rooms/${roomId}/join`, {
            method: 'POST',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to join room');
        }

        const data = await response.json();
        alert('Successfully joined the room!');

        closeModals();
        loadRooms(); // Reload rooms

        // Optionally open the room
        if (confirm('Do you want to open the room now?')) {
            openRoomChat(data.room.id, data.room.name);
        }
    } catch (error) {
        console.error('Error joining room:', error);
        alert('Failed to join room: ' + error.message);
    }
}

function copyRoomId(roomId) {
    navigator.clipboard.writeText(roomId).then(() => {
        alert('Room ID copied to clipboard!');
    }).catch(err => {
        console.error('Failed to copy:', err);
        // Fallback
        prompt('Copy this room ID:', roomId);
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
