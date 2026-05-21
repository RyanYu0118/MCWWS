(function () {
    const API_PORT = 8002;
    const PANEL_ID = 'mcwws-shop-panel';
    const PIN_LAYER_ID = 'mcwws-shop-pin-layer';
    const MAP_CONTROLS_ID = 'mcwws-map-controls';
    const CACHE_KEY = 'mcwws-shop-markers-cache';
    const FLAT_HEIGHT_KEY = 'mcwws-last-flat-distance';
    /** BlueMap：ortho=1 为完全正交俯视，ortho=0 为带透视的 flat */
    const FLAT_ORTHO_ON = 1;
    const VIEW_MODE_TRANSITION_MS = 520;
    const VIEW_PARAMS_TRANSITION_MS = 420;
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
    let mapControlsBound = false;
    let cleanModeActive = false;
    let layerMenuOpen = false;
    let dayNightAnimToken = 0;
    let dayNightManualUntil = 0;
    let dayNightPeriod = '';
    let dayNightDayTime = null;
    let dayNightSyncStarted = false;
    const DAY_NIGHT_STRENGTH_DAY = 1;
    const DAY_NIGHT_STRENGTH_NIGHT = 0.25;
    const DAY_NIGHT_THRESHOLD = 0.6;
    const DAY_NIGHT_ANIM_MS = 300;
    const WORLD_TIME_POLL_MS = 8000;
    const DAY_NIGHT_MANUAL_MS = 5 * 60 * 1000;
    const MC_DAY_TICKS = 24000;
    const PLAYER_LOCATE_POLL_MS = 8000;
    const PLAYER_EYE_OFFSET = 1.62;
    /** 与 BlueMap PlayerMarker 一致：脚底 Y + 1.8 为头像锚点 */
    const PLAYER_HEAD_OFFSET = 1.8;
    const LIVE_PLAYERS_CACHE_MS = 2000;

    let mapAuthToken = null;
    let mapAuthUser = null;
    let mapFollowExitBound = false;
    let playerFollowActive = false;
    let playerFollowApplying = false;
    let playerFollowSource = '';
    let cachedSavedPlayerLoc = null;
    let cachedSavedPlayerLocAt = 0;
    let cachedLivePlayers = null;
    let cachedLivePlayersMap = null;
    let cachedLivePlayersAt = 0;

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
                handlePinClick(marker);
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
            case 'perspective': {
                const view = parseHash();
                if (view) {
                    void applyBlueMapView(
                        { ...view, mode: 'perspective', ortho: 0 },
                        { keepControlsOrientation: true }
                    );
                } else {
                    setBlueMapViewMode('perspective');
                }
                break;
            }
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

    const markerSavedHashCache = new Map();

    /** 管理页保存的锚点原样使用，不做 rotation/angle 对调（否则会破坏合法 BlueMap 链接） */
    function getMarkerSavedViewHash(marker) {
        if (!marker?.viewUrl) return null;
        if (!markerSavedHashCache.has(marker.id)) {
            const raw = String(marker.viewUrl).trim();
            const hash = raw.includes('#') ? raw.slice(raw.indexOf('#') + 1) : raw;
            const parts = hash.split(':');
            markerSavedHashCache.set(
                marker.id,
                parts.length === 10 && parts[0] ? hash : null
            );
        }
        return markerSavedHashCache.get(marker.id);
    }

    function openMarker(marker) {
        if (!marker || !marker.viewUrl) return;
        selectedMarkerId = marker.id;
        selectedMarkerTopDown = false;
        const savedHash = getMarkerSavedViewHash(marker);
        if (savedHash) {
            const mode = savedHash.split(':')[9];
            if (mode === 'flat') {
                openMarkerTopDown(marker);
                return;
            }
            const view = parseHashParts(savedHash.split(':'));
            if (view) {
                void applyBlueMapView(view, { keepControlsOrientation: false });
            }
            return;
        }
        const target = parseViewUrl(marker.viewUrl);
        if (!target) {
            window.location.href = marker.viewUrl;
            return;
        }
        void applyBlueMapView({ ...target, mode: 'perspective' }, { keepControlsOrientation: false });
    }

    function openMarkerTopDown(marker) {
        if (!marker?.position || !marker.map) return;
        selectedMarkerId = marker.id;
        selectedMarkerTopDown = true;
        rememberFlatZoom();

        const x = Number(marker.position.x) + 0.5;
        const y = Number(marker.position.y);
        const z = Number(marker.position.z) + 0.5;
        const dist = clampMapDistance(lastFlatHeight);
        void applyBlueMapView({
            map: marker.map,
            x,
            y,
            z,
            distance: dist,
            height: dist,
            rotation: 0,
            angle: 0,
            tilt: 0,
            ortho: FLAT_ORTHO_ON,
            mode: 'flat'
        });
    }

    function handlePinClick(marker) {
        if (!marker) return;
        if (selectedMarkerId === marker.id && selectedMarkerTopDown) {
            openMarker(marker);
            return;
        }
        openMarkerTopDown(marker);
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

    /** BlueMap 锚点：map:x:y:z:distance:rotation:angle:tilt:ortho:mode */
    function parseHashParts(parts) {
        if (parts.length < 4) return null;
        const x = Number(parts[1]);
        const y = Number(parts[2]);
        const z = Number(parts[3]);
        if (![x, y, z].every(Number.isFinite)) return null;
        const distance = Number.isFinite(Number(parts[4])) ? Math.max(1, Number(parts[4])) : 128;
        const rotation = Number.isFinite(Number(parts[5])) ? Number(parts[5]) : 0;
        const angle = Number.isFinite(Number(parts[6])) ? Number(parts[6]) : 0;
        const tilt = Number.isFinite(Number(parts[7])) ? Number(parts[7]) : 0;
        const ortho = Number.isFinite(Number(parts[8])) ? Number(parts[8]) : 0;
        return {
            map: parts[0],
            x,
            y,
            z,
            distance,
            height: distance,
            rotation,
            angle,
            tilt,
            ortho,
            pitch: angle,
            yaw: rotation,
            roll: tilt,
            fov: ortho,
            mode: parts[9] || 'perspective'
        };
    }

    function formatViewHash(view) {
        const distance = view.distance ?? view.height ?? 128;
        return [
            view.map,
            roundViewNumber(view.x),
            roundViewNumber(view.y),
            roundViewNumber(view.z),
            roundViewNumber(distance),
            roundViewNumber(view.rotation ?? view.yaw ?? 0),
            roundViewNumber(view.angle ?? view.pitch ?? 0),
            roundViewNumber(view.tilt ?? view.roll ?? 0),
            roundViewNumber(view.ortho ?? view.fov ?? 0),
            view.mode || 'perspective'
        ].join(':');
    }

    function roundViewNumber(value) {
        return Number(value).toFixed(4).replace(/\.?0+$/, '');
    }

    function setViewHash(view) {
        void applyBlueMapView(view);
    }

    /** 仅写地址栏；真正移动相机须调用 loadBlueMapPageAddress() */
    function replaceLocationHash(hash) {
        const clean = String(hash || '').replace(/^#/, '');
        const url = `${window.location.pathname}${window.location.search}#${clean}`;
        window.history.replaceState(null, '', url);
        syncParentMapHash(clean);
    }

    function syncParentMapHash(hash) {
        if (window.parent === window) return;
        try {
            const parentUrl = new URL(window.parent.location.href);
            parentUrl.hash = hash;
            window.parent.history.replaceState(null, '', parentUrl.toString());
        } catch {
            /* 跨域 iframe 无法同步 */
        }
    }

    async function loadBlueMapPageAddress() {
        const bm = getBlueMapApp();
        if (!bm || typeof bm.loadPageAddress !== 'function') {
            return false;
        }
        cachedCamera = null;
        try {
            await bm.loadPageAddress();
            if (typeof bm.mapViewer?.updateLoadedMapArea === 'function') {
                bm.mapViewer.updateLoadedMapArea();
            }
            updatePinPositions();
            updateMapControlsState();
            return true;
        } catch (err) {
            console.warn('[mcwws-shops] loadPageAddress failed', err);
            return false;
        }
    }

    function waitForBlueMapViewAnimationAsync(bm, maxFrames = 150) {
        return new Promise((resolve) => {
            if (!bm) {
                resolve();
                return;
            }
            let frames = 0;
            function tick() {
                frames += 1;
                if (!bm.viewAnimation || frames >= maxFrames) {
                    resolve();
                    return;
                }
                requestAnimationFrame(tick);
            }
            requestAnimationFrame(tick);
        });
    }

    async function ensureMapForView(view, bm) {
        if (!view?.map || !bm || typeof bm.setMap !== 'function') {
            return;
        }
        const current = bm.mapViewer?.data?.map?.id || parseHash()?.map;
        if (current === view.map) {
            return;
        }
        bm.setMap(view.map);
        await waitForBlueMapViewAnimationAsync(bm);
    }

    /** BlueMap 在 setFlatView/setPerspectiveView 结束时会 mapControls.reset() 并挂回 controls */
    function restoreMapControls() {
        const bm = getBlueMapApp();
        const cm = getControlsManager();
        if (!bm || !cm || cm.controls) {
            return;
        }
        try {
            if (typeof bm.mapControls?.reset === 'function') {
                bm.mapControls.reset();
            }
            if (bm.mapControls) {
                cm.controls = bm.mapControls;
            }
        } catch (err) {
            console.warn('[mcwws-shops] restoreMapControls failed', err);
        }
    }

    function applyControlsFromView(view) {
        const bm = getBlueMapApp();
        const cm = getControlsManager();
        if (!cm || !view) {
            return;
        }
        const dist = clampMapDistance(view.distance ?? view.height);
        cm.position.x = view.x;
        cm.position.y = view.y;
        cm.position.z = view.z;
        cm.distance = dist;
        cm.rotation = view.rotation ?? view.yaw ?? 0;
        cm.angle = view.angle ?? view.pitch ?? 0;
        cm.tilt = view.tilt ?? view.roll ?? 0;
        cm.ortho = view.ortho ?? view.fov ?? 0;
        restoreMapControls();
        syncPageAddressFromControls();
        if (typeof bm?.mapViewer?.updateLoadedMapArea === 'function') {
            bm.mapViewer.updateLoadedMapArea();
        }
    }

    function easeOutQuad(t) {
        return 1 - (1 - t) * (1 - t);
    }

    function lerpNumber(a, b, t) {
        return a + (b - a) * t;
    }

    async function animateControlsToView(view, durationMs) {
        const cm = getControlsManager();
        if (!cm || !view || durationMs <= 0) {
            applyControlsFromView(view);
            return;
        }
        const dist = clampMapDistance(view.distance ?? view.height);
        const end = {
            x: view.x,
            y: view.y,
            z: view.z,
            distance: dist,
            rotation: view.rotation ?? view.yaw ?? 0,
            angle: view.angle ?? view.pitch ?? 0,
            tilt: view.tilt ?? view.roll ?? 0,
            ortho: view.ortho ?? view.fov ?? 0
        };
        const start = {
            x: cm.position.x,
            y: cm.position.y,
            z: cm.position.z,
            distance: cm.distance,
            rotation: cm.rotation ?? 0,
            angle: cm.angle ?? 0,
            tilt: cm.tilt ?? 0,
            ortho: cm.ortho ?? 0
        };
        const delta = Math.hypot(end.x - start.x, end.y - start.y, end.z - start.z)
            + Math.abs(end.distance - start.distance)
            + Math.abs(end.ortho - start.ortho) * 80
            + Math.abs(end.angle - start.angle) * 40;
        if (delta < 0.5) {
            applyControlsFromView(view);
            return;
        }

        const startTime = performance.now();
        let frame = 0;
        await new Promise((resolve) => {
            function step(now) {
                const t = Math.min(1, (now - startTime) / durationMs);
                const e = easeOutQuad(t);
                cm.position.x = lerpNumber(start.x, end.x, e);
                cm.position.y = lerpNumber(start.y, end.y, e);
                cm.position.z = lerpNumber(start.z, end.z, e);
                cm.distance = lerpNumber(start.distance, end.distance, e);
                cm.rotation = lerpNumber(start.rotation, end.rotation, e);
                cm.angle = lerpNumber(start.angle, end.angle, e);
                cm.tilt = lerpNumber(start.tilt, end.tilt, e);
                cm.ortho = lerpNumber(start.ortho, end.ortho, e);
                frame += 1;
                if (frame % 3 === 0) {
                    updatePinPositions();
                }
                if (t < 1) {
                    requestAnimationFrame(step);
                    return;
                }
                applyControlsFromView(view);
                resolve();
            }
            requestAnimationFrame(step);
        });
    }

    async function applyBlueMapView(view, options = {}) {
        let v = normalizeViewForBlueMap(view);
        if (!v) return false;
        const bm = getBlueMapApp();
        const modeMs = Number.isFinite(options.modeDuration)
            ? options.modeDuration
            : VIEW_MODE_TRANSITION_MS;
        const paramMs = Number.isFinite(options.paramDuration)
            ? options.paramDuration
            : VIEW_PARAMS_TRANSITION_MS;

        if (!bm) {
            applyControlsViewFallback(v);
            replaceLocationHash(formatViewHash(v));
            return false;
        }

        cachedCamera = null;
        const fromState = getMapViewState();
        const toState = v.mode || 'perspective';
        if (options.keepControlsOrientation && fromState === 'flat' && toState === 'perspective') {
            v = normalizeViewForBlueMap(mergeViewWithCurrentControls(v));
        }
        if (fromState !== 'flat' && toState === 'flat') {
            v.rotation = 0;
            v.tilt = 0;
        }

        await ensureMapForView(v, bm);

        const dist = clampMapDistance(v.distance ?? v.height);

        if (fromState !== toState && modeMs > 0) {
            if (toState === 'flat' && typeof bm.setFlatView === 'function') {
                bm.setFlatView(modeMs, dist);
            } else if (typeof bm.setPerspectiveView === 'function') {
                bm.setPerspectiveView(modeMs, 0);
            }
            await waitForBlueMapViewAnimationAsync(bm);
            await animateControlsToView(v, paramMs);
        } else if (options.fast || paramMs <= 0) {
            applyControlsFromView(v);
        } else {
            await animateControlsToView(v, paramMs);
        }

        replaceLocationHash(formatViewHash(v));
        restoreMapControls();
        syncMapKeyboardPause();
        updatePinPositions();
        updateMapControlsState();
        return true;
    }

    function updateHashSilently(hash) {
        replaceLocationHash(hash);
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
        const cm = getControlsManager();
        const dist = clampMapDistance(cm?.distance || lastFlatHeight);
        if (mode === 'flat' && typeof bluemap.setFlatView === 'function') {
            bluemap.setFlatView(500, dist);
        } else if (mode === 'perspective' && typeof bluemap.setPerspectiveView === 'function') {
            bluemap.setPerspectiveView(500, 0);
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

    function getMapViewState() {
        return getBlueMapApp()?.appState?.controls?.state || parseHash()?.mode || 'perspective';
    }

    function getMaxPerspectiveAngleForDistance(distance) {
        const HALF_PI = Math.PI / 2;
        const e = Math.max(Number(distance) || 128, 5);
        return Math.min(
            Math.max((1 - Math.pow(Math.max(e - 5, 0.001) * 5e-4, 0.5)) * HALF_PI, 0),
            HALF_PI
        );
    }

    function getControlsRotation() {
        const cm = getControlsManager();
        return Number(cm?.rotation) || 0;
    }

    /** 2D→3D 时保留当前相机方位/位置，避免 hash 里 rotation=0 导致回正 */
    function mergeViewWithCurrentControls(view) {
        const cm = getControlsManager();
        if (!cm || !view) {
            return view;
        }
        const merged = { ...view };
        if (Number.isFinite(cm.rotation)) {
            merged.rotation = cm.rotation;
        }
        if (Number.isFinite(cm.position?.x)) {
            merged.x = cm.position.x;
            merged.y = cm.position.y;
            merged.z = cm.position.z;
        }
        const dist = Number(cm.distance);
        if (Number.isFinite(dist)) {
            merged.distance = dist;
            merged.height = dist;
        }
        return merged;
    }

    /**
     * 屏幕对齐椭圆：长轴恒为横向（scaleX=1），短轴恒为竖向（scaleY 随俯仰变化）。
     * 内层表盘 rotateZ 表示方位；BlueMap angle 越小越俯视，短轴 scaleY 越大。
     */
    function buildCompassTransform() {
        const cm = getControlsManager();
        const isFlat = getMapViewState() === 'flat';
        const bearingDeg = (-getControlsRotation() * 180) / Math.PI;

        let angle = Math.abs(Number(cm?.angle) || 0);
        if (isFlat && angle < 0.02) {
            angle = 0;
        }
        const elevation = Math.min(Math.PI / 2, Math.max(0, angle));
        const shortScale = isFlat ? 1 : Math.max(0.2, Math.cos(elevation));

        return { bearingDeg, shortScale };
    }

    function migrateCompassDom(root) {
        const shell = root?.querySelector('.mcwws-compass-shell');
        if (!shell) return;

        let ellipse = shell.querySelector('.mcwws-compass-ellipse');
        let dial = shell.querySelector('.mcwws-compass-dial');

        dial?.querySelector('.mcwws-compass-n')?.remove();

        if (!ellipse && dial) {
            ellipse = document.createElement('span');
            ellipse.className = 'mcwws-compass-ellipse';
            shell.insertBefore(ellipse, dial);
            ellipse.appendChild(dial);
        }

        dial = shell.querySelector('.mcwws-compass-dial');
        const needle = shell.querySelector('.mcwws-compass-needle');
        if (needle && dial && needle.parentElement !== dial) {
            dial.appendChild(needle);
        }
    }

    function syncPageAddressFromControls() {
        const bm = getBlueMapApp();
        if (bm && typeof bm.updatePageAddress === 'function') {
            bm.updatePageAddress();
        }
    }

    function clampMapDistance(value) {
        const n = Number(value);
        if (!Number.isFinite(n) || n <= 0) return lastFlatHeight || 128;
        return Math.max(5, Math.min(8000, n));
    }

    /**
     * BlueMap 2D 俯视：angle 越小越垂直向下，越大越接近水平。
     * 仅需轻微透视时用小角度（约 0.1–0.38 rad），不能用 max*0.55（近水平）。
     */
    function defaultFlatTiltAngle(distance) {
        const max = getMaxPerspectiveAngleForDistance(distance);
        return Math.min(0.38, Math.max(0.1, max * 0.22));
    }

    function clampMapAngle(angle, distance) {
        const cap = defaultFlatTiltAngle(distance);
        const a = Number(angle);
        if (!Number.isFinite(a) || a <= 0.02) return cap;
        return Math.min(Math.max(a, 0.02), cap);
    }

    /** flat：正交俯视；perspective：必须 ortho=0，否则会保持正交投影 */
    function normalizeViewForBlueMap(view) {
        if (!view) return null;
        const v = { ...view };
        const mode = v.mode || 'perspective';

        if (mode === 'flat') {
            v.distance = clampMapDistance(v.distance ?? v.height);
            v.rotation = Number.isFinite(v.rotation) ? v.rotation : 0;
            v.angle = 0;
            v.tilt = 0;
            v.ortho = FLAT_ORTHO_ON;
            return v;
        }

        v.distance = clampMapDistance(v.distance ?? v.height);
        v.rotation = Number.isFinite(Number(v.rotation)) ? Number(v.rotation) : 0;
        v.tilt = Number.isFinite(Number(v.tilt)) ? Number(v.tilt) : 0;
        v.ortho = 0;
        const ang = Number(v.angle);
        if (!Number.isFinite(ang) || Math.abs(ang) < 0.05) {
            v.angle = -0.75;
        } else {
            v.angle = ang;
        }
        return v;
    }

    function applyControlsViewFallback(view) {
        const bm = getBlueMapApp();
        const mode = view?.mode || 'perspective';
        const dist = clampMapDistance(view?.distance ?? view?.height);
        if (mode === 'flat' && typeof bm?.setFlatView === 'function') {
            bm.setFlatView(VIEW_MODE_TRANSITION_MS, dist);
        } else if (typeof bm?.setPerspectiveView === 'function') {
            bm.setPerspectiveView(VIEW_MODE_TRANSITION_MS, 0);
        }
        applyControlsFromView(view);
        updatePinPositions();
        updateMapControlsState();
    }

    function animateControlsRotation(cm, targetRotation, onDone) {
        const start = Number(cm.rotation) || 0;
        const target = Number(targetRotation) || 0;
        if (Math.abs(start - target) < 0.001) {
            cm.rotation = target;
            onDone();
            return;
        }
        const duration = 300;
        const startTime = performance.now();
        function easeOutQuad(t) {
            return 1 - (1 - t) * (1 - t);
        }
        function step(now) {
            const t = Math.min(1, (now - startTime) / duration);
            cm.rotation = start + (target - start) * (1 - easeOutQuad(t));
            if (t < 1) {
                requestAnimationFrame(step);
            } else {
                cm.rotation = target;
                onDone();
            }
        }
        requestAnimationFrame(step);
    }

    function applyNorthFlatView(cm, distance, angle) {
        const dist = clampMapDistance(distance);
        const useOrtho = Number(cm.ortho) >= FLAT_ORTHO_ON;
        const ang = useOrtho ? 0 : clampMapAngle(angle, dist);
        cm.rotation = 0;
        cm.distance = dist;
        cm.angle = ang;
        cm.ortho = useOrtho ? FLAT_ORTHO_ON : (Number(cm.ortho) || 0);
        rememberFlatZoom();
        syncPageAddressFromControls();
        updateMapControlsState();
        updatePinPositions();
    }

    function waitForBlueMapViewAnimation(bm, onDone) {
        waitForBlueMapViewAnimationAsync(bm).then(() => onDone?.());
    }

    function resetCompassNorth() {
        const cm = getControlsManager();
        const bm = getBlueMapApp();
        if (!cm || !bm) return;

        const hash = parseHash();
        const savedDistance = clampMapDistance(
            Number.isFinite(cm.distance) ? cm.distance : (hash?.distance || lastFlatHeight)
        );
        const orthoOn = Number(hash?.ortho) >= FLAT_ORTHO_ON || Number(cm.ortho) >= FLAT_ORTHO_ON;
        const savedAngle = orthoOn
            ? 0
            : clampMapAngle(
                Number.isFinite(cm.angle) && cm.angle > 0.02 ? cm.angle : (hash?.angle || 0),
                savedDistance
            );
        const state = getMapViewState();

        if (state === 'flat') {
            if (orthoOn) {
                cm.ortho = FLAT_ORTHO_ON;
            }
            animateControlsRotation(cm, 0, () => {
                applyNorthFlatView(cm, savedDistance, savedAngle);
            });
            return;
        }

        if (typeof bm.setFlatView === 'function') {
            bm.setFlatView(500, savedDistance);
            waitForBlueMapViewAnimation(bm, () => {
                if (orthoOn) {
                    cm.ortho = FLAT_ORTHO_ON;
                }
                applyNorthFlatView(cm, savedDistance, savedAngle);
            });
            return;
        }

        setBlueMapViewMode('flat');
        if (orthoOn) {
            cm.ortho = FLAT_ORTHO_ON;
        }
        applyNorthFlatView(cm, savedDistance, savedAngle);
    }

    const ZOOM_FRAME_MS = 16.666;
    let zoomAnimFrame = 0;

    /** 与 BlueMap MouseZoomControls 滚轮一致：deltaZoom += deltaY×0.01，distance *= pow(1.5, …) */
    function findMouseZoomControl(cm) {
        if (!cm?.controls || !Array.isArray(cm.controls)) {
            return null;
        }
        return cm.controls.find((ctrl) => ctrl && typeof ctrl.onMouseWheel === 'function') || null;
    }

    function wheelDeltaZoomFromDirection(direction) {
        const deltaY = direction > 0 ? -100 : 100;
        return deltaY * 0.01;
    }

    function clampAngleAfterZoom(cm) {
        if (!cm) return;
        cm.angle = Math.min(
            Number(cm.angle) || 0,
            getMaxPerspectiveAngleForDistance(cm.distance)
        );
    }

    /** 逐帧调用 MouseZoomControls.update，与滚轮相同的多帧缓动 */
    function runZoomControlUntilSettled(zoom) {
        if (zoomAnimFrame) {
            cancelAnimationFrame(zoomAnimFrame);
            zoomAnimFrame = 0;
        }
        const frameMs = ZOOM_FRAME_MS;
        function tick(now) {
            if (typeof zoom.update === 'function') {
                zoom.update(frameMs, now);
            }
            const cm = zoom.manager || getControlsManager();
            clampAngleAfterZoom(cm);
            if (getMapViewState() === 'flat') {
                rememberFlatZoom();
            }
            updatePinPositions();
            if (Math.abs(Number(zoom.deltaZoom) || 0) > 1e-4) {
                zoomAnimFrame = requestAnimationFrame(tick);
                return;
            }
            zoomAnimFrame = 0;
            syncPageAddressFromControls();
        }
        zoomAnimFrame = requestAnimationFrame(tick);
    }

    function simulateWheelTargetDistance(startDist, direction) {
        let d = Number(startDist) || 128;
        let dz = wheelDeltaZoomFromDirection(direction);
        for (let i = 0; i < 64 && Math.abs(dz) > 1e-4; i++) {
            const blend = 0.2;
            d *= Math.pow(1.5, dz * blend);
            dz *= 1 - blend;
        }
        return Math.max(5, Math.min(8000, d));
    }

    function animateDistanceFallback(cm, direction) {
        if (!cm || !Number.isFinite(Number(cm.distance))) {
            return;
        }
        if (zoomAnimFrame) {
            cancelAnimationFrame(zoomAnimFrame);
            zoomAnimFrame = 0;
        }
        const startDist = Number(cm.distance);
        const targetDist = simulateWheelTargetDistance(startDist, direction);
        const startAngle = Number(cm.angle) || 0;
        const targetAngle = Math.min(startAngle, getMaxPerspectiveAngleForDistance(targetDist));
        const duration = 320;
        const t0 = performance.now();

        function step(now) {
            const t = Math.min(1, (now - t0) / duration);
            const e = 1 - (1 - t) * (1 - t);
            cm.distance = startDist + (targetDist - startDist) * e;
            cm.angle = startAngle + (targetAngle - startAngle) * e;
            if (getMapViewState() === 'flat') {
                rememberFlatZoom();
            }
            updatePinPositions();
            if (t < 1) {
                zoomAnimFrame = requestAnimationFrame(step);
                return;
            }
            zoomAnimFrame = 0;
            syncPageAddressFromControls();
        }
        zoomAnimFrame = requestAnimationFrame(step);
    }

    function applyMapZoomLikeWheel(direction) {
        const cm = getControlsManager();
        const zoom = findMouseZoomControl(cm);
        if (!zoom || !cm) {
            return false;
        }
        zoom.deltaZoom = (Number(zoom.deltaZoom) || 0) + wheelDeltaZoomFromDirection(direction);
        runZoomControlUntilSettled(zoom);
        return true;
    }

    function adjustMapZoom(direction) {
        if (applyMapZoomLikeWheel(direction)) {
            return;
        }
        const cm = getControlsManager();
        animateDistanceFallback(cm, direction);
    }

    function toggleMapViewMode() {
        const state = getMapViewState();
        const view = parseHash();
        const cm = getControlsManager();
        if (state === 'flat') {
            if (view) {
                void applyBlueMapView(
                    { ...view, mode: 'perspective', ortho: 0 },
                    { keepControlsOrientation: true }
                );
            } else {
                const bm = getBlueMapApp();
                bm?.setPerspectiveView?.(0, 0);
            }
            return;
        }
        rememberFlatZoom();
        const dist = Number.isFinite(cm?.distance) ? cm.distance : lastFlatHeight;
        if (view) {
            void applyBlueMapView({
                ...view,
                mode: 'flat',
                distance: dist,
                height: dist,
                rotation: 0,
                angle: 0,
                tilt: 0,
                ortho: FLAT_ORTHO_ON
            });
        } else {
            setBlueMapViewMode('flat');
        }
    }

    function getAvailableMaps() {
        const bm = getBlueMapApp();
        const list = bm?.appState?.maps;
        if (Array.isArray(list) && list.length) {
            return list.map((entry) => {
                if (typeof entry === 'string') {
                    return { id: entry, name: entry };
                }
                const id = entry?.id || entry?.mapId || entry?.map?.id;
                const name = entry?.name || entry?.map?.name || entry?.label || id;
                if (!id) return null;
                return { id: String(id), name: String(name || id) };
            }).filter(Boolean);
        }
        const ids = new Set();
        markers.forEach((marker) => {
            if (marker.map) ids.add(marker.map);
        });
        const current = parseHash()?.map;
        if (current) ids.add(current);
        if (!ids.size) ids.add('world');
        return [...ids].map((id) => ({ id, name: id }));
    }

    function switchMapLayer(mapId) {
        if (!mapId) return;
        const view = parseHash();
        if (view) {
            view.map = mapId;
            setViewHash(view);
        } else {
            setViewHash({
                map: mapId,
                x: 0,
                y: 64,
                z: 0,
                distance: lastFlatHeight,
                height: lastFlatHeight,
                rotation: 0,
                angle: 0,
                tilt: 0,
                ortho: FLAT_ORTHO_ON,
                mode: getMapViewState() === 'flat' ? 'flat' : 'perspective'
            });
        }
        const bm = getBlueMapApp();
        if (bm && typeof bm.setMap === 'function') {
            bm.setMap(mapId);
        }
        layerMenuOpen = false;
        renderLayerMenu();
        renderPanel();
        syncPinElements();
    }

    function setCleanMode(active) {
        cleanModeActive = !!active;
        document.body.classList.toggle('mcwws-clean-mode', cleanModeActive);
        const root = document.getElementById(MAP_CONTROLS_ID);
        root?.classList.toggle('is-clean', cleanModeActive);
        try {
            window.parent.postMessage({ type: 'mcwws-map-clean', active: cleanModeActive }, '*');
        } catch {
            /* ignore */
        }
        updateMapControlsState();
    }

    function toggleCleanMode() {
        setCleanMode(!cleanModeActive);
    }

    /** 登录态由父页面（8002）经 postMessage 同步，与商店/管理共用 authToken */
    async function applyExternalAuth(payload) {
        if (!payload || typeof payload !== 'object') return;
        const wasLoggedIn = !!mapAuthUser;
        mapAuthToken = payload.authToken || null;
        mapAuthUser = payload.user || null;

        if (mapAuthToken && !mapAuthUser) {
            try {
                const res = await fetch(`${NODE_API}/api/profile`, {
                    headers: authHeaders(),
                    cache: 'no-store'
                });
                if (res.ok) {
                    mapAuthUser = await res.json();
                } else {
                    mapAuthToken = null;
                }
            } catch {
                mapAuthToken = null;
                mapAuthUser = null;
            }
        }

        if (wasLoggedIn && !mapAuthUser) {
            stopPlayerFollow();
            cachedSavedPlayerLoc = null;
        }
        updateLocateAuthUi();
    }

    function authHeaders() {
        if (!mapAuthToken) return {};
        return { Authorization: `Bearer ${mapAuthToken}` };
    }

    function updateLocateAuthUi() {
        const locateBtn = document.querySelector('.mcwws-ctrl-locate');
        if (locateBtn) {
            locateBtn.disabled = !mapAuthUser;
            locateBtn.classList.toggle('is-disabled', !mapAuthUser);
        }
        updateMapControlsState();
    }

    function requestAuthFromParent() {
        if (window.parent === window) return;
        try {
            window.parent.postMessage({ type: 'mcwws-auth-request' }, '*');
        } catch {
            /* ignore */
        }
    }

    function requestLoginModalFromParent() {
        if (window.parent === window) return;
        try {
            window.parent.postMessage({ type: 'mcwws-auth-required' }, '*');
        } catch {
            /* ignore */
        }
    }

    function initMapAuth() {
        window.addEventListener('message', (event) => {
            if (event.data?.type === 'mcwws-auth') {
                void applyExternalAuth(event.data);
            }
        });
        updateLocateAuthUi();
        requestAuthFromParent();
        let retries = 0;
        const retryTimer = window.setInterval(() => {
            retries += 1;
            if (mapAuthUser || retries >= 12) {
                window.clearInterval(retryTimer);
                return;
            }
            requestAuthFromParent();
        }, 500);
    }

    function normalizePlayerKey(value) {
        return String(value || '').trim().toLowerCase();
    }

    function getCurrentMapId() {
        return getBlueMapApp()?.mapViewer?.data?.map?.id || parseHash()?.map || 'world';
    }

    async function fetchLivePlayersList(mapId) {
        const id = mapId || getCurrentMapId();
        if (
            cachedLivePlayers
            && cachedLivePlayersMap === id
            && Date.now() - cachedLivePlayersAt < LIVE_PLAYERS_CACHE_MS
        ) {
            return cachedLivePlayers;
        }
        try {
            const res = await fetch(
                `maps/${encodeURIComponent(id)}/live/players.json?t=${Date.now()}`,
                { cache: 'no-store' }
            );
            if (!res.ok) return null;
            const data = await res.json();
            const list = Array.isArray(data?.players) ? data.players : null;
            cachedLivePlayers = list;
            cachedLivePlayersMap = id;
            cachedLivePlayersAt = Date.now();
            return list;
        } catch {
            return null;
        }
    }

    function matchLivePlayerEntry(entry, playerId) {
        if (!entry || !playerId) return false;
        const target = normalizePlayerKey(playerId);
        const name = normalizePlayerKey(entry.name);
        if (name === target) return true;
        const alt = normalizePlayerKey(entry.displayName || entry.playerName);
        return alt === target;
    }

    function coordsFromLiveEntry(entry, mapId) {
        const p = entry?.position || {};
        const x = Number(p.x);
        const y = Number(p.y);
        const z = Number(p.z);
        if (![x, y, z].every(Number.isFinite)) return null;
        return {
            map: mapId || getCurrentMapId(),
            x,
            y: y + PLAYER_HEAD_OFFSET,
            z,
            online: true,
            source: 'live'
        };
    }

    function coordsFromPlayerMarker(marker, mapId) {
        const raw = marker?.data?.position || {};
        const rx = Number(raw.x);
        const ry = Number(raw.y);
        const rz = Number(raw.z);
        if ([rx, ry, rz].every(Number.isFinite)) {
            return {
                map: mapId || getCurrentMapId(),
                x: rx,
                y: ry + PLAYER_HEAD_OFFSET,
                z: rz,
                online: true,
                source: 'live-marker'
            };
        }
        const x = Number(marker?.position?.x);
        const y = Number(marker?.position?.y);
        const z = Number(marker?.position?.z);
        if (![x, y, z].every(Number.isFinite)) return null;
        return {
            map: mapId || getCurrentMapId(),
            x,
            y,
            z,
            online: true,
            source: 'live-marker'
        };
    }

    async function findLivePlayerPosition(playerId) {
        const mapId = getCurrentMapId();
        const players = await fetchLivePlayersList(mapId);
        if (players?.length) {
            const entry = players.find((item) => matchLivePlayerEntry(item, playerId));
            const fromJson = entry ? coordsFromLiveEntry(entry, mapId) : null;
            if (fromJson) return fromJson;
        }
        const markerSets = getBlueMapApp()?.mapViewer?.markers?.data?.markerSets || [];
        for (const set of markerSets) {
            if (set.id !== 'bm-players') continue;
            for (const marker of set.markers || []) {
                const id = normalizePlayerKey(marker.data?.id || marker.id);
                const label = normalizePlayerKey(
                    marker.data?.label || marker.data?.name || marker.data?.playerUuid
                );
                const target = normalizePlayerKey(playerId);
                if (id !== target && label !== target) continue;
                return coordsFromPlayerMarker(marker, mapId);
            }
        }
        return null;
    }

    function triggerControlsCameraUpdate(cm) {
        const controls = cm || getControlsManager();
        if (controls && typeof controls.updateCamera === 'function') {
            controls.updateCamera();
        }
        getBlueMapApp()?.mapViewer?.redraw?.();
    }

    /** 根据头像在屏幕上的投影微调相机焦点，使玩家头像尽量位于正中 */
    function nudgeFocusToScreenCenter(pos) {
        const cm = getControlsManager();
        const camera = getBlueMapCamera();
        if (!cm || !camera || !pos) return pos;

        const probe = { position: { x: pos.x, y: pos.y, z: pos.z } };
        const screen = projectWithCamera(probe, camera);
        if (!screen || screen.behind) return pos;

        const dx = screen.x - window.innerWidth * 0.5;
        const dy = screen.y - window.innerHeight * 0.5;
        if (Math.hypot(dx, dy) < 12) return pos;

        const rot = Number(cm.rotation) || 0;
        const dist = Math.max(Number(cm.distance) || 128, 20);
        const pxToWorld = dist / Math.max(window.innerHeight, 400);
        const rightX = Math.cos(rot);
        const rightZ = Math.sin(rot);
        const forwardX = Math.sin(rot);
        const forwardZ = -Math.cos(rot);

        return {
            ...pos,
            x: pos.x - rightX * dx * pxToWorld - forwardX * dy * pxToWorld * 0.35,
            y: pos.y - dy * pxToWorld * 0.25,
            z: pos.z - rightZ * dx * pxToWorld - forwardZ * dy * pxToWorld * 0.35
        };
    }

    function applyFollowCameraTarget(pos) {
        const cm = getControlsManager();
        if (!cm || !pos) return;
        const adjusted = nudgeFocusToScreenCenter(pos);
        cm.position.x = adjusted.x;
        cm.position.y = adjusted.y;
        cm.position.z = adjusted.z;
        triggerControlsCameraUpdate(cm);
    }

    async function fetchSavedPlayerLocation() {
        if (!mapAuthToken) return null;
        if (cachedSavedPlayerLoc && Date.now() - cachedSavedPlayerLocAt < PLAYER_LOCATE_POLL_MS) {
            return cachedSavedPlayerLoc;
        }
        try {
            const res = await fetch(`${NODE_API}/api/player-location?t=${Date.now()}`, {
                headers: authHeaders(),
                cache: 'no-store'
            });
            if (!res.ok) return null;
            const data = await res.json();
            cachedSavedPlayerLoc = {
                map: data.map || 'world',
                x: Number(data.x),
                y: Number(data.y) + PLAYER_EYE_OFFSET,
                z: Number(data.z),
                online: false,
                source: data.source || 'saved'
            };
            cachedSavedPlayerLocAt = Date.now();
            return cachedSavedPlayerLoc;
        } catch {
            return null;
        }
    }

    async function resolvePlayerPosition() {
        if (!mapAuthUser?.playerId) return null;
        const live = findLivePlayerPosition(mapAuthUser.playerId);
        if (live && [live.x, live.y, live.z].every(Number.isFinite)) {
            playerFollowSource = 'live';
            return live;
        }
        const saved = await fetchSavedPlayerLocation();
        if (saved && [saved.x, saved.y, saved.z].every(Number.isFinite)) {
            playerFollowSource = saved.source || 'saved';
            return saved;
        }
        return null;
    }

    function buildFollowViewFromPosition(pos) {
        const hash = parseHash();
        const cm = getControlsManager();
        const mode = getMapViewState();
        const dist = clampMapDistance(cm?.distance ?? hash?.distance ?? lastFlatHeight);
        const view = {
            map: pos.map || hash?.map || 'world',
            x: pos.x,
            y: pos.y,
            z: pos.z,
            distance: dist,
            height: dist,
            rotation: Number.isFinite(cm?.rotation) ? cm.rotation : (hash?.rotation ?? 0),
            angle: Number.isFinite(cm?.angle) ? cm.angle : (hash?.angle ?? 0),
            tilt: Number.isFinite(cm?.tilt) ? cm.tilt : (hash?.tilt ?? 0),
            ortho: Number(cm?.ortho ?? hash?.ortho ?? 0),
            mode
        };
        return normalizeViewForBlueMap(view);
    }

    function stopPlayerFollow() {
        if (!playerFollowActive) return;
        playerFollowActive = false;
        playerFollowSource = '';
        updateMapControlsState();
    }

    function bindMapFollowExitEvents() {
        if (mapFollowExitBound) return;
        mapFollowExitBound = true;

        const isMapUiTarget = (target) => !!(
            target?.closest?.('.mcwws-map-controls')
            || target?.closest?.('#mcwws-shop-panel')
        );

        const onUserMapGesture = (event) => {
            if (!playerFollowActive || playerFollowApplying) return;
            if (isMapUiTarget(event.target)) return;
            const app = document.getElementById('app');
            if (app && !app.contains(event.target)) return;
            stopPlayerFollow();
        };

        document.addEventListener('pointerdown', onUserMapGesture, true);
        document.addEventListener('wheel', onUserMapGesture, { passive: true, capture: true });
        document.addEventListener('keydown', (event) => {
            if (!playerFollowActive || playerFollowApplying) return;
            if (isDockTextInputFocused()) return;
            const key = String(event.key || '').toLowerCase();
            if (['w', 'a', 's', 'd', 'q', 'e', 'arrowup', 'arrowdown', 'arrowleft', 'arrowright'].includes(key)) {
                stopPlayerFollow();
            }
        }, true);
    }

    async function centerCameraOnPlayer(pos, options = {}) {
        const view = buildFollowViewFromPosition(pos);
        if (!view) return false;
        const bm = getBlueMapApp();
        playerFollowApplying = true;
        if (bm) {
            await ensureMapForView(view, bm);
        }
        if (options.animate) {
            await applyBlueMapView(view, { fast: false, paramDuration: 280, modeDuration: 0 });
        } else {
            applyControlsFromView(view);
            replaceLocationHash(formatViewHash(view));
            updatePinPositions();
        }
        applyFollowCameraTarget(pos);
        playerFollowApplying = false;
        return true;
    }

    async function tickPlayerFollowCenter() {
        if (!playerFollowActive || !mapAuthUser) return;
        const pos = await resolvePlayerPosition();
        if (!pos) return;
        const view = buildFollowViewFromPosition(pos);
        const cm = getControlsManager();
        const bm = getBlueMapApp();
        if (!view || !cm) return;
        playerFollowApplying = true;
        if (bm && view.map) {
            await ensureMapForView(view, bm);
        }
        applyFollowCameraTarget(pos);
        if (tickFrame % 15 === 0) {
            syncPageAddressFromControls();
        }
        updatePinPositions();
        playerFollowApplying = false;
        updateMapControlsState();
    }

    async function togglePlayerLocate() {
        if (playerFollowActive) {
            stopPlayerFollow();
            return;
        }
        if (!mapAuthUser?.playerId) {
            requestLoginModalFromParent();
            requestAuthFromParent();
            return;
        }
        const pos = await resolvePlayerPosition();
        if (!pos) {
            return;
        }
        playerFollowActive = true;
        await centerCameraOnPlayer(pos, { animate: true });
        updateMapControlsState();
    }

    function easeOutQuad(t) {
        return t * (2 - t);
    }

    function getMapViewerUniforms() {
        return getBlueMapApp()?.mapViewer?.data?.uniforms || null;
    }

    function getSunlightStrength() {
        const value = getMapViewerUniforms()?.sunlightStrength?.value;
        return Number.isFinite(value) ? value : DAY_NIGHT_STRENGTH_DAY;
    }

    function isMapDaylight() {
        return getSunlightStrength() > DAY_NIGHT_THRESHOLD;
    }

    function redrawMapViewer() {
        getBlueMapApp()?.mapViewer?.redraw?.();
    }

    function setSunlightStrengthImmediate(value) {
        const uniforms = getMapViewerUniforms();
        if (!uniforms?.sunlightStrength) return false;
        uniforms.sunlightStrength.value = value;
        redrawMapViewer();
        return true;
    }

    function animateSunlightStrength(target, durationMs = DAY_NIGHT_ANIM_MS) {
        const uniforms = getMapViewerUniforms();
        if (!uniforms?.sunlightStrength) return Promise.resolve(false);
        const from = getSunlightStrength();
        if (Math.abs(from - target) < 0.002) {
            setSunlightStrengthImmediate(target);
            return Promise.resolve(true);
        }
        const token = ++dayNightAnimToken;
        const start = performance.now();
        return new Promise((resolve) => {
            function frame(now) {
                if (token !== dayNightAnimToken) {
                    resolve(false);
                    return;
                }
                const progress = Math.min(1, (now - start) / durationMs);
                const eased = easeOutQuad(progress);
                uniforms.sunlightStrength.value = from + (target - from) * eased;
                redrawMapViewer();
                if (progress < 1) {
                    requestAnimationFrame(frame);
                    return;
                }
                uniforms.sunlightStrength.value = target;
                redrawMapViewer();
                resolve(true);
            }
            requestAnimationFrame(frame);
        });
    }

    /** 与 BlueMap DayNightSwitch 一致：>0.6 为日景，否则夜景；目标 1 / 0.25 */
    function sunlightStrengthFromDayTime(dayTime) {
        const tick = ((Number(dayTime) % MC_DAY_TICKS) + MC_DAY_TICKS) % MC_DAY_TICKS;
        return 0.625 + 0.375 * Math.cos((2 * Math.PI * (tick - 6000)) / MC_DAY_TICKS);
    }

    function isDayNightManualActive() {
        return Date.now() < dayNightManualUntil;
    }

    function toggleMapDayNight() {
        const target = isMapDaylight() ? DAY_NIGHT_STRENGTH_NIGHT : DAY_NIGHT_STRENGTH_DAY;
        dayNightManualUntil = Date.now() + DAY_NIGHT_MANUAL_MS;
        animateSunlightStrength(target).then(() => updateMapControlsState());
        updateMapControlsState();
    }

    async function syncMapDayNightFromServer() {
        if (isDayNightManualActive()) {
            return;
        }
        const uniforms = getMapViewerUniforms();
        if (!uniforms?.sunlightStrength) {
            return;
        }
        try {
            const res = await fetch(`${NODE_API}/api/world-time?t=${Date.now()}`, { cache: 'no-store' });
            if (!res.ok) return;
            const data = await res.json();
            if (isDayNightManualActive()) return;
            const target = Number(data.sunlightStrength);
            if (!Number.isFinite(target)) return;
            dayNightPeriod = String(data.period || '');
            dayNightDayTime = Number.isFinite(Number(data.dayTime)) ? Number(data.dayTime) : null;
            const current = getSunlightStrength();
            if (Math.abs(current - target) < 0.02) {
                if (Math.abs(current - target) > 0.001) {
                    setSunlightStrengthImmediate(target);
                }
                updateMapControlsState();
                return;
            }
            await animateSunlightStrength(target);
            updateMapControlsState();
        } catch {
            /* ignore */
        }
    }

    function startDayNightSync() {
        if (dayNightSyncStarted) return;
        dayNightSyncStarted = true;
        const tick = () => syncMapDayNightFromServer();
        const waitForBlueMap = () => {
            if (getMapViewerUniforms()?.sunlightStrength) {
                tick();
                setInterval(tick, WORLD_TIME_POLL_MS);
                return;
            }
            requestAnimationFrame(waitForBlueMap);
        };
        waitForBlueMap();
    }

    function renderLayerMenu() {
        const menu = document.getElementById(MAP_CONTROLS_ID)?.querySelector('.mcwws-layer-menu');
        if (!menu) return;
        const maps = getAvailableMaps();
        const current = parseHash()?.map || maps[0]?.id;
        menu.hidden = !layerMenuOpen;
        menu.innerHTML = maps.map((entry) => {
            const active = entry.id === current;
            return `
                <button type="button" class="mcwws-layer-item${active ? ' is-active' : ''}" data-map-id="${escapeHtml(entry.id)}">
                    <span class="mcwws-layer-item-thumb" aria-hidden="true"></span>
                    <span class="mcwws-layer-item-name">${escapeHtml(entry.name)}</span>
                </button>
            `;
        }).join('');
    }

    function updateMapControlsState() {
        const root = document.getElementById(MAP_CONTROLS_ID);
        if (!root) return;
        const shell = root.querySelector('.mcwws-compass-shell');
        const ellipse = root.querySelector('.mcwws-compass-ellipse');
        const dial = root.querySelector('.mcwws-compass-dial');
        const modeLabel = root.querySelector('.mcwws-ctrl-mode-label');
        const fsLabel = root.querySelector('.mcwws-ctrl-fs-label');
        const isFlat = getMapViewState() === 'flat';
        const compass = buildCompassTransform();
        if (shell) {
            shell.classList.toggle('is-flat', isFlat);
            shell.classList.toggle('is-perspective', !isFlat);
            shell.style.perspective = '';
            shell.style.perspectiveOrigin = '';
        }
        if (ellipse) {
            ellipse.style.transform = `scaleY(${compass.shortScale.toFixed(3)})`;
        }
        if (dial) {
            dial.style.transform = `rotate(${compass.bearingDeg.toFixed(2)}deg)`;
        }
        const compassBtn = root.querySelector('.mcwws-ctrl-compass');
        if (compassBtn) {
            compassBtn.title = isFlat
                ? '复位朝北（2D 正交俯视，上北下南）'
                : '复位朝北（3D 透视）';
        }
        if (modeLabel) {
            modeLabel.textContent = getMapViewState() === 'flat' ? '3D' : '2D';
        }
        if (fsLabel) {
            fsLabel.textContent = cleanModeActive ? '退出' : '全屏';
        }
        const fsIcon = root.querySelector('.mcwws-ctrl-fs-icon path');
        if (fsIcon) {
            fsIcon.setAttribute('d', cleanModeActive
                ? 'M7 7h4V5H5v6h2V7zm10 0v4h2V5h-6v2h4zM7 17H5v-6H3v8h8v-2H7zm10 0h-4v2h6v-8h-2v6z'
                : 'M4 9V4h5V6H6v3H4zm0 11v-5h2v3h3v2H4zm16-11V6h-3V4h5v5h-2zm0 16h-5v-2h3v-3h2v5z');
        }
        root.querySelector('.mcwws-ctrl-fullscreen')?.setAttribute(
            'title',
            cleanModeActive ? '退出全屏，还原功能模块' : '全屏，隐藏所有功能模块'
        );
        const dayBtn = root.querySelector('.mcwws-ctrl-daynight');
        if (dayBtn) {
            const isDay = isMapDaylight();
            dayBtn.classList.toggle('is-night', !isDay);
            const manual = isDayNightManualActive();
            const manualMin = manual ? Math.max(1, Math.ceil((dayNightManualUntil - Date.now()) / 60000)) : 0;
            const timeHint = dayNightDayTime != null ? `，游戏刻 ${dayNightDayTime}` : '';
            const periodHint = dayNightPeriod ? `（${dayNightPeriod}${timeHint}）` : (timeHint || '');
            if (manual) {
                dayBtn.title = isDay
                    ? `当前为日景${periodHint}；已手动切换，约 ${manualMin} 分钟后恢复跟随游戏时间`
                    : `当前为夜景${periodHint}；已手动切换，约 ${manualMin} 分钟后恢复跟随游戏时间`;
            } else {
                dayBtn.title = isDay
                    ? `切换为夜景${periodHint}（跟随主世界游戏时间）`
                    : `切换为日景${periodHint}（跟随主世界游戏时间）`;
            }
        }
        const locateBtn = root.querySelector('.mcwws-ctrl-locate');
        if (locateBtn) {
            locateBtn.classList.toggle('is-active', playerFollowActive);
            if (!mapAuthUser) {
                locateBtn.title = '请先在顶栏登录后再定位玩家';
            } else if (playerFollowActive) {
                const modeLabel = playerFollowSource === 'live' ? '实时跟踪' : '已保存位置';
                locateBtn.title = `正在定位 ${mapAuthUser.playerId}（${modeLabel}）— 点击取消；拖动地图将退出定位`;
            } else {
                locateBtn.title = `定位到 ${mapAuthUser.playerId}（在线实时 / 离线用服务器存档位置）`;
            }
        }
    }

    function ensureMapControls() {
        let root = document.getElementById(MAP_CONTROLS_ID);
        if (root?.dataset.ready === '1') {
            migrateCompassDom(root);
            updateMapControlsState();
            return root;
        }
        root = document.createElement('div');
        root.id = MAP_CONTROLS_ID;
        root.className = 'mcwws-map-controls';
        root.dataset.ready = '1';
        root.innerHTML = `
            <div class="mcwws-layer-menu" hidden></div>
            <div class="mcwws-map-controls-stack">
                <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-compass" title="复位朝北（2D 俯视，上北下南）">
                    <span class="mcwws-compass-shell" aria-hidden="true">
                        <span class="mcwws-compass-ellipse">
                            <span class="mcwws-compass-dial">
                                <span class="mcwws-compass-needle"></span>
                            </span>
                        </span>
                    </span>
                </button>
                <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-mode" title="切换 2D / 3D 视图">
                    <span class="mcwws-ctrl-mode-label">2D</span>
                </button>
                <div class="mcwws-ctrl-zoom">
                    <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-zoom-in" title="放大">+</button>
                    <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-zoom-out" title="缩小">−</button>
                </div>
                <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-daynight" title="切换日景 / 夜景">
                    <svg class="mcwws-ctrl-daynight-icon" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                        <circle class="mcwws-daynight-sun" cx="12" cy="12" r="4" fill="currentColor"/>
                        <g class="mcwws-daynight-moon" fill="currentColor">
                            <path d="M14.5 3.2a7.5 7.5 0 1 0 7.3 11.8A6.5 6.5 0 1 1 14.5 3.2z"/>
                        </g>
                    </svg>
                </button>
                <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-locate" title="定位到已登录玩家（需先登录）" disabled>
                    <svg class="mcwws-ctrl-locate-icon" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                        <path fill="currentColor" d="M12 2a7 7 0 0 0-7 7c0 5.25 7 13 7 13s7-7.75 7-13a7 7 0 0 0-7-7zm0 9.5A2.5 2.5 0 1 1 12 6a2.5 2.5 0 0 1 0 5.5z"/>
                    </svg>
                </button>
                <div class="mcwws-ctrl-bottom-row">
                    <button type="button" class="mcwws-ctrl-layer" title="图层选择">
                        <span class="mcwws-ctrl-layer-thumb" aria-hidden="true"></span>
                        <span class="mcwws-ctrl-layer-text">
                            <svg class="mcwws-ctrl-layer-icon" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true"><path fill="currentColor" d="M12 2 2 7l10 5 10-5-10-5zm0 8.5L2 6v2.5l10 5 10-5V6l-10 4.5zm0 4.5L2 10.5V13l10 5 10-5v-2.5L12 15z"/></svg>
                            图层
                        </span>
                    </button>
                    <button type="button" class="mcwws-ctrl-fullscreen" title="全屏，隐藏所有功能模块">
                        <svg class="mcwws-ctrl-fs-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path fill="currentColor" d="M4 9V4h5V6H6v3H4zm0 11v-5h2v3h3v2H4zm16-11V6h-3V4h5v5h-2zm0 16h-5v-2h3v-3h2v5z"/></svg>
                        <span class="mcwws-ctrl-fs-label">全屏</span>
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(root);
        bindMapControlEvents(root);
        renderLayerMenu();
        updateMapControlsState();
        return root;
    }

    function bindMapControlEvents(root) {
        if (mapControlsBound) return;
        mapControlsBound = true;

        root.querySelector('.mcwws-ctrl-compass')?.addEventListener('click', resetCompassNorth);
        root.querySelector('.mcwws-ctrl-mode')?.addEventListener('click', () => {
            toggleMapViewMode();
            updateMapControlsState();
        });
        root.querySelector('.mcwws-ctrl-zoom-in')?.addEventListener('click', () => adjustMapZoom(1));
        root.querySelector('.mcwws-ctrl-zoom-out')?.addEventListener('click', () => adjustMapZoom(-1));
        root.querySelector('.mcwws-ctrl-daynight')?.addEventListener('click', toggleMapDayNight);
        root.querySelector('.mcwws-ctrl-locate')?.addEventListener('click', () => {
            void togglePlayerLocate();
        });
        root.querySelector('.mcwws-ctrl-fullscreen')?.addEventListener('click', toggleCleanMode);
        root.querySelector('.mcwws-ctrl-layer')?.addEventListener('click', (e) => {
            e.stopPropagation();
            layerMenuOpen = !layerMenuOpen;
            renderLayerMenu();
        });

        root.addEventListener('click', (e) => {
            const item = e.target.closest('.mcwws-layer-item');
            if (!item) return;
            switchMapLayer(item.dataset.mapId);
            updateMapControlsState();
        });

        document.addEventListener('click', (e) => {
            if (!layerMenuOpen) return;
            if (e.target.closest('.mcwws-ctrl-layer') || e.target.closest('.mcwws-layer-menu')) return;
            layerMenuOpen = false;
            renderLayerMenu();
        });
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
        const yaw = view.rotation ?? view.yaw ?? 0;
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
        ensureMapControls();
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
            markerSavedHashCache.clear();
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
        initMapAuth();
        bindMapFollowExitEvents();
        startDayNightSync();
        animationId = requestAnimationFrame(tickPins);
    }

    function tickPins() {
        updatePinPositions();
        tickFrame++;
        if (playerFollowActive && tickFrame % 2 === 0) {
            void tickPlayerFollowCenter();
        }
        if (tickFrame % 10 === 0) {
            rememberFlatZoom();
        }
        if (tickFrame % 6 === 0) {
            updateMapControlsState();
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
        updateMapControlsState();
        if (layerMenuOpen) renderLayerMenu();
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
