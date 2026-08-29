(function () {
    const host = window.location.hostname || '127.0.0.1';
    const protocol = window.location.protocol || 'http:';
    const bluemapPort = 8100;
    const webApiPort = 8002;

    const KNOWN_HOSTS = {
        'mcs.ryanstudio.work': {
            bluemapUrl: 'https://mcsmap.ryanstudio.work/',
            webApiUrl: 'https://mcs.ryanstudio.work'
        },
        'mcsmap.ryanstudio.work': {
            bluemapUrl: 'https://mcsmap.ryanstudio.work/',
            webApiUrl: 'https://mcs.ryanstudio.work'
        }
    };

    function defaultWebApiUrl() {
        if (KNOWN_HOSTS[host]) return KNOWN_HOSTS[host].webApiUrl;
        if (protocol === 'https:' && host !== '127.0.0.1' && host !== 'localhost') {
            return `${protocol}//${host}`;
        }
        return `${protocol}//${host}:${webApiPort}`;
    }

    function defaultBluemapUrl() {
        if (KNOWN_HOSTS[host]) return KNOWN_HOSTS[host].bluemapUrl;
        return `${protocol}//${host}:${bluemapPort}/`;
    }

    function applyConfig(cfg) {
        window.MCWWS_SERVICES = {
            bluemapPort: cfg.bluemapPort || bluemapPort,
            bluemapUrl: cfg.bluemapUrl || defaultBluemapUrl(),
            webApiUrl: cfg.webApiUrl || defaultWebApiUrl()
        };
        window.dispatchEvent(new CustomEvent('mcwws-services-config', {
            detail: window.MCWWS_SERVICES
        }));
    }

    applyConfig({
        bluemapPort,
        bluemapUrl: defaultBluemapUrl(),
        webApiUrl: defaultWebApiUrl()
    });

    fetch('/api/services-config', { cache: 'no-store' })
        .then((res) => (res.ok ? res.json() : null))
        .then((data) => {
            if (data && data.bluemapUrl) {
                applyConfig(data);
            }
        })
        .catch(() => { /* 使用默认映射 */ });
})();
