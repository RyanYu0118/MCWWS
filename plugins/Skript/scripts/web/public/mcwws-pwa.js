/**
 * 注册 PWA，并按当前深/浅色更新状态栏颜色。
 */
(function initMcwwsPwa() {
    function schemeThemeColor() {
        const scheme = document.documentElement.getAttribute('data-color-scheme');
        return scheme === 'light' ? '#ebedf2' : '#050505';
    }

    function ensureMeta(name, content) {
        let meta = document.querySelector(`meta[name="${name}"]`);
        if (!meta) {
            meta = document.createElement('meta');
            meta.setAttribute('name', name);
            document.head.appendChild(meta);
        }
        meta.setAttribute('content', content);
        return meta;
    }

    function syncThemeColor() {
        const color = schemeThemeColor();
        ensureMeta('theme-color', color);
        if (navigator.serviceWorker && navigator.serviceWorker.controller) {
            /* theme-color 仅影响浏览器 UI，无需通知 SW */
        }
    }

    syncThemeColor();
    try {
        const observer = new MutationObserver(syncThemeColor);
        observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-color-scheme'] });
    } catch (_) { /* ignore */ }

    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js', { scope: '/' }).catch((err) => {
                console.warn('[mcwws-pwa] service worker 注册失败', err);
            });
        });
    }

    let deferredPrompt = null;
    window.addEventListener('beforeinstallprompt', (event) => {
        event.preventDefault();
        deferredPrompt = event;
        window.dispatchEvent(new CustomEvent('mcwws-pwa-installable'));
    });

    window.MCWWS_PWA = {
        promptInstall: async function promptInstall() {
            if (!deferredPrompt) {
                return false;
            }
            deferredPrompt.prompt();
            const choice = await deferredPrompt.userChoice;
            deferredPrompt = null;
            return choice && choice.outcome === 'accepted';
        },
        canPromptInstall: function canPromptInstall() {
            return !!deferredPrompt;
        }
    };
})();
