import { glass, supportsBackdropSvg } from './glassy-liquid-0.1.0.js';

const bars = '.mcwws-dock-search, .mcwws-ctrl-tools-cluster';
const reducedTransparency = matchMedia('(prefers-reduced-transparency: reduce), (forced-colors: active)');
const surfaces = new Map();
let frame = 0;

window.addEventListener('message', event => {
    if (event.source !== window.parent || ![location.origin, 'https://mcs.ryanstudio.work', 'https://www.mcs.ryanstudio.work'].includes(event.origin)) return;
    const { type, top, bottom } = event.data || {};
    if (type !== 'mcwws-workspace-insets' || !Number.isFinite(top) || !Number.isFinite(bottom)) return;
    document.documentElement.style.setProperty('--mcwws-workspace-top', `${Math.max(0, Math.min(240, top))}px`);
    document.documentElement.style.setProperty('--mcwws-workspace-bottom', `${Math.max(0, Math.min(240, bottom))}px`);
});

const contrastTargets = [
    '.mcwws-dock-brand', '.mcwws-dock-input-wrap', '.mcwws-dock-trade-toggle',
    '.mcwws-dock-search-btn', '.mcwws-dock-economy-btn', '.mcwws-ctrl-btn',
    '.mcwws-ctrl-fullscreen', '.mcwws-screen-pin',
].join(', ');
const sample = document.createElement('canvas');
sample.width = 192;
sample.height = 108;
const sampling = sample.getContext('2d', { willReadFrequently: true });

function linear(channel) {
    const value = channel / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
}

function updateContrast() {
    if (document.hidden || !sampling) return;
    const targets = document.querySelectorAll(contrastTargets);
    if (reducedTransparency.matches) {
        for (const target of targets) delete target.dataset.mcwwsInk;
        return;
    }
    const source = document.querySelector('#map-container canvas');
    if (!source?.width || !source.height) return;
    const bounds = source.getBoundingClientRect();
    if (!bounds.width || !bounds.height) return;
    let pixels;
    try {
        sampling.clearRect(0, 0, sample.width, sample.height);
        sampling.drawImage(source, 0, 0, sample.width, sample.height);
        pixels = sampling.getImageData(0, 0, sample.width, sample.height).data;
    } catch {
        return;
    }
    for (const target of targets) {
        const rect = target.getBoundingClientRect();
        if (!rect.width || !rect.height) continue;
        let sum = 0;
        let count = 0;
        for (const fractionX of [0.3, 0.5, 0.7]) {
            for (const fractionY of [0.3, 0.5, 0.7]) {
                const x = Math.floor((rect.left + rect.width * fractionX - bounds.left) * sample.width / bounds.width);
                const y = Math.floor((rect.top + rect.height * fractionY - bounds.top) * sample.height / bounds.height);
                if (x < 0 || x >= sample.width || y < 0 || y >= sample.height) continue;
                const offset = (y * sample.width + x) * 4;
                if (pixels[offset + 3] < 128) continue;
                sum += 0.2126 * linear(pixels[offset]) + 0.7152 * linear(pixels[offset + 1]) + 0.0722 * linear(pixels[offset + 2]);
                count++;
            }
        }
        if (!count) continue;
        const current = target.dataset.mcwwsInk;
        const threshold = current === 'dark' ? 0.25 : 0.30;
        const next = sum / count >= threshold ? 'dark' : 'light';
        if (next !== current) target.dataset.mcwwsInk = next;
    }
}

setInterval(updateContrast, 450);

const magneticControls = [
    '#mcwws-shop-panel button', '.mcwws-map-controls button',
    '.mcwws-layer-dialog button', '.mcwws-layer-menu button',
].join(', ');
const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
let magneticTarget;
let magneticPress;

function resetMagnet() {
    magneticTarget?.removeAttribute('data-mcwws-magnet');
    magneticTarget?.style.removeProperty('--mcwws-magnet-x');
    magneticTarget?.style.removeProperty('--mcwws-magnet-y');
    magneticTarget = undefined;
    magneticPress = undefined;
}

function magneticButton(event) {
    const target = event.target instanceof Element ? event.target.closest(magneticControls) : null;
    return target && !target.matches(':disabled, [aria-disabled="true"], [data-cart-dock-control], [data-dock-group-item]') ? target : null;
}

function moveMagnet(event, target) {
    if (!target || reducedMotion.matches) return resetMagnet();
    if (magneticTarget !== target) resetMagnet();
    magneticTarget = target;
    const rect = target.getBoundingClientRect();
    const computed = getComputedStyle(target).translate.split(' ').map(parseFloat);
    const centerX = rect.left + rect.width / 2 - (computed[0] || 0);
    const centerY = rect.top + rect.height / 2 - (computed[1] || 0);
    const limit = magneticPress ? 5 : 3;
    const amount = magneticPress ? 0.24 : 0.14;
    const offset = distance => `${Math.max(-limit, Math.min(limit, distance * amount))}px`;
    target.style.setProperty('--mcwws-magnet-x', offset(event.clientX - centerX));
    target.style.setProperty('--mcwws-magnet-y', offset(event.clientY - centerY));
    target.dataset.mcwwsMagnet = magneticPress ? 'held' : 'hover';
}

document.addEventListener('pointerdown', event => {
    if (event.button !== 0 || !event.isPrimary || reducedMotion.matches) return;
    const target = magneticButton(event);
    if (!target) return;
    resetMagnet();
    magneticTarget = target;
    magneticPress = event.pointerId;
    moveMagnet(event, target);
}, { passive: true, capture: true });

document.addEventListener('pointermove', event => {
    if (magneticPress !== undefined) {
        if (event.pointerId === magneticPress) moveMagnet(event, magneticTarget);
    } else if (event.pointerType === 'mouse' || event.pointerType === 'pen') {
        moveMagnet(event, magneticButton(event));
    }
}, { passive: true, capture: true });

for (const type of ['pointerup', 'pointercancel']) {
    window.addEventListener(type, event => {
        if (event.pointerId === magneticPress) resetMagnet();
    }, { capture: true, passive: true });
}
document.documentElement.addEventListener('pointerleave', () => {
    if (magneticPress === undefined) resetMagnet();
});
window.addEventListener('blur', resetMagnet);
reducedMotion.addEventListener('change', resetMagnet);

function syncSurfaces() {
    frame = 0;
    for (const [bar, surface] of surfaces) {
        if (!bar.isConnected || reducedTransparency.matches) {
            surface.destroy();
            delete bar.dataset.mcwwsOptics;
            surfaces.delete(bar);
        }
    }
    if (reducedTransparency.matches) return;
    for (const bar of document.querySelectorAll(bars)) {
        if (surfaces.has(bar)) continue;
        const surface = glass(bar, {
            material: 'clear', appearance: 'dark', profile: 'squircle',
            radius: 'capsule', specular: 0, fill: 'rgba(0, 0, 0, 0.10)',
            bezel: 22, refraction: 40, blur: 0.65, zoom: 1.08, dispersion: 0,
        });
        surfaces.set(bar, surface);
        bar.dataset.mcwwsOptics = 'lens';
    }
}

function schedule() {
    if (!frame) frame = requestAnimationFrame(syncSurfaces);
}

if (supportsBackdropSvg()) {
    new MutationObserver(records => {
        if (records.some(record => [...record.addedNodes, ...record.removedNodes].some(node =>
            node instanceof Element && (node.matches(bars) || node.querySelector(bars))
        ))) schedule();
    }).observe(document.body, { childList: true, subtree: true });
    reducedTransparency.addEventListener('change', schedule);
    schedule();
}
