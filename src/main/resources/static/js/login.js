// Login page functionality
document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('loginForm');
    const errorMessage = document.getElementById('error-message');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');

    // Check if already logged in
    checkExistingAuth();

    loginForm.addEventListener('submit', async function (e) {
        e.preventDefault();

        const username = usernameInput.value.trim();
        const password = passwordInput.value;

        if (!username || !password) {
            showError('Please enter both username and password');
            return;
        }

        // Disable form
        const submitBtn = loginForm.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Logging in...';

        try {
            // Use the new login endpoint
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    username: username,
                    password: password
                })
            });

            const data = await response.json();

            if (data.success) {
                // Store credentials for basic auth
                const basicAuth = btoa(username + ':' + password);

                // Store auth
                localStorage.setItem('baatkaro_auth', JSON.stringify({
                    username: username,
                    password: password,
                    basicAuth: basicAuth
                }));
                localStorage.setItem('baatkaro_username', username);

                // Redirect to dashboard
                window.location.href = '/pages/dashboard';
            } else {
                showError(data.message || 'Invalid username or password');
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            }
        } catch (error) {
            console.error('Login error:', error);
            showError('Login failed. Please try again.');
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    });

    async function checkExistingAuth() {
        const authData = localStorage.getItem('baatkaro_auth');
        if (authData) {
            try {
                const auth = JSON.parse(authData);
                const response = await fetch('/api/users/me', {
                    headers: {
                        'Authorization': 'Basic ' + auth.basicAuth
                    }
                });

                if (response.ok) {
                    // Already logged in, redirect to dashboard
                    window.location.href = '/pages/dashboard';
                }
            } catch (error) {
                // Invalid auth, clear it
                localStorage.removeItem('baatkaro_auth');
                localStorage.removeItem('baatkaro_username');
            }
        }
    }

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
        setTimeout(() => {
            errorMessage.style.display = 'none';
        }, 5000);
    }
});
