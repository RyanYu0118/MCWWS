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

const MCWWS_COLOR_SCHEME_TRANSITION_MS = 450;

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

function mcwwsColorSchemeApply(schemeId, options) {
    const animate = !options || options.animate !== false;
    const scheme = schemeId === 'light' ? 'light' : 'dark';
    const root = document.documentElement;
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const shouldAnimate = animate && !prefersReducedMotion;

    if (shouldAnimate) {
        root.classList.add('is-theme-animating');
    }

    root.setAttribute('data-color-scheme', scheme);
    try {
        localStorage.setItem('mcwws.web.colorScheme', scheme);
    } catch (_) { /* ignore */ }

    mcwwsColorSchemeSyncToggleUi();

    if (shouldAnimate) {
        window.setTimeout(() => {
            root.classList.remove('is-theme-animating');
        }, MCWWS_COLOR_SCHEME_TRANSITION_MS);
    }

    window.dispatchEvent(new CustomEvent('mcwws-color-scheme-change', { detail: { scheme } }));
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

    const mapFloat = document.getElementById('mapAuthFloat');
    if (mapFloat) {
        switcher.classList.add('mcwws-theme-switcher--map');
        mapFloat.insertBefore(switcher, mapFloat.firstChild);
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
    toggle: mcwwsColorSchemeToggle
};
