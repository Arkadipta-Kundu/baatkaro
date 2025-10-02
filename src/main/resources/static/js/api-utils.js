// API utilities
const API_BASE = '/api';

// Storage keys
const STORAGE_KEYS = {
    AUTH: 'baatkaro_auth',
    USERNAME: 'baatkaro_username'
};

// Get stored credentials
function getStoredAuth() {
    const authData = localStorage.getItem(STORAGE_KEYS.AUTH);
    if (authData) {
        return JSON.parse(authData);
    }
    return null;
}

// Store credentials
function storeAuth(username, password) {
    const authData = {
        username: username,
        password: password,
        basicAuth: btoa(username + ':' + password)
    };
    localStorage.setItem(STORAGE_KEYS.AUTH, JSON.stringify(authData));
    localStorage.setItem(STORAGE_KEYS.USERNAME, username);
}

// Clear credentials
function clearAuth() {
    localStorage.removeItem(STORAGE_KEYS.AUTH);
    localStorage.removeItem(STORAGE_KEYS.USERNAME);
}

// Get authorization header
function getAuthHeader() {
    const auth = getStoredAuth();
    if (auth && auth.basicAuth) {
        return 'Basic ' + auth.basicAuth;
    }
    return null;
}

// Make API request
async function apiRequest(endpoint, options = {}) {
    const url = API_BASE + endpoint;
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    const authHeader = getAuthHeader();
    if (authHeader) {
        headers['Authorization'] = authHeader;
    }

    const config = {
        ...options,
        headers
    };

    try {
        const response = await fetch(url, config);

        // Handle 401 Unauthorized
        if (response.status === 401) {
            clearAuth();
            window.location.href = '/pages/login';
            throw new Error('Unauthorized');
        }

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || data.message || 'Request failed');
        }

        return data;
    } catch (error) {
        console.error('API Request failed:', error);
        throw error;
    }
}

// Check if user is authenticated
async function checkAuth() {
    const auth = getStoredAuth();
    if (!auth) {
        return false;
    }

    try {
        await apiRequest('/users/me');
        return true;
    } catch (error) {
        clearAuth();
        return false;
    }
}

// Format date/time
function formatDateTime(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;

    // Less than 1 minute
    if (diff < 60000) {
        return 'Just now';
    }

    // Less than 1 hour
    if (diff < 3600000) {
        const minutes = Math.floor(diff / 60000);
        return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
    }

    // Less than 24 hours
    if (diff < 86400000) {
        const hours = Math.floor(diff / 3600000);
        return `${hours} hour${hours > 1 ? 's' : ''} ago`;
    }

    // Format as date
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// Format time only
function formatTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// Show error message
function showError(element, message) {
    element.textContent = message;
    element.style.display = 'block';
    setTimeout(() => {
        element.style.display = 'none';
    }, 5000);
}

// Show success message
function showSuccess(element, message) {
    element.textContent = message;
    element.style.display = 'block';
    setTimeout(() => {
        element.style.display = 'none';
    }, 5000);
}

// Export for use in other files
window.BaatKaro = {
    API_BASE,
    STORAGE_KEYS,
    getStoredAuth,
    storeAuth,
    clearAuth,
    getAuthHeader,
    apiRequest,
    checkAuth,
    formatDateTime,
    formatTime,
    showError,
    showSuccess
};
