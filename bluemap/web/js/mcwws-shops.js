(function () {
    const API_PORT = 8002;
    const PANEL_ID = 'mcwws-shop-panel';
    const PIN_LAYER_ID = 'mcwws-shop-pin-layer';
    const CACHE_KEY = 'mcwws-shop-markers-cache';
    const FLAT_HEIGHT_KEY = 'mcwws-last-flat-distance';
    const NODE_API = `${window.location.protocol}//${window.location.hostname}:${API_PORT}`;
    let markers = [];
    let loading = true;
    let started = false;
    let animationId = 0;
    let pinElements = new Map();
    let cachedCamera = null;
    let selectedMarkerId = null;
    let selectedMarkerTopDown = false;
    let lastPanelMap = null;
    let lastFlatHeight = 128;
    let tickFrame = 0;
    let dockExpanded = false;
    let searchQuery = '';
    let tradeMode = false;
    let tradeItemId = '';
    let launchTradeHandled = false;
    let dockEventsBound = false;

    const MODULE_ROW_PRIMARY = [
        { id: 'items', label: '\u7269\u54c1\u76ee\u5f55', icon: '\ud83d\uded2', color: '#3b82f6', action: 'economy-items' },
        { id: 'trade-search', label: '\u67e5\u4ef7\u4ea4\u6613', icon: '\ud83d\udcb0', color: '#f59e0b', action: 'economy-search' },
        { id: 'flat-all', label: '\u4fef\u89c6\u5168\u89c8', icon: '\ud83d\uddfa\ufe0f', color: '#22c55e', action: 'flat-overview' },
        { id: 'perspective', label: '3D\u89c6\u89d2', icon: '\ud83c\udfd4\ufe0f', color: '#8b5cf6', action: 'perspective' },
        { id: 'dashboard', label: '\u6570\u636e\u770b\u677f', icon: '\ud83d\udcca', color: '#06b6d4', action: 'economy-dashboard' }
    ];

    const MODULE_ROW_SECONDARY = [
        { id: 'shops-all', label: '\u5168\u90e8\u5546\u5e97', icon: '\ud83c\udfea', color: '#a855f7', soft: true, action: 'list-all' },
        { id: 'shops-map', label: '\u672c\u56fe\u5546\u5e97', icon: '\ud83d\udccd', color: '#ef4444', soft: true, action: 'list-map' },
        { id: 'flat-shop', label: '\u4fef\u89c6\u5b9a\u4f4d', icon: '\u2316', color: '#10b981', soft: true, action: 'flat-nearest' },
        { id: 'refresh', label: '\u5237\u65b0\u6807\u8bb0', icon: '\u27f3', color: '#64748b', soft: true, action: 'refresh' },
        { id: 'map-item', label: '\u5730\u56fe\u627e\u7269', icon: '\ud83e\udded', color: '#0ea5e9', soft: true, action: 'item-on-map' }
    ];

    try {
        const saved = Number(sessionStorage.getItem(FLAT_HEIGHT_KEY));
        if (Number.isFinite(saved) && saved > 0) lastFlatHeight = saved;
    } catch {
        /* ignore */
    }

    parseLaunchParams();

    document.addEventListener('keydown', stopMapKeyboardBubble, false);
    document.addEventListener('keyup', stopMapKeyboardBubble, false);

    function economyBaseUrl() {
        return `${window.location.protocol}//${window.location.hostname}:${API_PORT}`;
    }

    function economyUrl(path, params) {
        const url = new URL(path, economyBaseUrl());
        if (params && typeof params === 'object') {
            Object.entries(params).forEach(([k, v]) => {
                if (v != null && v !== '') url.searchParams.set(k, String(v));
            });
        }
        return url.toString();
    }

    function setDockExpanded(expanded) {
        dockExpanded = !!expanded;
        const panel = document.getElementById(PANEL_ID);
        if (panel) panel.classList.toggle('is-expanded', dockExpanded);
    }

    function normalizeMaterialId(value) {
        return String(value || '').trim().toLowerCase().replace(/-/g, '_');
    }

    function parseLaunchParams() {
        try {
            const params = new URLSearchParams(window.location.search);
            const item = normalizeMaterialId(params.get('item'));
            const q = String(params.get('q') || '').trim();
            const trade = params.get('trade') === '1' || params.get('trade') === 'true' || !!item;
            if (!trade && !q && !item) {
                return;
            }
            tradeMode = true;
            tradeItemId = item;
            searchQuery = q;
            dockExpanded = true;
        } catch {
            /* ignore */
        }
    }

    function applySearchUiState() {
        const panel = document.getElementById(PANEL_ID);
        if (!panel) return;
        const input = panel.querySelector('.mcwws-dock-input');
        const tradeToggle = panel.querySelector('.mcwws-dock-trade-mode');
        if (input && input.value !== searchQuery) {
            input.value = searchQuery;
        }
        if (tradeToggle) {
            tradeToggle.checked = tradeMode;
        }
        panel.classList.toggle('is-trade-mode', tradeMode);
        setDockExpanded(dockExpanded);
    }

    function hasActiveTradeFilter() {
        return tradeMode && (!!tradeItemId || !!searchQuery.trim());
    }

    function markerMatchesTrade(marker, query, itemId) {
        const ids = Array.isArray(marker.tradeItemIds) ? marker.tradeItemIds : [];
        const labels = Array.isArray(marker.tradeLabels) ? marker.tradeLabels : [];
        const idNorm = normalizeMaterialId(itemId);
        if (idNorm && ids.includes(idNorm)) {
            return true;
        }
        const q = String(query || '').trim().toLowerCase();
        if (!q) {
            return !idNorm;
        }
        if (ids.some((id) => id.includes(q))) {
            return true;
        }
        return labels.some((label) => String(label).toLowerCase().includes(q));
    }

    function isTradeHighlight(marker) {
        if (!hasActiveTradeFilter()) {
            return false;
        }
        return markerMatchesTrade(marker, searchQuery, tradeItemId);
    }

    function applyLaunchTradeFocus() {
        if (launchTradeHandled || !hasActiveTradeFilter()) {
            return;
        }
        launchTradeHandled = true;
        const matches = markers.filter((marker) => markerMatchesTrade(marker, searchQuery, tradeItemId));
        if (!matches.length) {
            return;
        }
        const view = parseHash();
        if (view) {
            const atShop = matches.find((m) => m.map === view.map
                && Math.hypot((m.position.x + 0.5) - view.x, (m.position.z + 0.5) - view.z) < 6);
            if (atShop) {
                selectedMarkerId = atShop.id;
                return;
            }
        }
        const target = matches.filter(sameMap)[0] || matches[0];
        if (target) {
            openMarkerTopDown(target);
        }
    }

    function filterMarkersByQuery(list, query) {
        const q = String(query || '').trim().toLowerCase();
        if (!q) return list;
        return list.filter((marker) => {
            const hay = [
                marker.label,
                marker.shopId,
                marker.description,
                marker.map
            ].join(' ').toLowerCase();
            return hay.includes(q);
        });
    }

    function getFilteredMarkers() {
        if (tradeMode) {
            if (!hasActiveTradeFilter()) {
                return markers;
            }
            return markers.filter((marker) => markerMatchesTrade(marker, searchQuery, tradeItemId));
        }
        return filterMarkersByQuery(markers, searchQuery);
    }

    function renderModuleGrid(container, modules) {
        if (!container) return;
        container.innerHTML = modules.map((mod) => `
            <button type="button" class="mcwws-dock-tile" data-action="${escapeHtml(mod.action)}">
                <span class="mcwws-dock-tile-icon${mod.soft ? ' is-soft' : ''}" style="${mod.soft ? `color:${mod.color}` : `background:${mod.color}`}">${mod.icon}</span>
                <span class="mcwws-dock-tile-label">${escapeHtml(mod.label)}</span>
            </button>
        `).join('');
    }

    function renderShopRows(list, emptyMsg) {
        if (!list.length) {
            return `<div class="mcwws-dock-result-hint">${escapeHtml(emptyMsg)}</div>`;
        }
        return list.map((marker) => `
            <button class="mcwws-shop-row${isTradeHighlight(marker) ? ' is-trade-hit' : ''}" type="button" data-shop-id="${escapeHtml(marker.id)}">
                <span class="mcwws-shop-pin">\u2316</span>
                <span class="mcwws-shop-main">
                    <span class="mcwws-shop-name">${escapeHtml(marker.label)}</span>
                    <span class="mcwws-shop-meta">${escapeHtml(marker.shopId)} \u00b7 ${marker.itemCount || 0} \u4e2a\u5546\u54c1</span>
                    <span class="mcwws-shop-pos">${escapeHtml(marker.map)} / ${marker.position.x}, ${marker.position.y}, ${marker.position.z}</span>
                    ${marker.description ? `<span class="mcwws-shop-desc">${escapeHtml(marker.description)}</span>` : ''}
                </span>
                <span class="mcwws-shop-row-actions">
                    <button type="button" class="mcwws-shop-mini-btn" data-view="perspective" data-shop-id="${escapeHtml(marker.id)}">3D</button>
                    <button type="button" class="mcwws-shop-mini-btn" data-view="flat" data-shop-id="${escapeHtml(marker.id)}">\u4fef\u89c6</button>
                </span>
            </button>
        `).join('');
    }

    function bindShopRowEvents(panel) {
        panel.querySelectorAll('.mcwws-shop-row').forEach((row) => {
            row.addEventListener('click', (e) => {
                if (e.target.closest('.mcwws-shop-mini-btn')) return;
                const marker = markers.find((item) => item.id === row.dataset.shopId);
                openMarker(marker);
            });
        });
        panel.querySelectorAll('.mcwws-shop-mini-btn').forEach((btn) => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const marker = markers.find((item) => item.id === btn.dataset.shopId);
                if (!marker) return;
                if (btn.dataset.view === 'flat') openMarkerTopDown(marker);
                else openMarker(marker);
            });
        });
    }

    function renderResultsPanel() {
        const panel = document.getElementById(PANEL_ID);
        const results = panel?.querySelector('.mcwws-dock-results');
        if (!results) return;

        const q = searchQuery.trim();
        let list;
        let emptyMsg;

        if (hasActiveTradeFilter()) {
            list = getFilteredMarkers();
            const total = list.length;
            const onMap = list.filter(sameMap).length;
            emptyMsg = loading
                ? '\u6b63\u5728\u52a0\u8f7d\u53ef\u4ea4\u6613\u5546\u5e97...'
                : `\u672a\u627e\u5230\u53ef\u4ea4\u6613\u300c${escapeHtml(q || tradeItemId)}\u300d\u7684\u5546\u5e97`;
            const hintText = onMap < total
                ? `\u5168\u670d ${total} \u5bb6\uff0c\u5f53\u524d\u5730\u56fe ${onMap} \u5bb6\u5df2\u9ad8\u4eae`
                : `\u5171 ${total} \u5bb6\u5546\u5e97\u5df2\u9ad8\u4eae`;
            const summaryPart = !loading && total > 0
                ? `<XX class="mcwws-dock-result-hint mcwws-dock-trade-summary">${hintText}</XX>`
                : '';
            results.innerHTML = [summaryPart, renderShopRows(list, emptyMsg)].join('')
                .replace(/<XX/g, '<div')
                .replace(/<\/XX>/g, '</div>');
            bindShopRowEvents(panel);
            syncPinElements();
            updatePinPositions();
            return;
        } else if (q) {
            list = getFilteredMarkers();
            emptyMsg = loading
                ? '\u6b63\u5728\u52a0\u8f7d...'
                : '\u672a\u627e\u5230\u5339\u914d\u7684\u5546\u5e97\uff0c\u53ef\u70b9\u300c\u7269\u54c1\u76ee\u5f55\u300d\u67e5\u4ef7\u4ea4\u6613';
        } else if (dockExpanded) {
            list = markers.filter(sameMap);
            emptyMsg = loading
                ? '\u6b63\u5728\u52a0\u8f7d\u5546\u5e97\u6807\u8bb0...'
                : '\u5f53\u524d\u5730\u56fe\u6ca1\u6709\u5df2\u914d\u7f6e\u7684\u5546\u5e97';
        } else {
            results.innerHTML = '';
            return;
        }

        results.innerHTML = renderShopRows(list, emptyMsg);
        bindShopRowEvents(panel);
    }

    function renderResultsPanelAll() {
        const panel = document.getElementById(PANEL_ID);
        const results = panel?.querySelector('.mcwws-dock-results');
        if (!results) return;
        const emptyMsg = loading
            ? '\u6b63\u5728\u52a0\u8f7d\u5546\u5e97\u6807\u8bb0...'
            : '\u6682\u65e0\u5df2\u914d\u7f6e\u7684\u5546\u5e97';
        results.innerHTML = renderShopRows(markers, emptyMsg);
        bindShopRowEvents(panel);
    }

    function handleModuleAction(action) {
        const q = searchQuery.trim();
        switch (action) {
            case 'economy-items':
                window.open(economyUrl('/items.html', q ? { q } : null), '_blank', 'noopener');
                break;
            case 'economy-search':
                window.open(economyUrl('/items.html', q ? { q } : null), '_blank', 'noopener');
                break;
            case 'economy-dashboard':
                window.open(economyUrl('/index.html'), '_blank', 'noopener');
                break;
            case 'perspective':
                setBlueMapViewMode('perspective');
                break;
            case 'flat-overview': {
                const visible = markers.filter(sameMap);
                if (visible.length) openMarkerTopDown(visible[0]);
                else setBlueMapViewMode('flat');
                break;
            }
            case 'list-all': {
                const input = document.querySelector('#mcwws-shop-panel .mcwws-dock-input');
                searchQuery = '';
                if (input) input.value = '';
                setDockExpanded(true);
                renderResultsPanelAll();
                break;
            }
            case 'list-map': {
                const input = document.querySelector('#mcwws-shop-panel .mcwws-dock-input');
                searchQuery = '';
                if (input) input.value = '';
                setDockExpanded(true);
                renderResultsPanel();
                break;
            }
            case 'flat-nearest': {
                const visible = getFilteredMarkers().filter(sameMap);
                const target = visible[0] || markers.filter(sameMap)[0];
                if (target) openMarkerTopDown(target);
                break;
            }
            case 'refresh':
                loadMarkers();
                break;
            case 'item-on-map':
                window.open(economyUrl('/items.html', q ? { q } : null), '_blank', 'noopener');
                break;
            default:
                break;
        }
    }

    function ensureDockShell() {
        let panel = document.getElementById(PANEL_ID);
        if (panel?.dataset.shellReady === '1' && !panel.querySelector('.mcwws-dock-trade-mode')) {
            panel.remove();
            panel = null;
        }
        if (panel?.dataset.shellReady === '1') {
            applySearchUiState();
            return panel;
        }

        panel = document.createElement('aside');
        panel.id = PANEL_ID;
        panel.dataset.shellReady = '1';
        panel.innerHTML = [
            '<XX class="mcwws-dock-card">',
            '  <XX class="mcwws-dock-search">',
            '    <span class="mcwws-dock-brand" title="MCWWS \u5546\u5e97\u5730\u56fe">\ud83d\uddfa\ufe0f</span>',
            '    <label class="mcwws-dock-input-wrap">',
            '      <input class="mcwws-dock-input" type="search" autocomplete="off" placeholder="\u641c\u7d22\u5546\u5e97\u3001\u7269\u54c1\u3001\u5750\u6807...">',
            '    </label>',
            '    <label class="mcwws-dock-trade-toggle" title="\u4ea4\u6613\u6a21\u5f0f\uff1a\u6309\u7269\u54c1\u7b5b\u9009\u53ef\u4ea4\u6613\u5546\u5e97">',
            '      <input class="mcwws-dock-trade-mode" type="checkbox">',
            '      <span>\u4ea4\u6613</span>',
            '    </label>',
            '    <button type="button" class="mcwws-dock-search-btn" title="\u641c\u7d22">\ud83d\udd0d</button>',
            '    <button type="button" class="mcwws-dock-economy-btn" title="\u6253\u5f00\u7269\u54c1\u7ad9">\ud83d\uded2</button>',
            '  </XX>',
            '  <XX class="mcwws-dock-body">',
            '    <XX class="mcwws-dock-modules">',
            '      <XX class="mcwws-dock-grid mcwws-dock-grid-primary"></XX>',
            '      <XX class="mcwws-dock-grid mcwws-dock-grid-secondary"></XX>',
            '    </XX>',
            '    <XX class="mcwws-dock-shortcuts">',
            '      <button type="button" class="mcwws-dock-shortcut" data-action="economy-items"><span class="mcwws-dock-shortcut-icon">\ud83d\uded2</span>\u7269\u54c1\u7ad9</button>',
            '      <button type="button" class="mcwws-dock-shortcut" data-action="economy-search"><span class="mcwws-dock-shortcut-icon">\ud83d\udcb0</span>\u67e5\u4ef7\u4ea4\u6613</button>',
            '      <button type="button" class="mcwws-dock-shortcut" data-action="collapse"><span class="mcwws-dock-shortcut-icon">\u25b2</span>\u6536\u8d77</button>',
            '    </XX>',
            '    <XX class="mcwws-dock-results"></XX>',
            '  </XX>',
            '</XX>'
        ].join('\n').replace(/<XX/g, '<div').replace(/<\/XX>/g, '</div>');
        document.body.appendChild(panel);
        renderModuleGrid(panel.querySelector('.mcwws-dock-grid-primary'), MODULE_ROW_PRIMARY);
        renderModuleGrid(panel.querySelector('.mcwws-dock-grid-secondary'), MODULE_ROW_SECONDARY);
        bindDockEvents(panel);
        applySearchUiState();
        return panel;
    }

    function bindDockEvents(panel) {
        if (dockEventsBound) return;
        dockEventsBound = true;

        const input = panel.querySelector('.mcwws-dock-input');
        const searchBtn = panel.querySelector('.mcwws-dock-search-btn');
        const economyBtn = panel.querySelector('.mcwws-dock-economy-btn');
        const tradeToggle = panel.querySelector('.mcwws-dock-trade-mode');

        const searchRow = panel.querySelector('.mcwws-dock-search');
        searchRow?.addEventListener('click', (e) => {
            if (e.target.closest('button')) return;
            setDockExpanded(true);
            input?.focus();
            renderResultsPanel();
        });

        input?.addEventListener('focus', () => {
            setMapKeyboardPaused(true);
            setDockExpanded(true);
            renderResultsPanel();
        });

        input?.addEventListener('blur', () => {
            window.setTimeout(syncMapKeyboardPause, 0);
        });

        panel.addEventListener('focusin', () => {
            if (isDockTextInputFocused()) {
                setMapKeyboardPaused(true);
            }
        });
        panel.addEventListener('focusout', () => {
            window.setTimeout(syncMapKeyboardPause, 0);
        });

        input?.addEventListener('input', () => {
            searchQuery = input.value;
            tradeItemId = '';
            setDockExpanded(true);
            renderResultsPanel();
            syncPinElements();
            updatePinPositions();
        });

        tradeToggle?.addEventListener('change', () => {
            tradeMode = !!tradeToggle.checked;
            panel.classList.toggle('is-trade-mode', tradeMode);
            if (!tradeMode) {
                tradeItemId = '';
            }
            setDockExpanded(true);
            renderResultsPanel();
            syncPinElements();
            updatePinPositions();
        });

        input?.addEventListener('keyup', stopMapKeyboardBubble);

        input?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleModuleAction(searchQuery.trim() ? 'economy-search' : 'list-all');
            }
            if (e.key === 'Escape') {
                input.blur();
                setDockExpanded(false);
                renderResultsPanel();
            }
        });

        searchBtn?.addEventListener('click', () => {
            searchQuery = input?.value || '';
            setDockExpanded(true);
            renderResultsPanel();
        });

        economyBtn?.addEventListener('click', () => {
            handleModuleAction('economy-items');
        });

        panel.addEventListener('click', (e) => {
            const tile = e.target.closest('[data-action]');
            if (!tile || !panel.contains(tile)) return;
            const action = tile.dataset.action;
            if (action === 'collapse') {
                setDockExpanded(false);
                input?.blur();
                renderResultsPanel();
                return;
            }
            if (tile.classList.contains('mcwws-dock-tile') || tile.classList.contains('mcwws-dock-shortcut')) {
                handleModuleAction(action);
            }
        });

        document.addEventListener('mousedown', (e) => {
            if (!dockExpanded) return;
            if (panel.contains(e.target)) return;
            setDockExpanded(false);
            renderResultsPanel();
        });
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function sameMap(marker) {
        const current = String(window.location.hash || '').replace(/^#/, '').split(':')[0];
        return !current || !marker.map || current === marker.map;
    }

    function openMarker(marker) {
        if (!marker || !marker.viewUrl) return;
        selectedMarkerId = marker.id;
        selectedMarkerTopDown = false;
        const target = parseViewUrl(marker.viewUrl);
        setBlueMapViewMode('perspective');
        if (target) {
            setViewHash({ ...target, mode: target.mode || 'perspective' });
        } else {
            const hashIndex = marker.viewUrl.indexOf('#');
            if (hashIndex >= 0) {
                window.location.hash = marker.viewUrl.slice(hashIndex + 1);
            } else {
                window.location.href = marker.viewUrl;
            }
        }
    }

    function openMarkerTopDown(marker) {
        if (!marker?.position || !marker.map) return;
        selectedMarkerId = marker.id;
        selectedMarkerTopDown = true;
        rememberFlatZoom();
        const x = Number(marker.position.x) + 0.5;
        const y = Number(marker.position.y);
        const z = Number(marker.position.z) + 0.5;
        setViewHash({
            map: marker.map,
            x,
            y,
            z,
            height: lastFlatHeight,
            pitch: 0,
            yaw: 0,
            roll: 0,
            fov: 1,
            mode: 'flat'
        });
    }

    function handlePinClick(marker) {
        if (!marker) return;
        if (selectedMarkerId === marker.id && !selectedMarkerTopDown) {
            openMarkerTopDown(marker);
        } else {
            openMarker(marker);
        }
    }

    function parseHash() {
        const parts = String(window.location.hash || '').replace(/^#/, '').split(':');
        return parseHashParts(parts);
    }

    function parseViewUrl(viewUrl) {
        const raw = String(viewUrl || '').trim();
        const hash = raw.includes('#') ? raw.slice(raw.indexOf('#') + 1) : raw;
        return parseHashParts(hash.split(':'));
    }

    function parseHashParts(parts) {
        if (parts.length < 4) return null;
        const x = Number(parts[1]);
        const y = Number(parts[2]);
        const z = Number(parts[3]);
        if (![x, y, z].every(Number.isFinite)) return null;
        return {
            map: parts[0],
            x,
            y,
            z,
            height: Number.isFinite(Number(parts[4])) ? Math.max(1, Number(parts[4])) : 128,
            pitch: Number.isFinite(Number(parts[5])) ? Number(parts[5]) : -0.8,
            yaw: Number.isFinite(Number(parts[6])) ? Number(parts[6]) : 0,
            roll: Number.isFinite(Number(parts[7])) ? Number(parts[7]) : 0,
            fov: Number.isFinite(Number(parts[8])) ? Number(parts[8]) : 0,
            mode: parts[9] || 'perspective'
        };
    }

    function formatViewHash(view) {
        return [
            view.map,
            roundViewNumber(view.x),
            roundViewNumber(view.y),
            roundViewNumber(view.z),
            roundViewNumber(view.height),
            roundViewNumber(view.pitch),
            roundViewNumber(view.yaw),
            roundViewNumber(view.roll || 0),
            roundViewNumber(view.fov || 0),
            view.mode || 'perspective'
        ].join(':');
    }

    function roundViewNumber(value) {
        return Number(value).toFixed(4).replace(/\.?0+$/, '');
    }

    function setViewHash(view) {
        updateHashSilently(formatViewHash(view));
        updatePinPositions();
    }

    function updateHashSilently(hash) {
        const url = `${window.location.pathname}${window.location.search}#${hash}`;
        window.history.replaceState(null, '', url);
        window.dispatchEvent(new HashChangeEvent('hashchange'));
    }

    function findCamera(root, seen = new Set(), depth = 0) {
        if (!root || typeof root !== 'object' || seen.has(root) || depth > 5) return null;
        seen.add(root);
        if (root.isCamera && root.projectionMatrix && root.matrixWorldInverse) return root;
        for (const key of Object.keys(root)) {
            if (key === 'parent' || key === 'children' || key === 'domElement') continue;
            const camera = findCamera(root[key], seen, depth + 1);
            if (camera) return camera;
        }
        return null;
    }

    function getBlueMapCamera() {
        if (cachedCamera?.projectionMatrix && cachedCamera?.matrixWorldInverse) return cachedCamera;
        const bluemap = getBlueMapApp();
        cachedCamera = findCamera(bluemap);
        return cachedCamera;
    }

    function getBlueMapApp() {
        const app = document.getElementById('app');
        return app?.__vue_app__?.config?.globalProperties?.$bluemap
            || app?.__vueParentComponent?.appContext?.config?.globalProperties?.$bluemap
            || null;
    }

    function setBlueMapViewMode(mode) {
        const bluemap = getBlueMapApp();
        if (!bluemap) return;
        cachedCamera = null;
        if (mode === 'flat' && typeof bluemap.setFlatView === 'function') {
            bluemap.setFlatView();
        } else if (mode === 'perspective' && typeof bluemap.setPerspectiveView === 'function') {
            bluemap.setPerspectiveView();
        }
    }

    function getControlsManager() {
        const bluemap = getBlueMapApp();
        return bluemap?.mapViewer?.controlsManager || null;
    }

    let mapKeyboardPaused = false;

    function isDockTextInputFocused() {
        const panel = document.getElementById(PANEL_ID);
        const active = document.activeElement;
        if (!panel || !active || !panel.contains(active)) {
            return false;
        }
        const tag = active.tagName;
        return tag === 'INPUT' || tag === 'TEXTAREA' || active.isContentEditable;
    }

    /** BlueMap 在 window 上监听 WASD；搜索框聚焦时暂停键盘控制 */
    function setMapKeyboardPaused(paused) {
        const cm = getControlsManager();
        if (!cm?.controls || !Array.isArray(cm.controls)) {
            return;
        }
        if (paused === mapKeyboardPaused) {
            return;
        }
        mapKeyboardPaused = paused;
        cm.controls.forEach((ctrl) => {
            if (!ctrl || typeof ctrl.stop !== 'function') {
                return;
            }
            if (paused) {
                ctrl.stop();
            } else if (typeof ctrl.start === 'function') {
                ctrl.start(cm);
            }
        });
    }

    function syncMapKeyboardPause() {
        setMapKeyboardPaused(isDockTextInputFocused());
    }

    function stopMapKeyboardBubble(e) {
        if (!isDockTextInputFocused()) {
            return;
        }
        e.stopPropagation();
    }

    /** BlueMap 用 appState.controls.state 判断模式；distance 即俯视缩放 */
    function rememberFlatZoom() {
        const bluemap = getBlueMapApp();
        if (!bluemap?.appState?.controls || bluemap.appState.controls.state !== 'flat') return;
        const dist = Number(getControlsManager()?.distance);
        if (!Number.isFinite(dist) || dist <= 0) return;
        lastFlatHeight = dist;
        try {
            sessionStorage.setItem(FLAT_HEIGHT_KEY, String(dist));
        } catch {
            /* ignore */
        }
    }

    function applyMatrix4(point, matrix) {
        const e = matrix.elements || matrix;
        const x = point.x;
        const y = point.y;
        const z = point.z;
        const w = point.w == null ? 1 : point.w;
        return {
            x: e[0] * x + e[4] * y + e[8] * z + e[12] * w,
            y: e[1] * x + e[5] * y + e[9] * z + e[13] * w,
            z: e[2] * x + e[6] * y + e[10] * z + e[14] * w,
            w: e[3] * x + e[7] * y + e[11] * z + e[15] * w
        };
    }

    function projectWithCamera(marker, camera) {
        const worldPoint = {
            x: marker.position.x + 0.5,
            y: marker.position.y + 1.2,
            z: marker.position.z + 0.5,
            w: 1
        };
        const cameraPoint = applyMatrix4(worldPoint, camera.matrixWorldInverse);
        const clipPoint = applyMatrix4(cameraPoint, camera.projectionMatrix);
        if (!clipPoint.w) return null;
        const nx = clipPoint.x / clipPoint.w;
        const ny = clipPoint.y / clipPoint.w;
        const nz = clipPoint.z / clipPoint.w;
        return {
            x: (nx * 0.5 + 0.5) * window.innerWidth,
            y: (-ny * 0.5 + 0.5) * window.innerHeight,
            behind: clipPoint.w < 0 || nz < -1 || nz > 1
        };
    }

    function projectMarker(marker, view) {
        const dx = marker.position.x - view.x;
        const dy = marker.position.y - view.y;
        const dz = marker.position.z - view.z;
        const yaw = view.yaw;
        const cos = Math.cos(yaw);
        const sin = Math.sin(yaw);
        const right = dx * cos - dz * sin;
        const forward = dx * sin + dz * cos;
        const perspectiveBoost = view.mode === 'perspective' ? 1.35 : 1;
        const scale = Math.max(2, Math.min(120, (window.innerHeight / Math.max(10, view.height * 2.2)) * perspectiveBoost));
        const pitchFactor = Math.max(0.2, Math.min(1, Math.abs(Math.sin(view.pitch || -0.8))));
        const x = window.innerWidth / 2 + right * scale;
        const y = window.innerHeight / 2 + forward * scale * pitchFactor - dy * scale * 0.65;
        return { x, y };
    }

    function ensurePinLayer() {
        if (!document.body) return null;
        let layer = document.getElementById(PIN_LAYER_ID);
        if (!layer) {
            layer = document.createElement('div');
            layer.id = PIN_LAYER_ID;
            document.body.appendChild(layer);
        }
        return layer;
    }

    function shouldShowPin(marker) {
        if (!sameMap(marker)) {
            return false;
        }
        if (hasActiveTradeFilter()) {
            return markerMatchesTrade(marker, searchQuery, tradeItemId);
        }
        return true;
    }

    function syncPinElements() {
        const layer = ensurePinLayer();
        if (!layer) return;
        const visible = markers.filter(shouldShowPin);
        const visibleIds = new Set(visible.map(marker => marker.id));
        pinElements.forEach((pin, id) => {
            if (!visibleIds.has(id)) {
                pin.remove();
                pinElements.delete(id);
            }
        });
        visible.forEach(marker => {
            if (pinElements.has(marker.id)) return;
            const pin = document.createElement('button');
            pin.className = 'mcwws-screen-pin';
            pin.type = 'button';
            pin.dataset.shopId = marker.id;
            pin.innerHTML = `
                <span class="mcwws-screen-pin-icon">\uD83D\uDCCD</span>
                <span class="mcwws-screen-pin-label">${escapeHtml(marker.label)}</span>
            `;
            pin.addEventListener('click', () => {
                const marker = markers.find(item => item.id === pin.dataset.shopId);
                handlePinClick(marker);
            });
            layer.appendChild(pin);
            pinElements.set(marker.id, pin);
        });
        pinElements.forEach((pin, id) => {
            const marker = markers.find((item) => item.id === id);
            if (!marker) return;
            pin.classList.toggle('is-trade-hit', isTradeHighlight(marker));
        });
    }

    function updatePinPositions() {
        syncPinElements();
        const view = parseHash();
        const camera = getBlueMapCamera();
        if (!view && !camera) return;
        markers.filter(shouldShowPin).forEach(marker => {
            const pin = pinElements.get(marker.id);
            if (!pin) return;
            pin.classList.toggle('is-trade-hit', isTradeHighlight(marker));
            const point = camera ? projectWithCamera(marker, camera) : projectMarker(marker, view);
            const offscreen = !point || point.behind || point.x < -80 || point.y < -80 || point.x > window.innerWidth + 80 || point.y > window.innerHeight + 80;
            pin.classList.toggle('is-offscreen', offscreen);
            if (!offscreen) {
                pin.style.transform = `translate3d(${point.x}px, ${point.y}px, 0) translate(-50%, -100%)`;
            }
        });
    }

    function renderPanel() {
        if (!document.body) return;
        lastPanelMap = parseHash()?.map || null;
        ensureDockShell();
        renderResultsPanel();
        updatePinPositions();
    }

    async function loadMarkers() {
        loading = true;
        renderPanel();
        try {
            const res = await fetch(`${NODE_API}/api/shop-map-markers?t=${Date.now()}`, { cache: 'no-store' });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            markers = Array.isArray(data.markers) ? data.markers : [];
            localStorage.setItem(CACHE_KEY, JSON.stringify(markers));
            loading = false;
            renderPanel();
            syncPinElements();
            updatePinPositions();
            applyLaunchTradeFocus();
        } catch (error) {
            loading = false;
            renderPanel();
            const panel = document.getElementById(PANEL_ID);
            if (panel) {
                panel.querySelector('.mcwws-dock-results').innerHTML = `<div class="mcwws-dock-result-hint">\u5546\u5e97\u6807\u8bb0\u52a0\u8f7d\u5931\u8d25\uff1a${escapeHtml(error.message)}</div>`;
            }
        }
    }

    function loadCachedMarkers() {
        try {
            const cached = JSON.parse(localStorage.getItem(CACHE_KEY) || '[]');
            markers = Array.isArray(cached) ? cached : [];
        } catch (error) {
            markers = [];
        }
    }

    function start() {
        if (started) return;
        started = true;
        loadCachedMarkers();
        loading = !markers.length;
        renderPanel();
        syncPinElements();
        updatePinPositions();
        loadMarkers();
        applySearchUiState();
        animationId = requestAnimationFrame(tickPins);
    }

    function tickPins() {
        updatePinPositions();
        tickFrame++;
        if (tickFrame % 10 === 0) {
            rememberFlatZoom();
        }
        animationId = requestAnimationFrame(tickPins);
    }

    window.addEventListener('hashchange', () => {
        const map = parseHash()?.map || null;
        if (map !== lastPanelMap) {
            renderPanel();
            syncPinElements();
        }
        updatePinPositions();
    });
    window.addEventListener('resize', updatePinPositions);
    window.addEventListener('beforeunload', () => {
        if (animationId) cancelAnimationFrame(animationId);
    });
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start, { once: true });
        window.addEventListener('load', start, { once: true });
    } else {
        start();
    }
    setInterval(loadMarkers, 10000);
})();
