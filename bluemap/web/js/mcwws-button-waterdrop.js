import { glass, supportsBackdropSvg } from './glassy-liquid-0.1.0.js';

const reduced = matchMedia('(prefers-reduced-motion: reduce), (prefers-reduced-transparency: reduce), (forced-colors: active)');
const selector = 'button, [role="button"], input:is([type="button"], [type="submit"], [type="reset"]), a:is(.primary, .primary-btn, .btn, .nav-btn, .mcwws-auth-ledger-btn)';
let press;

function control(target) {
    const button = target instanceof Element ? target.closest(selector) : null;
    if (button?.matches(':disabled, [aria-disabled="true"], [data-slider] > :is(a, button), #categoryTabs > .category-btn')) return null;
    return button;
}

function dispose(state) {
    state.shape.cancel();
    state.lens?.destroy();
    state.drop.remove();
}

function begin(button, pointerId, key) {
    if (!button || reduced.matches) return;
    if (press) dispose(press);
    const rect = button.getBoundingClientRect();
    if (!rect.width || !rect.height) { press = null; return; }
    const drop = document.createElement('span');
    drop.className = 'mcwws-button-drop';
    drop.setAttribute('aria-hidden', 'true');
    Object.assign(drop.style, {
        left: rect.left + 'px', top: rect.top + 'px',
        width: rect.width + 'px', height: rect.height + 'px'
    });
    (button.closest('dialog:modal') ?? document.body).append(drop);
    const lens = supportsBackdropSvg() ? glass(drop, {
        appearance: 'dark', material: 'clear', radius: 'capsule', profile: 'squircle',
        bezel: 14, refraction: 12, blur: .32, zoom: 1, dispersion: 0, specular: 0, shadow: false,
        fill: 'rgba(225,245,235,.06)'
    }) : null;
    const shape = drop.animate([
        { transform: 'translateY(0) scale(.96,.92)', opacity: 0 },
        { transform: 'translateY(-3px) scale(1.12,1.12)', opacity: 1, offset: .39, easing: 'cubic-bezier(.22,.75,.3,1)' },
        { transform: 'translateY(-2px) scale(1.22,.88)', opacity: 1 }
    ], { duration: 340, fill: 'forwards', easing: 'ease-out' });
    press = { drop, lens, shape, rect, pointerId, key };
}

function release() {
    if (!press) return;
    const state = press;
    press = null;
    const style = getComputedStyle(state.drop);
    const from = { transform: style.transform, opacity: style.opacity };
    state.shape.cancel();
    state.shape = state.drop.animate([
        from,
        { transform: 'translateY(1px) scale(1.04,.96)', opacity: .36, offset: .55 },
        { transform: 'translateY(0) scale(1)', opacity: 0 }
    ], { duration: 240, easing: 'ease-out', fill: 'forwards' });
    state.shape.onfinish = () => dispose(state);
}

document.addEventListener('pointerdown', event => {
    if (event.isPrimary && event.button === 0) begin(control(event.target), event.pointerId);
}, true);
window.addEventListener('pointermove', event => {
    if (!press || event.pointerId !== press.pointerId) return;
    const { rect, drop } = press;
    const pull = delta => 6 * Math.tanh(delta / 24);
    drop.style.translate = pull(event.clientX - rect.left - rect.width / 2) + 'px '
        + pull(event.clientY - rect.top - rect.height / 2) + 'px';
}, { passive: true });
for (const type of ['pointerup', 'pointercancel', 'lostpointercapture']) {
    window.addEventListener(type, event => { if (press?.pointerId === event.pointerId) release(); }, true);
}
document.addEventListener('keydown', event => {
    if (!event.repeat && (event.key === ' ' || event.key === 'Enter')) begin(control(event.target), undefined, event.key);
}, true);
document.addEventListener('keyup', event => { if (press?.key === event.key) release(); }, true);
window.addEventListener('blur', release);
reduced.addEventListener('change', release);
