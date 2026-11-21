const toolbar = document.querySelector('.shop-workspace-actions');
const backdrop = document.querySelector('.landscape');

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
