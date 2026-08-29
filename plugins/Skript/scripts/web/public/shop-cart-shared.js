/**
 * 商店购物车共享逻辑 — 供物品目录等页面复用
 */
(function () {
    const CART_STORAGE_KEY = 'mcwws_shop_cart';
    const MAX_CART_QTY = 10000;
    const MC_FONT_SEP = ' / ';

    let allItems = [];
    let shopCart = [];
    let cartDrawerOpen = false;
    let onCartChange = null;
    let syncUrlState = () => {};

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function limitCartQtyInputLive(input) {
        if (!input) return;
        const digits = String(input.value).replace(/\D/g, '');
        if (digits === '') {
            input.value = '';
            return;
        }
        let num = parseInt(digits, 10);
        if (!Number.isFinite(num)) {
            input.value = '';
            return;
        }
        if (num > MAX_CART_QTY) num = MAX_CART_QTY;
        input.value = String(num);
    }

    function renderCartQtyInput(value, extraAttrs = '') {
        const v = Math.max(0, Math.min(MAX_CART_QTY, Math.floor(Number(value) || 0)));
        return `<input type="text" class="cart-qty-input cart-qty-value" value="${v}" inputmode="numeric" pattern="[0-9]*" autocomplete="off" aria-label="数量" ${extraAttrs}>`;
    }

    function showToast(message, success = true) {
        const toast = document.createElement('div');
        toast.textContent = message;
        toast.style.cssText = [
            'position:fixed', 'bottom:24px', 'left:50%', 'transform:translateX(-50%)',
            'padding:14px 18px', 'border-radius:8px',
            `background:${success ? 'rgba(34,197,94,0.95)' : 'rgba(239,68,68,0.95)'}`,
            'color:#fff', 'font-size:0.95rem', 'z-index:10100',
            'box-shadow:0 10px 30px rgba(0,0,0,0.2)',
            'max-width:calc(100% - 40px)', 'text-align:center'
        ].join(';');
        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.transition = 'opacity 0.25s ease';
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 250);
        }, 3200);
    }

    function getItemIconHtml(itemId, itemName) {
        return window.getTextureHtml ? window.getTextureHtml(itemId, itemName) : '';
    }

    function mountItemIconsInContainer(root) {
        if (!root || !window.McItemIcon) return;
        void window.McItemIcon.mountGrid(root);
    }

    function cartEntryKey(itemId, offer) {
        return `${itemId}::${offer.shopId}::${offer.slot}`;
    }

    function resolveOfferUnitPrice(offer, item, kind = 'buy') {
        const resolvedKey = kind === 'buy' ? 'buyAmountResolved' : 'sellAmountResolved';
        const rawKey = kind === 'buy' ? 'buyAmount' : 'sellAmount';
        const val = offer[resolvedKey] != null ? offer[resolvedKey] : offer[rawKey];
        if (typeof val === 'number' && Number.isFinite(val)) {
            return val;
        }
        if (kind === 'buy') {
            return Number(item?.buyPrice) || 0;
        }
        return Number(item?.sellPrice) || 0;
    }

    function syncPageScrollLock() {
        const hasActiveModal = !!document.querySelector('.modal.active, .modal.closing');
        document.body.style.overflow = (cartDrawerOpen || hasActiveModal) ? 'hidden' : '';
    }

    function loadShopCart() {
        try {
            const raw = localStorage.getItem(CART_STORAGE_KEY);
            const parsed = raw ? JSON.parse(raw) : [];
            shopCart = Array.isArray(parsed) ? parsed.filter((e) => e && e.key && e.itemId) : [];
        } catch {
            shopCart = [];
        }
    }

    function saveShopCart() {
        try {
            localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(shopCart));
        } catch {
            /* ignore */
        }
    }

    function getCartTotalQuantity() {
        return shopCart.reduce((sum, e) => sum + (Number(e.quantity) || 0), 0);
    }

    function getCartTotalBuyPrice() {
        return shopCart.reduce((sum, e) => {
            const qty = Number(e.quantity) || 0;
            const unit = Number(e.unitBuyPrice) || 0;
            return sum + qty * unit;
        }, 0);
    }

    function updateCartBadge() {
        const badge = document.getElementById('cartBadge');
        if (!badge) return;
        const count = getCartTotalQuantity();
        badge.textContent = count > 99 ? '99+' : String(count);
        badge.hidden = count <= 0;
    }

    function formatOrderStatus(status) {
        if (status === 'delivering') return '发放中';
        if (status === 'delivered') return '已完成';
        if (status === 'failed') return '失败';
        return '待领取';
    }

    async function loadPendingOrdersUi() {
        const box = document.getElementById('cartPendingOrders');
        if (!box) return;
        const auth = window.MCWWS_AUTH;
        if (!auth?.getToken?.()) {
            box.hidden = true;
            box.innerHTML = '';
            return;
        }
        try {
            const res = await fetch('/api/shop/orders?limit=5', {
                headers: auth.headers(),
                cache: 'no-store'
            });
            if (!res.ok) {
                box.hidden = true;
                return;
            }
            const data = await res.json();
            const active = (data.orders || []).filter((o) => o.status === 'pending' || o.status === 'delivering');
            if (!active.length) {
                box.hidden = true;
                box.innerHTML = '';
                return;
            }
            box.hidden = false;
            box.innerHTML = `
                <p class="cart-pending-title">待领取订单</p>
                <ul class="cart-pending-list">
                    ${active.map((order) => `
                        <li>
                            <span class="cart-pending-id">#${escapeHtml(String(order.orderId))}</span>
                            <span class="cart-pending-status">${escapeHtml(formatOrderStatus(order.status))}</span>
                            <span class="cart-pending-total">${escapeHtml(order.totalFormatted || '')}</span>
                        </li>
                    `).join('')}
                </ul>
            `;
        } catch {
            box.hidden = true;
        }
    }

    function renderCartDrawer() {
        const body = document.getElementById('cartDrawerBody');
        const totalEl = document.getElementById('cartTotalPrice');
        const mapBtn = document.getElementById('cartMapBtn');
        const checkoutBtn = document.getElementById('cartCheckoutBtn');
        if (!body) return;

        if (!shopCart.length) {
            body.innerHTML = '<div class="cart-empty">购物车是空的<br>导入材料清单后可加入购物车</div>';
            if (totalEl) totalEl.textContent = '￥0.00';
            if (mapBtn) mapBtn.disabled = true;
            if (checkoutBtn) checkoutBtn.disabled = true;
            return;
        }

        body.innerHTML = shopCart.map((entry) => {
            const subtotal = (Number(entry.quantity) || 0) * (Number(entry.unitBuyPrice) || 0);
            const safeKey = escapeHtml(entry.key);
            return `
                <div class="cart-line" data-cart-key="${safeKey}">
                    <div class="cart-line-icon" data-item-id="${escapeHtml(entry.itemId)}" data-item-name="${escapeHtml(entry.itemName)}"></div>
                    <div class="cart-line-main">
                        <p class="cart-line-name" title="${escapeHtml(entry.itemName)}">${escapeHtml(entry.itemName)}</p>
                        <p class="cart-line-shop">${escapeHtml(entry.shopTitle)}${MC_FONT_SEP}槽位 ${escapeHtml(entry.slot)}</p>
                        <div class="cart-line-price">￥${entry.unitBuyPrice.toFixed(2)} x ${entry.quantity} = ￥${subtotal.toFixed(2)}</div>
                        <div class="cart-qty">
                            <button type="button" class="cart-qty-btn" data-cart-action="dec" data-cart-key="${safeKey}" aria-label="减少">-</button>
                            ${renderCartQtyInput(entry.quantity, `data-cart-qty-input data-cart-key="${safeKey}"`)}
                            <button type="button" class="cart-qty-btn" data-cart-action="inc" data-cart-key="${safeKey}" aria-label="增加">+</button>
                        </div>
                    </div>
                    <button type="button" class="cart-line-remove" data-cart-action="remove" data-cart-key="${safeKey}" aria-label="移除">x</button>
                </div>
            `;
        }).join('');

        body.querySelectorAll('.cart-line-icon').forEach((el) => {
            const itemId = el.getAttribute('data-item-id');
            const itemName = el.getAttribute('data-item-name');
            el.innerHTML = getItemIconHtml(itemId, itemName);
        });
        mountItemIconsInContainer(body);
        window.McTextureAnim?.initInContainer?.(body);
        window.McEnchantGlint?.initInContainer?.(body);

        if (totalEl) {
            totalEl.textContent = `￥${getCartTotalBuyPrice().toFixed(2)}`;
        }
        if (mapBtn) {
            mapBtn.disabled = false;
        }
        if (checkoutBtn) {
            checkoutBtn.disabled = !window.MCWWS_AUTH?.getToken?.();
        }
    }

    function openCartDrawer() {
        const drawer = document.getElementById('cartDrawer');
        const backdrop = document.getElementById('cartBackdrop');
        if (!drawer || !backdrop) return;
        cartDrawerOpen = true;
        drawer.classList.add('is-open');
        drawer.setAttribute('aria-hidden', 'false');
        backdrop.hidden = false;
        backdrop.classList.add('is-visible');
        backdrop.setAttribute('aria-hidden', 'false');
        syncPageScrollLock();
        renderCartDrawer();
        void loadPendingOrdersUi();
        syncUrlState();
    }

    function closeCartDrawer() {
        const drawer = document.getElementById('cartDrawer');
        const backdrop = document.getElementById('cartBackdrop');
        if (!drawer || !backdrop) return;
        cartDrawerOpen = false;
        drawer.classList.remove('is-open');
        drawer.setAttribute('aria-hidden', 'true');
        backdrop.classList.remove('is-visible');
        backdrop.setAttribute('aria-hidden', 'true');
        window.setTimeout(() => {
            if (!cartDrawerOpen) backdrop.hidden = true;
        }, 220);
        syncPageScrollLock();
        syncUrlState();
    }

    function addToCart(item, offer, quantity = 1) {
        const qty = Math.max(1, Math.floor(Number(quantity) || 1));
        const key = cartEntryKey(item.id, offer);
        const unitBuyPrice = resolveOfferUnitPrice(offer, item, 'buy');
        const shopName = offer.shopTitleResolved || offer.shopTitle || offer.shopId;
        const existing = shopCart.find((e) => e.key === key);
        if (existing) {
            existing.quantity = Math.min(MAX_CART_QTY, existing.quantity + qty);
            existing.unitBuyPrice = unitBuyPrice;
        } else {
            shopCart.push({
                key,
                itemId: item.id,
                itemName: item.name,
                shopId: offer.shopId,
                shopTitle: String(shopName),
                slot: String(offer.slot),
                quantity: qty,
                unitBuyPrice,
                location: offer.location || null
            });
        }
        saveShopCart();
        updateCartBadge();
        if (cartDrawerOpen) {
            renderCartDrawer();
        }
        onCartChange?.();
    }

    function setCartLineQuantity(key, quantity) {
        const entry = shopCart.find((e) => e.key === key);
        if (!entry) return;
        let qty = Math.floor(Number(quantity) || 0);
        if (!Number.isFinite(qty)) qty = 0;
        qty = Math.max(0, Math.min(MAX_CART_QTY, qty));
        if (qty <= 0) {
            shopCart = shopCart.filter((e) => e.key !== key);
        } else {
            entry.quantity = qty;
        }
        saveShopCart();
        updateCartBadge();
        renderCartDrawer();
        onCartChange?.();
    }

    function removeCartLine(key) {
        shopCart = shopCart.filter((e) => e.key !== key);
        saveShopCart();
        updateCartBadge();
        renderCartDrawer();
        onCartChange?.();
    }

    function clearShopCart() {
        shopCart = [];
        saveShopCart();
        updateCartBadge();
        renderCartDrawer();
        onCartChange?.();
    }

    function isValidShopLocation(location) {
        return !!(location && location.enabled !== false && location.viewUrl);
    }

    function blueMapUrlForLocation(location) {
        if (!isValidShopLocation(location)) return null;
        return String(location.viewUrl || '').trim() || null;
    }

    function appendTradeParamsToMapUrl(url, item) {
        const raw = String(url || '').trim();
        if (!raw) return null;
        const itemId = item && item.id ? String(item.id) : '';
        const q = item && (item.name || item.displayName) ? String(item.name || item.displayName) : '';

        let bluemapHash = '';
        try {
            const parsed = new URL(raw, window.location.href);
            bluemapHash = parsed.hash;
        } catch {
            const hashIdx = raw.indexOf('#');
            bluemapHash = hashIdx >= 0 ? raw.slice(hashIdx) : '';
        }

        const mapPage = new URL('map.html', window.location.href);
        if (bluemapHash) {
            mapPage.hash = bluemapHash.startsWith('#') ? bluemapHash.slice(1) : bluemapHash;
        }
        mapPage.searchParams.set('trade', '1');
        if (itemId) mapPage.searchParams.set('item', itemId);
        if (q) mapPage.searchParams.set('q', q);
        return mapPage.toString();
    }

    function openCartOnMap() {
        const withMap = shopCart.find((e) => isValidShopLocation(e.location));
        if (!withMap) {
            showToast('购物车中的物品均未配置地图位置。', false);
            return;
        }
        const item = allItems.find((i) => i.id === withMap.itemId) || {
            id: withMap.itemId,
            name: withMap.itemName
        };
        const url = appendTradeParamsToMapUrl(blueMapUrlForLocation(withMap.location), item);
        if (url) {
            window.open(url, '_blank', 'noopener');
        }
    }

    async function submitCartCheckout() {
        if (!shopCart.length) {
            showToast('购物车是空的。', false);
            return;
        }
        const auth = window.MCWWS_AUTH;
        if (!auth?.getToken?.()) {
            auth?.openModal?.();
            showToast('请先登录后再提交订单。', false);
            return;
        }
        const checkoutBtn = document.getElementById('cartCheckoutBtn');
        if (checkoutBtn) checkoutBtn.disabled = true;
        const lines = shopCart.map((entry) => ({
            itemId: entry.itemId,
            shopId: entry.shopId,
            slot: entry.slot,
            quantity: entry.quantity
        }));
        try {
            const res = await fetch('/api/shop/checkout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...auth.headers()
                },
                body: JSON.stringify({ lines })
            });
            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.error || '提交订单失败');
            }
            clearShopCart();
            if (data.balance != null || data.balanceFormatted) {
                auth.applyEconomySnapshot?.({
                    balance: data.balance,
                    balanceFormatted: data.balanceFormatted
                });
            } else {
                void auth.refreshEconomy?.(true);
            }
            const msg = data.message
                || `订单 #${data.orderId} 已提交，零钱已扣除，物品将发放至 BetterBags。`;
            showToast(msg, true);
            await loadPendingOrdersUi();
            closeCartDrawer();
        } catch (error) {
            showToast(error.message || '提交订单失败', false);
        } finally {
            if (checkoutBtn) checkoutBtn.disabled = false;
        }
    }

    function bindCartUi() {
        document.getElementById('cartToggleBtn')?.addEventListener('click', () => {
            if (cartDrawerOpen) closeCartDrawer();
            else openCartDrawer();
        });
        document.getElementById('cartCloseBtn')?.addEventListener('click', closeCartDrawer);
        document.getElementById('cartBackdrop')?.addEventListener('click', closeCartDrawer);
        document.getElementById('cartClearBtn')?.addEventListener('click', () => {
            if (!shopCart.length) return;
            if (window.confirm('确定清空购物车？')) clearShopCart();
        });
        document.getElementById('cartMapBtn')?.addEventListener('click', openCartOnMap);
        document.getElementById('cartCheckoutBtn')?.addEventListener('click', () => {
            void submitCartCheckout();
        });
        const cartDrawerBody = document.getElementById('cartDrawerBody');
        if (cartDrawerBody) {
            cartDrawerBody.addEventListener('click', (e) => {
                const btn = e.target.closest('[data-cart-action]');
                if (!btn) return;
                const key = btn.getAttribute('data-cart-key');
                const action = btn.getAttribute('data-cart-action');
                const entry = shopCart.find((line) => line.key === key);
                if (!entry) return;
                if (action === 'inc') {
                    setCartLineQuantity(key, entry.quantity + 1);
                } else if (action === 'dec') {
                    setCartLineQuantity(key, entry.quantity - 1);
                } else if (action === 'remove') {
                    removeCartLine(key);
                }
            });
            cartDrawerBody.addEventListener('change', (e) => {
                const input = e.target.closest('[data-cart-qty-input]');
                if (!input) return;
                const key = input.getAttribute('data-cart-key');
                if (key) setCartLineQuantity(key, input.value);
            });
            cartDrawerBody.addEventListener('input', (e) => {
                const input = e.target.closest('[data-cart-qty-input]');
                if (input) limitCartQtyInputLive(input);
            });
            cartDrawerBody.addEventListener('keydown', (e) => {
                if (e.key !== 'Enter') return;
                const input = e.target.closest('[data-cart-qty-input]');
                if (!input) return;
                e.preventDefault();
                input.blur();
            });
            cartDrawerBody.addEventListener('click', (e) => {
                if (e.target.closest('[data-cart-qty-input]')) {
                    e.stopPropagation();
                }
            });
        }
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && cartDrawerOpen) {
                closeCartDrawer();
            }
        });
    }

    function buildCatalogItems(rawData) {
        return Object.keys(rawData).map((key) => ({
            id: key,
            name: rawData[key].displayName
                || rawData[key].customDisplayName
                || (window.getChineseName ? window.getChineseName(key) : key),
            buyPrice: rawData[key].buy,
            sellPrice: rawData[key].sell,
            ultimateShopOffers: rawData[key].ultimateShopOffers || []
        }));
    }

    async function loadCatalogItems() {
        const response = await fetch('/api/prices?t=' + Date.now());
        if (!response.ok) throw new Error('网络响应失败');
        const rawData = await response.json();
        allItems = buildCatalogItems(rawData);
        return allItems;
    }

    function getLitematicaDeps() {
        return {
            allItems: () => allItems,
            addToCart,
            openCartDrawer,
            showToast,
            escapeHtml,
            getItemIconHtml,
            mountItemIconsInContainer
        };
    }

    async function init(options = {}) {
        onCartChange = options.onCartChange || null;
        syncUrlState = options.syncUrl || (() => {});
        loadShopCart();
        updateCartBadge();
        bindCartUi();
        if (options.loadCatalog !== false) {
            await loadCatalogItems();
        }
        return allItems;
    }

    window.MCWWS_ShopCart = {
        init,
        loadCatalogItems,
        getAllItems: () => allItems,
        addToCart,
        openCartDrawer,
        closeCartDrawer,
        showToast,
        escapeHtml,
        getItemIconHtml,
        mountItemIconsInContainer,
        getLitematicaDeps,
        isCartOpen: () => cartDrawerOpen
    };
})();
