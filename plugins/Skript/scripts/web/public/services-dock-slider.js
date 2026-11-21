import {
  addTask, clamp, glass, rubberBand, Spring, springs, tapSlop, wake,
} from './glassy-liquid-0.1.0.js';
import { dockThumbMaterial } from './dock-material.js';

const settle = { duration: 0.46, bounce: 0.42 };
const jelly = { duration: 0.24, bounce: 0.23 };

export function mountDockSlider(dock) {
  const slider = dock.querySelector('.dock-slider');
  const items = [...dock.querySelectorAll(':scope > a, :scope > button')];
  const lens = glass(slider, dockThumbMaterial);
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const position = new Spring();
  const width = new Spring();
  const lift = new Spring();
  const stretch = new Spring(0, jelly);
  let geometry;
  let lensWidth;
  let refracting = false;
  let gesture;
  let ignorePointerClick = false;

  const animation = {
    write(_now, dt) {
      const reduced = reducedMotion.matches;
      for (const spring of [position, width, lift]) {
        if (reduced) spring.set(spring.target);
        else spring.step(dt);
      }

      const rise = reduced ? 0 : clamp(lift.value, 0, 1.14);
      const elongation = Math.min(0.38, Math.abs(position.velocity) * 0.0008) * rise;
      if (reduced) stretch.set(0);
      else stretch.to(elongation).step(dt);
      const deformation = clamp(stretch.value, -0.06, 0.40);
      const scaleX = (width.value / lensWidth) * (1 + 0.22 * rise) * (1 + deformation);
      const scaleY = (1 + 0.10 * rise) / Math.sqrt(1 + deformation);
      const x = position.value - lensWidth / 2;
      const peel = rise * 2;
      slider.style.transform = `translate3d(${x.toFixed(2)}px, ${(-peel).toFixed(2)}px, 0) scale(${scaleX.toFixed(4)}, ${scaleY.toFixed(4)})`;
      slider.style.setProperty('--slider-lift', clamp(rise, 0, 1));
      dock.toggleAttribute('data-drop-lifted', rise > 0.025);
      for (const { item, center } of geometry ?? []) {
        const distance = center - position.value;
        const pull = rise * 32 / (32 + Math.abs(distance));
        const bend = Math.tanh(distance / 24) * pull;
        if (rise === 0) item.style.removeProperty('transform');
        else item.style.transform = `translate(${-bend * 5}px, ${-pull * 3}px) skewX(${bend * 9}deg) scale(${1 + pull * .13}, ${1 - pull * .09})`;
      }
      if (refracting) lens.setOpacity(clamp(rise * 1.5, 0, 1));
      if (refracting && (reduced || (lift.settled && lift.target === 0))) {
        lens.set({ backdrop: false });
        refracting = false;
      }
      return !position.settled || !width.settled || !lift.settled || !stretch.settled;
    },
  };
  addTask(animation);

  function currentItem() {
    return dock.querySelector('[aria-expanded="true"], [aria-pressed="true"]') ?? dock.querySelector('[aria-current]');
  }

  function place(item, config = springs.layout) {
    const target = geometry?.find(entry => entry.item === item);
    if (!target) return;
    if (position.target !== target.center) position.to(target.center, config);
    if (width.target !== target.width) width.to(target.width, config);
    wake(animation);
  }

  function syncSelection() {
    if (!gesture) place(currentItem());
  }

  function measure() {
    if (!dock.offsetWidth) return;
    const firstMeasure = !geometry;
    geometry = items.filter(item => item.offsetWidth)
      .map(item => ({ item, width: item.offsetWidth, center: item.offsetLeft + item.offsetWidth / 2 }));
    if (!geometry.length) return;
    lensWidth = Math.max(...geometry.map(entry => entry.width));
    slider.style.width = `${lensWidth}px`;
    if (firstMeasure) {
      const selected = geometry.find(entry => entry.item === currentItem()) ?? geometry[0];
      position.set(selected.center);
      width.set(selected.width);
      animation.write(0, 0);
    }
    syncSelection();
  }

  function localX(clientX) {
    const rect = dock.getBoundingClientRect();
    return ((clientX - rect.left) / rect.width) * dock.offsetWidth - dock.clientLeft;
  }

  function nearest(x) {
    return geometry.reduce((a, b) => Math.abs(b.center - x) < Math.abs(a.center - x) ? b : a);
  }

  function widthAt(x) {
    const nextIndex = geometry.findIndex(entry => entry.center >= x);
    if (nextIndex < 0) return geometry.at(-1).width;
    const next = geometry[nextIndex];
    if (nextIndex === 0) return next.width;
    const previous = geometry[nextIndex - 1];
    const fraction = (x - previous.center) / (next.center - previous.center);
    return previous.width + (next.width - previous.width) * fraction;
  }

  dock.addEventListener('pointerdown', event => {
    if (event.button !== 0 || !event.isPrimary || !geometry || !items.some(item => item.contains(event.target))) return;
    ignorePointerClick = false;
    const x = localX(event.clientX);
    const origin = geometry.find(entry => entry.item.contains(event.target)) ?? nearest(x);
    gesture = {
      id: event.pointerId,
      x: event.clientX,
      y: event.clientY,
      offset: origin.center - x,
      lastX: event.clientX,
      lastTime: event.timeStamp,
      velocity: 0,
      dragging: false,
    };
    place(origin.item, springs.press);
    lift.to(1, springs.press);
    if (!refracting && dock.dataset.optics === 'lens' && !reducedMotion.matches) {
      lens.setOpacity(0);
      lens.set({ backdrop: true });
      refracting = true;
    }
    wake(animation);
  });

  window.addEventListener('pointermove', event => {
    if (gesture?.id !== event.pointerId) return;
    if (!gesture.dragging) {
      const dx = Math.abs(event.clientX - gesture.x);
      const dy = Math.abs(event.clientY - gesture.y);
      const slop = tapSlop(event);
      if (dy > slop && dy > dx) {
        finish(event, true);
        return;
      }
      if (dx < slop) return;
      gesture.dragging = true;
      dock.setPointerCapture(event.pointerId);
      dock.dataset.sliding = '';
    }
    const elapsed = Math.max(8, event.timeStamp - gesture.lastTime);
    const velocity = clamp(1000 * (event.clientX - gesture.lastX) / elapsed, -2200, 2200);
    gesture.velocity = elapsed > 90 ? velocity : 0.48 * velocity + 0.52 * gesture.velocity;
    gesture.lastX = event.clientX;
    gesture.lastTime = event.timeStamp;
    const raw = localX(event.clientX) + gesture.offset;
    const end = clamp(raw, geometry[0].center, geometry.at(-1).center);
    const tracked = reducedMotion.matches ? end : end + rubberBand(raw - end, 16);
    position.to(tracked, springs.track);
    width.to(widthAt(end), springs.track);
    wake(animation);
  }, { passive: true });

  function finish(event, cancelled) {
    if (gesture?.id !== event.pointerId) return;
    const { dragging, offset, velocity, lastTime } = gesture;
    gesture = null;
    delete dock.dataset.sliding;
    lift.to(0, springs.release);
    wake(animation);
    if (cancelled || !dragging) {
      syncSelection();
      return;
    }
    const momentum = reducedMotion.matches ? 0 : velocity * Math.exp(-Math.max(0, event.timeStamp - lastTime) / 65);
    const projected = localX(event.clientX) + offset + clamp(momentum * 0.035, -28, 28);
    const selected = nearest(projected);
    position.velocity = clamp(0.65 * position.velocity + 0.35 * momentum, -1300, 1300);
    position.to(selected.center, settle);
    width.to(selected.width, settle);
    ignorePointerClick = true;
    selected.item.focus({ preventScroll: true });
    selected.item.click();
  }

  window.addEventListener('pointerup', event => finish(event, false));
  window.addEventListener('pointercancel', event => finish(event, true));
  dock.addEventListener('lostpointercapture', event => {
    // Touch starts with implicit capture on the item, then transfers it to the dock.
    if (event.target === dock) finish(event, true);
  });
  dock.addEventListener('click', event => {
    if (ignorePointerClick && event.detail > 0) {
      ignorePointerClick = false;
      event.preventDefault();
      event.stopImmediatePropagation();
      return;
    }
    const item = event.target.closest('a, button');
    if (items.includes(item)) place(item);
  }, { capture: true });

  const selectionObserver = new MutationObserver(syncSelection);
  selectionObserver.observe(dock, { subtree: true, attributes: true, attributeFilter: ['aria-current', 'aria-expanded', 'aria-pressed'] });
  new ResizeObserver(measure).observe(dock);
  for (const item of items) new ResizeObserver(measure).observe(item);
  reducedMotion.addEventListener('change', () => wake(animation));
  dock.dataset.slider = '';
  slider.hidden = false;
  measure();
}
