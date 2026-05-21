/**
 * 流浪世界 Web 统一登录（商店 / 地图 / 管理共用 localStorage authToken）
 */
(function () {
    const TOKEN_KEY = 'authToken';
    let authToken = null;
    let currentUser = null;
    let authMode = 'login';
    let bound = false;
    const listeners = new Set();

    function notify() {
        listeners.forEach((fn) => {
            try {
                fn({ token: authToken, user: currentUser });
            } catch (e) {
                console.warn('[mcwws-auth] listener error', e);
            }
        });
    }

    function readStoredToken() {
        try {
            return localStorage.getItem(TOKEN_KEY);
        } catch {
            return null;
        }
    }

    function writeStoredToken(token) {
        try {
            if (token) {
                localStorage.setItem(TOKEN_KEY, token);
            } else {
                localStorage.removeItem(TOKEN_KEY);
            }
        } catch {
            /* ignore */
        }
    }

    function authHeaders() {
        return authToken ? { Authorization: `Bearer ${authToken}` } : {};
    }

    function showDialog(modal) {
        if (!modal) return;
        modal.classList.remove('closing');
        modal.classList.add('active');
    }

    function hideDialog(modal) {
        if (!modal) return;
        if (!modal.classList.contains('active')) return;
        modal.classList.add('closing');
        modal.classList.remove('active');
        window.setTimeout(() => modal.classList.remove('closing'), 190);
    }

    function updateStatusUi() {
        const status = document.getElementById('userStatus')
            || document.getElementById('mapUserStatus');
        const btn = document.getElementById('authButton')
            || document.getElementById('mapAuthButton');
        if (status) {
            if (currentUser) {
                status.textContent = `已登录：${currentUser.username}（${currentUser.playerId}）`;
                status.classList.add('is-logged-in');
            } else {
                status.textContent = '未登录';
                status.classList.remove('is-logged-in');
            }
        }
        if (btn) {
            btn.textContent = currentUser ? '退出' : '登录 / 注册';
            btn.title = currentUser
                ? '退出登录（商店、地图、管理系统共用）'
                : '登录 / 注册（全站只需登录一次）';
        }
    }

    function switchAuthMode(mode) {
        authMode = mode === 'register' ? 'register' : 'login';
        document.getElementById('authModeLogin')?.classList.toggle('active', authMode === 'login');
        document.getElementById('authModeRegister')?.classList.toggle('active', authMode === 'register');
        const title = document.getElementById('authModalTitle');
        if (title) {
            title.textContent = authMode === 'login' ? '登录' : '注册';
        }
        document.querySelectorAll('.auth-register-only').forEach((el) => {
            el.style.display = authMode === 'register' ? 'flex' : 'none';
        });
        const message = document.getElementById('authMessage');
        if (message) {
            message.textContent = '';
            message.style.color = '';
        }
    }

    function openAuthModal() {
        authMode = 'login';
        switchAuthMode('login');
        const form = document.getElementById('authForm');
        form?.reset();
        showDialog(document.getElementById('authModal'));
    }

    function closeAuthModal() {
        hideDialog(document.getElementById('authModal'));
        const message = document.getElementById('authMessage');
        if (message) {
            message.textContent = '';
            message.style.color = '';
        }
    }

    async function handleAuthSubmit(event) {
        event.preventDefault();
        const username = document.getElementById('authUsername')?.value.trim();
        const password = document.getElementById('authPassword')?.value;
        const playerId = document.getElementById('authPlayerId')?.value.trim();
        const message = document.getElementById('authMessage');

        if (!username || !password) {
            if (message) message.textContent = '请填写用户名和密码。';
            return;
        }
        if (authMode === 'register' && !playerId) {
            if (message) message.textContent = '注册时请填写游戏玩家 ID。';
            return;
        }

        const endpoint = authMode === 'register' ? '/api/register' : '/api/login';
        const payload = authMode === 'register'
            ? { username, password, playerId }
            : { username, password };

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            if (!response.ok) {
                if (message) {
                    message.textContent = result.error || '操作失败，请重试。';
                    message.style.color = 'var(--danger, #dc2626)';
                }
                return;
            }
            authToken = result.authToken;
            currentUser = { username: result.username, playerId: result.playerId };
            writeStoredToken(authToken);
            updateStatusUi();
            notify();
            closeAuthModal();
        } catch (error) {
            if (message) {
                message.textContent = `网络错误：${error.message || '请检查服务器是否已启动。'}`;
                message.style.color = 'var(--danger, #dc2626)';
            }
        }
    }

    async function logout() {
        if (authToken) {
            try {
                await fetch('/api/logout', {
                    method: 'POST',
                    headers: authHeaders()
                });
            } catch {
                /* ignore */
            }
        }
        authToken = null;
        currentUser = null;
        writeStoredToken(null);
        updateStatusUi();
        notify();
    }

    async function loadProfile() {
        authToken = readStoredToken();
        if (!authToken) {
            currentUser = null;
            updateStatusUi();
            notify();
            return null;
        }
        try {
            const response = await fetch('/api/profile', {
                headers: authHeaders(),
                cache: 'no-store'
            });
            if (!response.ok) {
                throw new Error('未登录');
            }
            currentUser = await response.json();
        } catch {
            authToken = null;
            currentUser = null;
            writeStoredToken(null);
        }
        updateStatusUi();
        notify();
        return currentUser;
    }

    function bindDom() {
        if (bound) return;
        bound = true;

        const authBtn = document.getElementById('authButton')
            || document.getElementById('mapAuthButton');
        authBtn?.addEventListener('click', () => {
            if (currentUser) {
                void logout();
            } else {
                openAuthModal();
            }
        });

        document.getElementById('authModeLogin')?.addEventListener('click', () => switchAuthMode('login'));
        document.getElementById('authModeRegister')?.addEventListener('click', () => switchAuthMode('register'));
        document.getElementById('authForm')?.addEventListener('submit', handleAuthSubmit);
        document.querySelector('#authModal .modal-close')?.addEventListener('click', closeAuthModal);
        document.getElementById('authModal')?.addEventListener('click', (e) => {
            if (e.target.id === 'authModal') closeAuthModal();
        });
    }

    window.MCWWS_AUTH = {
        TOKEN_KEY,
        init() {
            bindDom();
            return loadProfile();
        },
        getToken: () => authToken,
        getUser: () => currentUser,
        headers: authHeaders,
        openModal: openAuthModal,
        closeModal: closeAuthModal,
        logout,
        refresh: loadProfile,
        onChange(fn) {
            listeners.add(fn);
            return () => listeners.delete(fn);
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            if (document.getElementById('authModal')) {
                bindDom();
            }
        }, { once: true });
    }
})();
