/**
 * 物品加入购物车时，从点击位置飞向右上角购物车按钮的动画
 */
(function () {
    const FLY_SIZE = 44;
    const DURATION_MS = 680;

    function prefersReducedMotion() {
        return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    function getCartTarget() {
        return document.getElementById('cartToggleBtn') || document.getElementById('cartBadge');
    }

    function pulseCartTarget() {
        const btn = document.getElementById('cartToggleBtn');
        if (!btn) return;
        btn.classList.remove('cart-nav-btn--pulse');
        void btn.offsetWidth;
        btn.classList.add('cart-nav-btn--pulse');
        window.setTimeout(() => btn.classList.remove('cart-nav-btn--pulse'), 480);
    }

    function quadBezier(t, p0, p1, p2) {
        const u = 1 - t;
        return u * u * p0 + 2 * u * t * p1 + t * t * p2;
    }

    function easeOutCubic(t) {
        return 1 - Math.pow(1 - t, 3);
    }

    function findIconMount(sourceEl, itemId) {
        const card = sourceEl?.closest?.('[data-item-card-id]')
            || sourceEl?.closest?.('#itemModal')
            || sourceEl?.closest?.('.modal');
        if (card) {
            const mount = card.querySelector('.item-icon-mount, .item-icon-3d');
            if (mount) return mount;
        }
        if (itemId) {
            const cardById = document.querySelector(`[data-item-card-id="${CSS.escape(String(itemId))}"]`);
            const mount = cardById?.querySelector('.item-icon-mount, .item-icon-3d');
            if (mount) return mount;
        }
        return null;
    }

    function buildFlyVisual(mount, itemId, itemName) {
        const img = mount?.querySelector('img');
        if (img?.src) {
            const flyImg = document.createElement('img');
            flyImg.src = img.src;
            flyImg.alt = '';
            flyImg.decoding = 'async';
            flyImg.referrerPolicy = 'no-referrer';
            flyImg.style.cssText = 'width:100%;height:100%;object-fit:contain;image-rendering:pixelated;display:block;';
            return flyImg;
        }

        const canvas = mount?.querySelector('canvas');
        if (canvas?.toDataURL) {
            try {
                const flyImg = document.createElement('img');
                flyImg.src = canvas.toDataURL('image/png');
                flyImg.alt = '';
                flyImg.style.cssText = 'width:100%;height:100%;object-fit:contain;image-rendering:pixelated;display:block;';
                return flyImg;
            } catch {
                /* canvas may be tainted */
            }
        }

        if (window.getTextureHtml) {
            const wrap = document.createElement('div');
            wrap.style.cssText = 'width:100%;height:100%;display:flex;align-items:center;justify-content:center;';
            wrap.innerHTML = window.getTextureHtml(itemId, itemName);
            const innerMount = wrap.querySelector('.item-icon-mount');
            if (innerMount) {
                innerMount.style.marginRight = '0';
                innerMount.style.width = '100%';
                innerMount.style.height = '100%';
            }
            return wrap;
        }

        const fallback = document.createElement('span');
        fallback.textContent = '+';
        fallback.style.cssText = 'font-size:1.4rem;font-weight:700;color:#fff;';
        return fallback;
    }

    function getStartRect(sourceEl, itemId) {
        if (sourceEl?.getBoundingClientRect) {
            const rect = sourceEl.getBoundingClientRect();
            if (rect.width > 0 && rect.height > 0) return rect;
        }
        const mount = findIconMount(sourceEl, itemId);
        if (mount?.getBoundingClientRect) {
            return mount.getBoundingClientRect();
        }
        return null;
    }

    function playFlyToCart(options = {}) {
        const { sourceEl, itemId, itemName } = options;
        const target = getCartTarget();
        if (!target) return;

        if (prefersReducedMotion()) {
            pulseCartTarget();
            return;
        }

        const fromRect = getStartRect(sourceEl, itemId);
        const toRect = target.getBoundingClientRect();
        if (!fromRect || !toRect.width) {
            pulseCartTarget();
            return;
        }

        const mount = findIconMount(sourceEl, itemId);
        const fly = document.createElement('div');
        fly.className = 'cart-fly-item';
        fly.setAttribute('aria-hidden', 'true');
        fly.appendChild(buildFlyVisual(mount, itemId, itemName));
        document.body.appendChild(fly);

        const startX = fromRect.left + fromRect.width / 2;
        const startY = fromRect.top + fromRect.height / 2;
        const endX = toRect.left + toRect.width / 2;
        const endY = toRect.top + toRect.height / 2;
        const controlX = (startX + endX) / 2;
        const controlY = Math.min(startY, endY) - Math.max(72, Math.abs(endX - startX) * 0.18);

        const half = FLY_SIZE / 2;
        fly.style.width = `${FLY_SIZE}px`;
        fly.style.height = `${FLY_SIZE}px`;
        fly.style.transform = `translate(${startX - half}px, ${startY - half}px) scale(1)`;

        const startTime = performance.now();

        function frame(now) {
            const rawT = Math.min(1, (now - startTime) / DURATION_MS);
            const t = easeOutCubic(rawT);
            const x = quadBezier(t, startX, controlX, endX);
            const y = quadBezier(t, startY, controlY, endY);
            const scale = 1 - t * 0.42;
            const fadeStart = 0.82;
            const opacity = rawT > fadeStart ? 1 - (rawT - fadeStart) / (1 - fadeStart) : 1;

            fly.style.transform = `translate(${x - half}px, ${y - half}px) scale(${scale})`;
            fly.style.opacity = String(opacity);

            if (rawT < 1) {
                requestAnimationFrame(frame);
            } else {
                fly.remove();
                pulseCartTarget();
            }
        }

        requestAnimationFrame(frame);
    }

    window.MCWWS_CartFlyAnim = {
        play: playFlyToCart,
        pulseCart: pulseCartTarget
    };
})();
