// Register page functionality
document.addEventListener('DOMContentLoaded', function () {
    const registerForm = document.getElementById('registerForm');
    const errorMessage = document.getElementById('error-message');
    const successMessage = document.getElementById('success-message');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');

    // Check if already logged in
    checkExistingAuth();

    // Username availability check (debounced)
    let usernameCheckTimeout;
    usernameInput.addEventListener('input', function () {
        clearTimeout(usernameCheckTimeout);
        usernameCheckTimeout = setTimeout(() => {
            const username = usernameInput.value.trim();
            if (username.length >= 3) {
                checkUsernameAvailability(username);
            }
        }, 500);
    });

    registerForm.addEventListener('submit', async function (e) {
        e.preventDefault();

        const username = usernameInput.value.trim();
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        // Validation
        if (username.length < 3) {
            showError('Username must be at least 3 characters long');
            return;
        }

        if (password.length < 6) {
            showError('Password must be at least 6 characters long');
            return;
        }

        if (password !== confirmPassword) {
            showError('Passwords do not match');
            return;
        }

        // Disable form
        const submitBtn = registerForm.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Registering...';

        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    username: username,
                    password: password,
                    confirmPassword: confirmPassword
                })
            });

            const data = await response.json();

            if (response.ok) {
                showSuccess('Registration successful! Redirecting to login...');

                // Clear form
                registerForm.reset();

                // Redirect to login after 2 seconds
                setTimeout(() => {
                    window.location.href = '/pages/login';
                }, 2000);
            } else {
                // Handle validation errors
                if (data.username) {
                    showError(data.username);
                } else if (data.password) {
                    showError(data.password);
                } else if (data.confirmPassword) {
                    showError(data.confirmPassword);
                } else if (data.error) {
                    showError(data.error);
                } else {
                    showError('Registration failed. Please try again.');
                }
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            }
        } catch (error) {
            console.error('Registration error:', error);
            showError('Registration failed. Please try again.');
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    });

    async function checkUsernameAvailability(username) {
        try {
            const response = await fetch(`/api/auth/check-username?username=${encodeURIComponent(username)}`);
            const data = await response.json();

            if (!data.available) {
                usernameInput.setCustomValidity('Username already taken');
                usernameInput.reportValidity();
            } else {
                usernameInput.setCustomValidity('');
            }
        } catch (error) {
            console.error('Username check error:', error);
        }
    }

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
        successMessage.style.display = 'none';
        setTimeout(() => {
            errorMessage.style.display = 'none';
        }, 5000);
    }

    function showSuccess(message) {
        successMessage.textContent = message;
        successMessage.style.display = 'block';
        errorMessage.style.display = 'none';
    }
});
