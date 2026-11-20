import { addTask, clamp, glass, rubberBand, Spring, springs, supportsBackdropSvg, wake } from './glassy-liquid-0.1.0.js';
import { dockThumbMaterial } from './dock-material.js';

const tabs = document.getElementById('categoryTabs');
if (tabs) {
    const slider = document.createElement('span');
    slider.className = 'category-dock-slider';
    slider.setAttribute('aria-hidden', 'true');
    const reduced = matchMedia('(prefers-reduced-motion: reduce)');
    const solid = matchMedia('(prefers-reduced-transparency: reduce), (forced-colors: active)');
    const x = new Spring(), y = new Spring(), width = new Spring(), height = new Spring();
    const lift = new Spring(), stretchX = new Spring(0, { duration: .24, bounce: .23 });
    const stretchY = new Spring(0, { duration: .24, bounce: .23 });
    let geometry = [], pending = 0, gesture, suppressClick = false, initialized = false;
    let lens, refracting = false;
    const buttons = () => [...tabs.querySelectorAll('.category-btn')];
    const current = () => geometry.find(g => g.item.classList.contains('active')) ?? geometry[0];
    function place(g, config = springs.layout) {
        if (!g) return;
        x.to(g.x, config); y.to(g.y, config);
        width.to(g.width, config); height.to(g.height, config);
        wake(animation);
    }
    const animation = {
        write(_now, dt) {
            if (!initialized) return false;
            for (const s of [x, y, width, height, lift]) reduced.matches ? s.set(s.target) : s.step(dt);
            const rise = reduced.matches ? 0 : clamp(lift.value, 0, 1.14);
            const ex = Math.min(.38, Math.abs(x.velocity) * .0008) * rise;
            const ey = Math.min(.38, Math.abs(y.velocity) * .0008) * rise;
            for (const [s, target] of [[stretchX, ex], [stretchY, ey]]) reduced.matches ? s.set(0) : s.to(target).step(dt);
            const dx = clamp(stretchX.value, -.06, .4), dy = clamp(stretchY.value, -.06, .4);
            // Resize the capsule itself: nonuniform scaling would turn its
            // semicircular ends (including the optical mask) into ellipses.
            const renderedHeight = Math.max(1, height.value * (1 + .10 * rise) * (1 + dy) / Math.sqrt(1 + dx));
            const renderedWidth = Math.max(renderedHeight, width.value * (1 + .22 * rise) * (1 + dx) / Math.sqrt(1 + dy));
            slider.style.width = renderedWidth.toFixed(2) + 'px';
            slider.style.height = renderedHeight.toFixed(2) + 'px';
            slider.style.borderRadius = (renderedHeight / 2).toFixed(2) + 'px';
            slider.style.transform = `translate3d(${x.value - renderedWidth / 2}px,${y.value - renderedHeight / 2 - rise * 2}px,0)`;
            slider.style.setProperty('--slider-lift', clamp(rise, 0, 1));
            if (gesture) {
                slider.style.setProperty('--press-x', clamp((gesture.pointerX - x.value) / Math.max(1, width.value) * 100 + 50, 0, 100) + '%');
                slider.style.setProperty('--press-y', clamp((gesture.pointerY - y.value) / Math.max(1, height.value) * 100 + 50, 0, 100) + '%');
            }
            tabs.toggleAttribute('data-drop-lifted', rise > .025);
            for (const g of geometry) {
                const ox = g.x - x.value, oy = g.y - y.value;
                const pull = rise * 32 / (32 + Math.hypot(ox, oy));
                const bx = Math.tanh(ox / 24) * pull, by = Math.tanh(oy / 24) * pull;
                if (rise < .001) g.item.style.removeProperty('transform');
                else g.item.style.transform = `translate(${-bx * 5}px,${-pull * 3 - by * 5}px) skew(${bx * 9}deg,${by * 6}deg) scale(${1 + pull * .13},${1 - pull * .09})`;
            }
            if (refracting) lens.setOpacity(clamp(rise * 1.5, 0, 1));
            if (refracting && (reduced.matches || (lift.settled && lift.target === 0))) {
                lens.set({ backdrop: false }); refracting = false;
            }
            return [x, y, width, height, lift, stretchX, stretchY].some(s => !s.settled);
        },
    };
    addTask(animation);
    function measure() {
        pending = 0;
        if (slider.parentElement !== tabs) tabs.prepend(slider);
        geometry = buttons().filter(b => b.offsetWidth).map(item => ({
            item, width: item.offsetWidth, height: item.offsetHeight,
            x: item.offsetLeft + item.offsetWidth / 2, y: item.offsetTop + item.offsetHeight / 2,
        }));
        slider.hidden = !geometry.length;
        if (!geometry.length) return;
        for (const g of geometry) g.item.setAttribute('aria-pressed', String(g.item.classList.contains('active')));
        const selected = current();
        if (!initialized) {
            x.set(selected.x); y.set(selected.y); width.set(selected.width); height.set(selected.height);
            initialized = true;
        }
        if (!gesture) place(selected);
        if (!lens && supportsBackdropSvg() && !solid.matches) {
            lens = glass(slider, dockThumbMaterial);
        }
        animation.write(0, 0);
    }
    const schedule = () => { if (!pending) pending = requestAnimationFrame(measure); };
    new MutationObserver(schedule).observe(tabs, { childList: true, subtree: true, attributes: true, attributeFilter: ['class'] });
    new ResizeObserver(schedule).observe(tabs);
    document.fonts.ready.then(schedule);
    function local(event) {
        const rect = tabs.getBoundingClientRect();
        return { x: (event.clientX - rect.left) * tabs.offsetWidth / rect.width - tabs.clientLeft,
            y: (event.clientY - rect.top) * tabs.offsetHeight / rect.height - tabs.clientTop };
    }
    function nearest(p) {
        return geometry.reduce((a, b) => {
            const distance = g => Math.hypot(Math.max(Math.abs(g.x - p.x) - g.width / 2, 0), Math.max(Math.abs(g.y - p.y) - g.height / 2, 0));
            return !a || distance(b) < distance(a) ? b : a;
        }, null);
    }
    tabs.addEventListener('pointerdown', event => {
        if (event.button !== 0 || !event.isPrimary || !event.target.closest('.category-btn') || !geometry.length) return;
        suppressClick = false;
        const p = local(event), origin = geometry.find(g => g.item.contains(event.target)) ?? nearest(p);
        gesture = { id: event.pointerId, startX: event.clientX, startY: event.clientY,
            pointerX: p.x, pointerY: p.y,
            offsetX: origin.x - p.x, offsetY: origin.y - p.y, dragging: false };
        place(origin, springs.press);
        lift.to(1, springs.press);
        if (lens && !solid.matches && !reduced.matches && !refracting) {
            lens.setOpacity(0); lens.set({ backdrop: true }); refracting = true;
        }
        wake(animation);
    });
    window.addEventListener('pointermove', event => {
        if (gesture?.id !== event.pointerId) return;
        if (!gesture.dragging && Math.hypot(event.clientX - gesture.startX, event.clientY - gesture.startY) < 5) return;
        gesture.dragging = true;
        if (!tabs.hasPointerCapture(event.pointerId)) tabs.setPointerCapture(event.pointerId);
        tabs.classList.add('is-dragging');
        const p = local(event), rawX = p.x + gesture.offsetX, rawY = p.y + gesture.offsetY;
        gesture.pointerX = p.x; gesture.pointerY = p.y;
        const g = nearest(p);
        const endX = clamp(rawX, g.width / 2, tabs.clientWidth - g.width / 2);
        const endY = clamp(rawY, g.height / 2, tabs.clientHeight - g.height / 2);
        x.to(endX + (reduced.matches ? 0 : rubberBand(rawX - endX, 16)), springs.track);
        y.to(endY + (reduced.matches ? 0 : rubberBand(rawY - endY, 16)), springs.track);
        width.to(g.width, springs.track); height.to(g.height, springs.track);
        wake(animation);
    }, { passive: true });
    function finish(event, cancelled = false) {
        if (!gesture || (event && event.pointerId !== gesture.id)) return;
        const previous = gesture; gesture = null;
        tabs.classList.remove('is-dragging');
        if (tabs.hasPointerCapture(previous.id)) tabs.releasePointerCapture(previous.id);
        lift.to(0, springs.release);
        if (!cancelled && previous.dragging) {
            const selected = nearest(local(event));
            place(selected, { duration: .46, bounce: .42 });
            suppressClick = true;
            selected.item.focus({ preventScroll: true });
            selected.item.click();
        } else place(current());
        wake(animation);
    }
    window.addEventListener('pointerup', event => finish(event));
    window.addEventListener('pointercancel', event => finish(event, true));
    window.addEventListener('blur', () => finish(null, true));
    tabs.addEventListener('lostpointercapture', event => { if (event.target === tabs) finish(event, true); });
    tabs.addEventListener('click', event => {
        if (suppressClick && event.isTrusted) {
            suppressClick = false; event.preventDefault(); event.stopImmediatePropagation();
        }
    }, true);
    reduced.addEventListener('change', () => wake(animation));
    solid.addEventListener('change', () => {
        if (solid.matches) { lens?.destroy(); lens = null; refracting = false; }
        schedule();
    });
    schedule();
}
