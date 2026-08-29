/* MCWWS PWA：满足可安装条件；接口与 APK 走网络，避免缓存过期物价。 */
const CACHE_NAME = 'mcwws-pwa-v1';
const OFFLINE_URL = '/offline.html';

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => cache.addAll([OFFLINE_URL, '/icons/icon-192.png']))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) => Promise.all(
            keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
        )).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    if (request.method !== 'GET') {
        return;
    }
    let url;
    try {
        url = new URL(request.url);
    } catch (_) {
        return;
    }
    if (url.origin !== self.location.origin) {
        return;
    }
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/app/')) {
        return;
    }

    if (request.mode === 'navigate') {
        event.respondWith(
            fetch(request).catch(() => caches.match(OFFLINE_URL).then((cached) => cached || Response.error()))
        );
        return;
    }
});
