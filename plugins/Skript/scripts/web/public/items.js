// ==========================================
// 流浪世界服务器商店系统 物品目录 - 核心逻辑 (Skript 适配版)
// ==========================================

// 全局状态（新增 sortReverse 变量）
let allItems = [];
let filteredItems = [];
let currentSort = 'name';
let searchQuery = '';
let letterFilter = ''; // A–Z 首字母筛选，空表示不限
let sortReverse = false; // 逆序状态，默认关闭
let hideUntradable = false; // 隐藏无 UltimateShop 上架（不可交易）的物品
let currentPage = 1;
let itemScope = 'in_shop';
const PAGE_SIZE = 60;
let searchTimer = null;
let pageBeforeSearch = null;
let clockTimeTimer = null;
let pointerBearingTimer = null;
let pointerBearingX = null;
let pointerBearingY = null;

const POINTER_COMPASS_TILT_COS = Math.cos(Math.PI / 4);
const MC_FONT_SEP = ' / ';
const MC_EMPTY = '-';

/** minecraftAE 未编码字符回退，避免显示乱码 */
function sanitizeMcFontText(text) {
    return String(text ?? '')
        .replace(/\u00a5/g, '￥')
        .replace(/\u2014/g, '-')
        .replace(/\u2013/g, '-')
        .replace(/\u00b7/g, MC_FONT_SEP)
        .replace(/\u00d7/g, 'x');
}

const SORT_VALUES = new Set(['name', 'buyPrice', 'sellPrice', 'stock']);
const STATIC_ITEM_SCOPES = new Set(['all', 'in_shop', 'custom']);
const ITEM_SCOPE_VALUES = new Set(['all', 'in_shop', 'custom']);
let shopCategoryList = [];
const CART_STORAGE_KEY = 'mcwws_shop_cart';
let shopCart = [];
let cartDrawerOpen = false;

function replaceUrlIfChanged(params) {
    const qs = params.toString();
    const next = qs
        ? `${window.location.pathname}?${qs}${window.location.hash || ''}`
        : `${window.location.pathname}${window.location.hash || ''}`;
    const cur = `${window.location.pathname}${window.location.search}${window.location.hash || ''}`;
    if (next !== cur) {
        history.replaceState(null, '', next);
    }
}

/** 从地址栏恢复：搜索、排序、复选框、页码（交易弹窗在数据加载后单独处理） */
function hydrateItemsStateFromUrl() {
    const params = new URLSearchParams(window.location.search);

    if (params.has('q')) {
        const trimmed = (params.get('q') || '').trim();
        searchQuery = normalizeSearchText(trimmed);
        const inp = document.getElementById('searchInput');
        if (inp) inp.value = trimmed;
        updateSearchClearButton();
    }

    const letterParam = (params.get('letter') || '').trim().toUpperCase();
    if (letterParam && /^[A-Z]$/.test(letterParam)) {
        letterFilter = letterParam;
    }

    const sortVal = params.get('sort');
    if (sortVal && SORT_VALUES.has(sortVal)) {
        currentSort = sortVal;
        const sortSelect = document.getElementById('sortSelect');
        if (sortSelect) sortSelect.value = sortVal;
    }

    const scopeVal = params.get('scope');
    if (scopeVal && (ITEM_SCOPE_VALUES.has(scopeVal) || shopCategoryList.some((cat) => cat.id === scopeVal))) {
        itemScope = scopeVal;
    } else {
        itemScope = 'in_shop';
    }
    updateCategoryScopeButtons();

    sortReverse = params.get('rev') === '1';
    const revEl = document.getElementById('sortReverse');
    if (revEl) revEl.checked = sortReverse;

    hideUntradable = params.get('hide') === '1';
    const hideEl = document.getElementById('hideUntradable');
    if (hideEl) hideEl.checked = hideUntradable;

    const pageNum = parseInt(params.get('page'), 10);
    if (Number.isFinite(pageNum) && pageNum >= 1) {
        currentPage = pageNum;
    }
}

/** 根据当前 UI 状态写回地址栏（不新增历史记录） */
function syncItemsStateToUrl() {
    const params = new URLSearchParams(window.location.search);

    const inp = document.getElementById('searchInput');
    const raw = inp ? inp.value.trim() : '';
    if (raw) params.set('q', raw);
    else params.delete('q');

    if (letterFilter) params.set('letter', letterFilter);
    else params.delete('letter');

    if (currentSort && currentSort !== 'name') params.set('sort', currentSort);
    else params.delete('sort');

    if (itemScope && itemScope !== 'in_shop') params.set('scope', itemScope);
    else params.delete('scope');

    if (sortReverse) params.set('rev', '1');
    else params.delete('rev');

    if (hideUntradable) params.set('hide', '1');
    else params.delete('hide');

    if (currentPage > 1) params.set('page', String(currentPage));
    else params.delete('page');

    const tradeModal = document.getElementById('tradeModal');
    const tradeOpen = tradeModal && tradeModal.classList.contains('active');
    const tradeId = tradeOpen && tradeModal.dataset.tradeItemId ? tradeModal.dataset.tradeItemId : null;
    if (tradeId) params.set('add', tradeId);
    else params.delete('add');
    params.delete('trade');

    if (cartDrawerOpen) params.set('cart', '1');
    else params.delete('cart');

    replaceUrlIfChanged(params);
}

/** 刷新后若带 ?add= 则打开上架选择弹窗；?cart=1 打开购物车 */
function tryOpenCartFromUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('cart') === '1') {
        openCartDrawer();
    }
    const legacyTrade = params.get('trade');
    const addId = params.get('add') || legacyTrade;
    if (!addId || !allItems.length) return;
    const item = allItems.find((i) => i.id === addId);
    const offers = item && item.ultimateShopOffers ? item.ultimateShopOffers : [];
    if (item && offers.length) {
        if (offers.length === 1) {
            addToCart(item, offers[0], 1);
            showToast(`已加入购物车：${item.name}`, true);
        } else {
            openCartOfferModal(item);
        }
    } else {
        params.delete('add');
        params.delete('trade');
        replaceUrlIfChanged(params);
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', async () => {
    if (window.mcLangReady) {
        await window.mcLangReady;
    }
    if (!window.McItemIcon) {
        console.warn('[物品图标] item-model-renderer.js 未加载，仍使用 PNG');
    } else if (!window.THREE) {
        console.warn('[物品图标] Three.js 未加载（检查 vendor/three.module.min.js），仍使用 PNG');
    }
    initItemsHeroCompass();
    window.MCWWS_LitematicaDeps = {
        allItems: () => allItems,
        addToCart,
        openCartDrawer,
        showToast,
        escapeHtml,
        getItemIconHtml,
        mountItemIconsInContainer
    };
    loadShopCart();
    updateCartBadge();
    loadItems();
    setupEventListeners();
    if (window.MCWWS_AUTH) {
        void window.MCWWS_AUTH.init();
    }
    ensureClockTimeTicker();
    ensurePointerBearingTicker();
});

// 绑定页面交互事件（新增逆序复选框监听）
// 该函数在后面定义为优化版，避免重复定义。

// 从 Node.js 后端拉取数据，并读取 UltimateShop 映射
async function loadShopCategories() {
    try {
        const response = await fetch('/api/shop/categories?t=' + Date.now());
        if (!response.ok) return;
        const data = await response.json();
        shopCategoryList = Array.isArray(data.categories) ? data.categories : [];
        shopCategoryList.forEach((cat) => ITEM_SCOPE_VALUES.add(cat.id));
        renderCategoryTabs();
        updateCategoryScopeButtons();
    } catch (error) {
        console.warn('加载商店分类失败', error);
    }
}

function renderCategoryTabs() {
    const tabs = document.getElementById('categoryTabs');
    if (!tabs) return;

    const customBtn = '<button class="category-btn" data-scope="custom">自定义</button>';
    const dynamic = shopCategoryList.map((cat) => {
        const count = cat.itemCount != null ? ` (${cat.itemCount})` : '';
        return `<button class="category-btn" data-scope="${escapeHtml(cat.id)}" title="${escapeHtml(cat.name)}">${escapeHtml(cat.name)}${count}</button>`;
    }).join('');

    tabs.innerHTML = `
        <button class="category-btn" data-scope="all">全部</button>
        <button class="category-btn" data-scope="in_shop">商店内</button>
        ${dynamic}
        ${customBtn}
    `;
    updateCategoryScopeButtons();
}

async function loadItems() {
    try {
        await loadShopCategories();
        const response = await fetch('/api/prices?t=' + new Date().getTime());

        if (!response.ok) throw new Error('网络响应失败');
        const rawData = await response.json();

        // 核心转换逻辑
        LETTER_SORT_CACHE.clear();
        allItems = Object.keys(rawData).map(key => ({
            id: key,
            name: rawData[key].displayName
                || rawData[key].customDisplayName
                || (window.getChineseName ? window.getChineseName(key) : key),
            buyPrice: rawData[key].buy,
            sellPrice: rawData[key].sell,
            source: rawData[key].source || 'vanilla',
            custom: rawData[key].custom === true,
            shop: rawData[key].shop || null,
            shopItem: rawData[key].item || null,
            buyAmount: rawData[key].amount || 1,
            displayName: rawData[key].displayName || rawData[key].customDisplayName || null,
            loreLine: normalizeLoreLine(rawData[key].loreLine
                || rawData[key].description
                || rawData[key].lore
                || (window.getItemLoreLine ? window.getItemLoreLine(key) : null)),
            shopCategories: Array.isArray(rawData[key].shopCategories) ? rawData[key].shopCategories : [],
            inShop: rawData[key].inShop === true,
            ultimateShopOffers: Array.isArray(rawData[key].ultimateShopOffers)
                ? rawData[key].ultimateShopOffers
                : []
        }));

        updateShopCategoryCounts();
        renderCategoryTabs();
        hydrateItemsStateFromUrl();
        initLetterIndexBar();
        updateLetterIndexButtons();
        filterAndRenderItems();
        tryOpenCartFromUrl();

    } catch (error) {
        console.error('加载物品数据出错:', error);
        const grid = document.getElementById('itemsGrid');
        if (grid) {
            grid.innerHTML = '<div style="color:#ef4444; text-align:center; grid-column:1/-1; padding: 40px; background: #1e293b; border-radius: 12px;">⚠️ 无法连接到后端数据库。</div>';
        }
    }
}

function formatUltimateShopPrice(val) {
    if (val === null || val === undefined) {
        return MC_EMPTY;
    }
    if (typeof val === 'number' && Number.isFinite(val)) {
        return `￥${val.toFixed(2)}`;
    }
    return String(val);
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

function formatOrderStatus(status) {
    if (status === 'delivering') return '发放中';
    if (status === 'delivered') return '已完成';
    if (status === 'failed') return '失败';
    return '待领取';
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

function updateCartBadge() {
    const badge = document.getElementById('cartBadge');
    if (!badge) return;
    const count = getCartTotalQuantity();
    badge.textContent = count > 99 ? '99+' : String(count);
    badge.hidden = count <= 0;
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
    document.body.style.overflow = 'hidden';
    renderCartDrawer();
    void loadPendingOrdersUi();
    syncItemsStateToUrl();
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
    document.body.style.overflow = '';
    syncItemsStateToUrl();
}

function addToCart(item, offer, quantity = 1) {
    const qty = Math.max(1, Math.floor(Number(quantity) || 1));
    const key = cartEntryKey(item.id, offer);
    const unitBuyPrice = resolveOfferUnitPrice(offer, item, 'buy');
    const shopName = offer.shopTitleResolved || offer.shopTitle || offer.shopId;
    const existing = shopCart.find((e) => e.key === key);
    if (existing) {
        existing.quantity += qty;
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
}

function setCartLineQuantity(key, quantity) {
    const entry = shopCart.find((e) => e.key === key);
    if (!entry) return;
    const qty = Math.floor(Number(quantity) || 0);
    if (qty <= 0) {
        shopCart = shopCart.filter((e) => e.key !== key);
    } else {
        entry.quantity = qty;
    }
    saveShopCart();
    updateCartBadge();
    renderCartDrawer();
}

function removeCartLine(key) {
    shopCart = shopCart.filter((e) => e.key !== key);
    saveShopCart();
    updateCartBadge();
    renderCartDrawer();
}

function clearShopCart() {
    shopCart = [];
    saveShopCart();
    updateCartBadge();
    renderCartDrawer();
}

function renderCartDrawer() {
    const body = document.getElementById('cartDrawerBody');
    const totalEl = document.getElementById('cartTotalPrice');
    const mapBtn = document.getElementById('cartMapBtn');
    const checkoutBtn = document.getElementById('cartCheckoutBtn');
    if (!body) return;

    if (!shopCart.length) {
        body.innerHTML = '<div class="cart-empty">购物车是空的<br>浏览物品并点击「加入购物车」</div>';
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
                        <span class="cart-qty-value">${entry.quantity}</span>
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
    if (window.McTextureAnim) window.McTextureAnim.initInContainer(body);
    if (window.McEnchantGlint) window.McEnchantGlint.initInContainer(body);

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

function isValidShopLocation(location) {
    return !!(location
        && location.enabled !== false
        && location.viewUrl);
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

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function normalizeLoreLine(value) {
    if (Array.isArray(value)) {
        return value.map(normalizeLoreLine).find(Boolean) || null;
    }
    if (value && typeof value === 'object') {
        if (value.text != null) return normalizeLoreLine(value.text);
        if (value.translate != null) return normalizeLoreLine(value.translate);
        return null;
    }
    if (value == null) return null;
    const text = String(value).replace(/§[0-9a-fk-or]/gi, '').trim();
    return text || null;
}

function isCustomCatalogItem(item) {
    if (!item || typeof item !== 'object') return false;
    return item.custom === true
        || item.isCustom === true
        || item.source === 'custom'
        || item.kind === 'custom'
        || item.type === 'custom';
}

function updateShopCategoryCounts() {
    const counts = {};
    shopCategoryList.forEach((cat) => {
        counts[cat.id] = 0;
    });
    allItems.forEach((item) => {
        (item.shopCategories || []).forEach((shopId) => {
            if (counts[shopId] != null) {
                counts[shopId] += 1;
            }
        });
    });
    shopCategoryList = shopCategoryList.map((cat) => ({
        ...cat,
        itemCount: counts[cat.id] || 0
    }));
}

function itemMatchesShopScope(item) {
    if (itemScope === 'all') {
        return true;
    }
    if (itemScope === 'in_shop') {
        return item.inShop === true || (item.ultimateShopOffers || []).length > 0;
    }
    if (itemScope === 'custom') {
        return isCustomCatalogItem(item);
    }
    if (ITEM_SCOPE_VALUES.has(itemScope) || shopCategoryList.some((cat) => cat.id === itemScope)) {
        return (item.shopCategories || []).includes(itemScope);
    }
    return true;
}

function updateCategoryScopeButtons() {
    const tabs = document.getElementById('categoryTabs');
    if (!tabs) return;
    tabs.querySelectorAll('.category-btn[data-scope]').forEach((btn) => {
        btn.classList.toggle('active', btn.dataset.scope === itemScope);
    });
}

function formatClockSystemTime(date) {
    if (window.McClockUi) return window.McClockUi.formatSystemTime(date);
    const d = date || new Date();
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    const ss = String(d.getSeconds()).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
}

function updateClockTimeDescriptions(root) {
    if (window.McClockUi) {
        window.McClockUi.updateTimeElements(root);
        return;
    }
    const host = root || document;
    const text = formatClockSystemTime();
    host.querySelectorAll('[data-clock-time-desc]').forEach((el) => {
        el.textContent = text;
        const container = el.closest('.scrolling-text');
        if (container) container.title = text;
    });
}

function ensureClockTimeTicker() {
    if (window.McClockUi) {
        window.McClockUi.ensureTimeTicker();
        return;
    }
    if (clockTimeTimer !== null) return;
    clockTimeTimer = setInterval(() => updateClockTimeDescriptions(document), 1000);
}

function formatCompassBearingForElement(el) {
    const deviceHeading = window.McPointerCompass && window.McPointerCompass.getDeviceHeading
        ? window.McPointerCompass.getDeviceHeading()
        : null;
    if (typeof deviceHeading === 'number') {
        return formatHeadingDegrees(deviceHeading);
    }
    const deviceStatus = window.McPointerCompass && window.McPointerCompass.getDeviceStatus
        ? window.McPointerCompass.getDeviceStatus()
        : '';
    if (isCoarsePointerDeviceForItems()) {
        if (deviceStatus === 'insecure') return '浏览器未开放罗盘：需要 HTTPS';
        if (deviceStatus === 'denied') return '系统指南针权限被拒绝';
        if (deviceStatus === 'unsupported') return '浏览器不支持系统指南针';
        if (deviceStatus === 'low-accuracy') return '罗盘精度较低，请校准手机';
        if (deviceStatus === 'permission') return '点击允许后读取系统指南针';
        return '等待系统指南针数据';
    }
    if (!el || pointerBearingX == null || pointerBearingY == null) return '移动鼠标查看方位';
    const heroWidget = el.closest('#heroCompassWidget');
    const card = el.closest('.glass');
    const icon = (heroWidget && heroWidget.querySelector('[data-item-id="compass"], [data-item-id="recovery_compass"]'))
        || (card && card.querySelector('[data-item-id="compass"], [data-item-id="recovery_compass"]'));
    if (!icon || !icon.getBoundingClientRect) return '移动鼠标查看方位';

    const rect = icon.getBoundingClientRect();
    const dx = pointerBearingX - (rect.left + rect.width / 2);
    const dy = pointerBearingY - (rect.top + rect.height / 2);
    if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) return '中心';

    const east = dx;
    const south = dy / POINTER_COMPASS_TILT_COS;
    const absE = Math.abs(east);
    const absS = Math.abs(south);
    if (absE < 0.001) return south >= 0 ? '正南' : '正北';
    if (absS < 0.001) return east >= 0 ? '正东' : '正西';

    const eastWest = east >= 0 ? '东' : '西';
    const northSouth = south >= 0 ? '南' : '北';
    const fromEastWest = Math.atan(absS / absE) * 180 / Math.PI;
    const fromNorthSouth = 90 - fromEastWest;
    if (fromEastWest <= 45) {
        return `${eastWest}偏${northSouth} ${fromEastWest.toFixed(1)} 度`;
    }
    return `${northSouth}偏${eastWest} ${fromNorthSouth.toFixed(1)} 度`;
}

function isCoarsePointerDeviceForItems() {
    return window.matchMedia && window.matchMedia('(pointer: coarse)').matches;
}

function formatHeadingDegrees(heading) {
    const h = ((heading % 360) + 360) % 360;
    if (h < 0.05 || h >= 359.95) return '正北';
    if (Math.abs(h - 90) < 0.05) return '正东';
    if (Math.abs(h - 180) < 0.05) return '正南';
    if (Math.abs(h - 270) < 0.05) return '正西';
    if (h < 45) return `北偏东 ${h.toFixed(1)} 度`;
    if (h < 90) return `东偏北 ${(90 - h).toFixed(1)} 度`;
    if (h < 135) return `东偏南 ${(h - 90).toFixed(1)} 度`;
    if (h < 180) return `南偏东 ${(180 - h).toFixed(1)} 度`;
    if (h < 225) return `南偏西 ${(h - 180).toFixed(1)} 度`;
    if (h < 270) return `西偏南 ${(270 - h).toFixed(1)} 度`;
    if (h < 315) return `西偏北 ${(h - 270).toFixed(1)} 度`;
    return `北偏西 ${(360 - h).toFixed(1)} 度`;
}

function updatePointerBearingDescriptions(root) {
    const host = root || document;
    host.querySelectorAll('[data-pointer-bearing-desc]').forEach((el) => {
        const text = formatCompassBearingForElement(el);
        el.textContent = text;
        if (el.hasAttribute('data-pointer-bearing-title')) {
            el.title = text;
        } else {
            const container = el.closest('.scrolling-text');
            if (container) container.title = text;
        }
    });
}

function initItemsHeroCompass() {
    const mount = document.getElementById('heroCompassIconMount');
    if (!mount || mount.dataset.heroCompassReady === '1') return;

    if (typeof getTextureHtml === 'function') {
        mount.innerHTML = getTextureHtml('compass', '指南针');
    } else if (window.getTextureHtml) {
        mount.innerHTML = window.getTextureHtml('compass', '指南针');
    }

    const canvas = mount.querySelector('canvas[data-item-id="compass"]');
    if (canvas && window.McTextureAnim) {
        const urls = window.mcMouseCompassTextureUrlsForItem
            ? window.mcMouseCompassTextureUrlsForItem('compass')
            : (canvas.dataset.texUrls || '').split('|').filter(Boolean);
        if (urls.length) {
            if (!canvas.dataset.texUrls) {
                canvas.dataset.texUrls = urls.join('|');
            }
            window.McTextureAnim.initCanvasFromUrls(canvas, urls).then((ok) => {
                if (ok) {
                    canvas.style.opacity = '1';
                    canvas.dataset.texReady = '1';
                }
            });
        }
    }

    const widget = document.getElementById('heroCompassWidget');
    updatePointerBearingDescriptions(widget);
    ensurePointerBearingTicker();
    mount.dataset.heroCompassReady = '1';
}

function ensurePointerBearingTicker() {
    if (pointerBearingTimer !== null) return;
    if (window.McPointerCompass && window.McPointerCompass.requestDeviceCompass) {
        window.McPointerCompass.requestDeviceCompass();
    }
    const updatePointer = (event) => {
        pointerBearingX = event.clientX;
        pointerBearingY = event.clientY;
        updatePointerBearingDescriptions(document);
    };
    window.addEventListener('pointermove', updatePointer, { passive: true });
    window.addEventListener('mousemove', updatePointer, { passive: true });
    pointerBearingTimer = setInterval(() => updatePointerBearingDescriptions(document), 100);
}

function showDialog(modal) {
    if (!modal) return;
    modal.classList.remove('closing');
    modal.classList.add('active');
}

function hideDialog(modal, afterClose) {
    if (!modal || !modal.classList.contains('active')) {
        if (typeof afterClose === 'function') afterClose();
        return;
    }
    modal.classList.add('closing');
    modal.classList.remove('active');
    window.setTimeout(() => {
        modal.classList.remove('closing');
        if (typeof afterClose === 'function') afterClose();
    }, 190);
}

function handleAddToCartClick(itemId) {
    const item = allItems.find(i => i.id === itemId);
    if (!item) {
        syncItemsStateToUrl();
        return showToast('未找到该物品。', false);
    }
    const offers = item.ultimateShopOffers || [];
    if (!offers.length) {
        syncItemsStateToUrl();
        return showToast('该物品未上架，无法加入购物车。', false);
    }
    if (offers.length === 1) {
        addToCart(item, offers[0], 1);
        showToast(`已加入购物车：${item.name}`, true);
        syncItemsStateToUrl();
        return;
    }
    openCartOfferModal(item);
}

function closeTradeModal() {
    const modal = document.getElementById('tradeModal');
    hideDialog(modal, () => {
        if (modal) delete modal.dataset.tradeItemId;
        syncItemsStateToUrl();
    });
}

function openCartOfferModal(item) {
    const modal = document.getElementById('tradeModal');
    const title = document.getElementById('tradeModalTitle');
    const body = document.getElementById('tradeModalBody');
    if (!modal || !title || !body) {
        return;
    }

    title.textContent = sanitizeMcFontText(`加入购物车${MC_FONT_SEP}${item.name}`);

    const offers = item.ultimateShopOffers || [];
    const blocks = offers.map((o, idx) => {
        const shopName = o.shopTitleResolved || o.shopTitle;
        const shopLabel = shopName != null && String(shopName).trim() !== ''
            ? escapeHtml(String(shopName))
            : escapeHtml(o.shopId);
        const buyDisplay = o.buyAmountResolved != null ? o.buyAmountResolved : o.buyAmount;
        const sellDisplay = o.sellAmountResolved != null ? o.sellAmountResolved : o.sellAmount;
        const mapUrl = appendTradeParamsToMapUrl(blueMapUrlForLocation(o.location), item);
        const mapButton = mapUrl
            ? `<a class="trade-map-link" href="${escapeHtml(mapUrl)}" target="_blank" rel="noopener">在地图上查看</a>`
            : '<span class="trade-map-missing">未配置地图位置</span>';
        const locationText = isValidShopLocation(o.location)
            ? sanitizeMcFontText(`${o.location.description ? `${o.location.description}${MC_FONT_SEP}` : ''}${o.location.viewUrl}`)
            : MC_EMPTY;
        const offerKey = cartEntryKey(item.id, o);
        return `
            <div class="trade-offer-card" data-offer-key="${escapeHtml(offerKey)}">
                <h4>上架位置 ${idx + 1}</h4>
                <dl class="trade-offer-meta">
                    <dt>商店 ID</dt>
                    <dd>${escapeHtml(o.shopId)}</dd>
                    <dt>商店名称</dt>
                    <dd>${shopLabel}</dd>
                    <dt>商品槽位</dt>
                    <dd>${escapeHtml(o.slot)}</dd>
                    <dt>买入价</dt>
                    <dd>${escapeHtml(formatUltimateShopPrice(buyDisplay))}</dd>
                    <dt>卖出价</dt>
                    <dd>${escapeHtml(formatUltimateShopPrice(sellDisplay))}</dd>
                    <dt>地图位置</dt>
                    <dd>${escapeHtml(locationText)}</dd>
                </dl>
                <div style="margin-top:12px; display:flex; justify-content:flex-end; gap:10px; flex-wrap:wrap;">
                    ${mapButton}
                    <button type="button" class="cart-offer-add-btn" data-cart-offer-add="${escapeHtml(offerKey)}">加入购物车</button>
                </div>
            </div>
        `;
    }).join('');

    body.innerHTML = `
        <p class="trade-modal-hint">
            该物品在多个商店上架，请选择要加入购物车的位置。
        </p>
        <div class="trade-offer-list">${blocks}</div>
    `;

    body.querySelectorAll('[data-cart-offer-add]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const offerKey = btn.getAttribute('data-cart-offer-add');
            const offer = offers.find((o) => cartEntryKey(item.id, o) === offerKey);
            if (!offer) return;
            addToCart(item, offer, 1);
            showToast(`已加入购物车：${item.name}`, true);
            closeTradeModal();
            openCartDrawer();
        });
    });

    showDialog(modal);
    modal.dataset.tradeItemId = item.id;
    syncItemsStateToUrl();
}

function showToast(message, success = true) {
    const toast = document.createElement('div');
    toast.textContent = message;
    toast.style.position = 'fixed';
    toast.style.bottom = '24px';
    toast.style.left = '50%';
    toast.style.transform = 'translateX(-50%)';
    toast.style.padding = '14px 18px';
    toast.style.borderRadius = '12px';
    toast.style.background = success ? 'rgba(34,197,94,0.95)' : 'rgba(239,68,68,0.95)';
    toast.style.color = '#fff';
    toast.style.fontSize = '0.95rem';
    toast.style.zIndex = 10100;
    toast.style.boxShadow = '0 10px 30px rgba(0,0,0,0.2)';
    toast.style.maxWidth = 'calc(100% - 40px)';
    toast.style.textAlign = 'center';
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'opacity 0.25s ease';
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 250);
    }, 3200);
}

// 搜索过滤与排序逻辑（新增逆序处理和拼音搜索支持）
/** 美式 QWERTY：盲打时易误触的邻键（仅 a-z、0-9 与常用符号） */
const KEYBOARD_NEIGHBORS = {
    '`': ['1', '~'],
    '1': ['`', '2', 'q'],
    '2': ['1', '3', 'q', 'w'],
    '3': ['2', '4', 'w', 'e'],
    '4': ['3', '5', 'e', 'r'],
    '5': ['4', '6', 'r', 't'],
    '6': ['5', '7', 't', 'y'],
    '7': ['6', '8', 'y', 'u'],
    '8': ['7', '9', 'u', 'i'],
    '9': ['8', '0', 'i', 'o'],
    '0': ['9', '-', 'o', 'p'],
    '-': ['0', '=', 'p', '['],
    '=': ['-', '[', ']'],
    q: ['1', '2', 'w', 'a'],
    w: ['q', '2', '3', 'e', 'a', 's'],
    e: ['w', '3', '4', 'r', 's', 'd'],
    r: ['e', '4', '5', 't', 'd', 'f'],
    t: ['r', '5', '6', 'y', 'f', 'g'],
    y: ['t', '6', '7', 'u', 'g', 'h'],
    u: ['y', '7', '8', 'i', 'h', 'j'],
    i: ['u', '8', '9', 'o', 'j', 'k'],
    o: ['i', '9', '0', 'p', 'k', 'l'],
    p: ['o', '0', '-', '[', 'l', ';'],
    a: ['q', 'w', 's', 'z'],
    s: ['a', 'w', 'e', 'd', 'z', 'x'],
    d: ['s', 'e', 'r', 'f', 'x', 'c'],
    f: ['d', 'r', 't', 'g', 'c', 'v'],
    g: ['f', 't', 'y', 'h', 'v', 'b'],
    h: ['g', 'y', 'u', 'j', 'b', 'n'],
    j: ['h', 'u', 'i', 'k', 'n', 'm'],
    k: ['j', 'i', 'o', 'l', 'm', ','],
    l: ['k', 'o', 'p', ';', ',', '.'],
    z: ['a', 's', 'x'],
    x: ['z', 's', 'd', 'c'],
    c: ['x', 'd', 'f', 'v'],
    v: ['c', 'f', 'g', 'b'],
    b: ['v', 'g', 'h', 'n'],
    n: ['b', 'h', 'j', 'm'],
    m: ['n', 'j', 'k', ','],
    '[': ['p', '-', '=', ']', ';', "'"],
    ']': ['[', '=', '\\', "'"],
    '\\': [']', "'"],
    ';': ['p', '[', 'l', "'", '/'],
    "'": [';', '[', ']', '/'],
    ',': ['m', 'k', 'l', '.'],
    '.': [',', 'l', ';', '/'],
    '/': ['.', ';', "'"]
};

const KEYBOARD_CHAR_SET_CACHE = new Map();

/** 忽略拼音输入法误触的单引号分隔（如 指南针 → z'n'z、co'm） */
function normalizeSearchText(value) {
    return String(value || '')
        .toLowerCase()
        .replace(/[''`´′＇]/g, '');
}

function compactSearchText(value) {
    return normalizeSearchText(value).replace(/[\s_'`´′＇_-]+/g, '');
}

function getKeyboardCharSet(ch) {
    if (KEYBOARD_CHAR_SET_CACHE.has(ch)) {
        return KEYBOARD_CHAR_SET_CACHE.get(ch);
    }
    const set = new Set([ch]);
    const neighbors = KEYBOARD_NEIGHBORS[ch];
    if (neighbors) {
        neighbors.forEach((n) => set.add(n));
    }
    KEYBOARD_CHAR_SET_CACHE.set(ch, set);
    return set;
}

/** 同一键或 QWERTY 邻键（仅拉丁字母/数字/符号） */
function charsKeyboardClose(a, b) {
    if (a === b) return true;
    if (!a || !b || a.length !== 1 || b.length !== 1) return false;
    if (!/^[a-z0-9`\-=[\]\\;',./]$/.test(a) || !/^[a-z0-9`\-=[\]\\;',./]$/.test(b)) {
        return false;
    }
    return getKeyboardCharSet(a).has(b);
}

/** 连续子串：每位为原字符或键盘邻键 */
function keyboardFuzzyIncludes(haystack, needle) {
    if (!needle) return true;
    if (!haystack || needle.length > haystack.length) return false;
    if (needle.length < 2) {
        return haystack.includes(needle);
    }

    const limit = haystack.length - needle.length;
    for (let start = 0; start <= limit; start += 1) {
        let ok = true;
        for (let i = 0; i < needle.length; i += 1) {
            if (!charsKeyboardClose(needle[i], haystack[start + i])) {
                ok = false;
                break;
            }
        }
        if (ok) return true;
    }
    return false;
}

function prefixKeyboardMatch(word, prefix) {
    if (!prefix) return true;
    if (!word || prefix.length > word.length) return false;
    for (let i = 0; i < prefix.length; i += 1) {
        if (!charsKeyboardClose(prefix[i], word[i])) return false;
    }
    return true;
}

function normalizePinyinNasal(value) {
    return normalizeSearchText(value)
        .replace(/\s+/g, '')
        .replace(/zh/g, 'z')
        .replace(/ch/g, 'c')
        .replace(/sh/g, 's')
        .replace(/ang/g, 'an')
        .replace(/eng/g, 'en')
        .replace(/ing/g, 'in');
}

function pinyinPrefixComboMatch(syllables, query) {
    const parts = syllables.map((part) => String(part || '').toLowerCase()).filter(Boolean);
    const q = normalizeSearchText(query).replace(/\s+/g, '');
    if (!parts.length || !q) return false;

    const canMatchFrom = (start) => {
        const memo = new Set();
        const dfs = (idx, rest) => {
            if (!rest) return true;
            if (idx >= parts.length) return false;
            const key = `${idx}|${rest}`;
            if (memo.has(key)) return false;
            const part = parts[idx];
            const maxLen = Math.min(part.length, rest.length);
            for (let len = 1; len <= maxLen; len += 1) {
                if (rest.startsWith(part.slice(0, len)) && dfs(idx + 1, rest.slice(len))) {
                    return true;
                }
            }
            memo.add(key);
            return false;
        };
        return dfs(start, q);
    };

    for (let i = 0; i < parts.length; i += 1) {
        if (canMatchFrom(i)) return true;
    }
    return false;
}

const LETTER_SORT_CACHE = new Map();

function getItemSortLetter(item) {
    if (!item || !item.id) return '';
    if (LETTER_SORT_CACHE.has(item.id)) {
        return LETTER_SORT_CACHE.get(item.id);
    }

    let letter = '';
    const name = String(item.name || '').trim();
    const firstChar = name.charAt(0);
    if (/[a-zA-Z]/.test(firstChar)) {
        letter = firstChar.toUpperCase();
    } else if (typeof pinyinPro !== 'undefined' && name) {
        try {
            const initial = pinyinPro.pinyin(name, {
                pattern: 'first',
                toneType: 'none',
                type: 'array'
            });
            const ch = Array.isArray(initial) && initial[0]
                ? String(initial[0]).charAt(0).toUpperCase()
                : '';
            if (/[A-Z]/.test(ch)) {
                letter = ch;
            } else {
                const initials = pinyinPro.pinyin(name, {
                    pattern: 'initial',
                    toneType: 'none',
                    type: 'string'
                }).replace(/\s+/g, '');
                const fromInitial = initials.charAt(0).toUpperCase();
                if (/[A-Z]/.test(fromInitial)) {
                    letter = fromInitial;
                }
            }
        } catch (e) {
            // ignore
        }
    }

    if (!letter) {
        const idFirst = String(item.id).replace(/^minecraft:/i, '').charAt(0).toUpperCase();
        if (/[A-Z]/.test(idFirst)) {
            letter = idFirst;
        }
    }

    LETTER_SORT_CACHE.set(item.id, letter);
    return letter;
}

function itemMatchesLetterFilter(item) {
    if (!letterFilter) return true;
    return getItemSortLetter(item) === letterFilter;
}

function updateLetterIndexButtons() {
    document.querySelectorAll('.letter-index-bar .letter-btn').forEach((btn) => {
        btn.classList.toggle('active', btn.dataset.letter === letterFilter);
    });
}

function setLetterFilter(letter) {
    const next = letter && /^[A-Z]$/.test(letter) ? letter : '';
    if (letterFilter === next) {
        if (next) {
            letterFilter = '';
        } else {
            return;
        }
    } else {
        letterFilter = next;
    }
    updateLetterIndexButtons();
    currentPage = 1;
    filterAndRenderItems();
}

function initLetterIndexBar() {
    const bar = document.getElementById('letterIndexBar');
    if (!bar || bar.dataset.ready === '1') return;

    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
    bar.innerHTML = letters.map((ch) => (
        `<button type="button" class="letter-btn" data-letter="${ch}" aria-label="首字母 ${ch}">${ch}</button>`
    )).join('');

    bar.addEventListener('click', (e) => {
        const btn = e.target.closest('.letter-btn');
        if (!btn) return;
        setLetterFilter(btn.dataset.letter || '');
    });

    bar.dataset.ready = '1';
    updateLetterIndexButtons();
}

function splitSearchWords(value) {
    return normalizeSearchText(value)
        .split(/[\s_'`´′＇_-]+/)
        .map((part) => part.trim())
        .filter(Boolean);
}

function prefixComboMatch(parts, query) {
    const words = parts.map((part) => String(part || '').toLowerCase()).filter(Boolean);
    const q = normalizeSearchText(query).replace(/[\s_'`´′＇_-]+/g, '');
    if (!words.length || !q) return false;

    const canMatchFrom = (start) => {
        const memo = new Set();
        const dfs = (idx, rest) => {
            if (!rest) return true;
            if (idx >= words.length) return false;
            const key = `${idx}|${rest}`;
            if (memo.has(key)) return false;
            const word = words[idx];
            const maxLen = Math.min(word.length, rest.length);
            for (let len = 1; len <= maxLen; len += 1) {
                if (rest.startsWith(word.slice(0, len)) && dfs(idx + 1, rest.slice(len))) {
                    return true;
                }
            }
            memo.add(key);
            return false;
        };
        return dfs(start, q);
    };

    for (let i = 0; i < words.length; i += 1) {
        if (canMatchFrom(i)) return true;
    }
    return false;
}

function keyboardPrefixComboMatch(parts, query) {
    const words = parts.map((part) => String(part || '').toLowerCase()).filter(Boolean);
    const q = compactSearchText(query);
    if (!words.length || !q || q.length < 2) return false;

    const canMatchFrom = (start) => {
        const memo = new Set();
        const dfs = (idx, rest) => {
            if (!rest) return true;
            if (idx >= words.length) return false;
            const key = `${idx}|${rest}`;
            if (memo.has(key)) return false;
            const word = words[idx];
            const maxLen = Math.min(word.length, rest.length);
            for (let len = 1; len <= maxLen; len += 1) {
                if (prefixKeyboardMatch(word, rest.slice(0, len)) && dfs(idx + 1, rest.slice(len))) {
                    return true;
                }
            }
            memo.add(key);
            return false;
        };
        return dfs(start, q);
    };

    for (let i = 0; i < words.length; i += 1) {
        if (canMatchFrom(i)) return true;
    }
    return false;
}

function keyboardPinyinPrefixComboMatch(syllables, query) {
    const parts = syllables.map((part) => String(part || '').toLowerCase()).filter(Boolean);
    const q = compactSearchText(query);
    if (!parts.length || !q || q.length < 2) return false;

    const canMatchFrom = (start) => {
        const memo = new Set();
        const dfs = (idx, rest) => {
            if (!rest) return true;
            if (idx >= parts.length) return false;
            const key = `${idx}|${rest}`;
            if (memo.has(key)) return false;
            const part = parts[idx];
            const maxLen = Math.min(part.length, rest.length);
            for (let len = 1; len <= maxLen; len += 1) {
                if (prefixKeyboardMatch(part, rest.slice(0, len)) && dfs(idx + 1, rest.slice(len))) {
                    return true;
                }
            }
            memo.add(key);
            return false;
        };
        return dfs(start, q);
    };

    for (let i = 0; i < parts.length; i += 1) {
        if (canMatchFrom(i)) return true;
    }
    return false;
}

function textMatchesSearchQuery(text, query) {
    const lower = String(text || '').toLowerCase();
    const compactText = compactSearchText(lower);
    const compactQuery = compactSearchText(query);
    if (!compactQuery) return true;

    if (lower.includes(query) || compactText.includes(compactQuery)) return true;
    if (prefixComboMatch(splitSearchWords(lower), query)) return true;

    if (compactQuery.length < 2) return false;
    if (keyboardFuzzyIncludes(compactText, compactQuery)) return true;
    return keyboardPrefixComboMatch(splitSearchWords(lower), query);
}

function itemMatchesSearchQuery(item, query) {
    const compactQuery = compactSearchText(query);
    if (!compactQuery) return true;

    const itemNameLower = item.name.toLowerCase();
    const itemIdLower = item.id.toLowerCase();
    if (textMatchesSearchQuery(itemNameLower, query) || textMatchesSearchQuery(itemIdLower, query)) {
        return true;
    }

    if (typeof pinyinPro === 'undefined') return false;

    try {
        const namePinyinSpaced = pinyinPro.pinyin(item.name, { toneType: 'none', type: 'string' }).toLowerCase();
        const namePinyinSyllables = namePinyinSpaced.split(/\s+/).filter(Boolean);
        const namePinyin = namePinyinSpaced.replace(/\s+/g, '');
        const namePinyinInitial = pinyinPro.pinyin(item.name, { pattern: 'initial', toneType: 'none', type: 'string' }).toLowerCase().replace(/\s+/g, '');
        const namePinyinComputedInitial = namePinyinSyllables
            .map((part) => part.charAt(0))
            .join('');
        const fuzzyNamePinyin = normalizePinyinNasal(namePinyin);
        const fuzzyNamePinyinInitial = normalizePinyinNasal(namePinyinInitial);
        const fuzzyNamePinyinComputedInitial = normalizePinyinNasal(namePinyinComputedInitial);
        const fuzzyNamePinyinSyllables = namePinyinSyllables.map(normalizePinyinNasal);
        const fuzzyQuery = normalizePinyinNasal(query);

        if (textMatchesSearchQuery(namePinyin, query)
            || textMatchesSearchQuery(namePinyinInitial, query)
            || textMatchesSearchQuery(namePinyinComputedInitial, query)
            || textMatchesSearchQuery(fuzzyNamePinyin, fuzzyQuery)
            || textMatchesSearchQuery(fuzzyNamePinyinInitial, fuzzyQuery)
            || textMatchesSearchQuery(fuzzyNamePinyinComputedInitial, fuzzyQuery)) {
            return true;
        }

        if (pinyinPrefixComboMatch(namePinyinSyllables, query)
            || pinyinPrefixComboMatch(fuzzyNamePinyinSyllables, fuzzyQuery)) {
            return true;
        }

        if (compactQuery.length >= 2) {
            return keyboardPinyinPrefixComboMatch(namePinyinSyllables, query)
                || keyboardPinyinPrefixComboMatch(fuzzyNamePinyinSyllables, fuzzyQuery);
        }
    } catch (e) {
        // ignore
    }

    return false;
}

function filterAndRenderItems() {
    filteredItems = allItems.filter(item => {
        if (!itemMatchesLetterFilter(item)) {
            return false;
        }

        const query = normalizeSearchText(searchQuery);
        if (!query) return true;

        return itemMatchesSearchQuery(item, query);
    });

    filteredItems = filteredItems.filter((item) => itemMatchesShopScope(item));

    if (hideUntradable && itemScope === 'all') {
        filteredItems = filteredItems.filter(item => (item.ultimateShopOffers || []).length > 0);
    }

    // 原有排序逻辑
    if (currentSort === 'name') {
        filteredItems.sort((a, b) => a.name.localeCompare(b.name));
    } else if (currentSort === 'buyPrice') {
        filteredItems.sort((a, b) => a.buyPrice - b.buyPrice);
    } else if (currentSort === 'sellPrice') {
        filteredItems.sort((a, b) => a.sellPrice - b.sellPrice);
    } else if (currentSort === 'stock') {
        // 补充 stock 排序（原HTML有选项但JS未处理）
        filteredItems.sort((a, b) => (b.stock || 0) - (a.stock || 0));
    }

    // 新增：如果勾选逆序，反转排序结果
    if (sortReverse) {
        filteredItems.reverse();
    }

    const pageCount = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
    if (currentPage > pageCount) {
        currentPage = pageCount;
    }
    if (currentPage < 1) {
        currentPage = 1;
    }

    renderCards();
}

function updateSearchClearButton() {
    const inp = document.getElementById('searchInput');
    const btn = document.getElementById('searchClearBtn');
    if (!inp || !btn) return;
    btn.hidden = inp.value.length === 0;
}

function pageForClearedSearch() {
    const fallback = pageBeforeSearch || currentPage || 1;
    pageBeforeSearch = null;
    return fallback;
}

function clearSearchInput() {
    const inp = document.getElementById('searchInput');
    if (!inp) return;
    const restorePage = pageForClearedSearch();
    inp.value = '';
    searchQuery = '';
    updateSearchClearButton();
    currentPage = restorePage;
    filterAndRenderItems();
    inp.focus();
}

function setupEventListeners() {
    initLetterIndexBar();

    // 监听搜索框输入
    const searchInput = document.getElementById('searchInput');
    const searchClearBtn = document.getElementById('searchClearBtn');
    if (searchInput) {
        updateSearchClearButton();
        searchInput.addEventListener('input', (e) => {
            const previousQuery = searchQuery;
            const nextQuery = normalizeSearchText(e.target.value);
            if (!previousQuery && nextQuery && pageBeforeSearch === null) {
                pageBeforeSearch = currentPage;
            }
            searchQuery = nextQuery;
            updateSearchClearButton();
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                currentPage = searchQuery ? 1 : pageForClearedSearch();
                filterAndRenderItems();
            }, 200);
        });
    }
    if (searchClearBtn) {
        searchClearBtn.addEventListener('click', clearSearchInput);
    }

    // 监听刷新按钮
    const refreshButton = document.getElementById('refreshButton');
    if (refreshButton) {
        refreshButton.addEventListener('click', async () => {
            refreshButton.disabled = true;
            refreshButton.textContent = '刷新中...';
            await loadItems();
            refreshButton.disabled = false;
            refreshButton.innerHTML = '<span class="refresh-icon">⟳</span>';
        });
    }

    // 监听排序下拉菜单切换
    const sortSelect = document.getElementById('sortSelect');
    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            currentSort = e.target.value;
            currentPage = 1;
            filterAndRenderItems();
        });
    }

    // 新增：监听逆序复选框切换
    const sortReverseCheckbox = document.getElementById('sortReverse');
    if (sortReverseCheckbox) {
        sortReverseCheckbox.addEventListener('change', (e) => {
            sortReverse = e.target.checked;
            currentPage = 1;
            filterAndRenderItems(); // 切换后重新排序渲染
        });
    }

    const hideUntradableCheckbox = document.getElementById('hideUntradable');
    if (hideUntradableCheckbox) {
        hideUntradableCheckbox.addEventListener('change', (e) => {
            hideUntradable = e.target.checked;
            currentPage = 1;
            filterAndRenderItems();
        });
    }

    /** 页尾翻页后滚到物品区时，在网格顶端之上多留的像素（避免第一行被导航栏裁切） */
    const SCROLL_GRID_EXTRA_TOP_PX = 120;

    function scrollItemsGridIntoView() {
        const grid = document.getElementById('itemsGrid');
        if (!grid) {
            return;
        }
        const y = grid.getBoundingClientRect().top + window.scrollY - SCROLL_GRID_EXTRA_TOP_PX;
        window.scrollTo({ top: Math.max(0, y), behavior: 'smooth' });
    }

    function bindPager(firstId, prevId, nextId, lastId, scrollAfter) {
        const pageFirst = document.getElementById(firstId);
        const pagePrev = document.getElementById(prevId);
        const pageNext = document.getElementById(nextId);
        const pageLast = document.getElementById(lastId);
        const afterPageChange = () => {
            if (scrollAfter) {
                scrollItemsGridIntoView();
            }
        };
        if (pageFirst) {
            pageFirst.addEventListener('click', () => {
                if (currentPage > 1) {
                    currentPage = 1;
                    renderCards();
                    afterPageChange();
                }
            });
        }
        if (pagePrev) {
            pagePrev.addEventListener('click', () => {
                if (currentPage > 1) {
                    currentPage -= 1;
                    renderCards();
                    afterPageChange();
                }
            });
        }
        if (pageNext) {
            pageNext.addEventListener('click', () => {
                const pageCount = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
                if (currentPage < pageCount) {
                    currentPage += 1;
                    renderCards();
                    afterPageChange();
                }
            });
        }
        if (pageLast) {
            pageLast.addEventListener('click', () => {
                const pageCount = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
                if (currentPage < pageCount) {
                    currentPage = pageCount;
                    renderCards();
                    afterPageChange();
                }
            });
        }
    }

    bindPager('pageFirst', 'pagePrev', 'pageNext', 'pageLast', false);
    bindPager('pageFirstBottom', 'pagePrevBottom', 'pageNextBottom', 'pageLastBottom', true);

    // 绑定跳转功能
    function bindPageJump(inputId, btnId, scrollAfter) {
        const input = document.getElementById(inputId);
        const btn = document.getElementById(btnId);
        
        const jumpToPage = () => {
            const pageCount = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
            const targetPage = parseInt(input.value);
            
            if (isNaN(targetPage) || targetPage < 1 || targetPage > pageCount) {
                // 显示错误提示
                showToast(`请输入有效的页码 (1-${pageCount})`, false);
                input.focus();
                return;
            }
            
            currentPage = targetPage;
            renderCards();
            if (scrollAfter) {
                scrollItemsGridIntoView();
            }
            input.value = ''; // 清空输入框
        };
        
        if (btn) {
            btn.addEventListener('click', jumpToPage);
        }
        
        if (input) {
            input.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    jumpToPage();
                }
            });
        }
    }

    bindPageJump('pageJumpInput', 'pageJumpBtn', false);
    bindPageJump('pageJumpInputBottom', 'pageJumpBtnBottom', true);

    const itemsGrid = document.getElementById('itemsGrid');
    if (itemsGrid) {
        itemsGrid.addEventListener('click', (e) => {
            const btn = e.target.closest('.cart-btn, .trade-btn');
            if (!btn) {
                return;
            }
            e.preventDefault();
            const itemId = btn.getAttribute('data-item-id');
            if (itemId) {
                handleAddToCartClick(itemId);
            }
        });
    }

    const cartToggleBtn = document.getElementById('cartToggleBtn');
    cartToggleBtn?.addEventListener('click', () => {
        if (cartDrawerOpen) closeCartDrawer();
        else openCartDrawer();
    });
    document.getElementById('cartCloseBtn')?.addEventListener('click', closeCartDrawer);
    document.getElementById('cartBackdrop')?.addEventListener('click', closeCartDrawer);
    document.getElementById('cartClearBtn')?.addEventListener('click', () => {
        if (!shopCart.length) return;
        clearShopCart();
        showToast('购物车已清空', true);
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
            if (action === 'remove') {
                removeCartLine(key);
                return;
            }
            if (action === 'inc') {
                setCartLineQuantity(key, entry.quantity + 1);
                return;
            }
            if (action === 'dec') {
                setCartLineQuantity(key, entry.quantity - 1);
            }
        });
    }

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && cartDrawerOpen) {
            closeCartDrawer();
        }
    });

    const categoryTabs = document.getElementById('categoryTabs');
    if (categoryTabs) {
        categoryTabs.addEventListener('click', (e) => {
            const btn = e.target.closest('.category-btn[data-scope]');
            if (!btn) return;
            const nextScope = btn.dataset.scope || 'all';
            if (!STATIC_ITEM_SCOPES.has(nextScope) && !shopCategoryList.some((cat) => cat.id === nextScope)) return;
            if (nextScope === itemScope) return;
            itemScope = nextScope;
            currentPage = 1;
            updateCategoryScopeButtons();
            filterAndRenderItems();
        });
        updateCategoryScopeButtons();
    }

    const tradeModal = document.getElementById('tradeModal');
    if (tradeModal) {
        tradeModal.addEventListener('click', (e) => {
            if (e.target === tradeModal) {
                closeTradeModal();
            }
        });
    }
}

// 将数据渲染为 HTML 卡片（无修改）
function renderCards() {
    const grid = document.getElementById('itemsGrid');
    if (!grid) {
        syncItemsStateToUrl();
        return;
    }

    // 更新页面顶部和列表显示计数
    const itemCount = document.getElementById('itemCount');
    if (itemCount) {
        itemCount.textContent = `共计 ${allItems.length} 个物品`;
    }

    const itemsShowing = document.getElementById('itemsShowing');
    if (itemsShowing) {
        itemsShowing.textContent = `显示 ${filteredItems.length} 个物品`;
    }

    if (filteredItems.length === 0) {
        grid.innerHTML = '<div style="text-align:center; color:#94a3b8; grid-column:1/-1; padding: 40px;">没有找到匹配的物品 📦</div>';
        renderPagination();
        syncItemsStateToUrl();
        return;
    }

    const pageCount = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
    const startIndex = (currentPage - 1) * PAGE_SIZE;
    const pageItems = filteredItems.slice(startIndex, startIndex + PAGE_SIZE);
    const duplicateNames = new Set();
    const nameCounts = new Map();
    allItems.forEach(item => {
        nameCounts.set(item.name, (nameCounts.get(item.name) || 0) + 1);
    });
    nameCounts.forEach((count, name) => {
        if (count > 1) duplicateNames.add(name);
    });

    grid.innerHTML = pageItems.map(item => {
        const offers = item.ultimateShopOffers || [];
        const canAdd = offers.length > 0;
        const cartBtnClass = canAdd ? 'cart-btn cart-btn--active' : 'cart-btn cart-btn--disabled';
        const isClock = item.id === 'clock';
        const isPointerCompass = item.id === 'compass' || item.id === 'recovery_compass';
        const descriptionText = isClock
            ? formatClockSystemTime()
            : isPointerCompass
                ? '移动鼠标查看方位'
            : (duplicateNames.has(item.name) && item.loreLine ? item.loreLine : '');
        const loreLine = descriptionText
            ? `<span class="scrolling-text" style="margin-top:2px; font-size:0.82rem; color:#94a3b8;" title="${escapeHtml(descriptionText)}"><span class="scrolling-text-inner"${isClock ? ' data-clock-time-desc="1"' : ''}${isPointerCompass ? ' data-pointer-bearing-desc="1"' : ''}>${escapeHtml(descriptionText)}</span></span>`
            : '';
        const safeName = escapeHtml(item.name);
        const safeId = escapeHtml(item.id);

        return `
        <div class="glass card-hover" style="border-radius:12px; padding:20px; transition:all 0.3s ease; position:relative; overflow:hidden; border:1px solid rgba(255,255,255,0.05); background: var(--bg-card);">
            
            <div style="position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg, #3b82f6, #8b5cf6);"></div>

            <div style="display:flex; align-items:center; margin-bottom:15px; margin-top: 5px;">
                ${getItemIconHtml(item.id, item.name)}
                <div style="min-width:0; width:100%;">
                    <h3 class="scrolling-text" style="margin:0; font-size:1.1rem; color:#F1F5F9; font-weight: 600;" title="${safeName}"><span class="scrolling-text-inner">${safeName}</span></h3>
                    ${loreLine}
                    <span class="scrolling-text scrolling-id" style="font-size:0.75rem; color:#64748b; text-transform: lowercase;" title="${safeId}"><span class="scrolling-text-inner scrolling-id-text">${safeId}</span></span>
                </div>
            </div>
            
            <div style="background: rgba(15,23,42,0.6); padding:12px; border-radius:8px; border: 1px solid rgba(255,255,255,0.02);">
                <div style="display:flex; justify-content:space-between; margin-bottom:8px; align-items: center;">
                    <span style="color:#94A3B8; font-size:0.85rem;">系统买入</span>
                    <strong style="color:#34D399; font-family: monospace; font-size: 1.05rem;">￥${item.buyPrice.toFixed(2)}</strong>
                </div>
                <div style="display:flex; justify-content:space-between; align-items: center;">
                    <span style="color:#94A3B8; font-size:0.85rem;">玩家回收</span>
                    <strong style="color:#F87171; font-family: monospace; font-size: 1.05rem;">￥${item.sellPrice.toFixed(2)}</strong>
                </div>
                <div style="margin-top:14px; display:flex; justify-content:flex-end;">
                    <button type="button" class="${cartBtnClass}" data-item-id="${String(item.id).replace(/"/g, '&quot;')}">加入购物车</button>
                </div>
            </div>
        </div>
    `;
    }).join('');
    initScrollingText(grid);
    requestAnimationFrame(() => initScrollingText(grid));
    if (document.fonts && document.fonts.ready) {
        document.fonts.ready.then(() => {
            if (grid.isConnected) initScrollingText(grid);
        });
    }
    renderPagination();
    mountItemIcons();
    if (window.McTextureAnim) {
        window.McTextureAnim.initInContainer(grid);
    }
    if (window.McEnchantGlint) {
        window.McEnchantGlint.initInContainer(grid);
    }
    updateClockTimeDescriptions(grid);
    ensureClockTimeTicker();
    updatePointerBearingDescriptions(grid);
    ensurePointerBearingTicker();
    syncItemsStateToUrl();
}

function getItemIconHtml(itemId, itemName) {
    return window.getTextureHtml ? window.getTextureHtml(itemId, itemName) : '';
}

function mountItemIcons() {
    const grid = document.getElementById('itemsGrid');
    mountItemIconsInContainer(grid);
}

function mountItemIconsInContainer(root) {
    if (!root || !window.McItemIcon) return;
    void window.McItemIcon.mountGrid(root);
}

function renderPagination() {
    const pageCount = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
    const label = `第 ${currentPage} / ${pageCount} 页`;
    const prevDisabled = currentPage <= 1;
    const nextDisabled = currentPage >= pageCount;

    const pairs = [
        ['pageInfo', 'pageFirst', 'pagePrev', 'pageNext', 'pageLast', 'pageJumpInput'],
        ['pageInfoBottom', 'pageFirstBottom', 'pagePrevBottom', 'pageNextBottom', 'pageLastBottom', 'pageJumpInputBottom']
    ];
    pairs.forEach(([infoId, firstId, prevId, nextId, lastId, inputId]) => {
        const pageInfo = document.getElementById(infoId);
        const pageFirst = document.getElementById(firstId);
        const pagePrev = document.getElementById(prevId);
        const pageNext = document.getElementById(nextId);
        const pageLast = document.getElementById(lastId);
        const pageInput = document.getElementById(inputId);
        
        if (pageInfo) {
            pageInfo.textContent = label;
        }
        if (pageFirst) {
            pageFirst.disabled = prevDisabled;
        }
        if (pagePrev) {
            pagePrev.disabled = prevDisabled;
        }
        if (pageNext) {
            pageNext.disabled = nextDisabled;
        }
        if (pageLast) {
            pageLast.disabled = nextDisabled;
        }
        if (pageInput) {
            pageInput.max = pageCount;
            pageInput.placeholder = `1-${pageCount}`;
        }
    });
}

function initScrollingText(root) {
    const host = root || document;
    const OVERFLOW_SAFETY_RATIO = 1;
    const EDGE_PAUSE_MS = 1000;
    host.querySelectorAll('.scrolling-text, .scrolling-id').forEach(container => {
        const text = container.querySelector('.scrolling-text-inner, .scrolling-id-text');
        if (!text) return;

        text.getAnimations().forEach(animation => animation.cancel());
        text.style.animation = 'none';
        text.style.transform = '';
        text.style.removeProperty('--scroll-distance');

        const safeScrollWidth = text.scrollWidth * OVERFLOW_SAFETY_RATIO;
        if (safeScrollWidth > container.clientWidth) {
            const distance = Math.max(1, safeScrollWidth - container.clientWidth);
            const forwardMs = Math.max(2500, (distance / 60) * 1000);
            const returnMs = forwardMs / 5;
            const totalMs = EDGE_PAUSE_MS + forwardMs + EDGE_PAUSE_MS + returnMs;
            const leftPauseEnd = EDGE_PAUSE_MS / totalMs;
            const rightArrive = (EDGE_PAUSE_MS + forwardMs) / totalMs;
            const rightPauseEnd = (EDGE_PAUSE_MS + forwardMs + EDGE_PAUSE_MS) / totalMs;
            const rightTransform = `translateX(-${distance}px)`;

            text.animate([
                { transform: 'translateX(0)', offset: 0 },
                { transform: 'translateX(0)', offset: leftPauseEnd },
                { transform: rightTransform, offset: rightArrive },
                { transform: rightTransform, offset: rightPauseEnd },
                { transform: 'translateX(0)', offset: 1 }
            ], {
                duration: totalMs,
                iterations: Infinity,
                easing: 'linear'
            });
        }
    });
}

function initScrollingIds() {
    initScrollingText(document);
}