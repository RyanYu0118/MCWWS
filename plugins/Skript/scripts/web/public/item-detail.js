const ITEM_HISTORY_RANGES = [
    ['10m', '10分钟'],
    ['30m', '30分钟'],
    ['1h', '1小时'],
    ['6h', '6小时'],
    ['24h', '24小时'],
    ['7d', '7天'],
    ['1mo', '1个月'],
    ['1y', '1年'],
    ['3y', '3年'],
    ['all', '全部']
];

let detailChart = null;
let detailRefreshTimer = null;
let detailRefreshInFlight = false;
let detailRequestSeq = 0;
let currentItemId = '';
let currentRange = '7d';

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatCurrency(amount) {
    return `￥${Number(amount || 0).toFixed(2)}`;
}

function formatTime(timestamp) {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return String(timestamp || '');
    const diff = Date.now() - date.getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return '刚刚';
    if (mins < 60) return `${mins} 分钟前`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours} 小时前`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days} 天前`;
    return date.toLocaleString('zh-CN', { hour12: false });
}

function formatAbsoluteTime(timestamp) {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleString('zh-CN', { hour12: false });
}

function txTypeLabel(type) {
    if (type === 'BUY') return '买入';
    if (type === 'SELL') return '回收';
    return type || '未知';
}

function categoryDisplayName(category) {
    const raw = String(category || 'unknown');
    const map = {
        archaeology: '考古',
        wood: '木材',
        brick: '砖块',
        minerals: '矿物',
        redstone: '红石',
        farming: '农作物',
        fish: '渔业',
        flowers: '花卉',
        drops: '掉落物',
        brewing: '酿造',
        tools: '工具',
        weapons: '武器',
        armor: '护甲',
        utility: '实用',
        transport: '交通',
        unknown: '未分类'
    };
    return map[raw] || raw.replace(/_/g, ' ');
}

function formatHistoryAxisLabel(timestamp, rangeKey) {
    const date = new Date(String(timestamp).replace(' ', 'T'));
    if (Number.isNaN(date.getTime())) return String(timestamp || '');
    const pad = (n) => String(n).padStart(2, '0');
    if (rangeKey === '10m' || rangeKey === '30m' || rangeKey === '1h' || rangeKey === '6h') {
        return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }
    if (rangeKey === '24h') {
        return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }
    if (rangeKey === '7d' || rangeKey === '1mo') {
        return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
    }
    if (rangeKey === '1y') {
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
    }
    if (rangeKey === '3y' || rangeKey === 'all') {
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`;
    }
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function getItemIconHtml(itemId, itemName) {
    return window.getTextureHtml ? window.getTextureHtml(itemId, itemName) : '';
}

function mountItemIconsInContainer(root) {
    if (!root || !window.McItemIcon) return;
    void window.McItemIcon.mountGrid(root);
}

function destroyDetailChart() {
    if (window.McPriceHistoryChart) {
        detailChart = window.McPriceHistoryChart.destroy(detailChart);
    } else if (detailChart) {
        detailChart.destroy();
        detailChart = null;
    }
}

function stopDetailRefresh() {
    if (detailRefreshTimer) {
        window.clearInterval(detailRefreshTimer);
        detailRefreshTimer = null;
    }
}

function startDetailRefresh() {
    stopDetailRefresh();
    detailRefreshTimer = window.setInterval(() => {
        void refreshDetailPage();
    }, 1000);
}

async function fetchItemDetailSnapshot(itemId) {
    const response = await fetch(`/api/shop/item-snapshot/${encodeURIComponent(itemId)}?range=full`);
    const snapshot = await response.json();
    if (!response.ok) {
        throw new Error(snapshot?.error || '读取物品详情失败');
    }
    return snapshot;
}

function renderRecentTransactions(recentTxs) {
    return recentTxs.slice(0, 10).map((tx) => `
        <div class="item-modal-trade-row">
            <div>
                <div class="item-modal-trade-time">${escapeHtml(formatTime(tx.timestamp))}</div>
                <div class="item-modal-trade-player">${escapeHtml(tx.playerName || '未知玩家')}</div>
            </div>
            <div class="item-modal-trade-meta">
                <span class="type-badge ${(tx.type || '').toLowerCase()}">${txTypeLabel(tx.type)}</span>
                <span>${Number(tx.amount || 0)} 件</span>
                <span>${formatCurrency(tx.price)}</span>
            </div>
        </div>
    `).join('') || '<div class="item-modal-empty">暂无最近交易</div>';
}

function setRangeButtonsActive(activeKey) {
    document.querySelectorAll('[data-item-history-range]').forEach((btn) => {
        btn.classList.toggle('active', btn.dataset.itemHistoryRange === activeKey);
    });
}

function renderDetailChart(priceHistory, rangeKey) {
    const chartWrap = document.querySelector('[data-item-history-chart]');
    if (!chartWrap) return;
    if (!Array.isArray(priceHistory) || !priceHistory.length) {
        destroyDetailChart();
        chartWrap.innerHTML = '<div class="item-modal-empty">该时间范围内暂无价格历史</div>';
        return;
    }
    let canvas = chartWrap.querySelector('#itemDetailChart');
    if (!canvas || !canvas.isConnected || (detailChart && detailChart.canvas !== canvas)) {
        destroyDetailChart();
        chartWrap.innerHTML = '<canvas id="itemDetailChart"></canvas>';
        canvas = chartWrap.querySelector('#itemDetailChart');
    }
    if (!canvas) return;
    detailChart = window.McPriceHistoryChart.render({
        chart: detailChart,
        canvas,
        priceHistory,
        rangeKey
    });
}

function updateHeader(snapshot) {
    const itemData = snapshot?.item || {};
    const title = itemData.displayName || currentItemId || '物品详情';
    document.title = `${title} - 物品详情`;
    const titleEl = document.getElementById('itemDetailTitle');
    const subtitleEl = document.getElementById('itemDetailSubtitle');
    const updatedEl = document.getElementById('itemDetailUpdated');
    const listLink = document.getElementById('itemDetailListLink');
    if (titleEl) titleEl.textContent = title;
    if (subtitleEl) subtitleEl.textContent = `${categoryDisplayName(itemData.category)} · ${currentItemId}`;
    if (updatedEl) {
        const timeText = formatAbsoluteTime(snapshot?.serverTime);
        updatedEl.textContent = timeText ? `每秒自动刷新 · 最近同步 ${timeText}` : '每秒自动刷新';
    }
    if (listLink) {
        listLink.href = `items.html?detail=${encodeURIComponent(currentItemId)}`;
    }
}

function ensureDetailMarkup(snapshot) {
    const content = document.getElementById('itemDetailContent');
    if (!content || content.dataset.rendered === '1') return;
    const itemData = snapshot?.item || {};
    content.innerHTML = `
        <div class="modal-item-info">
            <div class="modal-item-icon">${getItemIconHtml(currentItemId, itemData.displayName || currentItemId)}</div>
            <div style="min-width:0; flex:1;">
                <div style="font-size: 1.4rem; font-weight: 700; color: var(--text-primary);">${escapeHtml(itemData.displayName || currentItemId)}</div>
                <div style="color: var(--text-muted); margin: 0.35rem 0 0.85rem;">${escapeHtml(categoryDisplayName(itemData.category))} · ${escapeHtml(currentItemId)}</div>
                <div class="item-modal-stat-grid">
                    <div class="item-modal-stat">
                        <div class="item-modal-stat-label">系统买入</div>
                        <div class="item-modal-stat-value item-modal-stat-value--buy" data-item-detail-buy>${formatCurrency(itemData.buyPrice)}</div>
                    </div>
                    <div class="item-modal-stat">
                        <div class="item-modal-stat-label">玩家回收</div>
                        <div class="item-modal-stat-value item-modal-stat-value--sell" data-item-detail-sell>${formatCurrency(itemData.sellPrice)}</div>
                    </div>
                    <div class="item-modal-stat">
                        <div class="item-modal-stat-label">最近 20 笔买入</div>
                        <div class="item-modal-stat-value" data-item-detail-total-buys>${Number(itemData.totalBuys || 0)}</div>
                    </div>
                    <div class="item-modal-stat">
                        <div class="item-modal-stat-label">最近 20 笔回收</div>
                        <div class="item-modal-stat-value" data-item-detail-total-sells>${Number(itemData.totalSells || 0)}</div>
                    </div>
                </div>
            </div>
        </div>
        <h3 class="item-modal-section-title">价格历史</h3>
        <div class="item-history-range-tabs">
            ${ITEM_HISTORY_RANGES.map(([key, label]) => `
                <button type="button" class="tab-btn${key === currentRange ? ' active' : ''}" data-item-history-range="${escapeHtml(key)}">${escapeHtml(label)}</button>
            `).join('')}
        </div>
        <div class="item-modal-chart-wrap" data-item-history-chart>
            <div class="loading-spinner"></div>
        </div>
        <h3 class="item-modal-section-title">最近交易</h3>
        <div class="item-modal-trade-list" data-item-detail-recent-txs>
            ${renderRecentTransactions(Array.isArray(itemData.recentTransactions) ? itemData.recentTransactions : [])}
        </div>
    `;
    content.dataset.rendered = '1';
    mountItemIconsInContainer(content);
    if (window.McTextureAnim) window.McTextureAnim.initInContainer(content);
    if (window.McEnchantGlint) window.McEnchantGlint.initInContainer(content);
}

function updateDetailContent(snapshot) {
    const itemData = snapshot?.item || {};
    updateHeader(snapshot);
    ensureDetailMarkup(snapshot);
    const buyEl = document.querySelector('[data-item-detail-buy]');
    const sellEl = document.querySelector('[data-item-detail-sell]');
    const totalBuysEl = document.querySelector('[data-item-detail-total-buys]');
    const totalSellsEl = document.querySelector('[data-item-detail-total-sells]');
    const txListEl = document.querySelector('[data-item-detail-recent-txs]');
    if (buyEl) buyEl.textContent = formatCurrency(itemData.buyPrice);
    if (sellEl) sellEl.textContent = formatCurrency(itemData.sellPrice);
    if (totalBuysEl) totalBuysEl.textContent = String(Number(itemData.totalBuys || 0));
    if (totalSellsEl) totalSellsEl.textContent = String(Number(itemData.totalSells || 0));
    if (txListEl) {
        txListEl.innerHTML = renderRecentTransactions(Array.isArray(itemData.recentTransactions) ? itemData.recentTransactions : []);
    }
    if (!window.McPriceHistoryChart?.isViewportCustomized(detailChart)) {
        setRangeButtonsActive(currentRange);
    }
    renderDetailChart(Array.isArray(snapshot.priceHistory) ? snapshot.priceHistory : [], currentRange);
}

async function loadDetailSnapshot(showLoading = true) {
    const content = document.getElementById('itemDetailContent');
    const chartWrap = content?.querySelector('[data-item-history-chart]');
    const requestSeq = ++detailRequestSeq;
    if (showLoading && chartWrap) {
        destroyDetailChart();
        chartWrap.innerHTML = '<div class="loading-spinner"></div>';
    }
    try {
        const [snapshotResponse, historyResponse] = await Promise.all([
            fetch(`/api/shop/item/${encodeURIComponent(currentItemId)}`),
            fetch(`/api/analytics/price-history/${encodeURIComponent(currentItemId)}?range=full`)
        ]);
        const snapshotItem = await snapshotResponse.json();
        const priceHistory = await historyResponse.json();
        if (!snapshotResponse.ok) {
            throw new Error(snapshotItem?.error || '读取物品详情失败');
        }
        if (!historyResponse.ok) {
            throw new Error(priceHistory?.error || '读取价格历史失败');
        }
        if (requestSeq !== detailRequestSeq) return;
        updateDetailContent({
            item: snapshotItem,
            priceHistory: Array.isArray(priceHistory) ? priceHistory : [],
            serverTime: new Date().toISOString()
        });
    } catch (error) {
        if (requestSeq !== detailRequestSeq) return;
        throw error;
    }
}

function loadDetailHistoryRange(rangeKey = currentRange) {
    currentRange = rangeKey;
    setRangeButtonsActive(rangeKey);
    if (detailChart && window.McPriceHistoryChart) {
        window.McPriceHistoryChart.applyPresetRange(detailChart, rangeKey, { force: true });
    }
}

async function refreshDetailPage() {
    if (!currentItemId || document.hidden || detailRefreshInFlight) return;
    detailRefreshInFlight = true;
    try {
        await loadDetailSnapshot(false);
    } catch (error) {
        console.warn('刷新物品详情页失败:', error);
    } finally {
        detailRefreshInFlight = false;
    }
}

function bindInteractions() {
    const content = document.getElementById('itemDetailContent');
    if (!content || content.dataset.bound === '1') return;
    content.dataset.bound = '1';
    content.addEventListener('click', (event) => {
        const btn = event.target.closest('[data-item-history-range]');
        if (!btn) return;
        currentRange = btn.dataset.itemHistoryRange || '7d';
        loadDetailHistoryRange(currentRange);
    });
}

function showPageError(message) {
    const content = document.getElementById('itemDetailContent');
    if (!content) return;
    destroyDetailChart();
    content.innerHTML = `<div class="item-modal-empty">${escapeHtml(message)}</div>`;
}

document.addEventListener('DOMContentLoaded', async () => {
    if (window.mcLangReady) {
        await window.mcLangReady;
    }
    const params = new URLSearchParams(window.location.search);
    currentItemId = (params.get('item') || '').trim();
    const requestedRange = (params.get('range') || '').trim().toLowerCase();
    if (ITEM_HISTORY_RANGES.some(([key]) => key === requestedRange)) {
        currentRange = requestedRange;
    }
    bindInteractions();
    if (!currentItemId) {
        showPageError('缺少物品 ID，无法加载详情页。');
        return;
    }
    try {
        await loadDetailSnapshot(false);
        startDetailRefresh();
    } catch (error) {
        console.error('加载物品详情页失败:', error);
        showPageError(error.message || '加载物品详情失败');
    }
});

window.addEventListener('beforeunload', () => {
    stopDetailRefresh();
    destroyDetailChart();
});
