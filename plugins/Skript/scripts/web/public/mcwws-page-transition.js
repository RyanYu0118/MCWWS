/**
 * MCWWS 站内页面切换过渡
 * - 支持浏览器：原生 cross-document View Transition（交叉淡入，无黑屏空档）
 * - 回退：遮罩层过渡 + 链接 prefetch + body 就绪即淡入（不等 DOMContentLoaded）
 */
(function initMcwwsPageTransitionCritical() {
    const STYLE_ID = 'mcwws-pt-critical';
    let entering = false;
    try {
        entering = sessionStorage.getItem('mcwws.pageTransition') === 'out';
    } catch (_) { /* ignore */ }

    const css = `
html.mcwws-pt-curtain::after {
    content: '';
    position: fixed;
    inset: 0;
    z-index: 2147483646;
    background: #050505;
    opacity: 0;
    pointer-events: none;
}
html[data-color-scheme="light"].mcwws-pt-curtain::after {
    background: #ebedf2;
}
html.mcwws-pt-curtain.mcwws-pt-curtain-cover::after {
    opacity: 1;
    pointer-events: auto;
}
html.mcwws-pt-curtain.mcwws-pt-curtain-enter::after {
    opacity: 1;
    transition: none;
}
html.mcwws-pt-curtain.mcwws-pt-curtain-enter-active::after {
    opacity: 0;
    transition: opacity var(--page-transition-duration, 0.28s) ease;
}
`;

    if (!document.getElementById(STYLE_ID)) {
        const el = document.createElement('style');
        el.id = STYLE_ID;
        el.textContent = css;
        document.head.appendChild(el);
    }

    if (entering) {
        document.documentElement.classList.add(
            'mcwws-pt-curtain',
            'mcwws-pt-curtain-enter',
            'mcwws-pt-curtain-cover'
        );
    }
})();

const MCWWS_PAGE_TRANSITION_MS = 280;
const MCWWS_PAGE_NAV_DELAY_MS = 240;
const prefetchedHrefs = new Set();
let enterScheduled = false;

function mcwwsPageTransitionPrefersReducedMotion() {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function mcwwsPageTransitionUsesNative() {
    if (mcwwsPageTransitionPrefersReducedMotion()) {
        return false;
    }
    try {
        return CSS.supports('navigation', 'auto');
    } catch (_) {
        return false;
    }
}

function mcwwsPageTransitionClearState() {
    document.documentElement.classList.remove(
        'mcwws-pt-curtain',
        'mcwws-pt-curtain-cover',
        'mcwws-pt-curtain-enter',
        'mcwws-pt-curtain-enter-active',
        'mcwws-page-exit',
        'mcwws-page-enter',
        'mcwws-page-enter-active',
        'mcwws-page-enter-pending'
    );
    try {
        sessionStorage.removeItem('mcwws.pageTransition');
    } catch (_) { /* ignore */ }
}

function mcwwsPageTransitionIsInternalLink(anchor) {
    if (!anchor || anchor.tagName !== 'A') {
        return false;
    }
    if (anchor.hasAttribute('download')) {
        return false;
    }
    const target = (anchor.getAttribute('target') || '').toLowerCase();
    if (target && target !== '_self') {
        return false;
    }
    const rawHref = anchor.getAttribute('href');
    if (!rawHref || rawHref.startsWith('#') || rawHref.startsWith('javascript:')) {
        return false;
    }
    let url;
    try {
        url = new URL(anchor.href, window.location.href);
    } catch (_) {
        return false;
    }
    if (url.origin !== window.location.origin) {
        return false;
    }
    if (url.pathname === window.location.pathname && url.search === window.location.search && url.hash) {
        return false;
    }
    return true;
}

function mcwwsPageTransitionWhenBodyReady(callback) {
    if (document.body) {
        callback();
        return;
    }
    const observer = new MutationObserver(() => {
        if (document.body) {
            observer.disconnect();
            callback();
        }
    });
    observer.observe(document.documentElement, { childList: true });
}

function mcwwsPageTransitionPlayEnter() {
    if (mcwwsPageTransitionPrefersReducedMotion()) {
        mcwwsPageTransitionClearState();
        return;
    }

    const root = document.documentElement;
    if (!root.classList.contains('mcwws-pt-curtain-enter')) {
        // 残留遮罩（例如 WebView 中断导航）时强制清掉，避免整页“空白”
        if (root.classList.contains('mcwws-pt-curtain') || root.classList.contains('mcwws-pt-curtain-cover')) {
            mcwwsPageTransitionClearState();
        }
        return;
    }

    try {
        sessionStorage.removeItem('mcwws.pageTransition');
    } catch (_) { /* ignore */ }

    root.classList.add('mcwws-page-enter');

    // 兜底：动画回调未触发时，最多 1.2s 后去掉遮罩
    window.setTimeout(() => {
        if (root.classList.contains('mcwws-pt-curtain') || root.classList.contains('mcwws-pt-curtain-cover')) {
            mcwwsPageTransitionClearState();
        }
    }, 1200);

    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            root.classList.remove('mcwws-pt-curtain-enter');
            root.classList.add('mcwws-pt-curtain-enter-active', 'mcwws-page-enter-active');
            window.setTimeout(() => {
                root.classList.remove(
                    'mcwws-pt-curtain',
                    'mcwws-pt-curtain-cover',
                    'mcwws-pt-curtain-enter-active',
                    'mcwws-page-enter',
                    'mcwws-page-enter-active'
                );
            }, MCWWS_PAGE_TRANSITION_MS + 40);
        });
    });
}

function mcwwsPageTransitionScheduleEnter() {
    if (enterScheduled) {
        return;
    }
    enterScheduled = true;
    mcwwsPageTransitionWhenBodyReady(mcwwsPageTransitionPlayEnter);
}

function mcwwsPageTransitionNavigate(href) {
    if (mcwwsPageTransitionPrefersReducedMotion()) {
        window.location.href = href;
        return;
    }

    const root = document.documentElement;
    if (root.classList.contains('mcwws-page-exit')) {
        return;
    }

    root.classList.add('mcwws-pt-curtain', 'mcwws-page-exit');
    try {
        sessionStorage.setItem('mcwws.pageTransition', 'out');
    } catch (_) { /* ignore */ }

    requestAnimationFrame(() => {
        root.classList.add('mcwws-pt-curtain-cover');
    });

    window.setTimeout(() => {
        window.location.href = href;
    }, MCWWS_PAGE_NAV_DELAY_MS);
}

function mcwwsPageTransitionPrefetchHref(href) {
    if (!href || prefetchedHrefs.has(href)) {
        return;
    }
    prefetchedHrefs.add(href);
    const link = document.createElement('link');
    link.rel = 'prefetch';
    link.href = href;
    link.as = 'document';
    document.head.appendChild(link);
}

function mcwwsPageTransitionOnLinkPointerOver(event) {
    if (mcwwsPageTransitionUsesNative()) {
        return;
    }
    const link = event.target.closest('a[href]');
    if (!mcwwsPageTransitionIsInternalLink(link)) {
        return;
    }
    try {
        mcwwsPageTransitionPrefetchHref(new URL(link.href, window.location.href).href);
    } catch (_) { /* ignore */ }
}

function mcwwsPageTransitionOnLinkClick(event) {
    if (mcwwsPageTransitionUsesNative()) {
        return;
    }
    if (event.defaultPrevented) {
        return;
    }
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
    }

    const link = event.target.closest('a[href]');
    if (!mcwwsPageTransitionIsInternalLink(link)) {
        return;
    }

    let url;
    try {
        url = new URL(link.href, window.location.href);
    } catch (_) {
        return;
    }

    if (url.pathname === window.location.pathname && url.search === window.location.search && !url.hash) {
        event.preventDefault();
        return;
    }

    event.preventDefault();
    mcwwsPageTransitionNavigate(link.href);
}

function mcwwsPageTransitionInit() {
    mcwwsPageTransitionScheduleEnter();
    document.addEventListener('click', mcwwsPageTransitionOnLinkClick);
    document.addEventListener('mouseover', mcwwsPageTransitionOnLinkPointerOver, { passive: true });

    window.addEventListener('pageshow', (event) => {
        if (event.persisted) {
            enterScheduled = false;
            mcwwsPageTransitionClearState();
            mcwwsPageTransitionScheduleEnter();
        }
    });
}

mcwwsPageTransitionScheduleEnter();

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', mcwwsPageTransitionInit, { once: true });
} else {
    mcwwsPageTransitionInit();
}

window.MCWWS_PAGE_TRANSITION = {
    navigate: mcwwsPageTransitionNavigate,
    durationMs: MCWWS_PAGE_TRANSITION_MS,
    usesNative: mcwwsPageTransitionUsesNative
};
