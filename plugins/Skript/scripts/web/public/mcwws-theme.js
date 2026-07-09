/**
 * MCWWS 网页主题切换（localStorage 持久化）
 */
(function initMcwwsThemeEarly() {
    const STORAGE_KEY = 'mcwws.web.theme';
    const DEFAULT_THEME = 'default';
    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        const theme = saved === 'ios26-glass' ? 'ios26-glass' : DEFAULT_THEME;
        document.documentElement.setAttribute('data-theme', theme);
    } catch (_) {
        document.documentElement.setAttribute('data-theme', DEFAULT_THEME);
    }
})();

const MCWWS_THEME_CATALOG = [
    {
        id: 'default',
        label: '默认深色',
        description: '经典紫青渐变玻璃风格'
    },
    {
        id: 'ios26-glass',
        label: 'iOS 26 玻璃',
        description: '液态玻璃 · 高斯模糊 · 饱和光晕'
    }
];

function mcwwsThemeGet() {
    const current = document.documentElement.getAttribute('data-theme');
    return current === 'ios26-glass' ? 'ios26-glass' : 'default';
}

function mcwwsThemeApply(themeId) {
    const theme = themeId === 'ios26-glass' ? 'ios26-glass' : 'default';
    document.documentElement.setAttribute('data-theme', theme);
    try {
        localStorage.setItem('mcwws.web.theme', theme);
    } catch (_) { /* ignore */ }
    document.querySelectorAll('.mcwws-theme-option').forEach((btn) => {
        const active = btn.getAttribute('data-theme') === theme;
        btn.classList.toggle('is-active', active);
        btn.setAttribute('aria-pressed', active ? 'true' : 'false');
    });
    window.dispatchEvent(new CustomEvent('mcwws-theme-change', { detail: { theme } }));
}

function mcwwsThemeBuildSwitcher() {
    const wrap = document.createElement('div');
    wrap.className = 'mcwws-theme-switcher';
    wrap.setAttribute('role', 'group');
    wrap.setAttribute('aria-label', '网页主题');

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'mcwws-theme-switcher-btn';
    btn.setAttribute('aria-haspopup', 'true');
    btn.setAttribute('aria-expanded', 'false');
    btn.innerHTML = '<span class="mcwws-theme-switcher-icon" aria-hidden="true">◐</span><span class="mcwws-theme-switcher-label">主题</span>';

    const menu = document.createElement('div');
    menu.className = 'mcwws-theme-switcher-menu glass';
    menu.hidden = true;

    const title = document.createElement('p');
    title.className = 'mcwws-theme-switcher-title';
    title.textContent = '界面主题';
    menu.appendChild(title);

    MCWWS_THEME_CATALOG.forEach((item) => {
        const option = document.createElement('button');
        option.type = 'button';
        option.className = 'mcwws-theme-option';
        option.setAttribute('data-theme', item.id);
        option.setAttribute('aria-pressed', mcwwsThemeGet() === item.id ? 'true' : 'false');
        if (mcwwsThemeGet() === item.id) {
            option.classList.add('is-active');
        }
        option.innerHTML = `<span class="mcwws-theme-option-label">${item.label}</span><span class="mcwws-theme-option-desc">${item.description}</span>`;
        option.addEventListener('click', () => {
            mcwwsThemeApply(item.id);
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

function mcwwsThemeMountSwitcher() {
    if (document.getElementById('mcwwsThemeSwitcher')) {
        return;
    }
    const switcher = mcwwsThemeBuildSwitcher();
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
    document.addEventListener('DOMContentLoaded', mcwwsThemeMountSwitcher);
} else {
    mcwwsThemeMountSwitcher();
}

window.MCWWS_THEME = {
    catalog: MCWWS_THEME_CATALOG,
    get: mcwwsThemeGet,
    apply: mcwwsThemeApply
};
