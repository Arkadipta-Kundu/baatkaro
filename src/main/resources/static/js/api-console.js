// API Console JavaScript
class ApiConsole {
    constructor() {
        this.currentUser = null;
        this.jwtToken = null;
        this.websocket = null;
        this.stompClient = null;
        this.apiBaseUrl = '/api';
        this.wsEndpoint = '/ws';

        this.apiEndpoints = {
            // Authentication APIs
            'register': {
                method: 'POST',
                url: '/auth/register',
                description: 'Register a new user',
                fields: [
                    { name: 'username', type: 'text', required: true, placeholder: 'Enter username' },
                    { name: 'email', type: 'email', required: true, placeholder: 'Enter email' },
                    { name: 'password', type: 'password', required: true, placeholder: 'Enter password' },
                    { name: 'fullName', type: 'text', required: true, placeholder: 'Enter full name' }
                ]
            },
            'check-username': {
                method: 'GET',
                url: '/auth/check-username',
                description: 'Check if username is available',
                fields: [
                    { name: 'username', type: 'text', required: true, placeholder: 'Username to check' }
                ]
            },
            'logout': {
                method: 'POST',
                url: '/auth/logout',
                description: 'Logout current user',
                fields: []
            },

            // User APIs
            'users-all': {
                method: 'GET',
                url: '/users',
                description: 'Get all users',
                requiresAuth: true,
                fields: []
            },
            'users-search': {
                method: 'GET',
                url: '/users/search',
                description: 'Search users by username',
                requiresAuth: true,
                fields: [
                    { name: 'query', type: 'text', required: true, placeholder: 'Search query' }
                ]
            },
            'users-profile': {
                method: 'GET',
                url: '/users/profile',
                description: 'Get user profile by username',
                requiresAuth: true,
                fields: [
                    { name: 'username', type: 'text', required: true, placeholder: 'Username' }
                ]
            },

            // Room APIs
            'rooms-create': {
                method: 'POST',
                url: '/rooms',
                description: 'Create a new room',
                requiresAuth: true,
                fields: [
                    { name: 'name', type: 'text', required: true, placeholder: 'Room name' },
                    { name: 'description', type: 'text', required: false, placeholder: 'Room description' },
                    { name: 'isPrivate', type: 'checkbox', required: false, label: 'Private room' }
                ]
            },
            'rooms-all': {
                method: 'GET',
                url: '/rooms',
                description: 'Get all rooms',
                requiresAuth: true,
                fields: []
            },
            'rooms-join': {
                method: 'POST',
                url: '/rooms/{roomId}/join',
                description: 'Join a room',
                requiresAuth: true,
                fields: [
                    { name: 'roomId', type: 'text', required: true, placeholder: 'Room ID (UUID)' }
                ]
            },
            'rooms-leave': {
                method: 'POST',
                url: '/rooms/{roomId}/leave',
                description: 'Leave a room',
                requiresAuth: true,
                fields: [
                    { name: 'roomId', type: 'text', required: true, placeholder: 'Room ID (UUID)' }
                ]
            },

            // Message APIs
            'messages-private': {
                method: 'POST',
                url: '/messages/private',
                description: 'Send a private message',
                requiresAuth: true,
                fields: [
                    { name: 'receiver', type: 'text', required: true, placeholder: 'Receiver username' },
                    { name: 'content', type: 'textarea', required: true, placeholder: 'Message content' }
                ]
            },
            'messages-history': {
                method: 'GET',
                url: '/messages/private/history',
                description: 'Get private message history',
                requiresAuth: true,
                fields: [
                    { name: 'otherUser', type: 'text', required: true, placeholder: 'Other user username' },
                    { name: 'page', type: 'number', required: false, placeholder: 'Page number (default: 0)' },
                    { name: 'size', type: 'number', required: false, placeholder: 'Page size (default: 20)' }
                ]
            },

            // Redis Testing APIs
            'redis-status': {
                method: 'GET',
                url: '/redis/status',
                description: 'Check Redis connection status',
                requiresAuth: true,
                fields: []
            },
            'redis-test-pubsub': {
                method: 'POST',
                url: '/redis/test-pubsub',
                description: 'Test Redis Pub/Sub functionality',
                requiresAuth: true,
                fields: [
                    { name: 'message', type: 'text', required: true, placeholder: 'Test message for Redis Pub/Sub' }
                ]
            },
            'redis-simulate-multi': {
                method: 'POST',
                url: '/redis/simulate-multi-instance',
                description: 'Simulate multi-instance server environment',
                requiresAuth: true,
                fields: [
                    { name: 'message', type: 'text', required: true, placeholder: 'Multi-instance test message' }
                ]
            },
            'redis-metrics': {
                method: 'GET',
                url: '/redis/metrics',
                description: 'Get Redis performance metrics',
                requiresAuth: true,
                fields: []
            }
        };

        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthentication();
        setTimeout(() => this.checkRedisStatus(), 1000); // Check Redis status after initialization
    }

    setupEventListeners() {
        // Login form
        document.getElementById('login-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleLogin();
        });

        // Logout button
        document.getElementById('logout-btn').addEventListener('click', () => {
            this.handleLogout();
        });

        // API endpoint selection
        document.querySelectorAll('.api-endpoint').forEach(element => {
            element.addEventListener('click', (e) => {
                e.preventDefault();
                const endpoint = e.currentTarget.dataset.endpoint;
                this.selectEndpoint(endpoint);
            });
        });

        // WebSocket controls
        document.getElementById('ws-connect').addEventListener('click', () => {
            this.connectWebSocket();
        });

        document.getElementById('ws-disconnect').addEventListener('click', () => {
            this.disconnectWebSocket();
        });

        document.getElementById('ws-send').addEventListener('click', () => {
            this.sendWebSocketMessage();
        });

        // Clear buttons
        document.getElementById('clear-responses').addEventListener('click', () => {
            document.getElementById('response-container').innerHTML = `
                <div class="text-muted text-center">
                    <i class="fas fa-inbox fa-2x"></i>
                    <p class="mt-2">API responses will appear here</p>
                </div>
            `;
        });

        document.getElementById('clear-ws-messages').addEventListener('click', () => {
            document.getElementById('ws-messages-container').innerHTML = `
                <div class="text-muted text-center">
                    <i class="fas fa-comments fa-2x"></i>
                    <p class="mt-2">WebSocket messages will appear here</p>
                </div>
            `;
        });

        // Redis testing controls
        document.getElementById('test-redis-broadcast').addEventListener('click', () => {
            this.testRedisBroadcast();
        });

        document.getElementById('simulate-server-instance').addEventListener('click', () => {
            this.simulateMultiInstance();
        });
    }

    checkAuthentication() {
        // Check if user is already logged in (JWT in localStorage or cookie)
        const token = localStorage.getItem('jwt_token') || this.getCookie('jwt_token');
        if (token) {
            this.jwtToken = token;
            this.showMainConsole();
            this.connectWebSocket();
        } else {
            this.showLoginSection();
        }
    }

    async checkRedisStatus() {
        try {
            // We'll check Redis status by calling the API info endpoint
            const response = await fetch(`${this.apiBaseUrl}/info`);
            if (response.ok) {
                const statusElement = document.getElementById('redis-status');
                statusElement.className = 'badge bg-success me-3';
                statusElement.innerHTML = '<i class="fas fa-database"></i> Redis: Connected ✅';
            }
        } catch (error) {
            console.warn('Could not check Redis status:', error);
        }
    }

    async handleLogin() {
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        if (!username || !password) {
            document.getElementById('login-response').innerHTML = `
                <div class="alert alert-warning">
                    <i class="fas fa-exclamation-triangle"></i> Please enter both username and password
                </div>
            `;
            return;
        }

        const responseDiv = document.getElementById('login-response');
        const loginBtn = document.querySelector('#login-form button[type="submit"]');
        const originalBtnContent = loginBtn.innerHTML;

        // Show loading state
        loginBtn.innerHTML = '<div class="spinner-border spinner-border-sm" role="status"></div> Logging in...';
        loginBtn.disabled = true;
        responseDiv.innerHTML = '<div class="text-info"><i class="fas fa-spinner fa-spin"></i> Authenticating...</div>';

        try {
            const response = await fetch(`${this.apiBaseUrl}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                this.jwtToken = data.token;
                this.currentUser = data.user;
                localStorage.setItem('jwt_token', this.jwtToken);

                responseDiv.innerHTML = `
                    <div class="alert alert-success">
                        <i class="fas fa-check-circle"></i> Login successful! Welcome, ${data.user.username}
                        <br><small>Redirecting to console...</small>
                    </div>
                `;

                setTimeout(() => {
                    this.showMainConsole();
                    this.connectWebSocket();
                }, 1500);
            } else {
                responseDiv.innerHTML = `
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle"></i> ${data.message || 'Login failed'}
                        <br><small>Please check your credentials and try again.</small>
                    </div>
                `;
            }
        } catch (error) {
            console.error('Login error:', error);
            responseDiv.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle"></i> Network error: ${error.message}
                    <br><small>Please check your connection and try again.</small>
                </div>
            `;
        } finally {
            // Restore button state
            loginBtn.innerHTML = originalBtnContent;
            loginBtn.disabled = false;
        }
    }

    async handleLogout() {
        try {
            await fetch(`${this.apiBaseUrl}/auth/logout`, {
                method: 'POST',
                headers: this.getAuthHeaders()
            });
        } catch (error) {
            console.warn('Logout API call failed:', error);
        }

        this.jwtToken = null;
        this.currentUser = null;
        localStorage.removeItem('jwt_token');
        this.disconnectWebSocket();
        this.showLoginSection();
    }

    showLoginSection() {
        document.getElementById('login-section').style.display = 'block';
        document.getElementById('main-console').style.display = 'none';
        document.getElementById('logout-btn').style.display = 'none';
    }

    showMainConsole() {
        document.getElementById('login-section').style.display = 'none';
        document.getElementById('main-console').style.display = 'block';
        document.getElementById('logout-btn').style.display = 'block';
    }

    selectEndpoint(endpointKey) {
        // Remove active class from all endpoints
        document.querySelectorAll('.api-endpoint').forEach(el => {
            el.classList.remove('active');
        });

        // Add active class to selected endpoint
        document.querySelector(`[data-endpoint="${endpointKey}"]`).classList.add('active');

        // Build form for selected endpoint
        this.buildApiForm(endpointKey);
    }

    buildApiForm(endpointKey) {
        const endpoint = this.apiEndpoints[endpointKey];
        if (!endpoint) return;

        const container = document.getElementById('api-form-container');

        let formHtml = `
            <div class="form-builder fade-in">
                <div class="endpoint-info">
                    <span class="endpoint-method badge bg-${this.getMethodColor(endpoint.method)}">${endpoint.method}</span>
                    <span class="endpoint-url">${this.apiBaseUrl}${endpoint.url}</span>
                    <p class="mt-2 mb-0 text-muted">${endpoint.description}</p>
                </div>
                
                <form id="api-form" data-endpoint="${endpointKey}">
        `;

        // Generate form fields
        endpoint.fields.forEach(field => {
            if (field.type === 'textarea') {
                formHtml += `
                    <div class="mb-3">
                        <label for="${field.name}" class="form-label">
                            ${this.capitalizeFirst(field.name)}
                            ${field.required ? '<span class="text-danger">*</span>' : ''}
                        </label>
                        <textarea class="form-control" id="${field.name}" name="${field.name}" 
                                  placeholder="${field.placeholder || ''}" ${field.required ? 'required' : ''}
                                  rows="3"></textarea>
                    </div>
                `;
            } else if (field.type === 'checkbox') {
                formHtml += `
                    <div class="mb-3 form-check">
                        <input type="checkbox" class="form-check-input" id="${field.name}" name="${field.name}">
                        <label class="form-check-label" for="${field.name}">
                            ${field.label || this.capitalizeFirst(field.name)}
                        </label>
                    </div>
                `;
            } else {
                formHtml += `
                    <div class="mb-3">
                        <label for="${field.name}" class="form-label">
                            ${this.capitalizeFirst(field.name)}
                            ${field.required ? '<span class="text-danger">*</span>' : ''}
                        </label>
                        <input type="${field.type}" class="form-control" id="${field.name}" name="${field.name}" 
                               placeholder="${field.placeholder || ''}" ${field.required ? 'required' : ''}>
                    </div>
                `;
            }
        });

        formHtml += `
                    <button type="submit" class="btn btn-primary">
                        <i class="fas fa-paper-plane"></i> Send Request
                    </button>
                </form>
            </div>
        `;

        container.innerHTML = formHtml;

        // Add form submit handler
        document.getElementById('api-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.executeApiRequest(endpointKey);
        });
    }

    async executeApiRequest(endpointKey) {
        const endpoint = this.apiEndpoints[endpointKey];
        const form = document.getElementById('api-form');
        const formData = new FormData(form);

        // Build request data
        const requestData = {};
        const queryParams = {};

        endpoint.fields.forEach(field => {
            const value = field.type === 'checkbox'
                ? document.getElementById(field.name).checked
                : formData.get(field.name);

            if (value !== null && value !== '') {
                if (endpoint.method === 'GET') {
                    queryParams[field.name] = value;
                } else {
                    requestData[field.name] = value;
                }
            }
        });

        // Build URL
        let url = `${this.apiBaseUrl}${endpoint.url}`;

        // Replace path parameters
        endpoint.fields.forEach(field => {
            if (url.includes(`{${field.name}}`)) {
                url = url.replace(`{${field.name}}`, formData.get(field.name));
            }
        });

        // Add query parameters for GET requests
        if (endpoint.method === 'GET' && Object.keys(queryParams).length > 0) {
            const params = new URLSearchParams(queryParams);
            url += `?${params.toString()}`;
        }

        // Prepare request options
        const options = {
            method: endpoint.method,
            headers: endpoint.requiresAuth ? this.getAuthHeaders() : { 'Content-Type': 'application/json' }
        };

        if (endpoint.method !== 'GET' && Object.keys(requestData).length > 0) {
            options.body = JSON.stringify(requestData);
        }

        // Show loading state
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalBtnContent = submitBtn.innerHTML;
        submitBtn.innerHTML = '<div class="spinner-border spinner-border-sm" role="status"></div> Sending...';
        submitBtn.disabled = true;

        try {
            const response = await fetch(url, options);
            const responseData = await response.json();

            this.displayApiResponse({
                endpoint: endpointKey,
                method: endpoint.method,
                url: url,
                requestData: endpoint.method !== 'GET' ? requestData : queryParams,
                status: response.status,
                statusText: response.statusText,
                response: responseData,
                timestamp: new Date().toISOString()
            });

        } catch (error) {
            this.displayApiResponse({
                endpoint: endpointKey,
                method: endpoint.method,
                url: url,
                requestData: endpoint.method !== 'GET' ? requestData : queryParams,
                status: 0,
                statusText: 'Network Error',
                response: { error: error.message },
                timestamp: new Date().toISOString()
            });
        } finally {
            // Restore button state
            submitBtn.innerHTML = originalBtnContent;
            submitBtn.disabled = false;
        }
    }

    displayApiResponse(responseInfo) {
        const container = document.getElementById('response-container');
        const isSuccess = responseInfo.status >= 200 && responseInfo.status < 300;

        const responseHtml = `
            <div class="response-item ${isSuccess ? 'response-success' : 'response-error'} fade-in">
                <div class="response-header">
                    <div>
                        <strong>${responseInfo.endpoint}</strong>
                        <span class="badge bg-${this.getMethodColor(responseInfo.method)} ms-2">${responseInfo.method}</span>
                        <span class="badge bg-${isSuccess ? 'success' : 'danger'} ms-2">${responseInfo.status}</span>
                    </div>
                    <small>${new Date(responseInfo.timestamp).toLocaleTimeString()}</small>
                </div>
                <div class="response-body">
                    <div class="mb-2">
                        <strong>URL:</strong> <code>${responseInfo.url}</code>
                    </div>
                    ${Object.keys(responseInfo.requestData).length > 0 ? `
                        <div class="mb-2">
                            <strong>Request Data:</strong>
                            <pre class="response-code">${JSON.stringify(responseInfo.requestData, null, 2)}</pre>
                        </div>
                    ` : ''}
                    <div>
                        <strong>Response:</strong>
                        <pre class="response-code">${JSON.stringify(responseInfo.response, null, 2)}</pre>
                    </div>
                </div>
            </div>
        `;

        // If container has placeholder, replace it
        if (container.querySelector('.text-muted')) {
            container.innerHTML = responseHtml;
        } else {
            container.insertAdjacentHTML('afterbegin', responseHtml);
        }

        // Scroll to top of response container
        container.scrollTop = 0;
    }

    connectWebSocket() {
        if (this.stompClient && this.stompClient.connected) {
            this.addWebSocketMessage('system', 'WebSocket already connected');
            return;
        }

        if (!this.jwtToken || !this.currentUser) {
            this.addWebSocketMessage('system', 'Cannot connect: Not authenticated');
            return;
        }

        this.updateConnectionStatus('connecting');
        this.addWebSocketMessage('system', `Connecting WebSocket for user: ${this.currentUser.username}`);

        try {
            // Create WebSocket URL with username parameter for authentication
            const wsUrl = `${this.wsEndpoint}?username=${encodeURIComponent(this.currentUser.username)}`;
            console.log('Connecting to WebSocket URL:', wsUrl);

            const socket = new SockJS(wsUrl);

            this.stompClient = new StompJs.Client({
                webSocketFactory: () => socket,
                debug: (str) => {
                    console.log('STOMP Debug: ' + str);
                },
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                connectionTimeout: 10000, // 10 second timeout
            });

            this.stompClient.onConnect = (frame) => {
                console.log('WebSocket Connected: ' + frame);
                this.updateConnectionStatus('connected');
                this.addWebSocketMessage('system', 'WebSocket connected successfully');

                // Subscribe to private messages
                if (this.currentUser && this.currentUser.username) {
                    try {
                        this.stompClient.subscribe('/user/queue/private', (message) => {
                            const msg = JSON.parse(message.body);
                            this.addWebSocketMessage('incoming', `Private from ${msg.sender}: ${msg.content}`);
                        });
                        console.log('Subscribed to private messages queue');
                    } catch (error) {
                        console.error('Error subscribing to private messages:', error);
                    }
                }

                // Subscribe to a demo room
                try {
                    this.stompClient.subscribe('/topic/room/demo-room', (message) => {
                        const msg = JSON.parse(message.body);
                        this.addWebSocketMessage('incoming', `Room message from ${msg.sender}: ${msg.content}`);
                    });
                    console.log('Subscribed to demo room');
                } catch (error) {
                    console.error('Error subscribing to room messages:', error);
                }

                // Enable WebSocket controls
                document.getElementById('ws-connect').disabled = true;
                document.getElementById('ws-disconnect').disabled = false;
                document.getElementById('ws-send').disabled = false;
            };

            this.stompClient.onStompError = (frame) => {
                console.error('STOMP Error - Broker reported error: ' + frame.headers['message']);
                console.error('STOMP Error - Additional details: ' + frame.body);
                this.updateConnectionStatus('disconnected');
                this.addWebSocketMessage('system', `WebSocket error: ${frame.headers['message'] || 'Unknown error'}`);

                // Re-enable connect button
                document.getElementById('ws-connect').disabled = false;
                document.getElementById('ws-disconnect').disabled = true;
                document.getElementById('ws-send').disabled = true;
            };

            this.stompClient.onWebSocketError = (error) => {
                console.error('WebSocket Error:', error);
                this.updateConnectionStatus('disconnected');
                this.addWebSocketMessage('system', `WebSocket connection error: ${error.message || 'Connection failed'}`);

                // Re-enable connect button
                document.getElementById('ws-connect').disabled = false;
                document.getElementById('ws-disconnect').disabled = true;
                document.getElementById('ws-send').disabled = true;
            };

            this.stompClient.onDisconnect = () => {
                console.log('WebSocket Disconnected');
                this.updateConnectionStatus('disconnected');
                this.addWebSocketMessage('system', 'WebSocket disconnected');

                // Disable WebSocket controls
                document.getElementById('ws-connect').disabled = false;
                document.getElementById('ws-disconnect').disabled = true;
                document.getElementById('ws-send').disabled = true;
            };

            // Add connection timeout
            const connectionTimeout = setTimeout(() => {
                if (!this.stompClient.connected) {
                    console.error('WebSocket connection timeout');
                    this.addWebSocketMessage('system', 'WebSocket connection timeout - check server status');
                    this.updateConnectionStatus('disconnected');

                    // Re-enable connect button
                    document.getElementById('ws-connect').disabled = false;
                    document.getElementById('ws-disconnect').disabled = true;
                    document.getElementById('ws-send').disabled = true;

                    if (this.stompClient) {
                        this.stompClient.deactivate();
                    }
                }
            }, 15000); // 15 second timeout

            // Clear timeout when connected
            this.stompClient.onConnect = (frame) => {
                clearTimeout(connectionTimeout);
                // ... rest of onConnect code from above
                console.log('WebSocket Connected: ' + frame);
                this.updateConnectionStatus('connected');
                this.addWebSocketMessage('system', 'WebSocket connected successfully');

                // Subscribe to private messages
                if (this.currentUser && this.currentUser.username) {
                    try {
                        this.stompClient.subscribe('/user/queue/private', (message) => {
                            const msg = JSON.parse(message.body);
                            this.addWebSocketMessage('incoming', `Private from ${msg.sender}: ${msg.content}`);
                        });
                        console.log('Subscribed to private messages queue');
                    } catch (error) {
                        console.error('Error subscribing to private messages:', error);
                    }
                }

                // Subscribe to a demo room
                try {
                    this.stompClient.subscribe('/topic/room/demo-room', (message) => {
                        const msg = JSON.parse(message.body);
                        this.addWebSocketMessage('incoming', `Room message from ${msg.sender}: ${msg.content}`);
                    });
                    console.log('Subscribed to demo room');
                } catch (error) {
                    console.error('Error subscribing to room messages:', error);
                }

                // Enable WebSocket controls
                document.getElementById('ws-connect').disabled = true;
                document.getElementById('ws-disconnect').disabled = false;
                document.getElementById('ws-send').disabled = false;
            };

            this.stompClient.activate();

        } catch (error) {
            console.error('WebSocket connection error:', error);
            this.updateConnectionStatus('disconnected');
            this.addWebSocketMessage('system', `Connection error: ${error.message}`);

            // Re-enable connect button
            document.getElementById('ws-connect').disabled = false;
            document.getElementById('ws-disconnect').disabled = true;
            document.getElementById('ws-send').disabled = true;
        }
    } disconnectWebSocket() {
        if (this.stompClient) {
            this.stompClient.deactivate();
        }
    }

    sendWebSocketMessage() {
        if (!this.stompClient || !this.stompClient.connected) {
            this.addWebSocketMessage('system', 'Not connected to WebSocket');
            return;
        }

        const messageType = document.getElementById('ws-message-type').value;
        const target = document.getElementById('ws-target').value;
        const content = document.getElementById('ws-message-content').value;

        if (!target || !content) {
            this.addWebSocketMessage('system', 'Please fill in target and message content');
            return;
        }

        try {
            if (messageType === 'private') {
                this.stompClient.publish({
                    destination: '/app/chat.private',
                    body: JSON.stringify({
                        sender: this.currentUser.username,
                        receiver: target,
                        content: content,
                        type: 'PRIVATE'
                    })
                });
                this.addWebSocketMessage('outgoing', `Private to ${target}: ${content}`);
            } else if (messageType === 'room') {
                this.stompClient.publish({
                    destination: '/app/chat.sendMessage',
                    body: JSON.stringify({
                        sender: this.currentUser.username,
                        roomId: target,
                        content: content,
                        type: 'ROOM'
                    })
                });
                this.addWebSocketMessage('outgoing', `Room ${target}: ${content}`);
            }

            // Clear message input
            document.getElementById('ws-message-content').value = '';

        } catch (error) {
            this.addWebSocketMessage('system', `Error sending message: ${error.message}`);
        }
    }

    updateConnectionStatus(status) {
        const statusElement = document.getElementById('connection-status');
        switch (status) {
            case 'connected':
                statusElement.className = 'badge bg-success me-3 connected';
                statusElement.innerHTML = '<i class="fas fa-circle"></i> Connected';
                break;
            case 'connecting':
                statusElement.className = 'badge bg-warning me-3';
                statusElement.innerHTML = '<i class="fas fa-circle"></i> Connecting...';
                break;
            case 'disconnected':
            default:
                statusElement.className = 'badge bg-danger me-3';
                statusElement.innerHTML = '<i class="fas fa-circle"></i> Disconnected';
                break;
        }
    }

    addWebSocketMessage(type, content) {
        const container = document.getElementById('ws-messages-container');
        const timestamp = new Date().toLocaleTimeString();

        const messageHtml = `
            <div class="ws-message ${type} fade-in">
                <span class="ws-message-time">${timestamp}</span>
                <div class="ws-message-content">${content}</div>
            </div>
        `;

        // If container has placeholder, replace it
        if (container.querySelector('.text-muted')) {
            container.innerHTML = messageHtml;
        } else {
            container.insertAdjacentHTML('beforeend', messageHtml);
        }

        // Scroll to bottom
        container.scrollTop = container.scrollHeight;
    }

    getAuthHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.jwtToken}`
        };
    }

    getMethodColor(method) {
        const colors = {
            'GET': 'info',
            'POST': 'success',
            'PUT': 'warning',
            'DELETE': 'danger',
            'PATCH': 'secondary'
        };
        return colors[method] || 'secondary';
    }

    capitalizeFirst(str) {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    }

    // Redis Testing Methods
    async testRedisBroadcast() {
        const testMessage = document.getElementById('redis-test-message').value.trim();
        if (!testMessage) {
            this.addRedisMonitorMessage('error', 'Please enter a test message');
            return;
        }

        try {
            this.addRedisMonitorMessage('info', `Testing Redis broadcast: "${testMessage}"`);

            // Call the Redis test API
            const response = await fetch(`${this.apiBaseUrl}/redis/test-pubsub`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ message: testMessage })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                this.addRedisMonitorMessage('success', 'Redis Pub/Sub test successful');
                this.addRedisMonitorMessage('system', `Published to: ${data.publishedTo}`);
                this.addRedisMonitorMessage('system', `Server instance: ${data.serverInstance}`);

                // Also send via WebSocket if connected
                if (this.stompClient && this.stompClient.connected) {
                    this.stompClient.publish({
                        destination: '/app/chat.sendMessage',
                        body: JSON.stringify({
                            sender: this.currentUser.username,
                            roomId: 'redis-test-room',
                            content: `[REDIS TEST] ${testMessage}`,
                            type: 'ROOM'
                        })
                    });

                    this.addRedisMonitorMessage('success', 'Message sent via WebSocket → Redis Pub/Sub');
                    this.addWebSocketMessage('outgoing', `Redis Test to room redis-test-room: ${testMessage}`);
                }
            } else {
                this.addRedisMonitorMessage('error', `Redis test failed: ${data.error || 'Unknown error'}`);
            }

        } catch (error) {
            this.addRedisMonitorMessage('error', `Redis test failed: ${error.message}`);
        }

        // Clear the test message input
        document.getElementById('redis-test-message').value = '';
    }

    async simulateMultiInstance() {
        this.addRedisMonitorMessage('info', 'Simulating multi-instance server deployment...');

        try {
            // Call the multi-instance simulation API
            const response = await fetch(`${this.apiBaseUrl}/redis/simulate-multi-instance`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ message: 'Multi-instance test message' })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                this.addRedisMonitorMessage('success', 'Multi-instance simulation initiated');
                this.addRedisMonitorMessage('system', `Current instance: ${data.currentInstance}`);

                // Simulate visual feedback for different instances
                const instances = data.instancesSimulated || ['Server-A', 'Server-B', 'Server-C'];
                let currentInstance = 0;

                const instanceInterval = setInterval(() => {
                    const instanceElement = document.getElementById('server-instance');
                    if (instanceElement) {
                        instanceElement.textContent = instances[currentInstance] + (currentInstance === 0 ? ' (Current)' : '');
                        instanceElement.className = `badge bg-${currentInstance === 0 ? 'info' : 'secondary'}`;
                    }

                    this.addRedisMonitorMessage('system', `Simulating message from: ${instances[currentInstance]}`);

                    // Simulate Redis pub/sub message routing between instances
                    if (currentInstance > 0) {
                        setTimeout(() => {
                            this.addRedisMonitorMessage('info', `Redis routing message between instances`);
                            this.addRedisMonitorMessage('success', `Message delivered via ${data.redisChannelsUsed}`);
                        }, 500);
                    }

                    currentInstance = (currentInstance + 1) % instances.length;

                    if (currentInstance === 0) {
                        clearInterval(instanceInterval);
                        this.addRedisMonitorMessage('success', 'Multi-instance simulation complete. All instances synchronized via Redis.');
                    }
                }, 2000);
            } else {
                this.addRedisMonitorMessage('error', `Multi-instance simulation failed: ${data.error || 'Unknown error'}`);
            }

        } catch (error) {
            this.addRedisMonitorMessage('error', `Multi-instance simulation failed: ${error.message}`);
        }
    }

    addRedisMonitorMessage(type, message) {
        const container = document.getElementById('redis-monitor');
        const timestamp = new Date().toLocaleTimeString();

        const typeColors = {
            'info': 'text-info',
            'success': 'text-success',
            'error': 'text-danger',
            'system': 'text-warning'
        };

        const typeIcons = {
            'info': 'fas fa-info-circle',
            'success': 'fas fa-check-circle',
            'error': 'fas fa-exclamation-triangle',
            'system': 'fas fa-cog'
        };

        const messageHtml = `
            <div class="redis-monitor-entry mb-1">
                <span class="text-muted">[${timestamp}]</span>
                <i class="${typeIcons[type]} ${typeColors[type]}"></i>
                <span class="${typeColors[type]}">${message}</span>
            </div>
        `;

        // If container has placeholder, replace it
        if (container.querySelector('.text-muted.text-center')) {
            container.innerHTML = messageHtml;
        } else {
            container.insertAdjacentHTML('beforeend', messageHtml);
        }

        // Scroll to bottom
        container.scrollTop = container.scrollHeight;
    }

    async checkRedisStatus() {
        try {
            // Check Redis status by calling the API info endpoint
            const response = await fetch(`${this.apiBaseUrl}/info`);
            if (response.ok) {
                const statusElement = document.getElementById('redis-status');
                const testStatusElement = document.getElementById('redis-test-status');

                statusElement.className = 'badge bg-success me-3';
                statusElement.innerHTML = '<i class="fas fa-database"></i> Redis: Connected ✅';

                if (testStatusElement) {
                    testStatusElement.className = 'badge bg-success';
                    testStatusElement.textContent = 'Connected ✅';

                    this.addRedisMonitorMessage('success', 'Redis connection verified - Ready for cross-server testing');
                }
            }
        } catch (error) {
            console.warn('Could not check Redis status:', error);
            const testStatusElement = document.getElementById('redis-test-status');
            if (testStatusElement) {
                testStatusElement.className = 'badge bg-danger';
                testStatusElement.textContent = 'Disconnected ❌';

                this.addRedisMonitorMessage('error', 'Redis connection failed - Cross-server features unavailable');
            }
        }
    }

    async loadRedisStatus() {
        try {
            // Get Redis status from backend
            const response = await fetch(`${this.apiBaseUrl}/redis/status`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });

            const data = await response.json();

            // Update status indicators
            const statusElement = document.getElementById('redis-status');
            const metricsElement = document.getElementById('redis-metrics');
            const instanceElement = document.getElementById('server-instance');

            if (response.ok && data.connected) {
                statusElement.innerHTML = '<span class="text-success">Connected</span>';
                instanceElement.textContent = data.serverInstance || 'Unknown';

                // Update metrics if available
                if (data.metrics) {
                    metricsElement.innerHTML = `
                        <div class="row">
                            <div class="col-6">
                                <small class="text-muted">Memory Usage:</small><br>
                                <strong>${data.metrics.usedMemory || 'N/A'}</strong>
                            </div>
                            <div class="col-6">
                                <small class="text-muted">Total Keys:</small><br>
                                <strong>${data.metrics.totalKeys || 'N/A'}</strong>
                            </div>
                        </div>
                        <div class="row mt-2">
                            <div class="col-6">
                                <small class="text-muted">Pub/Sub Channels:</small><br>
                                <strong>${data.metrics.pubsubChannels || 'N/A'}</strong>
                            </div>
                            <div class="col-6">
                                <small class="text-muted">Client Connections:</small><br>
                                <strong>${data.metrics.connectedClients || 'N/A'}</strong>
                            </div>
                        </div>
                    `;
                } else {
                    metricsElement.innerHTML = '<small class="text-muted">Metrics not available</small>';
                }

                this.addRedisMonitorMessage('success', 'Redis status loaded successfully');

            } else {
                statusElement.innerHTML = '<span class="text-danger">Disconnected</span>';
                metricsElement.innerHTML = '<small class="text-danger">Unable to fetch metrics</small>';
                instanceElement.textContent = 'Unknown';

                this.addRedisMonitorMessage('error', data.error || 'Redis connection failed');
            }

        } catch (error) {
            const statusElement = document.getElementById('redis-status');
            const metricsElement = document.getElementById('redis-metrics');
            const instanceElement = document.getElementById('server-instance');

            statusElement.innerHTML = '<span class="text-danger">Error</span>';
            metricsElement.innerHTML = '<small class="text-danger">Unable to fetch metrics</small>';
            instanceElement.textContent = 'Unknown';

            this.addRedisMonitorMessage('error', `Failed to load Redis status: ${error.message}`);
        }
    }
}

// Initialize the API Console when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new ApiConsole();
});