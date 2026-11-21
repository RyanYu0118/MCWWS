const toolbar = document.querySelector('.shop-workspace-actions');
const backdrop = document.querySelector('.landscape');
const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');

for (const button of toolbar.querySelectorAll('button[data-shop-action="refresh"]')) {
    let press;
    let dragged = false;

    function follow(event, held) {
        const { width, height } = button.getBoundingClientRect();
        const rect = press?.rect ?? button.getBoundingClientRect();
        const dx = event.clientX - rect.left - rect.width / 2;
        const dy = event.clientY - rect.top - rect.height / 2;
        const limit = held ? 7 : 3;
        const pull = offset => limit * Math.tanh(offset / (held ? 20 : 25));
        button.style.setProperty('--pull-x', `${pull(dx)}px`);
        button.style.setProperty('--pull-y', `${pull(dy)}px`);
        button.style.setProperty('--stretch-x', held ? 0.95 + Math.min(.19, Math.abs(dx) / (width * 6)) : 1);
        button.style.setProperty('--stretch-y', held ? 0.95 + Math.min(.19, Math.abs(dy) / (height * 6)) : 1);
    }

    function release() {
        press = null;
        delete button.dataset.held;
        for (const name of ['--pull-x', '--pull-y', '--stretch-x', '--stretch-y']) button.style.removeProperty(name);
    }

    button.addEventListener('pointerdown', event => {
        if (event.button !== 0 || !event.isPrimary || button.disabled) return;
        dragged = false;
        press = { id: event.pointerId, x: event.clientX, y: event.clientY, rect: button.getBoundingClientRect() };
        button.setPointerCapture(event.pointerId);
        if (!reducedMotion.matches) {
            button.dataset.held = '';
            follow(event, true);
        }
    });
    button.addEventListener('pointermove', event => {
        const held = press?.id === event.pointerId;
        if (held && Math.hypot(event.clientX - press.x, event.clientY - press.y) > 7) dragged = true;
        if (!button.disabled && !reducedMotion.matches && (held || event.pointerType === 'mouse')) follow(event, held);
    });
    for (const type of ['pointerup', 'pointercancel', 'lostpointercapture']) {
        button.addEventListener(type, event => { if (event.pointerId === press?.id) release(); });
    }
    button.addEventListener('pointerleave', () => { if (!press) release(); });
    button.addEventListener('click', event => {
        if (dragged && event.detail > 0) {
            dragged = false;
            event.preventDefault();
            event.stopPropagation();
        }
    });
}

// Sample the shared wallpaper at the controls, using its current CSS crop.
const image = new Image();
const context = document.createElement('canvas').getContext('2d', { willReadFrequently: true });
function offset(position, available) {
    return [...position.matchAll(/([+-]?)\s*(\d+(?:\.\d+)?)(%|px)/g)].reduce((total, [, sign, value, unit]) =>
        total + (sign === '-' ? -1 : 1) * Number(value) * (unit === '%' ? available / 100 : 1), 0);
}
function updateInk() {
    if (!context || !image.naturalWidth || !toolbar.getClientRects().length) return;
    const bounds = backdrop.getBoundingClientRect();
    const style = getComputedStyle(backdrop);
    const width = Math.max(bounds.width, innerHeight * 2.45);
    const height = width * image.naturalHeight / image.naturalWidth;
    const left = bounds.left + offset(style.backgroundPositionX.split(',')[2] || '50%', bounds.width - width);
    const top = bounds.top + offset(style.backgroundPositionY.split(',')[2] || '10%', bounds.height - height);
    for (const button of toolbar.querySelectorAll('button[data-shop-action]')) {
        if (button.hidden) continue;
        const rect = button.getBoundingClientRect();
        const x = (rect.left + rect.width / 2 - left) / width * image.naturalWidth;
        const y = (rect.top + rect.height / 2 - top) / height * image.naturalHeight;
        if (x < 0 || y < 0 || x >= image.naturalWidth || y >= image.naturalHeight) {
            delete button.dataset.ink;
            continue;
        }
        const sampleX = Math.floor(x / image.naturalWidth * context.canvas.width);
        const sampleY = Math.floor(y / image.naturalHeight * context.canvas.height);
        const [r, g, b] = context.getImageData(sampleX, sampleY, 1, 1).data;
        const brightness = .2126 * r + .7152 * g + .0722 * b;
        button.dataset.ink = brightness >= (button.dataset.ink === 'dark' ? 150 : 165) ? 'dark' : 'light';
    }
}
image.addEventListener('load', () => {
    if (!context) return;
    context.canvas.width = 320;
    context.canvas.height = Math.max(1, Math.round(320 * image.naturalHeight / image.naturalWidth));
    context.drawImage(image, 0, 0, context.canvas.width, context.canvas.height);
    updateInk();
});
image.src = new URL('./services-world-night.png', import.meta.url);
new ResizeObserver(updateInk).observe(toolbar);
backdrop.addEventListener('transitionend', updateInk);
window.addEventListener('resize', updateInk);
