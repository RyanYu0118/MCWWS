const PREFIX = 'mcwws-map-';
const ASSETS = PREFIX + 'assets-v1';
const TILES = PREFIX + 'tiles-v1';
const MAX_TILES = 320;
const MAX_TILE_BYTES = 384 * 1024;
const MAX_ASSETS = 48;
const MAX_ASSET_BYTES = 12 * 1024 * 1024;
const SCOPE = new URL(self.registration.scope);
const INDEX = new URL('index.html', SCOPE).href;
let writer = Promise.resolve();

self.addEventListener('install', event => event.waitUntil((async () => {
    // Capture the entry page even when the first navigation predates worker control.
    try {
        const cache = await caches.open(ASSETS);
        await cache.add(new Request(INDEX, { cache: 'reload' }));
    } catch {
        // An online map still works if storage is unavailable.
    }
    await self.skipWaiting();
})()));
self.addEventListener('activate', event => event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(key => key.startsWith(PREFIX) && key !== ASSETS && key !== TILES)
        .map(key => caches.delete(key)));
    await self.clients.claim();
})()));

function enqueueSave(cacheName, key, response, limit, maxBytes) {
    const length = Number(response.headers.get('content-length'));
    if (length > maxBytes) return Promise.resolve();
    if (cacheName === TILES && response.headers.get('content-type')?.includes('text/html')) return Promise.resolve();
    const copy = response.clone();
    // Check actual size before entering the write queue, including chunked responses.
    return (async () => {
        const stored = copy.clone();
        const reader = copy.body?.getReader();
        if (!reader) return;
        let bytes = 0;
        try {
            for (;;) {
                const { value, done } = await reader.read();
                if (done) break;
                bytes += value.byteLength;
                if (bytes > maxBytes) {
                    void reader.cancel();
                    void stored.body?.cancel();
                    return;
                }
            }
        } finally {
            reader.releaseLock();
        }
        const save = writer.then(async () => {
            const cache = await caches.open(cacheName);
            await cache.put(key, stored);
            const keys = await cache.keys();
            await Promise.all(keys.slice(0, Math.max(0, keys.length - limit)).map(entry => cache.delete(entry)));
        });
        writer = save.catch(() => {});
        await save;
    })();
}

function serve(event, cacheName, key, { cacheFirst = false, limit = MAX_ASSETS, maxBytes = MAX_ASSET_BYTES } = {}) {
    let pendingSave;
    const result = (async () => {
        const cached = await caches.match(key, { cacheName }).catch(() => undefined);
        if (cacheFirst && cached) return cached;
        try {
            const response = await fetch(event.request);
            if (response.status === 200 && response.type === 'basic') {
                pendingSave = enqueueSave(cacheName, key, response, limit, maxBytes).catch(() => {});
            }
            if (response.status >= 500 && cached) return cached;
            return response;
        } catch (error) {
            if (cached) return cached;
            throw error;
        }
    })();
    event.respondWith(result);
    event.waitUntil(result.then(() => pendingSave, () => {}));
}

self.addEventListener('fetch', event => {
    const request = event.request;
    const url = new URL(request.url);
    if (request.method !== 'GET' || url.origin !== SCOPE.origin || !url.pathname.startsWith(SCOPE.pathname)
        || request.headers.has('range') || request.cache === 'no-store') return;
    const path = url.pathname.slice(SCOPE.pathname.length);
    if (request.mode === 'navigate' && (path === '' || path === 'index.html')) {
        serve(event, ASSETS, INDEX);
        return;
    }
    if (/^maps\/[^/]+\/tiles\//.test(path)) {
        const versionedSurface = path.startsWith('maps/world_surface/') && url.searchParams.has('mcwwsSurface');
        serve(event, TILES, request, {
            cacheFirst: versionedSurface && !['reload', 'no-cache'].includes(request.cache),
            limit: MAX_TILES, maxBytes: MAX_TILE_BYTES,
        });
        return;
    }
    if (/^(?:assets|css|js|lang)\/.+\.(?:js|css|woff2|ttf|png|svg|json|webmanifest)$/i.test(path)
        || /^(?:settings\.json|maps\/[^/]+\/(?:settings\.json|textures\.json(?:\.gz)?|assets\/.+\.(?:png|jpg|webp)))$/.test(path)) {
        serve(event, ASSETS, request, {
            cacheFirst: (path.startsWith('assets/') || url.searchParams.has('v'))
                && !['reload', 'no-cache'].includes(request.cache),
        });
    }
});
