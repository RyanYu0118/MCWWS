import { addTask, clamp, glass, Spring, springs, supportsBackdropSvg, wake } from './glassy-liquid-0.1.0.js';
import { dockThumbMaterial } from './dock-material.js';
import { dockThumbPose, mountDockSlider } from './services-dock-slider.js?v=7';

const controls = 'button, [role="button"], [role="tab"], input:is([type="button"], [type="submit"], [type="reset"]), a:is(.primary, .primary-btn, .btn, .nav-btn, .mcwws-auth-ledger-btn, .mcwws-auth-popover-services-link, .nav-link)';
const groups = '.auth-toggle, .leaderboard-tabs, .trends-tabs, .mcwws-ledger-filters, .nav-tabs, .letter-index-bar, .mcwws-ctrl-tools-cluster, .mcwws-gis-volume-shapes, [role="tablist"], [role="toolbar"]';
const excluded = '.dock > :is(a, button), .segmented > :is(a, button), #categoryTabs .category-btn';
const selected = '[aria-current]:not([aria-current="false"]), [aria-selected="true"], [aria-pressed="true"], .active, .is-active';
const reduce = matchMedia('(prefers-reduced-motion: reduce)');
const solid = matchMedia('(prefers-reduced-transparency: reduce), (forced-colors: active)');
const supportsLens = supportsBackdropSvg();

const mounted = new Map();
const singleControls = new Map();
const disabled = item => item.matches(':disabled, [aria-disabled="true"]');
const eligible = item => !item.matches(excluded);
let pending = 0;
let gesture;
let drop;
let lens;
let coveredRest;
let suppressUntil = 0;

function groupItems(group) {
    return [...group.querySelectorAll(controls)].filter(item =>
        eligible(item) && item.closest(groups) === group && item.getClientRects().length);
}

function refresh() {
    pending = 0;
    for (const [group, state] of mounted) {
        if (group.isConnected) continue;
        resize.unobserve(group);
        for (const item of state.items) {
            resize.unobserve(item);
            delete item.dataset.dockGroupItem;
        }
        mounted.delete(group);
    }
    for (const group of document.querySelectorAll(groups)) {
        if (group.matches('.dock, .segmented, #categoryTabs') || group.closest('#categoryTabs')) continue;
        const items = groupItems(group);
        let state = mounted.get(group);
        group.toggleAttribute('data-function-dock-single', items.length === 1);
        if (items.length < 2) {
            delete group.dataset.functionDock;
            if (state) {
                resize.unobserve(group);
                for (const item of state.items) {
                    resize.unobserve(item);
                    delete item.dataset.dockGroupItem;
                }
                state.slider.remove();
                mounted.delete(group);
            }
            continue;
        }
        if (!state) {
            const slider = document.createElement('span');
            slider.className = 'mcwws-function-dock-slider';
            slider.setAttribute('aria-hidden', 'true');
            state = { slider, items: [], last: null };
            mounted.set(group, state);
            resize.observe(group);
        }
        if (state.slider.parentElement !== group) group.prepend(state.slider);
        for (const item of state.items) {
            if (items.includes(item)) continue;
            resize.unobserve(item);
            delete item.dataset.dockGroupItem;
        }
        for (const item of items) {
            item.dataset.dockGroupItem = '';
            resize.observe(item);
        }
        state.items = items;
        group.dataset.functionDock = '';
        const active = items.find(item => item.matches(selected)) ??
            (items.includes(state.last) ? state.last : items[0]);
        const bounds = group.getBoundingClientRect();
        const box = active.getBoundingClientRect();
        const sx = bounds.width / group.offsetWidth || 1;
        const sy = bounds.height / group.offsetHeight || 1;
        state.slider.style.width = (box.width / sx) + 'px';
        state.slider.style.height = (box.height / sy) + 'px';
        const x = (box.left - bounds.left) / sx - group.clientLeft + group.scrollLeft;
        const y = (box.top - bounds.top) / sy - group.clientTop + group.scrollTop;
        state.slider.style.transform = `translate3d(${x}px,${y}px,0)`;
    }
    for (const [item, state] of singleControls) {
        if (item.isConnected && state.slider.parentElement === item && eligible(item) && !item.hasAttribute('data-dock-group-item')) continue;
        state.controller?.abort();
        state.slider.remove();
        delete item.dataset.cartDockControl;
        delete item.dataset.cartDockPosition;
        delete item.dataset.slider;
        delete item.dataset.dropLifted;
        delete item.dataset.optics;
        singleControls.delete(item);
    }
    for (const item of document.querySelectorAll(controls)) {
        if (!eligible(item) || item.matches('input, [data-dock-group-item]') || singleControls.has(item)) continue;
        const slider = document.createElement('span');
        slider.className = 'dock-slider';
        slider.setAttribute('aria-hidden', 'true');
        if (getComputedStyle(item).position === 'static') item.dataset.cartDockPosition = '';
        item.dataset.cartDockControl = '';
        item.prepend(slider);
        singleControls.set(item, { slider });
    }
}

function schedule() {
    if (!pending) pending = requestAnimationFrame(refresh);
}
const resize = new ResizeObserver(schedule);
new MutationObserver(records => {
    if (records.some(record => record.type === 'attributes'
        ? record.target.matches(controls + ', ' + groups) || record.target.querySelector(groups)
        : record.target.matches?.('[data-cart-dock-control]') && !record.target.querySelector(':scope > .dock-slider')
            || [...record.addedNodes, ...record.removedNodes].some(node =>
            node instanceof Element && (node.matches(controls + ', ' + groups) || node.querySelector(controls + ', ' + groups))))) schedule();
}).observe(document.body, {
    subtree: true, childList: true, attributes: true,
    attributeFilter: ['hidden', 'class', 'aria-current', 'aria-pressed', 'aria-selected', 'disabled']
});
document.fonts.ready.then(schedule);
schedule();

const x = new Spring(), y = new Spring(), width = new Spring(), height = new Spring();
const lift = new Spring(), stretch = new Spring(0, { duration: .24, bounce: .23 });
const springList = [x, y, width, height, lift];

function moveTo(rect, config) {
    x.to(rect.left + rect.width / 2, config);
    y.to(rect.top + rect.height / 2, config);
    width.to(rect.width, config);
    height.to(rect.height, config);
    wake(animation);
}

function clearDrop() {
    coveredRest?.removeAttribute('data-rest-slider-covered');
    coveredRest = null;
    lens?.destroy();
    drop?.remove();
    drop = lens = null;
}

const animation = {
    write(_now, dt) {
        if (!drop) return false;
        for (const spring of springList) spring.step(dt);
        const { rise, scaleX, scaleY, peel } = dockThumbPose(lift.value, stretch, x.velocity, dt);
        drop.style.width = width.value.toFixed(2) + 'px';
        drop.style.height = height.value.toFixed(2) + 'px';
        drop.style.transform = `translate3d(${x.value - width.value / 2}px,${y.value - height.value / 2 - peel}px,0) scale(${scaleX},${scaleY})`;
        // The outer element stays fully opaque so the SVG can sample behind it.
        drop.style.setProperty('--function-drop-lift', clamp(rise, 0, 1));
        lens?.setOpacity(clamp(rise * 1.5, 0, 1));
        if (!gesture && lift.settled && lift.target === 0) {
            clearDrop();
            return false;
        }
        return springList.some(spring => !spring.settled) || !stretch.settled;
    }
};
addTask(animation);

function findControl(target) {
    const item = target instanceof Element ? target.closest(controls) : null;
    return item && eligible(item) && !disabled(item) ? item : null;
}

function begin(item, event) {
    if (reduce.matches || solid.matches || gesture) return;
    const rect = item.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    const group = item.closest('[data-function-dock]');
    clearDrop();
    coveredRest = group ?? item;
    coveredRest.setAttribute('data-rest-slider-covered', '');
    drop = document.createElement('span');
    drop.className = 'mcwws-button-drop';
    drop.setAttribute('aria-hidden', 'true');
    Object.assign(drop.style, { width: rect.width + 'px', height: rect.height + 'px' });
    // Fullscreen and native dialogs have their own stacking context.
    (item.closest('dialog:modal') ?? document.fullscreenElement ?? document.body).append(drop);
    if (supportsLens) {
        lens = glass(drop, { ...dockThumbMaterial, backdrop: true, fill: 'transparent' });
        lens.setOpacity(0);
    }
    x.set(rect.left + rect.width / 2);
    y.set(rect.top + rect.height / 2);
    width.set(rect.width);
    height.set(rect.height);
    lift.set(0);
    stretch.set(0);
    gesture = { item, group, rect, id: event.pointerId, key: event.key, startX: event.clientX, startY: event.clientY, dragged: false, target: item };
    lift.to(1, springs.press);
    animation.write(0, 0);
    wake(animation);
}

document.addEventListener('pointerdown', event => {
    if (!event.isPrimary || event.button !== 0) return;
    suppressUntil = 0;
    const item = findControl(event.target);
    const single = singleControls.get(item);
    if (single) {
        if (!single.controller) {
            single.controller = new AbortController();
            item.dataset.optics = supportsLens && !solid.matches ? 'lens' : 'frost';
            mountDockSlider(item, { self: true, signal: single.controller.signal });
        }
        return;
    }
    if (item) begin(item, event);
}, true);

function nearest(items, px, py) {
    let best, distance = Infinity;
    for (const item of items) {
        if (!item.isConnected || disabled(item)) continue;
        const box = item.getBoundingClientRect();
        if (!box.width || !box.height) continue;
        const d = Math.hypot(Math.max(box.left - px, px - box.right, 0), Math.max(box.top - py, py - box.bottom, 0));
        if (d < distance) { best = item; distance = d; }
    }
    return best;
}

window.addEventListener('pointermove', event => {
    if (!gesture || gesture.id !== event.pointerId) return;
    const { rect, startX, startY, group } = gesture;
    const dx = event.clientX - startX, dy = event.clientY - startY;
    if (!gesture.dragged && Math.hypot(dx, dy) < 7) return;
    gesture.dragged = true;
    const items = mounted.get(group)?.items;
    const target = items && nearest(items, event.clientX, event.clientY);
    if (target) {
        gesture.target = target;
        const bounds = group.getBoundingClientRect();
        const box = target.getBoundingClientRect();
        moveTo({
            left: clamp(rect.left + dx, bounds.left, Math.max(bounds.left, bounds.right - box.width)),
            top: clamp(rect.top + dy, bounds.top, Math.max(bounds.top, bounds.bottom - box.height)),
            width: box.width,
            height: box.height
        }, springs.track);
    } else {
        moveTo({ left: rect.left + 13 * Math.tanh(dx / 30), top: rect.top + 13 * Math.tanh(dy / 30), width: rect.width, height: rect.height }, springs.track);
    }
}, { passive: true });

function release(cancelled = false) {
    if (!gesture) return;
    const { item, target, group, dragged } = gesture;
    gesture = null;
    if (dragged) suppressUntil = performance.now() + 400;
    const dest = !cancelled && group ? target : item;
    if (dest.isConnected) moveTo(dest.getBoundingClientRect(), springs.release);
    lift.to(0, springs.release);
    wake(animation);
    if (!cancelled && dragged && group && target !== item && findControl(target)) {
        target.focus({ preventScroll: true });
        target.click();
    }
}

window.addEventListener('pointerup', event => {
    if (gesture?.id === event.pointerId) release();
}, true);
window.addEventListener('pointercancel', event => {
    if (gesture?.id === event.pointerId) release(true);
}, true);
window.addEventListener('blur', () => release(true));
window.addEventListener('scroll', () => release(true), { passive: true, capture: true });
document.addEventListener('keydown', event => {
    if (!event.repeat && (event.key === 'Enter' || event.key === ' ')) {
        const item = findControl(event.target);
        if (item && !singleControls.has(item)) begin(item, event);
    }
}, true);
document.addEventListener('keyup', event => {
    if (gesture?.key === event.key) release();
}, true);
document.addEventListener('click', event => {
    if (event.isTrusted && event.detail > 0 && performance.now() < suppressUntil) {
        suppressUntil = 0;
        event.preventDefault();
        event.stopImmediatePropagation();
        return;
    }
    const item = findControl(event.target);
    const state = mounted.get(item?.closest('[data-function-dock]'));
    if (state) { state.last = item; schedule(); }
}, true);
for (const preference of [reduce, solid]) preference.addEventListener('change', () => {
    gesture = null;
    clearDrop();
});
