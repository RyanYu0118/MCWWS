/**
 * MCWWS 站内页面切换过渡（淡出 → 导航 → 淡入）
 */
(function initMcwwsPageTransitionEarly() {
    try {
        if (sessionStorage.getItem('mcwws.pageTransition') === 'out') {
            document.documentElement.classList.add('mcwws-page-enter-pending');
        }
    } catch (_) { /* ignore */ }
})();

const MCWWS_PAGE_TRANSITION_MS = 450;

function mcwwsPageTransitionPrefersReducedMotion() {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
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

function mcwwsPageTransitionPlayEnter() {
    if (mcwwsPageTransitionPrefersReducedMotion()) {
        document.documentElement.classList.remove('mcwws-page-enter-pending');
        try {
            sessionStorage.removeItem('mcwws.pageTransition');
        } catch (_) { /* ignore */ }
        return;
    }

    const root = document.documentElement;
    if (!root.classList.contains('mcwws-page-enter-pending')) {
        return;
    }

    try {
        sessionStorage.removeItem('mcwws.pageTransition');
    } catch (_) { /* ignore */ }

    root.classList.add('mcwws-page-enter');
    root.classList.remove('mcwws-page-enter-pending');

    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            root.classList.add('mcwws-page-enter-active');
            window.setTimeout(() => {
                root.classList.remove('mcwws-page-enter', 'mcwws-page-enter-active');
            }, MCWWS_PAGE_TRANSITION_MS);
        });
    });
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

    root.classList.add('mcwws-page-exit');
    try {
        sessionStorage.setItem('mcwws.pageTransition', 'out');
    } catch (_) { /* ignore */ }

    window.setTimeout(() => {
        window.location.href = href;
    }, MCWWS_PAGE_TRANSITION_MS);
}

function mcwwsPageTransitionOnLinkClick(event) {
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
    mcwwsPageTransitionPlayEnter();
    document.addEventListener('click', mcwwsPageTransitionOnLinkClick);

    window.addEventListener('pageshow', (event) => {
        if (event.persisted) {
            document.documentElement.classList.remove(
                'mcwws-page-exit',
                'mcwws-page-enter',
                'mcwws-page-enter-active',
                'mcwws-page-enter-pending'
            );
            try {
                sessionStorage.removeItem('mcwws.pageTransition');
            } catch (_) { /* ignore */ }
        }
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', mcwwsPageTransitionInit);
} else {
    mcwwsPageTransitionInit();
}

window.MCWWS_PAGE_TRANSITION = {
    navigate: mcwwsPageTransitionNavigate,
    durationMs: MCWWS_PAGE_TRANSITION_MS
};
