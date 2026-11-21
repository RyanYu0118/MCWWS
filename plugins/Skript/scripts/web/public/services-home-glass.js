import { attachPress, glass, supportsBackdropSvg } from './glassy-liquid-0.1.0.js';
import { dockMaterial } from './dock-material.js';
import { mountDockSlider } from './services-dock-slider.js?v=6';

const root = document.documentElement;
const presets = {
  soft: { bezel: 12, refraction: 17, blur: 2.2, zoom: 1.01, dispersion: 0 },
  balanced: { bezel: 17, refraction: 31, blur: 0.75, zoom: 1.045, dispersion: 0 },
  vivid: { bezel: 22, refraction: 40, blur: 0.65, zoom: 1.08, dispersion: 0 },
};

const hasLens = supportsBackdropSvg();
const bars = [...document.querySelectorAll('.dock, .segmented')];
const surfaces = [...document.querySelectorAll('.dock, [data-liquid], .sample')].map(element => {
  if (!hasLens) return { element };
  const surface = glass(element, {
    material: 'clear',
    appearance: 'dark',
    profile: 'squircle',
    radius: element.matches('.dock, .segmented, .primary') ? 'capsule' : 28,
    specular: 0,
    fill: element.matches('.panel, .tip') ? 'rgba(29, 43, 38, 0.52)' : 'rgba(29, 43, 38, 0.10)',
    ...presets[root.dataset.intensity],
    ...(element.matches('.dock') ? { ...dockMaterial, ...presets[root.dataset.intensity] } : {}),
  });
  element.dataset.optics = 'lens';
  const press = element.matches('.dock, .segmented, button') ? attachPress(element, surface, { grow: 0.055 }) : null;
  return { element, surface, press };
});

function updateIntensity() {
  for (const { surface } of surfaces) surface?.set(presets[root.dataset.intensity]);
}

for (const bar of bars) {
  if (bar.hasAttribute('data-single-action')) bar.dataset.optics = hasLens ? 'lens' : 'frost';
  mountDockSlider(bar);
}

new MutationObserver(updateIntensity).observe(root, {
  attributes: true,
  attributeFilter: ['data-intensity'],
});

const intensityButtons = [...document.querySelectorAll('button[data-intensity]')];
for (const button of intensityButtons) {
  button.addEventListener('click', () => {
    root.dataset.intensity = button.dataset.intensity;
    for (const control of intensityButtons) {
      control.setAttribute('aria-pressed', String(control.dataset.intensity === root.dataset.intensity));
    }
  });
}

const panel = document.querySelector('#light-panel');
document.querySelector('#open-panel').addEventListener('click', () => panel.showModal());
panel.addEventListener('click', event => {
  if (event.target !== panel) return;
  const box = panel.getBoundingClientRect();
  if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) panel.close();
});

let pointerFrame = 0;
let hovered;
document.addEventListener('pointermove', event => {
  if (event.pointerType !== 'mouse' || pointerFrame) return;
  pointerFrame = requestAnimationFrame(() => {
    pointerFrame = 0;
    const target = event.target instanceof Element ? event.target.closest('[data-liquid], .dock') : null;
    if (hovered !== target) {
      hovered?.style.removeProperty('--x');
      hovered?.style.removeProperty('--y');
      hovered?.style.removeProperty('--hub-light-x');
      hovered?.style.removeProperty('--hub-light-y');
      hovered = target;
    }
    if (!target) return;
    const box = target.getBoundingClientRect();
    target.style.setProperty('--x', (event.clientX - box.left) + 'px');
    target.style.setProperty('--y', (event.clientY - box.top) + 'px');
    target.style.setProperty('--hub-light-x', (event.clientX - box.left) + 'px');
    target.style.setProperty('--hub-light-y', (event.clientY - box.top) + 'px');
  });
}, { passive: true });

let scrollFrame = 0;
function updateScroll() {
  scrollFrame = 0;
  const travel = Math.max(1, root.scrollHeight - root.clientHeight);
  const progress = Math.min(1, Math.max(0, window.scrollY / travel));
  root.style.setProperty('--landscape-pan', (10 + 80 * progress) + '%');
  root.style.setProperty('--scroll', Math.min(Math.max(window.scrollY / 190, 0), 1));
}
function scheduleScroll() {
  if (!scrollFrame) scrollFrame = requestAnimationFrame(updateScroll);
}
window.addEventListener('scroll', scheduleScroll, { passive: true });
window.addEventListener('resize', scheduleScroll);
updateScroll();

const hint = document.querySelector('#mcwwsPwaHint');
const inApp = /MCWWSApp/i.test(navigator.userAgent)
  || matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
if (inApp) {
  hint.hidden = true;
} else {
  window.addEventListener('mcwws-pwa-installable', () => {
    if (!window.MCWWS_PWA) return;
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'glass primary';
    button.textContent = '安装到桌面';
    button.addEventListener('click', () => window.MCWWS_PWA.promptInstall());
    hint.replaceChildren(button);
  });
}
