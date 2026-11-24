// This worker is registered on the map origin so it can also cover embedded views.
if (window.isSecureContext && 'serviceWorker' in navigator) {
    navigator.serviceWorker.register('./mcwws-map-sw.js', { updateViaCache: 'none' }).catch(error => {
        console.debug('[map cache] unavailable', error.message);
    });
}
