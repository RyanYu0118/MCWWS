/* 必须在 BlueMap 主模块之前执行。
 * 主模块是 defer 的，会先把出生点一带的瓦片请求发出去。
 * 图层脚本要等地图起来才插入，拦不住这第一批。
 * 正 X、正 Z 正好是行星发动机和默认视角，所以只有那边还显示完整图。
 */
(function mcwwsInstallTileRoute() {
    if (window.__mcwwsTileRouteInstalled) {
        return;
    }
    window.__mcwwsTileRouteInstalled = true;
    const STORAGE_KEY = 'mcwws-gis-render-above-320';

    function renderAbove320() {
        try {
            return localStorage.getItem(STORAGE_KEY) === '1';
        } catch (err) {
            return false;
        }
    }

    function rewriteWorldTileUrl(url) {
        const text = String(url || '');
        if (renderAbove320() || !text.includes('maps/world/tiles/')) {
            return text;
        }
        if (text.includes('maps/world_surface/tiles/')) {
            return text;
        }
        return text.replace('maps/world/tiles/', 'maps/world_surface/tiles/');
    }

    const origFetch = window.fetch;
    if (typeof origFetch === 'function') {
        window.fetch = function mcwwsTileFetch(input, init) {
            const original = typeof input === 'string'
                ? input
                : (input && typeof input.url === 'string' ? input.url : '');
            const rewritten = rewriteWorldTileUrl(original);
            if (!original || rewritten === original) {
                return origFetch.apply(this, arguments);
            }
            const request = typeof input === 'string'
                ? rewritten
                : new Request(rewritten, input.clone());
            return origFetch.call(this, request, init).then((response) => {
                if (response.status !== 404 && response.status !== 204) {
                    return response;
                }
                const fallback = typeof input === 'string'
                    ? input
                    : new Request(original, input.clone());
                return origFetch.call(this, fallback, init);
            });
        };
    }

    const imgSrc = Object.getOwnPropertyDescriptor(HTMLImageElement.prototype, 'src');
    if (imgSrc && imgSrc.set && imgSrc.get) {
        Object.defineProperty(HTMLImageElement.prototype, 'src', {
            configurable: true,
            enumerable: imgSrc.enumerable,
            get() {
                return imgSrc.get.call(this);
            },
            set(value) {
                const original = String(value || '');
                const rewritten = rewriteWorldTileUrl(original);
                if (rewritten !== original) {
                    this.addEventListener('error', function mcwwsImgFallback() {
                        this.removeEventListener('error', mcwwsImgFallback);
                        imgSrc.set.call(this, original);
                    });
                }
                imgSrc.set.call(this, rewritten);
            }
        });
    }
})();
