(function () {
    const DEFAULT_WEB_API_PORT = 8002;
    const DEFAULT_BLUEMAP_PORT = 8100;

    /** 外网反代：商店站与地图站分域名时，API 仍在商店域 */
    const HOST_PAIRS = {
        'mcs.ryanstudio.work': {
            webApiUrl: 'https://mcs.ryanstudio.work',
            bluemapUrl: 'https://mcsmap.ryanstudio.work/'
        },
        'mcsmap.ryanstudio.work': {
            webApiUrl: 'https://mcs.ryanstudio.work',
            bluemapUrl: 'https://mcsmap.ryanstudio.work/'
        }
    };

    let parentWebApiBase = '';

    function normalizeBase(url) {
        if (!url) return '';
        return String(url).replace(/\/$/, '');
    }

    function isLocalHost(host) {
        return host === '127.0.0.1' || host === 'localhost' || host === '::1';
    }

    function currentHost() {
        return window.location.hostname || '127.0.0.1';
    }

    function currentProtocol() {
        return window.location.protocol || 'http:';
    }

    function resolveWebApiBase() {
        if (parentWebApiBase) return parentWebApiBase;
        if (window.MCWWS_API_BASE) return normalizeBase(window.MCWWS_API_BASE);
        const host = currentHost();
        const proto = currentProtocol();
        if (HOST_PAIRS[host]) return HOST_PAIRS[host].webApiUrl;
        if (proto === 'https:' && !isLocalHost(host)) {
            return `${proto}//${host}`;
        }
        return `${proto}//${host}:${DEFAULT_WEB_API_PORT}`;
    }

    function resolveBluemapBase() {
        const host = currentHost();
        const proto = currentProtocol();
        if (HOST_PAIRS[host]) return HOST_PAIRS[host].bluemapUrl;
        return `${proto}//${host}:${DEFAULT_BLUEMAP_PORT}/`;
    }

    window.MCWWS_API = {
        getWebApiBase() {
            return normalizeBase(resolveWebApiBase());
        },
        getBluemapBase() {
            return resolveBluemapBase();
        },
        setWebApiBaseFromParent(url) {
            parentWebApiBase = normalizeBase(url);
        }
    };

    window.addEventListener('message', (event) => {
        const data = event.data || {};
        if ((data.type === 'mcwws-auth' || data.type === 'mcwws-services') && data.webApiUrl) {
            window.MCWWS_API.setWebApiBaseFromParent(data.webApiUrl);
        }
    });

    if (window.parent !== window) {
        try {
            window.parent.postMessage({ type: 'mcwws-auth-request' }, '*');
        } catch (_) {
            /* ignore */
        }
    }
})();
