(function () {
    const host = window.location.hostname || '127.0.0.1';
    const protocol = window.location.protocol || 'http:';
    const port = 8100;

    function applyConfig(cfg) {
        window.MCWWS_SERVICES = {
            bluemapPort: cfg.bluemapPort || port,
            bluemapUrl: cfg.bluemapUrl || `${protocol}//${host}:${cfg.bluemapPort || port}/`
        };
        window.dispatchEvent(new CustomEvent('mcwws-services-config', {
            detail: window.MCWWS_SERVICES
        }));
    }

    applyConfig({ bluemapPort: port, bluemapUrl: `${protocol}//${host}:${port}/` });

    fetch('/api/services-config', { cache: 'no-store' })
        .then((res) => (res.ok ? res.json() : null))
        .then((data) => {
            if (data && data.bluemapUrl) {
                applyConfig(data);
            }
        })
        .catch(() => { /* 使用默认端口 */ });
})();
