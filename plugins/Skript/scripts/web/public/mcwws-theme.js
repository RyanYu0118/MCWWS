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

const MCWWS_COLOR_SCHEME_CATALOG = [
    {
        id: 'dark',
        label: '深色',
        description: 'iOS 26 玻璃 · 简约黑白',
        icon: '🌙'
    },
    {
        id: 'light',
        label: '浅色',
        description: 'iOS 26 玻璃 · 明亮界面',
        icon: '☀️'
    }
];

function mcwwsColorSchemeGet() {
    const current = document.documentElement.getAttribute('data-color-scheme');
    return current === 'light' ? 'light' : 'dark';
}

function mcwwsColorSchemeApply(schemeId) {
    const scheme = schemeId === 'light' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-color-scheme', scheme);
    try {
        localStorage.setItem('mcwws.web.colorScheme', scheme);
    } catch (_) { /* ignore */ }

    document.querySelectorAll('.mcwws-theme-option').forEach((btn) => {
        const active = btn.getAttribute('data-color-scheme') === scheme;
        btn.classList.toggle('is-active', active);
        btn.setAttribute('aria-pressed', active ? 'true' : 'false');
    });

    const toggleBtn = document.querySelector('.mcwws-theme-switcher-btn');
    if (toggleBtn) {
        const meta = MCWWS_COLOR_SCHEME_CATALOG.find((item) => item.id === scheme) || MCWWS_COLOR_SCHEME_CATALOG[0];
        toggleBtn.title = `当前：${meta.label}，点击切换`;
        const iconEl = toggleBtn.querySelector('.mcwws-theme-switcher-icon');
        if (iconEl) {
            iconEl.textContent = meta.icon;
        }
    }

    window.dispatchEvent(new CustomEvent('mcwws-color-scheme-change', { detail: { scheme } }));
}

function mcwwsColorSchemeBuildSwitcher() {
    const wrap = document.createElement('div');
    wrap.className = 'mcwws-theme-switcher';
    wrap.setAttribute('role', 'group');
    wrap.setAttribute('aria-label', '界面主题');

    const current = mcwwsColorSchemeGet();
    const currentMeta = MCWWS_COLOR_SCHEME_CATALOG.find((item) => item.id === current) || MCWWS_COLOR_SCHEME_CATALOG[0];

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'mcwws-theme-switcher-btn';
    btn.setAttribute('aria-haspopup', 'true');
    btn.setAttribute('aria-expanded', 'false');
    btn.title = `当前：${currentMeta.label}，点击切换`;
    btn.innerHTML = `<span class="mcwws-theme-switcher-icon" aria-hidden="true">${currentMeta.icon}</span><span class="mcwws-theme-switcher-label">主题</span>`;

    const menu = document.createElement('div');
    menu.className = 'mcwws-theme-switcher-menu glass';
    menu.hidden = true;

    const title = document.createElement('p');
    title.className = 'mcwws-theme-switcher-title';
    title.textContent = '界面主题';
    menu.appendChild(title);

    MCWWS_COLOR_SCHEME_CATALOG.forEach((item) => {
        const option = document.createElement('button');
        option.type = 'button';
        option.className = 'mcwws-theme-option';
        option.setAttribute('data-color-scheme', item.id);
        option.setAttribute('aria-pressed', mcwwsColorSchemeGet() === item.id ? 'true' : 'false');
        if (mcwwsColorSchemeGet() === item.id) {
            option.classList.add('is-active');
        }
        option.innerHTML = `<span class="mcwws-theme-option-label">${item.icon} ${item.label}</span><span class="mcwws-theme-option-desc">${item.description}</span>`;
        option.addEventListener('click', () => {
            mcwwsColorSchemeApply(item.id);
            menu.hidden = true;
            btn.setAttribute('aria-expanded', 'false');
        });
        menu.appendChild(option);
    });

    btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const open = menu.hidden;
        menu.hidden = !open;
        btn.setAttribute('aria-expanded', open ? 'true' : 'false');
    });

    document.addEventListener('click', () => {
        if (!menu.hidden) {
            menu.hidden = true;
            btn.setAttribute('aria-expanded', 'false');
        }
    });

    menu.addEventListener('click', (e) => e.stopPropagation());

    wrap.appendChild(btn);
    wrap.appendChild(menu);
    return wrap;
}

function mcwwsColorSchemeMountSwitcher() {
    if (document.getElementById('mcwwsThemeSwitcher')) {
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
    catalog: MCWWS_COLOR_SCHEME_CATALOG,
    get: mcwwsColorSchemeGet,
    apply: mcwwsColorSchemeApply
};
