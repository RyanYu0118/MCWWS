/**
 * MCWWS 深/浅色主题切换（localStorage 持久化）
 */
(function initMcwwsColorSchemeEarly() {
    const STORAGE_KEY = 'mcwws.web.colorScheme';
    const DEFAULT_SCHEME = 'dark';

    function normalizeScheme(value) {
        return value === 'light' ? 'light' : 'dark';
    }

    function readSavedScheme() {
        try {
            const saved = localStorage.getItem(STORAGE_KEY);
            if (saved === 'light' || saved === 'dark') {
                return saved;
            }
            const legacyTheme = localStorage.getItem('mcwws.web.theme');
            if (legacyTheme === 'ios26-glass' || legacyTheme === 'default') {
                return DEFAULT_SCHEME;
            }
        } catch (_) { /* ignore */ }
        return DEFAULT_SCHEME;
    }

    document.documentElement.setAttribute('data-color-scheme', normalizeScheme(readSavedScheme()));
})();

const MCWWS_COLOR_SCHEME_TRANSITION_MS = 320;
const MCWWS_THEME_OVERLAY_COLORS = {
    dark: '#050505',
    light: '#ebedf2'
};

function mcwwsColorSchemeGet() {
    const current = document.documentElement.getAttribute('data-color-scheme');
    return current === 'light' ? 'light' : 'dark';
}

function mcwwsColorSchemeUpdateToggleButton(toggleBtn) {
    if (!toggleBtn) {
        return;
    }
    const scheme = mcwwsColorSchemeGet();
    toggleBtn.dataset.active = scheme;
    toggleBtn.setAttribute(
        'aria-label',
        scheme === 'light' ? '当前浅色主题，单击切换为深色' : '当前深色主题，单击切换为浅色'
    );
    toggleBtn.setAttribute('title', scheme === 'light' ? '切换为深色' : '切换为浅色');
}

function mcwwsColorSchemeSyncToggleUi() {
    mcwwsColorSchemeUpdateToggleButton(document.querySelector('.mcwws-theme-toggle'));
}

function mcwwsColorSchemeApplyCore(scheme) {
    const root = document.documentElement;
    root.setAttribute('data-color-scheme', scheme);
    try {
        localStorage.setItem('mcwws.web.colorScheme', scheme);
    } catch (_) { /* ignore */ }
    mcwwsColorSchemeSyncToggleUi();
    window.dispatchEvent(new CustomEvent('mcwws-color-scheme-change', { detail: { scheme } }));
}

function mcwwsColorSchemeOverlayTransition(fromScheme, applyCore) {
    const host = document.body || document.documentElement;
    let overlay = document.getElementById('mcwws-theme-switch-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'mcwws-theme-switch-overlay';
        overlay.className = 'mcwws-theme-switch-overlay';
        host.appendChild(overlay);
    }

    overlay.classList.remove('mcwws-theme-switch-overlay--out');
    overlay.style.background = MCWWS_THEME_OVERLAY_COLORS[fromScheme] || MCWWS_THEME_OVERLAY_COLORS.dark;
    overlay.style.opacity = '1';

    applyCore();

    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            overlay.classList.add('mcwws-theme-switch-overlay--out');
        });
    });

    const cleanup = () => {
        overlay.classList.remove('mcwws-theme-switch-overlay--out');
        overlay.remove();
    };

    overlay.addEventListener('transitionend', (event) => {
        if (event.propertyName === 'opacity') {
            cleanup();
        }
    }, { once: true });

    window.setTimeout(cleanup, MCWWS_COLOR_SCHEME_TRANSITION_MS + 80);
}

function mcwwsColorSchemeApply(schemeId, options) {
    const animate = !options || options.animate !== false;
    const scheme = schemeId === 'light' ? 'light' : 'dark';
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const shouldAnimate = animate && !prefersReducedMotion;
    const fromScheme = mcwwsColorSchemeGet();
    const applyCore = () => mcwwsColorSchemeApplyCore(scheme);

    if (!shouldAnimate || fromScheme === scheme) {
        applyCore();
        return;
    }

    if (typeof document.startViewTransition === 'function') {
        document.startViewTransition(applyCore);
        return;
    }

    mcwwsColorSchemeOverlayTransition(fromScheme, applyCore);
}

function mcwwsColorSchemeToggle() {
    const next = mcwwsColorSchemeGet() === 'light' ? 'dark' : 'light';
    mcwwsColorSchemeApply(next);
}

function mcwwsColorSchemeBuildSwitcher() {
    const wrap = document.createElement('div');
    wrap.className = 'mcwws-theme-switcher';
    wrap.setAttribute('role', 'group');
    wrap.setAttribute('aria-label', '界面主题');

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'mcwws-theme-toggle';
    btn.innerHTML = [
        '<span class="mcwws-theme-toggle-icon mcwws-theme-toggle-icon--moon" aria-hidden="true">🌙</span>',
        '<span class="mcwws-theme-toggle-icon mcwws-theme-toggle-icon--sun" aria-hidden="true">☀️</span>'
    ].join('');

    btn.addEventListener('click', (e) => {
        e.stopPropagation();
        mcwwsColorSchemeToggle();
    });

    mcwwsColorSchemeUpdateToggleButton(btn);
    wrap.appendChild(btn);
    return wrap;
}

function mcwwsColorSchemeMountSwitcher() {
    const mapThemeSlot = document.getElementById('mapAuthThemeSlot');
    if (mapThemeSlot) {
        let switcher = document.getElementById('mcwwsThemeSwitcher');
        if (!switcher) {
            switcher = mcwwsColorSchemeBuildSwitcher();
            switcher.id = 'mcwwsThemeSwitcher';
        }
        switcher.classList.add('mcwws-theme-switcher--popover');
        if (!mapThemeSlot.querySelector('.mcwws-auth-popover-theme-label')) {
            const label = document.createElement('span');
            label.className = 'mcwws-auth-popover-theme-label';
            label.textContent = '界面主题';
            mapThemeSlot.appendChild(label);
        }
        if (!mapThemeSlot.contains(switcher)) {
            mapThemeSlot.appendChild(switcher);
        }
        mcwwsColorSchemeSyncToggleUi();
        return;
    }

    if (document.getElementById('mcwwsThemeSwitcher')) {
        mcwwsColorSchemeSyncToggleUi();
        return;
    }
    const switcher = mcwwsColorSchemeBuildSwitcher();
    switcher.id = 'mcwwsThemeSwitcher';

    const navActions = document.querySelector('.nav-actions');
    if (navActions) {
        navActions.insertBefore(switcher, navActions.firstChild);
        return;
    }

    const hubFooter = document.querySelector('.services-hub-footer');
    if (hubFooter) {
        const bar = document.createElement('div');
        bar.className = 'mcwws-theme-switcher-bar';
        bar.appendChild(switcher);
        hubFooter.insertBefore(bar, hubFooter.firstChild);
        return;
    }

    switcher.classList.add('mcwws-theme-switcher--float');
    document.body.appendChild(switcher);
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', mcwwsColorSchemeMountSwitcher);
} else {
    mcwwsColorSchemeMountSwitcher();
}

window.MCWWS_COLOR_SCHEME = {
    get: mcwwsColorSchemeGet,
    apply: mcwwsColorSchemeApply,
    toggle: mcwwsColorSchemeToggle,
    mountSwitcher: mcwwsColorSchemeMountSwitcher
};
