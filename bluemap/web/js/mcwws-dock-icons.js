const icons = {
    search: '<circle cx="10.5" cy="10.5" r="5.5"/><path d="m15 15 5 5"/>',
    cart: '<path d="M3 4h2l2.5 11h11L21 7H6"/><circle cx="9" cy="19" r="1.25"/><circle cx="17" cy="19" r="1.25"/>',
    map: '<path d="m3 5 6-2 6 2 6-2v16l-6 2-6-2-6 2V5Zm6-2v16m6-14v16"/>',
    plus: '<path d="M5 12h14M12 5v14"/>',
    minus: '<path d="M5 12h14"/>',
    close: '<path d="m6 6 12 12M18 6 6 18"/>',
    refresh: '<path d="M20 10a8 8 0 1 0-.9 6M20 4v6h-6"/>',
    pin: '<path d="M19 9c0 5-7 11-7 11S5 14 5 9a7 7 0 1 1 14 0Z"/><circle cx="12" cy="9" r="2"/>',
    target: '<circle cx="12" cy="12" r="6"/><path d="M12 3v4m0 10v4M3 12h4m10 0h4"/>',
    cube: '<path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Zm0 9 8-4.5M12 12 4 7.5M12 12v9"/>',
    chart: '<path d="M4 4v16h16M8 16v-4m4 4V8m4 8v-6"/>',
    store: '<path d="M4 10h16l-2-5H6l-2 5Zm1 0v9h14v-9m-9 9v-5h4v5"/>',
    collapse: '<path d="m6 15 6-6 6 6"/>',
    fullscreen: '<path d="M9 4H4v5m11-5h5v5M4 15v5h5m11-5v5h-5"/>',
    layers: '<path d="m12 4 8 4-8 4-8-4 8-4Zm-8 8 8 4 8-4M4 16l8 4 8-4"/>',
    fly: '<path d="m3 11 17-7-7 17-2.5-7.5L3 11Zm7.5 2.5L20 4"/>',
    daynight: '<g class="mcwws-daynight-sun"><circle cx="12" cy="12" r="4"/><path d="M12 2v3m0 14v3M2 12h3m14 0h3M5 5l2 2m10 10 2 2M5 19l2-2M17 7l2-2"/></g><path class="mcwws-daynight-moon" d="M20 15A8 8 0 0 1 9 4a8 8 0 1 0 11 11Z"/>',
};
const targets = new Map([
    ['.mcwws-dock-brand', 'map'], ['.mcwws-dock-search-btn', 'search'], ['.mcwws-dock-economy-btn', 'cart'],
    ['.mcwws-ctrl-zoom-in', 'plus'], ['.mcwws-ctrl-zoom-out', 'minus'], ['.mcwws-layer-sheet-close', 'close'],
    ['.mcwws-ctrl-locate-icon', 'pin'], ['.mcwws-ctrl-fs-icon', 'fullscreen'], ['.mcwws-ctrl-layer-icon', 'layers'],
    ['.mcwws-ctrl-fly-icon', 'fly'], ['.mcwws-ctrl-daynight-icon', 'daynight'],
]);
for (const [action, icon] of Object.entries({
    'economy-items': 'cart', 'economy-search': 'search', 'flat-overview': 'map', perspective: 'cube',
    'economy-dashboard': 'chart', 'list-all': 'store', 'list-map': 'pin', 'flat-nearest': 'target',
    refresh: 'refresh', 'item-on-map': 'search', collapse: 'collapse',
})) {
    targets.set(`[data-action="${action}"] :is(.mcwws-dock-tile-icon, .mcwws-dock-shortcut-icon)`, icon);
}

function applyIcons() {
    for (const [selector, name] of targets) {
        for (const target of document.querySelectorAll(selector)) {
            if (target.hasAttribute('data-mcwws-outline')) continue;
            target.dataset.mcwwsOutline = '';
            if (target.matches('button') && !target.hasAttribute('aria-label')) {
                target.setAttribute('aria-label', target.title || target.textContent.trim());
            }
            const slider = target.querySelector(':scope > .dock-slider');
            if (target instanceof SVGElement) {
                target.classList.add('mcwws-map-outline-icon');
                target.innerHTML = icons[name];
            } else {
                target.innerHTML = `<svg class="mcwws-map-outline-icon" viewBox="0 0 24 24" aria-hidden="true">${icons[name]}</svg>`;
                if (slider) target.prepend(slider);
            }
        }
    }
}

let frame = 0;
new MutationObserver(records => {
    if (frame || !records.some(record => [...record.addedNodes].some(node =>
        node instanceof HTMLElement && !node.matches('.dock-slider, .mcwws-button-drop, .mcwws-function-dock-slider')))) return;
    frame = requestAnimationFrame(() => { frame = 0; applyIcons(); });
}).observe(document.body, { childList: true, subtree: true });
applyIcons();
