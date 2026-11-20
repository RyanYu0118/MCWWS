import { attachPress, glass, supportsBackdropSvg } from './glassy-liquid-0.1.0.js';
import { dockMaterial } from './dock-material.js';

const root = document.documentElement;
const preferSolid = matchMedia('(prefers-reduced-transparency: reduce), (forced-colors: active)');
const targets = '.glass, .item-card, .shop-item-card, .health-card, .trade-offer-card, .cart-drawer, .cart-delivery-menu, .mcwws-auth-popover-body, .hero-search, .filter-select';
const mounted = new Map();
const observed = new Set();
const dockPresses = new Map();
let backdrop;
let frame = 0;

function unmount(element) {
    dockPresses.get(element)?.dispose();
    dockPresses.delete(element);
    mounted.get(element)?.destroy();
    mounted.delete(element);
    delete element.dataset.shopLens;
}

function syncBackdrop() {
    if (!backdrop) return;
    try {
        const landscape = parent.document.querySelector('.landscape');
        if (landscape) {
            const style = parent.getComputedStyle(landscape);
            for (const property of ['backgroundImage', 'backgroundSize', 'backgroundRepeat', 'backgroundPosition']) {
                backdrop.style[property] = style[property];
            }
        }
    } catch {
        return;
    }
}

const visibility = new IntersectionObserver(entries => {
    for (const { target, isIntersecting } of entries) {
        if (!isIntersecting || preferSolid.matches) {
            unmount(target);
        } else if (!mounted.has(target)) {
            const dense = target.matches('.cart-drawer, .cart-delivery-menu, .modal-content, .mcwws-auth-popover-body');
            const categoryDock = target.matches('.category-section');
            const surface = glass(target, categoryDock ? dockMaterial : {
                material: 'clear', appearance: 'dark', profile: 'squircle',
                radius: dense ? 22 : 16, bezel: 18, refraction: 48, blur: .55,
                zoom: 1, dispersion: 0, specular: 0, shadow: false,
                fill: dense ? 'rgba(22,32,28,.82)' : 'rgba(29,43,38,.48)',
            });
            mounted.set(target, surface);
            if (categoryDock) dockPresses.set(target, attachPress(target, surface, { grow: .055 }));
            target.dataset.shopLens = '';
        }
    }
});

function scan() {
    frame = 0;
    for (const element of observed) {
        if (element.isConnected) continue;
        visibility.unobserve(element);
        observed.delete(element);
        unmount(element);
    }
    for (const element of document.querySelectorAll(targets)) {
        if (observed.has(element) || element.matches('input, select, textarea')) continue;
        observed.add(element);
        visibility.observe(element);
    }
}

if (supportsBackdropSvg()) {
    if (root.classList.contains('mcwws-embedded-shop-root')) {
        backdrop = document.createElement('div');
        backdrop.className = 'shop-refraction-backdrop';
        backdrop.setAttribute('aria-hidden', 'true');
        document.body.prepend(backdrop);
        syncBackdrop();
        window.addEventListener('resize', syncBackdrop);
        window.addEventListener('focus', syncBackdrop);
        try {
            const landscape = parent.document.querySelector('.landscape');
            if (landscape) {
                landscape.addEventListener('transitionend', syncBackdrop);
                window.addEventListener('pagehide', () => landscape.removeEventListener('transitionend', syncBackdrop), { once: true });
            }
        } catch {
            syncBackdrop();
        }
    }
    new MutationObserver(records => {
        if (!frame && records.some(record => [...record.addedNodes, ...record.removedNodes].some(node =>
            node instanceof Element && (node.matches(targets) || node.querySelector(targets))
        ))) frame = requestAnimationFrame(scan);
    }).observe(document.body, { childList: true, subtree: true });
    preferSolid.addEventListener('change', () => {
        for (const element of observed) {
            visibility.unobserve(element);
            unmount(element);
            visibility.observe(element);
        }
    });
    scan();
}
