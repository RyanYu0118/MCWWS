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
    /** 右下角 2D/3D 按钮：短过渡，比全量 520/420 更快但仍可见动画 */
    const VIEW_MODE_UI_TOGGLE_MS = 260;
    const VIEW_MODE_UI_PARAM_MS = 220;
    const VIEW_MODE_UI_ANIM_WAIT_FRAMES = 36;
    /** 商店钉回到 2D B：模式切换 + 飞回原点，略长于单次参数动画 */
    const VIEW_RESTORE_TRANSITION_MS = 720;
    /** BlueMap setFlatView(durationMs, minDistance)：第二参数是最小缩放，不是俯仰角 */
    const FLAT_VIEW_MIN_DISTANCE = 5;
    const NODE_API = `${window.location.protocol}//${window.location.hostname}:${API_PORT}`;
    let markers = [];
    let loading = true;
    let started = false;
    let animationId = 0;
    let pinElements = new Map();
    let cachedCamera = null;
    let selectedMarkerId = null;
    let selectedMarkerTopDown = false;
    /** 点击商店钉前的视角 B；再次点击回到 B */
    let preShopPinView = null;
    let shopPinAtViewA = false;
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
    /** @type {null | 'day' | 'night'} */
    let dayNightLock = null;
    let dayNightLongPressTimer = 0;
    let dayNightLongPressHandled = false;
    let dayNightLockRafId = 0;
    let dayNightStrengthInternalSet = false;
    let dayNightPeriod = '';
    let dayNightDayTime = null;
    let dayNightSyncStarted = false;
    const DAY_NIGHT_STRENGTH_DAY = 1;
    const DAY_NIGHT_STRENGTH_NIGHT = 0.25;
    const DAY_NIGHT_THRESHOLD = 0.6;
    const DAY_NIGHT_ANIM_MS = 300;
    const WORLD_TIME_POLL_MS = 8000;
    const DAY_NIGHT_MANUAL_MS = 5 * 60 * 1000;
    const DAY_NIGHT_LONG_PRESS_MS = 600;
    const DAY_NIGHT_LOCK_RING_LEN = 100;
    const STORAGE_DAY_NIGHT_LOCK = 'mcwws-daynight-lock';
    const MC_DAY_TICKS = 24000;
    const PLAYER_LOCATE_POLL_MS = 8000;
    const LIVE_PLAYERS_CACHE_MS = 1500;
    const PLAYER_FOLLOW_POLL_MS = 350;
    const PLAYER_FOLLOW_LIVE_CACHE_MS = 280;
    const PLAYER_FOLLOW_SMOOTH_MS = 420;
    const PLAYER_FOLLOW_SMOOTH_Y_MS = 560;
    const PLAYER_FOLLOW_TELEPORT_BLOCKS = 48;
    const PLAYER_FOLLOW_START_ANIM_MS = 380;
    const PLAYER_FOLLOW_SNAP_BACK_MS = 620;
    const PLAYER_FOLLOW_DRAG_EXIT_MS = 2000;
    /** 按下后移动超过该像素才视为拖拽，避免左键按下瞬间停跟导致 Y 被地形逻辑拉偏 */
    const PLAYER_FOLLOW_PAN_DRAG_PX = 6;
    const LOCATE_PROGRESS_RING_LEN = 100;
    /** Essentials 存档为脚底；BlueMap 在线数据同为脚底，显示高度 +1.8 */
    const PLAYER_EYE_OFFSET = 1.62;
    const PLAYER_HEAD_OFFSET = 1.8;

    let mapAuthToken = null;
    let mapAuthUser = null;
    let mapFollowExitBound = false;
    let playerFollowActive = false;
    let playerFollowApplying = false;
    let playerFollowSource = '';
    let cachedPlayerLoc = null;
    let cachedPlayerLocAt = 0;
    let cachedLivePlayers = null;
    let cachedLivePlayersMap = null;
    let cachedLivePlayersAt = 0;
    let playerFollowTarget = null;
    let playerFollowMapId = null;
    let playerFollowPollBusy = false;
    let playerFollowLastPollAt = 0;
    let playerFollowLastSmoothAt = 0;
    let playerFollowTeleportToken = 0;
    let mapHeightUpdateSaved = null;
    let playerFollowNoticeTimer = 0;
    let playerFollowSmooth = null;
    let playerFollowPanPointerId = null;
    let playerFollowPanDragging = false;
    let playerFollowPanHoldMs = 0;
    let playerFollowPanStartX = 0;
    let playerFollowPanStartY = 0;
    let playerFollowSnapBackToken = 0;
    let playerFollowDragHintShown = false;
    let playerFollowGestureGuardUntil = 0;
    /** 离线定位时由前端注入的 BlueMap 玩家钉（在线后由 live 数据接管） */
    let playerFollowSyntheticMarker = false;
    let playerMarkerManagerPatched = false;
    /** 开启定位前所在维度/视角，退出定位时还原 */
    let playerFollowRestoreView = null;
    let playerFollowRestoreToken = 0;
    const AUTH_PLAYER_MARKER_CLASS = 'mcwws-auth-player-marker';
    const PLAYER_FOLLOW_BLOCK_MSG = '玩家定位已开启时无法使用 WASD 移动地图，请先关闭定位。';
    const PLAYER_FOLLOW_3D_START_MSG = '已切换到 2D 俯视并开始定位玩家。';
    /** 自动切 2D / 飞到玩家期间，忽略指针移动，避免误报「拖拽」提示 */
    const PLAYER_FOLLOW_GESTURE_GUARD_MS = 1600;
    const PLAYER_FOLLOW_3D_EXIT_MSG = '已切换到 3D 模式，玩家定位跟踪已关闭。（定位跟踪仅支持 2D 俯视）';
    const FREE_FLIGHT_TRANSITION_MS = 500;
    const FREE_FLIGHT_EXIT_MIN_DISTANCE = 100;
    const FREE_FLIGHT_ENTER_MSG = '已开启自由漫游：WASD 移动，空格/Shift 升降，按住右键环视。';
    const FREE_FLIGHT_EXIT_MSG = '已退出自由漫游。';
    const FREE_FLIGHT_FOLLOW_EXIT_MSG = '已开启自由漫游，玩家定位已关闭。';
    const PLAYER_FOLLOW_DRAG_HINT_MSG = '定位跟踪中：松手将回弹至玩家位置；持续拖拽约 2 秒可退出定位。';
    const PLAYER_FOLLOW_OFFLINE_LOCATE_MSG_LOGOUT = '玩家当前离线，已定位到上一次离开服务器时的位置。';
    const PLAYER_FOLLOW_OFFLINE_LOCATE_MSG_SAVED = '玩家当前离线，已定位到存档中的上次已知位置。';
    const MAP_DIMENSION_LABELS = {
        world: '主世界',
        world_nether: '下界',
        world_the_end: '末地',
        dimensionalhome: '维度家园'
    };
    const PLAYER_LOCATE_MAP_IDS = ['world', 'world_nether', 'world_the_end'];
    /** 不在维度菜单中展示的地图（仍可在 BlueMap 配置中存在） */
    const SHOP_HIDDEN_MAP_IDS = ['dimensionalhome'];

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

    const MAP_KEYBOARD_MOVE_KEYS = new Set([
        'w', 'a', 's', 'd', 'q', 'e',
        'arrowup', 'arrowdown', 'arrowleft', 'arrowright', ' '
    ]);
    const NON_TEXT_INPUT_TYPES = new Set([
        'checkbox', 'radio', 'button', 'submit', 'reset', 'file', 'hidden', 'image', 'range', 'color'
    ]);

    document.addEventListener('focusin', () => {
        if (isTextInputFocused()) {
            setMapKeyboardPaused(true);
        }
    }, true);
    document.addEventListener('focusout', () => {
        window.setTimeout(syncMapKeyboardPause, 0);
    }, true);
    document.addEventListener('keydown', stopMapKeyboardBubble, true);
    document.addEventListener('keyup', stopMapKeyboardBubble, true);

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
            if (isTextInputFocused()) {
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

    function isBrokenControlsWorldY(y) {
        const n = Number(y);
        return !Number.isFinite(n) || n < -500;
    }

    /** BlueMap 2D 每帧会改写 controls.position.y；hash / 跟随平滑态才是真实焦点坐标 */
    function resolveCapturedWorldPosition(cm, hash) {
        if (playerFollowActive && playerFollowSmooth) {
            const { x, y, z } = playerFollowSmooth;
            if ([x, y, z].every(Number.isFinite)) {
                return { x, y, z };
            }
        }
        const hx = Number(hash?.x);
        const hy = Number(hash?.y);
        const hz = Number(hash?.z);
        const hashOk = [hx, hy, hz].every(Number.isFinite);
        const cx = Number(cm?.position?.x);
        const cy = Number(cm?.position?.y);
        const cz = Number(cm?.position?.z);
        const controlsOk = [cx, cy, cz].every(Number.isFinite);
        const flat = getMapViewState() === 'flat';

        if (hashOk && (flat || isBrokenControlsWorldY(cy) || !controlsOk)) {
            return { x: hx, y: hy, z: hz };
        }
        if (controlsOk) {
            return {
                x: cx,
                y: isBrokenControlsWorldY(cy) && hashOk ? hy : cy,
                z: cz
            };
        }
        if (hashOk) {
            return { x: hx, y: hy, z: hz };
        }
        return null;
    }

    function captureViewFromControls() {
        const bm = getBlueMapApp();
        if (bm?.updatePageAddress) {
            bm.updatePageAddress();
        }
        syncPageAddressFromControls();
        const hash = parseHash();
        const cm = getControlsManager();
        const pos = resolveCapturedWorldPosition(cm, hash);
        if (!pos) {
            return hash ? { ...hash } : null;
        }
        const dist = clampMapDistance(
            Number.isFinite(cm?.distance) ? cm.distance : (hash?.distance ?? lastFlatHeight)
        );
        return {
            map: hash?.map || getCurrentMapId(),
            x: pos.x,
            y: pos.y,
            z: pos.z,
            distance: dist,
            height: dist,
            rotation: Number.isFinite(cm?.rotation) ? cm.rotation : (hash?.rotation ?? 0),
            angle: Number.isFinite(cm?.angle) ? cm.angle : (hash?.angle ?? 0),
            tilt: Number.isFinite(cm?.tilt) ? cm.tilt : (hash?.tilt ?? 0),
            ortho: Number.isFinite(cm?.ortho) ? cm.ortho : (hash?.ortho ?? 0),
            mode: getMapViewState()
        };
    }

    /** 保存完整视角快照 B（含 2D/3D 模式与俯仰） */
    function captureShopPinViewB() {
        const raw = captureViewFromControls();
        if (!raw) return null;
        const dist = clampMapDistance(raw.distance ?? raw.height);
        if (raw.mode === 'flat') {
            return normalizeViewForBlueMap({
                ...raw,
                mode: 'flat',
                distance: dist,
                height: dist,
                rotation: 0,
                angle: 0,
                tilt: 0,
                ortho: FLAT_ORTHO_ON
            });
        }
        const ang = Number(raw.angle);
        if (Number.isFinite(ang) && ang >= 0 && ang < 0.05) {
            return normalizeViewForBlueMap({
                ...raw,
                mode: 'perspective',
                distance: dist,
                height: dist,
                ortho: 0,
                angle: 0,
                tilt: 0,
                preserveVerticalDown: true
            });
        }
        return normalizeViewForBlueMap({
            ...raw,
            mode: 'perspective',
            distance: dist,
            height: dist,
            ortho: 0
        });
    }

    /** 商店预设三维视角 A（管理页 viewUrl / 锚点） */
    function getMarkerPresetViewA(marker) {
        if (!marker) return null;
        const savedHash = getMarkerSavedViewHash(marker);
        if (savedHash) {
            const parts = savedHash.split(':');
            const view = parseHashParts(parts);
            if (view) {
                return normalizeViewForBlueMap({
                    ...view,
                    mode: 'perspective',
                    ortho: 0
                });
            }
        }
        const target = parseViewUrl(marker.viewUrl);
        if (target) {
            return normalizeViewForBlueMap({
                ...target,
                mode: 'perspective',
                ortho: 0
            });
        }
        if (!marker.position || !marker.map) return null;
        const dist = clampMapDistance(lastFlatHeight);
        return normalizeViewForBlueMap({
            map: marker.map,
            x: Number(marker.position.x) + 0.5,
            y: Number(marker.position.y),
            z: Number(marker.position.z) + 0.5,
            distance: dist,
            height: dist,
            mode: 'perspective',
            ortho: 0,
            rotation: 0,
            angle: 0,
            tilt: 0
        });
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

    function restoreShopPinViewB() {
        if (!preShopPinView) return;
        shopPinAtViewA = false;
        selectedMarkerTopDown = false;
        selectedMarkerId = null;
        void applyBlueMapView({ ...preShopPinView }, {
            useExactView: true,
            restoreTransition: true
        });
    }

    function flyToShopPinViewA(marker) {
        selectedMarkerId = marker.id;
        selectedMarkerTopDown = true;
        const viewA = getMarkerPresetViewA(marker);
        if (viewA) {
            void applyBlueMapView(viewA, { keepControlsOrientation: false });
            return;
        }
        openMarkerTopDown(marker);
    }

    function handlePinClick(marker) {
        if (!marker) return;
        if (playerFollowActive) {
            stopPlayerFollow();
        }
        if (shopPinAtViewA && preShopPinView) {
            if (selectedMarkerId === marker.id) {
                restoreShopPinViewB();
                return;
            }
            flyToShopPinViewA(marker);
            return;
        }
        preShopPinView = captureShopPinViewB();
        shopPinAtViewA = true;
        flyToShopPinViewA(marker);
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

    function getPlayerLocateMapIds() {
        const ids = new Set(
            PLAYER_LOCATE_MAP_IDS
                .filter((id) => !isShopHiddenMap(id))
                .map(normalizePlayerMapId)
        );
        getAvailableMaps().forEach((entry) => {
            if (entry?.id) ids.add(normalizePlayerMapId(entry.id));
        });
        const current = getCurrentMapId();
        if (current && !isShopHiddenMap(current)) {
            ids.add(normalizePlayerMapId(current));
        }
        return [...ids];
    }

    function resolveBlueMapId(mapId, normalizeDimension = false) {
        const raw = String(mapId || '').trim();
        if (!raw) return '';
        return normalizeDimension ? normalizePlayerMapId(raw) : raw;
    }

    /** BlueMap 官方 API 为 switchMap(mapId, resetCamera)，无 setMap */
    async function blueMapSwitchMap(bm, mapId, resetCamera = false) {
        const app = bm || getBlueMapApp();
        const target = String(mapId || '').trim();
        if (!app || !target || typeof app.switchMap !== 'function') {
            return false;
        }
        const current = String(app.mapViewer?.data?.map?.id || '').trim();
        if (current === target) {
            return true;
        }
        try {
            await app.switchMap(target, !!resetCamera);
            await waitForBlueMapMapLoaded(app, target, 12000, false);
            return String(app.mapViewer?.data?.map?.id || '').trim() === target;
        } catch (err) {
            console.warn('[mcwws-shops] blueMapSwitchMap failed', target, err);
            return false;
        }
    }

    function waitForBlueMapMapLoaded(bm, mapId, maxMs = 12000, normalizeDimension = false) {
        const app = bm || getBlueMapApp();
        const target = resolveBlueMapId(mapId, normalizeDimension);
        if (!app?.mapViewer?.data || !target) {
            return Promise.resolve(false);
        }
        const start = performance.now();
        return new Promise((resolve) => {
            const step = () => {
                const id = String(app.mapViewer?.data?.map?.id || '').trim();
                const state = app.mapViewer?.data?.mapState;
                if (id === target && state === 'loaded') {
                    resolve(true);
                    return;
                }
                if (performance.now() - start > maxMs) {
                    resolve(id === target);
                    return;
                }
                requestAnimationFrame(step);
            };
            step();
        });
    }

    /** 仅玩家定位：归一化维度 ID、先写 hash 再 switchMap */
    async function switchBlueMapForPlayerFollow(mapId, bm, viewHint = null) {
        const app = bm || getBlueMapApp();
        const target = resolveBlueMapId(mapId, true);
        if (!app || !target) return false;
        const hashBase = viewHint || parseHash() || {
            map: target,
            x: 0,
            y: 64,
            z: 0,
            distance: lastFlatHeight,
            height: lastFlatHeight,
            rotation: 0,
            angle: 0,
            tilt: 0,
            ortho: FLAT_ORTHO_ON,
            mode: getMapViewState()
        };
        replaceLocationHash(formatViewHash({ ...hashBase, map: target }));
        const ok = await blueMapSwitchMap(app, target, false);
        if (!ok) return false;
        await waitForBlueMapViewAnimationAsync(app);
        return String(app.mapViewer?.data?.map?.id || '').trim() === target;
    }

    /** 通用视角切换：使用 BlueMap switchMap，保留当前相机 */
    async function ensureMapForView(view, bm) {
        if (!view?.map || !bm) return false;
        const target = String(view.map).trim();
        if (!target) return false;
        const current = String(bm.mapViewer?.data?.map?.id || parseHash()?.map || '').trim();
        if (current === target) {
            return true;
        }
        const ok = await blueMapSwitchMap(bm, target, false);
        if (ok) {
            await waitForBlueMapViewAnimationAsync(bm);
        }
        return ok;
    }

    /** BlueMap 在 setFlatView/setPerspectiveView 结束时会 mapControls.reset() 并挂回 controls */
    function callBlueMapSetFlatView(bm, durationMs, minDistance = FLAT_VIEW_MIN_DISTANCE) {
        if (typeof bm?.setFlatView === 'function') {
            bm.setFlatView(durationMs, minDistance);
        }
    }

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

    async function animateControlsToView(view, durationMs, options = {}) {
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
            + Math.abs(end.angle - start.angle) * 40
            + Math.abs(end.rotation - start.rotation) * 40;
        const skipOrientationLerp = !!options.skipOrientationLerp;
        if (skipOrientationLerp) {
            cm.rotation = end.rotation;
            cm.angle = end.angle;
            cm.tilt = end.tilt;
            cm.ortho = end.ortho;
        }
        if (!options.force && delta < 0.5) {
            applyControlsFromView(view);
            if (options.syncFollowSmooth && playerFollowSmooth) {
                syncPlayerFollowSmoothFromControls(cm);
            }
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
                if (skipOrientationLerp) {
                    cm.rotation = end.rotation;
                    cm.angle = end.angle;
                    cm.tilt = end.tilt;
                    cm.ortho = end.ortho;
                } else {
                    cm.rotation = lerpNumber(start.rotation, end.rotation, e);
                    cm.angle = lerpNumber(start.angle, end.angle, e);
                    cm.tilt = lerpNumber(start.tilt, end.tilt, e);
                    cm.ortho = lerpNumber(start.ortho, end.ortho, e);
                }
                if (options.syncFollowSmooth && playerFollowSmooth) {
                    playerFollowSmooth.x = cm.position.x;
                    playerFollowSmooth.y = cm.position.y;
                    playerFollowSmooth.z = cm.position.z;
                }
                if (options.refreshCameraEachFrame) {
                    triggerControlsCameraUpdate(cm);
                }
                frame += 1;
                if (frame % 2 === 0) {
                    updatePinPositions();
                }
                if (t < 1) {
                    requestAnimationFrame(step);
                    return;
                }
                applyControlsFromView(view);
                if (options.syncFollowSmooth && playerFollowSmooth) {
                    syncPlayerFollowSmoothFromControls(cm);
                }
                if (options.refreshCameraEachFrame) {
                    triggerControlsCameraUpdate(cm);
                }
                resolve();
            }
            requestAnimationFrame(step);
        });
    }

    async function applyBlueMapView(view, options = {}) {
        if (!view) return false;
        const bm = getBlueMapApp();
        const fromState = getMapViewState();
        const toState = view.mode || 'perspective';
        const flatToPerspective = fromState === 'flat' && toState === 'perspective';
        const perspectiveToFlat = fromState !== 'flat' && toState === 'flat';
        let v;
        if (options.useExactView && view) {
            const dist = clampMapDistance(view.distance ?? view.height);
            v = normalizeViewForBlueMap({
                ...view,
                distance: dist,
                height: dist
            }) || {
                ...view,
                distance: dist,
                height: dist
            };
        } else if (options.keepControlsOrientation && flatToPerspective) {
            v = buildTopDownPerspectiveView(mergeViewWithCurrentControls(view));
        } else if (perspectiveToFlat) {
            v = buildFlatViewFromCurrentControls(view);
        } else {
            v = normalizeViewForBlueMap(view);
        }
        if (!v) return false;
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

        if (!options.skipMapEnsure) {
            await ensureMapForView(v, bm);
        }

        const restoreView = !!options.restoreTransition;
        const restoreToFlat = restoreView && perspectiveToFlat;
        const restorePerspective = restoreView && fromState === 'perspective' && toState === 'perspective';
        const restoreFlyMs = restoreView
            ? Math.max(modeMs, paramMs, VIEW_RESTORE_TRANSITION_MS)
            : paramMs;
        const restoreFlyOpts = restoreView
            ? { force: true, refreshCameraEachFrame: true, skipOrientationLerp: restoreToFlat }
            : undefined;
        const followStart = !!options.followStart;
        const followFlyMs = followStart
            ? Math.max(paramMs, modeMs, VIEW_RESTORE_TRANSITION_MS)
            : paramMs;
        const followFlyOpts = followStart
            ? { force: true, refreshCameraEachFrame: true, skipOrientationLerp: true }
            : undefined;

        const animWaitFrames = Number.isFinite(options.animWaitFrames)
            ? options.animWaitFrames
            : ((options.fast || modeMs <= 0) ? 8 : 150);

        if (fromState !== toState && modeMs >= 0) {
            if (toState === 'flat' && typeof bm.setFlatView === 'function') {
                // minDistance 只用下限 5；传 B 的 distance 会触发 max(当前,B) 把镜头拉过头
                const flatModeMs = followStart ? Math.max(modeMs, VIEW_MODE_TRANSITION_MS) : modeMs;
                callBlueMapSetFlatView(bm, flatModeMs, FLAT_VIEW_MIN_DISTANCE);
                await waitForBlueMapViewAnimationAsync(bm, animWaitFrames);
                if (options.fast || paramMs <= 0) {
                    applyControlsFromView(v);
                } else {
                    const flyMs = followStart ? followFlyMs : (restoreToFlat ? restoreFlyMs : paramMs);
                    const flyOpts = followStart ? followFlyOpts : restoreFlyOpts;
                    await animateControlsToView(v, flyMs, flyOpts);
                }
            } else if (toState === 'perspective' && typeof bm.setPerspectiveView === 'function') {
                bm.setPerspectiveView(modeMs, 0);
                await waitForBlueMapViewAnimationAsync(bm, animWaitFrames);
                if (options.fast || paramMs <= 0) {
                    applyControlsFromView(v);
                } else {
                    await animateControlsToView(v, paramMs, {
                        force: true,
                        refreshCameraEachFrame: true
                    });
                }
            } else {
                await animateControlsToView(v, paramMs);
            }
        } else if (restorePerspective) {
            await animateControlsToView(v, restoreFlyMs, restoreFlyOpts);
        } else if (options.fast || paramMs <= 0) {
            applyControlsFromView(v);
        } else {
            await animateControlsToView(
                v,
                followStart ? followFlyMs : paramMs,
                followStart ? followFlyOpts : undefined
            );
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

    function refreshBlueMapCameraMatrices(camera) {
        if (!camera) return null;
        if (typeof camera.updateMatrixWorld === 'function') {
            camera.updateMatrixWorld(true);
        }
        if (typeof camera.updateProjectionMatrix === 'function') {
            camera.updateProjectionMatrix();
        }
        return camera;
    }

    function getBlueMapCamera() {
        const bluemap = getBlueMapApp();
        const cm = getControlsManager();
        if (playerFollowActive && cm && typeof cm.updateCamera === 'function') {
            cm.updateCamera();
            if (!cachedCamera?.projectionMatrix) {
                cachedCamera = findCamera(bluemap);
            }
            return refreshBlueMapCameraMatrices(cachedCamera);
        }
        if (cachedCamera?.projectionMatrix && cachedCamera?.matrixWorldInverse) {
            return cachedCamera;
        }
        cachedCamera = findCamera(bluemap);
        return refreshBlueMapCameraMatrices(cachedCamera);
    }

    /** 跟随玩家时 URL hash 滞后于 controls，钉点须用当前相机状态投影 */
    function getViewForPinProjection() {
        if (!playerFollowActive) {
            return parseHash();
        }
        const cm = getControlsManager();
        const hash = parseHash();
        if (!cm) return hash;
        const dist = clampMapDistance(cm.distance ?? hash?.distance ?? lastFlatHeight);
        return {
            map: getCurrentMapId(),
            x: cm.position.x,
            y: cm.position.y,
            z: cm.position.z,
            distance: dist,
            height: dist,
            rotation: cm.rotation ?? 0,
            angle: cm.angle ?? 0,
            pitch: cm.angle ?? 0,
            yaw: cm.rotation ?? 0,
            tilt: cm.tilt ?? 0,
            ortho: cm.ortho ?? 0,
            mode: getMapViewState()
        };
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
            callBlueMapSetFlatView(bluemap, 500, FLAT_VIEW_MIN_DISTANCE);
        } else if (mode === 'perspective' && typeof bluemap.setPerspectiveView === 'function') {
            bluemap.setPerspectiveView(500, 0);
        }
    }

    function getControlsManager() {
        const bluemap = getBlueMapApp();
        return bluemap?.mapViewer?.controlsManager || null;
    }

    let mapKeyboardPaused = false;

    function isTextInputFocused() {
        const active = document.activeElement;
        if (!active || active === document.body || active === document.documentElement) {
            return false;
        }
        if (active.isContentEditable && active.getAttribute('contenteditable') !== 'false') {
            return true;
        }
        const tag = active.tagName;
        if (tag === 'TEXTAREA') {
            return !active.disabled;
        }
        if (tag !== 'INPUT') {
            return false;
        }
        if (active.disabled) {
            return false;
        }
        const type = String(active.type || 'text').toLowerCase();
        return !NON_TEXT_INPUT_TYPES.has(type);
    }

    window.mcwwsIsTextInputFocused = isTextInputFocused;

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
        setMapKeyboardPaused(isTextInputFocused());
    }

    function stopMapKeyboardBubble(e) {
        if (!isTextInputFocused()) {
            return;
        }
        const key = String(e.key || '').toLowerCase();
        if (MAP_KEYBOARD_MOVE_KEYS.has(key)) {
            e.preventDefault();
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

    /** 2D→3D 时保留当前相机方位/位置/俯仰，避免 hash 里 rotation=0、angle=0 导致歪斜再回弹 */
    function mergeViewWithCurrentControls(view) {
        const cm = getControlsManager();
        if (!cm || !view) {
            return view;
        }
        const merged = { ...view };
        if (Number.isFinite(cm.rotation)) {
            merged.rotation = cm.rotation;
            merged.yaw = cm.rotation;
        }
        if (Number.isFinite(cm.angle)) {
            merged.angle = cm.angle;
            merged.pitch = cm.angle;
        }
        if (Number.isFinite(cm.tilt)) {
            merged.tilt = cm.tilt;
            merged.roll = cm.tilt;
        }
        if (Number.isFinite(cm.ortho)) {
            merged.ortho = cm.ortho;
            merged.fov = cm.ortho;
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

    /** 2D 切 3D：与 2D 一致保持竖直向下（angle=0），不用斜视默认 -0.75 或轻微透视角 */
    function buildTopDownPerspectiveView(view) {
        const cm = getControlsManager();
        const dist = clampMapDistance(view?.distance ?? view?.height ?? cm?.distance);
        return normalizeViewForBlueMap({
            ...view,
            mode: 'perspective',
            ortho: 0,
            distance: dist,
            height: dist,
            rotation: Number.isFinite(view?.rotation) ? view.rotation : 0,
            angle: 0,
            tilt: 0,
            preserveVerticalDown: true
        });
    }

    /** 3D 切 2D：保留当前相机焦点（x/y/z）与缩放，仅切回正交俯视 */
    function buildFlatViewFromCurrentControls(view) {
        const cm = getControlsManager();
        const base = view || parseHash() || {};
        const merged = mergeViewWithCurrentControls({
            ...base,
            map: base.map || getCurrentMapId()
        });
        const dist = clampMapDistance(
            Number.isFinite(cm?.distance) ? cm.distance : (merged.distance ?? lastFlatHeight)
        );
        return normalizeViewForBlueMap({
            ...merged,
            mode: 'flat',
            distance: dist,
            height: dist,
            rotation: 0,
            angle: 0,
            tilt: 0,
            ortho: FLAT_ORTHO_ON
        });
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

    function migrateLayerMenuDom(root) {
        if (!root) return;
        const layerBtn = root.querySelector('.mcwws-ctrl-layer');
        let menu = root.querySelector('.mcwws-layer-menu');
        if (!layerBtn || !menu) return;

        if (layerBtn.title === '图层选择') {
            layerBtn.title = '维度选择';
        }
        const label = layerBtn.querySelector('.mcwws-ctrl-layer-text');
        if (label && label.textContent.trim() === '图层') {
            const icon = label.querySelector('.mcwws-ctrl-layer-icon');
            label.textContent = '';
            if (icon) label.appendChild(icon);
            label.append(document.createTextNode('维度'));
        }

        let wrap = layerBtn.closest('.mcwws-ctrl-layer-wrap');
        if (!wrap) {
            wrap = document.createElement('div');
            wrap.className = 'mcwws-ctrl-layer-wrap';
            const dimCol = layerBtn.closest('.mcwws-ctrl-dimension-column');
            const bottomRow = layerBtn.closest('.mcwws-ctrl-bottom-row');
            if (dimCol) {
                dimCol.insertBefore(wrap, layerBtn);
            } else if (bottomRow) {
                bottomRow.insertBefore(wrap, layerBtn);
            } else {
                root.insertBefore(wrap, root.firstChild);
            }
            wrap.appendChild(layerBtn);
        }
        if (menu.parentElement !== wrap) {
            wrap.appendChild(menu);
        }
    }

    /** 旧版纵向堆叠 → 左侧维度列 + 右侧工具条（指南针/2D/缩放/日夜/定位/全屏） */
    function migrateMapControlsLayout(root) {
        if (!root) return;
        const stack = root.querySelector('.mcwws-map-controls-stack');
        if (!stack || stack.querySelector('.mcwws-ctrl-main-row')) {
            return;
        }

        const mainRow = document.createElement('div');
        mainRow.className = 'mcwws-ctrl-main-row';

        const cluster = document.createElement('div');
        cluster.className = 'mcwws-ctrl-tools-cluster';

        [
            '.mcwws-ctrl-compass',
            '.mcwws-ctrl-mode',
            '.mcwws-ctrl-zoom',
            '.mcwws-ctrl-daynight',
            '.mcwws-ctrl-locate',
            '.mcwws-ctrl-fly'
        ].forEach((sel) => {
            const el = stack.querySelector(sel);
            if (el) {
                cluster.appendChild(el);
            }
        });

        const bottomRow = stack.querySelector('.mcwws-ctrl-bottom-row');
        const fs = bottomRow?.querySelector('.mcwws-ctrl-fullscreen')
            || stack.querySelector('.mcwws-ctrl-fullscreen');
        if (fs) {
            cluster.appendChild(fs);
        }

        let dimCol = bottomRow?.querySelector('.mcwws-ctrl-dimension-column')
            || stack.querySelector('.mcwws-ctrl-dimension-column');
        if (!dimCol) {
            dimCol = document.createElement('div');
            dimCol.className = 'mcwws-ctrl-dimension-column';
            const layerWrap = bottomRow?.querySelector('.mcwws-ctrl-layer-wrap')
                || stack.querySelector('.mcwws-ctrl-layer-wrap');
            if (layerWrap) {
                dimCol.appendChild(layerWrap);
            } else if (root.querySelector('.mcwws-ctrl-layer')) {
                migrateLayerMenuDom(root);
                const wrap = root.querySelector('.mcwws-ctrl-layer-wrap');
                if (wrap) {
                    dimCol.appendChild(wrap);
                }
            }
        }

        const gisWrap = document.getElementById('mcwws-gis-wrap');
        if (gisWrap && gisWrap.parentElement !== dimCol) {
            dimCol.insertBefore(gisWrap, dimCol.firstChild);
        }

        mainRow.appendChild(dimCol);
        mainRow.appendChild(cluster);

        while (stack.firstChild) {
            stack.removeChild(stack.firstChild);
        }
        stack.appendChild(mainRow);
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
        v.tilt = 0;
        v.ortho = 0;
        if (v.preserveVerticalDown) {
            v.angle = 0;
            delete v.preserveVerticalDown;
            return v;
        }
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
            callBlueMapSetFlatView(bm, VIEW_MODE_TRANSITION_MS, FLAT_VIEW_MIN_DISTANCE);
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
            callBlueMapSetFlatView(bm, 500, savedDistance);
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

    function isPlayerFollowAllowedMode() {
        return getMapViewState() === 'flat';
    }

    function isFreeFlightViewEnabled() {
        const map = getBlueMapApp()?.mapViewer?.data?.map;
        return !!map?.freeFlightView;
    }

    function createFreeFlightControlButton() {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'mcwws-ctrl-btn mcwws-ctrl-fly';
        btn.title = '自由漫游：WASD 移动，空格/Shift 升降，右键环视';
        btn.innerHTML = `
            <svg class="mcwws-ctrl-fly-icon" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                <path fill="currentColor" d="M19.2 8.4c-.2-.4-.7-.7-1.2-.7h-1.6c-.4 0-.6-.3-.3-.6 0 0 .6-.7.6-1.6 0-1.7-1.4-3-3-3s-3 1.3-3 3c0 .9.6 1.6.6 1.6.3.3.1.6-.3.6l-1.6 0c-.4 0-.9.3-1.1.7l-.7.9c-.3.3-.8.4-1.2.2l-1.5-1c-.4-.2-.5-.8-.3-1.2l3.8-2.4c.3-.2.7-.2 1 0l3.8 2.4c.2.4.1 1-.3 1.2l-1.5 1c-.4.2-.9.1-1.2-.2l-.7-.9z"/>
            </svg>
        `;
        return btn;
    }

    function bindFreeFlightControl(root) {
        const btn = root?.querySelector('.mcwws-ctrl-fly');
        if (!btn || btn.dataset.bound === '1') {
            return;
        }
        btn.dataset.bound = '1';
        btn.addEventListener('click', () => {
            toggleFreeFlightView();
        });
    }

    /** 旧版控件条缺少自由漫游按钮时，插入到定位与全屏之间 */
    function migrateFreeFlightButton(root) {
        const cluster = root?.querySelector('.mcwws-ctrl-tools-cluster');
        if (!cluster || cluster.querySelector('.mcwws-ctrl-fly')) {
            return;
        }
        const fs = cluster.querySelector('.mcwws-ctrl-fullscreen');
        const btn = createFreeFlightControlButton();
        if (fs) {
            cluster.insertBefore(btn, fs);
        } else {
            const locate = cluster.querySelector('.mcwws-ctrl-locate');
            if (locate) {
                locate.insertAdjacentElement('afterend', btn);
            } else {
                cluster.appendChild(btn);
            }
        }
        bindFreeFlightControl(root);
    }

    function toggleFreeFlightView() {
        const bm = getBlueMapApp();
        if (!bm || !isFreeFlightViewEnabled()) {
            return;
        }
        const state = getMapViewState();
        if (state === 'free') {
            if (typeof bm.setPerspectiveView === 'function') {
                bm.setPerspectiveView(FREE_FLIGHT_TRANSITION_MS, FREE_FLIGHT_EXIT_MIN_DISTANCE);
            } else {
                applyControlsViewFallback({ mode: 'perspective' });
            }
            showPlayerFollowNotice(FREE_FLIGHT_EXIT_MSG, 2800);
            updatePinPositions();
            updateMapControlsState();
            return;
        }
        if (playerFollowActive) {
            stopPlayerFollow();
            showPlayerFollowNotice(FREE_FLIGHT_FOLLOW_EXIT_MSG, 3600);
        }
        if (typeof bm.setFreeFlight !== 'function') {
            return;
        }
        bm.setFreeFlight(FREE_FLIGHT_TRANSITION_MS);
        showPlayerFollowNotice(FREE_FLIGHT_ENTER_MSG, 4500);
        updatePinPositions();
        updateMapControlsState();
    }

    function stopPlayerFollowFor3DMode() {
        if (!playerFollowActive) return;
        stopPlayerFollow();
        showPlayerFollowNotice(PLAYER_FOLLOW_3D_EXIT_MSG);
    }

    function toggleMapViewMode() {
        const state = getMapViewState();
        const view = parseHash();
        const uiToggle = {
            modeDuration: VIEW_MODE_UI_TOGGLE_MS,
            paramDuration: VIEW_MODE_UI_PARAM_MS,
            animWaitFrames: VIEW_MODE_UI_ANIM_WAIT_FRAMES
        };
        if (state === 'flat') {
            if (playerFollowActive) {
                stopPlayerFollow();
                showPlayerFollowNotice(PLAYER_FOLLOW_3D_EXIT_MSG);
            }
            if (view) {
                void applyBlueMapView(
                    { ...view, mode: 'perspective', ortho: 0 },
                    { keepControlsOrientation: true, ...uiToggle }
                );
            } else {
                const bm = getBlueMapApp();
                bm?.setPerspectiveView?.(0, 0);
                updateMapControlsState();
            }
            return;
        }
        rememberFlatZoom();
        void applyBlueMapView({
            ...(view || {}),
            map: getCurrentMapId(),
            mode: 'flat'
        }, uiToggle);
    }

    function isShopHiddenMap(mapId) {
        const id = String(mapId || '').trim().toLowerCase();
        return SHOP_HIDDEN_MAP_IDS.some((hidden) => hidden.toLowerCase() === id);
    }

    function getAvailableMaps() {
        const bm = getBlueMapApp();
        const list = bm?.appState?.maps;
        let maps;
        if (Array.isArray(list) && list.length) {
            maps = list.map((entry) => {
                if (typeof entry === 'string') {
                    return { id: entry, name: entry };
                }
                const id = entry?.id || entry?.mapId || entry?.map?.id;
                const name = entry?.name || entry?.map?.name || entry?.label || id;
                if (!id) return null;
                return { id: String(id), name: String(name || id) };
            }).filter(Boolean);
        } else {
            const ids = new Set();
            markers.forEach((marker) => {
                if (marker.map) ids.add(marker.map);
            });
            const current = parseHash()?.map;
            if (current) ids.add(current);
            if (!ids.size) ids.add('world');
            maps = [...ids].map((id) => ({ id, name: id }));
        }
        return maps.filter((entry) => !isShopHiddenMap(entry.id));
    }

    function stopPlayerFollowForLayerSwitch(targetMapId) {
        if (!playerFollowActive) return;
        const followMap = playerFollowMapId || playerFollowTarget?.map;
        const target = String(targetMapId || '').trim();
        if (!followMap || !target || mapsMatchForFollow(followMap, target)) {
            return;
        }
        playerFollowRestoreView = null;
        stopPlayerFollow({ skipRestore: true });
        showPlayerFollowNotice(
            `已切换到${formatMapDimensionLabel(target)}，玩家定位已关闭。`,
            4200
        );
    }

    function switchMapLayer(mapId) {
        if (!mapId || isShopHiddenMap(mapId)) return;
        const target = String(mapId).trim();
        stopPlayerFollowForLayerSwitch(target);
        const view = parseHash();
        const nextView = view ? { ...view, map: target } : {
            map: target,
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
        };
        replaceLocationHash(formatViewHash(nextView));
        layerMenuOpen = false;
        renderLayerMenu();
        renderPanel();
        syncPinElements();
        void (async () => {
            const bm = getBlueMapApp();
            const ok = await blueMapSwitchMap(bm, target, false);
            if (!ok && bm) {
                await loadBlueMapPageAddress();
            }
            cachedCamera = null;
            lastPanelMap = target;
            updatePinPositions();
            updateMapControlsState();
            renderPanel();
            syncPinElements();
            if (layerMenuOpen) renderLayerMenu();
        })();
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
            cachedPlayerLoc = null;
            cachedLivePlayers = null;
            playerFollowTarget = null;
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

    function formatMapDimensionLabel(mapId) {
        const id = String(mapId || '').trim();
        if (!id) return '未知维度';
        return MAP_DIMENSION_LABELS[id] || id;
    }

    function normalizePlayerMapId(mapId) {
        const id = String(mapId || '').trim();
        if (!id) return getCurrentMapId();
        const lower = id.toLowerCase();
        if (lower === 'overworld' || lower === 'minecraft:overworld') return 'world';
        if (lower === 'the_nether' || lower === 'minecraft:the_nether' || lower === 'nether') return 'world_nether';
        if (lower === 'the_end' || lower === 'minecraft:the_end' || lower === 'end') return 'world_the_end';
        if (lower === 'dim-1' || lower === 'dim_1') return 'world_nether';
        if (lower === 'dim1') return 'world_the_end';
        return id;
    }

    function mapsMatchForFollow(a, b) {
        return normalizePlayerMapId(a) === normalizePlayerMapId(b);
    }

    function playerMarkerMatchesName(marker, playerId) {
        const target = normalizePlayerKey(playerId);
        const name = normalizePlayerKey(marker?.data?.name);
        return !!target && name === target;
    }

    function coordsFromLivePayload(mapId, raw, source) {
        const x = Number(raw?.x);
        const y = Number(raw?.y);
        const z = Number(raw?.z);
        if (![x, y, z].every(Number.isFinite)) return null;
        const atOrigin = Math.abs(x) < 0.01 && Math.abs(z) < 0.01 && Math.abs(y) < 1;
        if (atOrigin) return null;
        return {
            map: normalizePlayerMapId(mapId || getCurrentMapId()),
            x,
            y: y + PLAYER_HEAD_OFFSET,
            z,
            online: true,
            source: source || 'live',
            uuid: raw?.uuid || null
        };
    }

    function getLivePlayersCacheMs() {
        return playerFollowActive ? PLAYER_FOLLOW_LIVE_CACHE_MS : LIVE_PLAYERS_CACHE_MS;
    }

    function invalidatePlayerFollowCaches() {
        cachedLivePlayersAt = 0;
        cachedPlayerLocAt = 0;
    }

    async function fetchLivePlayersJson(mapId, options = {}) {
        const id = mapId || getCurrentMapId();
        const cacheMs = getLivePlayersCacheMs();
        if (
            !options.bypassCache
            && cachedLivePlayers
            && cachedLivePlayersMap === id
            && Date.now() - cachedLivePlayersAt < cacheMs
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

    async function fetchLivePlayerFromMap(playerId, mapId, options = {}) {
        const target = normalizePlayerKey(playerId);
        if (!target) return null;
        const normalizedMap = normalizePlayerMapId(mapId);
        const players = await fetchLivePlayersJson(normalizedMap, options);
        if (!players?.length) return null;
        const entry = players.find((p) => normalizePlayerKey(p?.name) === target);
        if (!entry?.position) return null;
        const fromJson = coordsFromLivePayload(normalizedMap, {
            ...entry.position,
            uuid: entry.uuid
        }, 'live-json');
        if (!fromJson) return null;
        fromJson.uuid = entry.uuid || fromJson.uuid;
        return fromJson;
    }

    /** 遍历所有 BlueMap 地图的 live/players.json，避免只看当前维度 */
    async function fetchLivePlayerFromAllMaps(playerId, options = {}) {
        const target = normalizePlayerKey(playerId);
        if (!target) return null;
        for (const mapId of getPlayerLocateMapIds()) {
            const hit = await fetchLivePlayerFromMap(playerId, mapId, options);
            if (hit) return hit;
        }
        return null;
    }

    async function fetchPlayerLocationFromApi(options = {}) {
        if (!mapAuthToken) return null;
        const cacheMs = playerFollowActive ? PLAYER_FOLLOW_LIVE_CACHE_MS : PLAYER_LOCATE_POLL_MS;
        if (
            !options.bypassCache
            && cachedPlayerLoc
            && Date.now() - cachedPlayerLocAt < cacheMs
        ) {
            return cachedPlayerLoc;
        }
        try {
            const res = await fetch(`${NODE_API}/api/player-location?t=${Date.now()}`, {
                headers: authHeaders(),
                cache: 'no-store'
            });
            if (!res.ok) return null;
            const data = await res.json();
            const online = !!data.online;
            const yOff = online ? PLAYER_HEAD_OFFSET : PLAYER_EYE_OFFSET;
            cachedPlayerLoc = {
                map: normalizePlayerMapId(data.map || 'world'),
                x: Number(data.x),
                y: Number(data.y) + yOff,
                z: Number(data.z),
                online,
                source: data.source || (online ? 'live' : 'saved'),
                uuid: data.uuid || null
            };
            cachedPlayerLocAt = Date.now();
            return cachedPlayerLoc;
        } catch {
            return null;
        }
    }

    async function resolvePlayerPosition(options = {}) {
        if (!mapAuthUser?.playerId) return null;
        const fetchOpts = { bypassCache: !!options.forceFresh };

        if (mapAuthToken) {
            const api = await fetchPlayerLocationFromApi(fetchOpts);
            if (api && [api.x, api.y, api.z].every(Number.isFinite)) {
                playerFollowSource = api.online ? (api.source || 'live') : (api.source || 'saved');
                return { ...api, map: normalizePlayerMapId(api.map) };
            }
        }

        const live = await fetchLivePlayerFromAllMaps(mapAuthUser.playerId, fetchOpts);
        if (live) {
            playerFollowSource = live.source || 'live';
            return live;
        }
        return null;
    }

    function initPlayerFollowSmoothFromControls(cm) {
        const controls = cm || getControlsManager();
        if (!controls) return;
        playerFollowSmooth = {
            x: Number(controls.position.x),
            y: Number(controls.position.y),
            z: Number(controls.position.z)
        };
    }

    function syncPlayerFollowSmoothFromControls(cm) {
        const controls = cm || getControlsManager();
        if (!controls || !playerFollowSmooth) return;
        playerFollowSmooth.x = Number(controls.position.x);
        playerFollowSmooth.y = Number(controls.position.y);
        playerFollowSmooth.z = Number(controls.position.z);
    }

    function restorePlayerFollowSmoothedPosition(cm) {
        const controls = cm || getControlsManager();
        if (!playerFollowActive || !controls || !playerFollowSmooth) return;
        if (playerFollowPanDragging) return;
        if ([playerFollowSmooth.x, playerFollowSmooth.y, playerFollowSmooth.z].every(Number.isFinite)) {
            controls.position.x = playerFollowSmooth.x;
            controls.position.y = playerFollowSmooth.y;
            controls.position.z = playerFollowSmooth.z;
        }
    }

    function triggerControlsCameraUpdate(cm) {
        const controls = cm || getControlsManager();
        if (playerFollowActive) {
            restorePlayerFollowSmoothedPosition(controls);
        }
        if (controls && typeof controls.updateCamera === 'function') {
            controls.updateCamera();
        }
        if (playerFollowActive) {
            refreshBlueMapCameraMatrices(cachedCamera || findCamera(getBlueMapApp()));
        }
        getBlueMapApp()?.mapViewer?.redraw?.();
    }

    function buildFollowViewFromPosition(pos) {
        const hash = parseHash();
        const cm = getControlsManager();
        const mode = getMapViewState();
        const dist = clampMapDistance(cm?.distance ?? hash?.distance ?? lastFlatHeight);
        const view = {
            map: normalizePlayerMapId(pos.map || hash?.map || 'world'),
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

    function setPlayerFollowMapHeightBypass(enabled) {
        const mapHeight = getBlueMapApp()?.mapControls?.mapHeight;
        if (!mapHeight || typeof mapHeight.update !== 'function') {
            return;
        }
        if (enabled) {
            if (!mapHeightUpdateSaved) {
                mapHeightUpdateSaved = mapHeight.update.bind(mapHeight);
                mapHeight.update = function playerFollowMapHeightUpdate(dt, terrain) {
                    mapHeightUpdateSaved.call(this, dt, terrain);
                    if (playerFollowActive && !playerFollowPanDragging) {
                        restorePlayerFollowSmoothedPosition(this.manager);
                    }
                };
            }
            return;
        }
        if (mapHeightUpdateSaved) {
            mapHeight.update = mapHeightUpdateSaved;
            mapHeightUpdateSaved = null;
        }
    }

    function ensurePlayerFollowNotice() {
        let el = document.getElementById('mcwws-player-follow-notice');
        if (!el) {
            el = document.createElement('div');
            el.id = 'mcwws-player-follow-notice';
            el.className = 'mcwws-player-follow-notice';
            el.setAttribute('role', 'status');
            el.setAttribute('aria-live', 'polite');
            document.body.appendChild(el);
        }
        return el;
    }

    function showPlayerFollowNotice(text, durationMs = 3200) {
        const el = ensurePlayerFollowNotice();
        el.textContent = String(text || '');
        el.classList.add('is-visible');
        window.clearTimeout(playerFollowNoticeTimer);
        playerFollowNoticeTimer = window.setTimeout(() => {
            el.classList.remove('is-visible');
        }, Math.max(1200, Number(durationMs) || 3200));
    }

    function showPlayerFollowBlockNotice() {
        showPlayerFollowNotice(PLAYER_FOLLOW_BLOCK_MSG);
    }

    function getPlayerOfflineLocateBannerText(pos) {
        const source = String(pos?.source || playerFollowSource || '').toLowerCase();
        if (source === 'logout') {
            return PLAYER_FOLLOW_OFFLINE_LOCATE_MSG_LOGOUT;
        }
        if (source === 'lastlocation' || source === 'saved') {
            return PLAYER_FOLLOW_OFFLINE_LOCATE_MSG_SAVED;
        }
        return PLAYER_FOLLOW_OFFLINE_LOCATE_MSG_LOGOUT;
    }

    function updatePlayerOfflineLocateBanner() {
        const el = document.getElementById('mcwws-player-offline-locate-banner');
        if (!el) {
            document.body.classList.remove('mcwws-offline-locate-active');
            return;
        }
        el.classList.remove('is-visible');
        el.textContent = '';
        document.body.classList.remove('mcwws-offline-locate-active');
    }

    function getBlueMapRootElement() {
        return getBlueMapApp()?.mapControls?.rootElement || null;
    }

    function isBlueMapGestureTarget(target) {
        if (!target) return false;
        const root = getBlueMapRootElement();
        if (root && (root === target || root.contains(target))) {
            return true;
        }
        return target.tagName === 'CANVAS' || !!target.closest?.('canvas');
    }

    function getPlayerMarkerManager() {
        return getBlueMapApp()?.playerMarkerManager || null;
    }

    /** BlueMap 每秒拉 live/players.json；离线为空时会删掉玩家钉，需把离线玩家合并进列表 */
    function mergeOfflinePlayerIntoLiveData(data) {
        if (!playerFollowActive || !playerFollowSyntheticMarker || !playerFollowTarget) {
            return data;
        }
        const entry = buildBlueMapPlayerMarkerPayload(playerFollowTarget, mapAuthUser?.playerId);
        if (!entry) return data;
        const players = Array.isArray(data?.players) ? [...data.players] : [];
        if (!players.some((p) => String(p?.uuid || '') === entry.uuid)) {
            players.push(entry);
        }
        return { ...(data && typeof data === 'object' ? data : {}), players };
    }

    function ensurePlayerMarkerManagerOfflinePatch() {
        const pm = getPlayerMarkerManager();
        if (!pm || playerMarkerManagerPatched) return;
        playerMarkerManagerPatched = true;
        const origUpdateFromData = pm.updateFromData.bind(pm);
        pm.updateFromData = function mcwwsPlayerMarkerUpdateFromData(data) {
            return origUpdateFromData(mergeOfflinePlayerIntoLiveData(data));
        };
    }

    function findAuthPlayerMarker() {
        if (!mapAuthUser?.playerId) return null;
        const uuid = playerFollowTarget?.uuid || cachedPlayerLoc?.uuid;
        const pm = getPlayerMarkerManager();
        if (pm && uuid && typeof pm.getPlayerMarker === 'function') {
            const byUuid = pm.getPlayerMarker(uuid);
            if (byUuid) return byUuid;
        }
        const markerSets = getBlueMapApp()?.mapViewer?.markers?.data?.markerSets || [];
        for (const set of markerSets) {
            if (set.id !== 'bm-players') continue;
            const list = set.markers;
            const iterable = list instanceof Map ? list.values() : (list || []);
            for (const marker of iterable) {
                if (playerMarkerMatchesName(marker, mapAuthUser.playerId)) {
                    return marker;
                }
            }
        }
        return null;
    }

    function playerPositionFeetY(pos) {
        if (!pos) return null;
        const lift = pos.online ? PLAYER_HEAD_OFFSET : PLAYER_EYE_OFFSET;
        const feetY = Number(pos.y) - lift;
        return Number.isFinite(feetY) ? feetY : null;
    }

    function buildBlueMapPlayerMarkerPayload(pos, playerId) {
        const uuid = String(pos?.uuid || cachedPlayerLoc?.uuid || '').trim();
        if (!uuid) return null;
        const feetY = playerPositionFeetY(pos);
        const x = Number(pos.x);
        const z = Number(pos.z);
        if (feetY == null || ![x, z].every(Number.isFinite)) return null;
        return {
            uuid,
            name: playerId || mapAuthUser?.playerId,
            position: { x, y: feetY, z },
            rotation: {
                pitch: Number.isFinite(Number(pos.pitch)) ? Number(pos.pitch) : 0,
                yaw: Number.isFinite(Number(pos.yaw)) ? Number(pos.yaw) : 0
            },
            foreign: false
        };
    }

    function shouldStabilizeAuthPlayerMarker(pos) {
        if (playerFollowActive) {
            return true;
        }
        return !!(pos && !pos.online && playerFollowSyntheticMarker);
    }

    function stabilizeAuthPlayerMarkerElement(marker) {
        const el = marker?.element;
        if (!el) return;
        el.classList.add(AUTH_PLAYER_MARKER_CLASS);
        el.setAttribute('distance-data', 'near');
        const wrap = el.parentNode;
        if (wrap?.style) {
            wrap.style.opacity = '';
        }
        el.style.opacity = '';
        const img = marker.playerHeadElement || el.querySelector('img');
        if (img) img.style.opacity = '';
        const nameEl = marker.playerNameElement || el.querySelector('.bm-player-name');
        if (nameEl) nameEl.style.opacity = '';
    }

    function clearAuthPlayerMarkerStyling(marker) {
        marker?.element?.classList?.remove(AUTH_PLAYER_MARKER_CLASS);
    }

    /** 避免 BlueMap 每秒 updateFromData 触发 1s 位移动画导致头像闪烁 */
    function patchAuthPlayerMarkerBehavior(marker) {
        if (!marker || marker._mcwwsAuthMarkerPatched) return;
        marker._mcwwsAuthMarkerPatched = true;
        const origUpdate = marker.updateFromData.bind(marker);
        marker.updateFromData = function mcwwsAuthPlayerUpdateFromData(data) {
            if (!marker.element?.classList?.contains(AUTH_PLAYER_MARKER_CLASS)) {
                return origUpdate(data);
            }
            const pos = data.position || {};
            const rot = data.rotation || {};
            marker.position.set(pos.x || 0, (pos.y || 0) + 1.8, pos.z || 0);
            marker.data.rotation.pitch = rot.pitch || 0;
            marker.data.rotation.yaw = rot.yaw || 0;
            const name = data.name || marker.data.playerUuid;
            marker.data.name = name;
            if (marker.playerNameElement && marker.playerNameElement.innerHTML !== name) {
                marker.playerNameElement.innerHTML = name;
            }
            marker.data.foreign = !!data.foreign;
            marker.visible = !data.foreign;
            stabilizeAuthPlayerMarkerElement(marker);
        };
        const origBeforeRender = marker.onBeforeRender?.bind(marker);
        marker.onBeforeRender = function mcwwsAuthPlayerBeforeRender(renderer, scene, camera) {
            stabilizeAuthPlayerMarkerElement(marker);
            if (origBeforeRender) {
                origBeforeRender(renderer, scene, camera);
            }
            marker.element?.setAttribute('distance-data', 'near');
        };
    }

    function removeAuthPlayerSyntheticMarker() {
        const marker = findAuthPlayerMarker();
        if (!marker) return;
        clearAuthPlayerMarkerStyling(marker);
        const set = getPlayerMarkerManager()?.getPlayerMarkerSet?.(false);
        if (set?.remove) {
            set.remove(marker);
        }
    }

    function applyAuthPlayerMarkerTransform(marker, pos) {
        if (!marker || !pos) return null;
        const feetY = playerPositionFeetY(pos);
        const x = Number(pos.x);
        const z = Number(pos.z);
        if (feetY == null || ![x, z].every(Number.isFinite)) return null;
        const displayY = feetY + PLAYER_HEAD_OFFSET;
        if (marker.position?.set) {
            marker.position.set(x, displayY, z);
        } else if (marker.position) {
            marker.position.x = x;
            marker.position.y = displayY;
            marker.position.z = z;
        }
        const dataPos = marker.data?.position;
        if (dataPos) {
            dataPos.x = x;
            dataPos.y = feetY;
            dataPos.z = z;
        }
        if (marker.data && mapAuthUser?.playerId) {
            marker.data.name = mapAuthUser.playerId;
        }
        if (marker.playerNameElement && mapAuthUser?.playerId) {
            const name = mapAuthUser.playerId;
            if (marker.playerNameElement.textContent !== name) {
                marker.playerNameElement.textContent = name;
            }
        }
        if (shouldStabilizeAuthPlayerMarker(pos)) {
            patchAuthPlayerMarkerBehavior(marker);
            stabilizeAuthPlayerMarkerElement(marker);
        } else {
            clearAuthPlayerMarkerStyling(marker);
        }
        marker.visible = true;
        marker.data.foreign = false;
        return marker;
    }

    /**
     * 确保显示玩家头像与 ID：在线用 live 钉；离线则注入 BlueMap PlayerMarker。
     */
    function ensureAuthPlayerMarkerVisible(pos) {
        if (!mapAuthUser?.playerId || !pos) return null;
        if (!pos.online) {
            ensurePlayerMarkerManagerOfflinePatch();
        }
        let marker = findAuthPlayerMarker();
        if (marker) {
            if (pos.online) {
                playerFollowSyntheticMarker = false;
                if (!playerFollowActive) {
                    clearAuthPlayerMarkerStyling(marker);
                }
            } else {
                playerFollowSyntheticMarker = true;
            }
            return applyAuthPlayerMarkerTransform(marker, pos);
        }
        if (!pos.online || playerFollowActive) {
            if (!pos.online) {
                playerFollowSyntheticMarker = true;
            }
            const payload = buildBlueMapPlayerMarkerPayload(pos, mapAuthUser.playerId);
            if (!payload) return null;
            const set = getPlayerMarkerManager()?.getPlayerMarkerSet?.(true);
            if (!set?.updatePlayerMarkerFromData) return null;
            try {
                marker = set.updatePlayerMarkerFromData(payload);
                return applyAuthPlayerMarkerTransform(marker, pos);
            } catch (err) {
                console.warn('[mcwws-shops] ensureAuthPlayerMarkerVisible failed', err);
                return null;
            }
        }
        return null;
    }

    function syncAuthPlayerMarkerPosition(pos) {
        if (!mapAuthUser?.playerId || !pos) return null;
        const marker = findAuthPlayerMarker();
        if (!marker) {
            if (playerFollowActive || (playerFollowSyntheticMarker && !pos.online)) {
                return ensureAuthPlayerMarkerVisible(pos);
            }
            return null;
        }
        return applyAuthPlayerMarkerTransform(marker, pos);
    }

    function resetPlayerFollowPanState() {
        playerFollowPanPointerId = null;
        playerFollowPanDragging = false;
        playerFollowPanHoldMs = 0;
        playerFollowPanStartX = 0;
        playerFollowPanStartY = 0;
        playerFollowDragHintShown = false;
        updateLocateDragProgress(0);
    }

    function armPlayerFollowGestureGuard(extraMs = 0) {
        playerFollowGestureGuardUntil = performance.now()
            + PLAYER_FOLLOW_GESTURE_GUARD_MS
            + Math.max(0, Number(extraMs) || 0);
        resetPlayerFollowPanState();
    }

    function isPlayerFollowGestureGuarded() {
        return playerFollowApplying || performance.now() < playerFollowGestureGuardUntil;
    }

    function updateLocateDragProgress(ratio) {
        const btn = document.querySelector('.mcwws-ctrl-locate');
        const bar = btn?.querySelector('.mcwws-ctrl-locate-progress-bar');
        if (!btn || !bar) return;
        const clamped = Math.max(0, Math.min(1, Number(ratio) || 0));
        btn.classList.toggle('is-drag-exit-pending', playerFollowPanPointerId != null && clamped < 1);
        bar.style.strokeDashoffset = String(LOCATE_PROGRESS_RING_LEN * (1 - clamped));
    }

    function migrateLocateProgressRing(root) {
        const btn = root?.querySelector?.('.mcwws-ctrl-locate')
            || document.querySelector('.mcwws-ctrl-locate');
        if (!btn || btn.querySelector('.mcwws-ctrl-locate-progress')) return;
        const ring = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        ring.setAttribute('class', 'mcwws-ctrl-locate-progress');
        ring.setAttribute('viewBox', '0 0 40 40');
        ring.setAttribute('aria-hidden', 'true');
        ring.innerHTML = `
            <circle class="mcwws-ctrl-locate-progress-track" cx="20" cy="20" r="17" pathLength="${LOCATE_PROGRESS_RING_LEN}" />
            <circle class="mcwws-ctrl-locate-progress-bar" cx="20" cy="20" r="17" pathLength="${LOCATE_PROGRESS_RING_LEN}" />
        `;
        const icon = btn.querySelector('.mcwws-ctrl-locate-icon');
        if (icon) {
            btn.insertBefore(ring, icon);
        } else {
            btn.prepend(ring);
        }
    }

    function stopPlayerFollow(options = {}) {
        if (!playerFollowActive) return;
        const wasSynthetic = playerFollowSyntheticMarker;
        const restoreView = options.skipRestore ? null : playerFollowRestoreView;
        playerFollowRestoreView = null;
        playerFollowActive = false;
        playerFollowSource = '';
        playerFollowTarget = null;
        playerFollowMapId = null;
        playerFollowPollBusy = false;
        playerFollowTeleportToken += 1;
        playerFollowSmooth = null;
        playerFollowSnapBackToken += 1;
        playerFollowGestureGuardUntil = 0;
        playerFollowSyntheticMarker = false;
        if (wasSynthetic) {
            removeAuthPlayerSyntheticMarker();
        }
        resetPlayerFollowPanState();
        setPlayerFollowMapHeightBypass(false);
        updatePlayerOfflineLocateBanner();
        updateMapControlsState();
        if (restoreView) {
            void restorePlayerFollowPreviousMap(restoreView);
        }
    }

    async function restorePlayerFollowPreviousMap(view) {
        if (!view?.map) return;
        const token = ++playerFollowRestoreToken;
        const bm = getBlueMapApp();
        if (!bm) return;
        const dist = clampMapDistance(view.distance ?? view.height ?? lastFlatHeight);
        const normalized = normalizeViewForBlueMap({
            ...view,
            map: normalizePlayerMapId(view.map),
            distance: dist,
            height: dist
        });
        if (!normalized) return;
        try {
            await switchBlueMapForPlayerFollow(normalized.map, bm, normalized);
            if (token !== playerFollowRestoreToken) return;
            await applyBlueMapView(normalized, {
                useExactView: true,
                fast: false,
                skipMapEnsure: true,
                paramDuration: VIEW_RESTORE_TRANSITION_MS,
                modeDuration: normalized.mode !== getMapViewState() ? VIEW_MODE_TRANSITION_MS : 0
            });
            if (token !== playerFollowRestoreToken) return;
            renderPanel();
            syncPinElements();
            updatePinPositions();
            showPlayerFollowNotice(
                `已恢复到${formatMapDimensionLabel(normalized.map)}地图。`,
                3200
            );
        } catch (err) {
            console.warn('[mcwws-shops] restorePlayerFollowPreviousMap failed', err);
        }
    }

    async function snapBackPlayerFollowView() {
        if (!playerFollowActive || !playerFollowTarget) return;
        const view = buildFollowViewFromPosition(playerFollowTarget);
        const cm = getControlsManager();
        if (!view || !cm) {
            playerFollowApplying = false;
            return;
        }
        const token = ++playerFollowSnapBackToken;
        playerFollowApplying = true;
        initPlayerFollowSmoothFromControls(cm);
        try {
            await animateControlsToView(view, PLAYER_FOLLOW_SNAP_BACK_MS, {
                force: true,
                syncFollowSmooth: true,
                refreshCameraEachFrame: true
            });
            if (token !== playerFollowSnapBackToken) return;
            syncPlayerFollowSmoothFromControls(cm);
            triggerControlsCameraUpdate(cm);
            updatePinPositions();
        } finally {
            playerFollowApplying = false;
        }
    }

    function followPositionDistance(a, b) {
        if (!a || !b) return 0;
        return Math.hypot(
            Number(a.x) - Number(b.x),
            Number(a.y) - Number(b.y),
            Number(a.z) - Number(b.z)
        );
    }

    async function applyFollowMapSwitch(pos) {
        const bm = getBlueMapApp();
        const view = buildFollowViewFromPosition(pos);
        if (!bm || !view?.map) return;
        const targetMap = normalizePlayerMapId(view.map);
        if (mapsMatchForFollow(playerFollowMapId, targetMap)) return;
        playerFollowMapId = targetMap;
        playerFollowApplying = true;
        try {
            await switchBlueMapForPlayerFollow(targetMap, bm, { ...view, map: targetMap });
        } finally {
            playerFollowApplying = false;
        }
    }

    async function animateFollowTeleport(pos) {
        const view = buildFollowViewFromPosition(pos);
        const cm = getControlsManager();
        if (!view || !cm) return;
        const token = ++playerFollowTeleportToken;
        playerFollowApplying = true;
        try {
            await applyFollowMapSwitch(pos);
            if (token !== playerFollowTeleportToken) return;
            await animateControlsToView(view, Math.min(PLAYER_FOLLOW_START_ANIM_MS, 420));
            if (token !== playerFollowTeleportToken) return;
            triggerControlsCameraUpdate(cm);
            if (tickFrame % 3 === 0) {
                syncPageAddressFromControls();
            }
        } finally {
            playerFollowApplying = false;
        }
    }

    async function pollPlayerFollowTarget() {
        if (!playerFollowActive || !mapAuthUser || playerFollowPollBusy) return;
        playerFollowPollBusy = true;
        try {
            const pos = await resolvePlayerPosition();
            if (!pos || !playerFollowActive) return;

            const prev = playerFollowTarget;
            const jumped = prev && followPositionDistance(prev, pos) >= PLAYER_FOLLOW_TELEPORT_BLOCKS;
            const mapChanged = prev && prev.map && pos.map && !mapsMatchForFollow(prev.map, pos.map);

            playerFollowTarget = {
                ...pos,
                map: normalizePlayerMapId(pos.map)
            };
            playerFollowSource = pos.source || playerFollowSource;
            if (pos.online) {
                playerFollowSyntheticMarker = false;
            } else {
                playerFollowSyntheticMarker = true;
                ensurePlayerMarkerManagerOfflinePatch();
            }
            syncAuthPlayerMarkerPosition(pos);
            updatePlayerOfflineLocateBanner();
            updateMapControlsState();

            if (mapChanged || jumped) {
                if (mapChanged) {
                    showPlayerFollowNotice(
                        `玩家已进入${formatMapDimensionLabel(pos.map)}，已切换地图维度。`,
                        3600
                    );
                }
                void animateFollowTeleport(playerFollowTarget);
            }
        } finally {
            playerFollowPollBusy = false;
            playerFollowLastPollAt = performance.now();
        }
    }

    function stepPlayerFollowSmooth(dtMs) {
        if (!playerFollowActive || !playerFollowTarget || playerFollowApplying || playerFollowPanDragging) {
            return;
        }
        const cm = getControlsManager();
        if (!cm) return;
        if (!playerFollowSmooth) {
            initPlayerFollowSmoothFromControls(cm);
        }

        const target = playerFollowTarget;
        const dt = Math.max(8, Math.min(64, Number(dtMs) || 16));
        const alphaXz = 1 - Math.exp(-dt / PLAYER_FOLLOW_SMOOTH_MS);
        const alphaY = 1 - Math.exp(-dt / PLAYER_FOLLOW_SMOOTH_Y_MS);

        playerFollowSmooth.x += (Number(target.x) - playerFollowSmooth.x) * alphaXz;
        playerFollowSmooth.y += (Number(target.y) - playerFollowSmooth.y) * alphaY;
        playerFollowSmooth.z += (Number(target.z) - playerFollowSmooth.z) * alphaXz;

        cm.position.x = playerFollowSmooth.x;
        cm.position.y = playerFollowSmooth.y;
        cm.position.z = playerFollowSmooth.z;

        triggerControlsCameraUpdate(cm);
        syncAuthPlayerMarkerPosition(target);

        if (tickFrame % 12 === 0) {
            syncPageAddressFromControls();
        }
        updatePinPositions();
    }

    function tickPlayerFollowPanHold(dtMs) {
        if (playerFollowPanPointerId == null) return;
        playerFollowPanHoldMs += Math.max(0, Number(dtMs) || 0);
        const ratio = playerFollowPanHoldMs / PLAYER_FOLLOW_DRAG_EXIT_MS;
        updateLocateDragProgress(ratio);
        if (playerFollowPanHoldMs >= PLAYER_FOLLOW_DRAG_EXIT_MS) {
            stopPlayerFollow();
        }
    }

    function tickPlayerFollow() {
        if (!playerFollowActive) return;
        if (!isPlayerFollowAllowedMode()) {
            stopPlayerFollowFor3DMode();
            return;
        }

        const now = performance.now();
        const dt = playerFollowLastSmoothAt
            ? now - playerFollowLastSmoothAt
            : 16;
        playerFollowLastSmoothAt = now;

        if (playerFollowPanPointerId != null) {
            tickPlayerFollowPanHold(dt);
        }
        if (!playerFollowPanDragging) {
            stepPlayerFollowSmooth(dt);
        }

        if (!playerFollowPollBusy && now - playerFollowLastPollAt >= PLAYER_FOLLOW_POLL_MS) {
            void pollPlayerFollowTarget();
        }
    }

    function bindMapFollowExitEvents() {
        if (mapFollowExitBound) return;
        mapFollowExitBound = true;

        const isMapUiTarget = (target) => !!(
            target?.closest?.('.mcwws-map-controls')
            || target?.closest?.('#mcwws-shop-panel')
            || target?.closest?.('.mcwws-screen-pin')
            || target?.closest?.('#mcwws-player-follow-notice')
        );

        const onFollowPanPointerDown = (event) => {
            if (!playerFollowActive || isPlayerFollowGestureGuarded()) return;
            if (event.button !== 0) return;
            if (isMapUiTarget(event.target)) return;
            if (!isBlueMapGestureTarget(event.target)) return;
            playerFollowPanPointerId = event.pointerId;
            playerFollowPanDragging = false;
            playerFollowPanHoldMs = 0;
            playerFollowPanStartX = event.clientX;
            playerFollowPanStartY = event.clientY;
            updateLocateDragProgress(0);
        };

        const onFollowPanPointerMove = (event) => {
            if (playerFollowPanPointerId == null || event.pointerId !== playerFollowPanPointerId) {
                return;
            }
            if (!playerFollowActive || playerFollowPanDragging || isPlayerFollowGestureGuarded()) {
                return;
            }
            const moved = Math.hypot(
                event.clientX - playerFollowPanStartX,
                event.clientY - playerFollowPanStartY
            );
            if (moved < PLAYER_FOLLOW_PAN_DRAG_PX) return;
            playerFollowPanDragging = true;
            syncPlayerFollowSmoothFromControls(getControlsManager());
            if (!playerFollowDragHintShown) {
                playerFollowDragHintShown = true;
                showPlayerFollowNotice(PLAYER_FOLLOW_DRAG_HINT_MSG, 4500);
            }
        };

        const onFollowPanPointerUp = (event) => {
            if (playerFollowPanPointerId == null || event.pointerId !== playerFollowPanPointerId) {
                return;
            }
            const held = playerFollowPanHoldMs;
            const wasDragging = playerFollowPanDragging;
            resetPlayerFollowPanState();
            if (!playerFollowActive) return;
            if (wasDragging && held < PLAYER_FOLLOW_DRAG_EXIT_MS) {
                playerFollowApplying = true;
                void snapBackPlayerFollowView();
            }
        };

        document.addEventListener('pointerdown', onFollowPanPointerDown, true);
        document.addEventListener('pointermove', onFollowPanPointerMove, true);
        document.addEventListener('pointerup', onFollowPanPointerUp, true);
        document.addEventListener('pointercancel', onFollowPanPointerUp, true);
        document.addEventListener('keydown', (event) => {
            if (!playerFollowActive || playerFollowApplying) return;
            if (isTextInputFocused()) return;
            const key = String(event.key || '').toLowerCase();
            if (['w', 'a', 's', 'd', 'q', 'e', 'arrowup', 'arrowdown', 'arrowleft', 'arrowright'].includes(key)) {
                event.preventDefault();
                event.stopPropagation();
                showPlayerFollowBlockNotice();
            }
        }, true);
    }

    async function centerCameraOnPlayer(pos, options = {}) {
        let view = buildFollowViewFromPosition(pos);
        if (!view) return false;
        const dist = clampMapDistance(
            view.distance ?? view.height ?? lastFlatHeight
        );
        view = normalizeViewForBlueMap({
            ...view,
            map: normalizePlayerMapId(pos.map || view.map),
            x: pos.x,
            y: pos.y,
            z: pos.z,
            mode: 'flat',
            distance: dist,
            height: dist,
            rotation: 0,
            angle: 0,
            tilt: 0,
            ortho: FLAT_ORTHO_ON
        });
        const bm = getBlueMapApp();
        const needsModeSwitch = getMapViewState() !== 'flat';
        const flyMs = needsModeSwitch
            ? Math.max(PLAYER_FOLLOW_START_ANIM_MS, VIEW_MODE_TRANSITION_MS, VIEW_RESTORE_TRANSITION_MS)
            : Math.max(PLAYER_FOLLOW_START_ANIM_MS, VIEW_RESTORE_TRANSITION_MS);
        armPlayerFollowGestureGuard(flyMs);
        playerFollowApplying = true;
        if (bm) {
            const switched = await switchBlueMapForPlayerFollow(view.map, bm, view);
            if (!switched) {
                console.warn('[mcwws-shops] switchBlueMapForPlayerFollow failed', view.map);
            }
        }
        if (options.animate) {
            await applyBlueMapView(view, {
                fast: false,
                followStart: true,
                skipMapEnsure: true,
                paramDuration: PLAYER_FOLLOW_START_ANIM_MS,
                modeDuration: needsModeSwitch ? VIEW_MODE_TRANSITION_MS : 0
            });
        } else {
            applyControlsFromView(view);
            replaceLocationHash(formatViewHash(view));
            updatePinPositions();
        }
        syncPlayerFollowSmoothFromControls(getControlsManager());
        playerFollowApplying = false;
        return true;
    }

    async function togglePlayerLocate() {
        if (playerFollowActive) {
            stopPlayerFollow();
            return;
        }
        playerFollowRestoreToken += 1;
        if (!mapAuthUser?.playerId) {
            requestLoginModalFromParent();
            requestAuthFromParent();
            return;
        }
        const from3d = !isPlayerFollowAllowedMode();
        invalidatePlayerFollowCaches();
        const pos = await resolvePlayerPosition({ forceFresh: true });
        if (!pos) {
            return;
        }
        const playerMapId = normalizePlayerMapId(pos.map || getCurrentMapId());
        const currentMapId = getCurrentMapId();
        const switchedDimension = !mapsMatchForFollow(playerMapId, currentMapId);
        if (switchedDimension) {
            playerFollowRestoreView = captureViewFromControls();
        } else {
            playerFollowRestoreView = null;
        }
        playerFollowTarget = { ...pos, map: playerMapId };
        playerFollowMapId = playerMapId;
        playerFollowLastPollAt = performance.now();
        playerFollowLastSmoothAt = performance.now();
        setPlayerFollowMapHeightBypass(true);
        initPlayerFollowSmoothFromControls(getControlsManager());
        const centered = await centerCameraOnPlayer(playerFollowTarget, { animate: true });
        if (!centered) {
            playerFollowTarget = null;
            playerFollowMapId = null;
            playerFollowRestoreView = null;
            playerFollowSmooth = null;
            setPlayerFollowMapHeightBypass(false);
            return;
        }
        playerFollowActive = true;
        armPlayerFollowGestureGuard(480);
        syncPlayerFollowSmoothFromControls(getControlsManager());
        updateMapControlsState();
        if (!playerFollowTarget.online) {
            playerFollowSyntheticMarker = true;
            ensurePlayerMarkerManagerOfflinePatch();
        }
        ensureAuthPlayerMarkerVisible(playerFollowTarget);
        refreshAuthPlayerMarkerAfterLocate(playerFollowTarget);
        void pollPlayerFollowTarget();
        if (from3d) {
            showPlayerFollowNotice(PLAYER_FOLLOW_3D_START_MSG, 3600);
        }
        if (switchedDimension) {
            const dimMsg = `玩家在${formatMapDimensionLabel(playerMapId)}，已自动切换到该维度地图。`;
            if (from3d) {
                window.setTimeout(() => showPlayerFollowNotice(dimMsg, 4500), 3800);
            } else {
                showPlayerFollowNotice(dimMsg, 4500);
            }
        } else if (!playerFollowTarget.online) {
            showPlayerFollowNotice(getPlayerOfflineLocateBannerText(playerFollowTarget), 4200);
        }
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

    function loadDayNightLockFromStorage() {
        try {
            const stored = localStorage.getItem(STORAGE_DAY_NIGHT_LOCK);
            if (stored === 'day' || stored === 'night') {
                dayNightLock = stored;
            }
        } catch {
            /* ignore */
        }
    }

    function saveDayNightLockToStorage() {
        try {
            if (isDayNightLocked()) {
                localStorage.setItem(STORAGE_DAY_NIGHT_LOCK, dayNightLock);
            } else {
                localStorage.removeItem(STORAGE_DAY_NIGHT_LOCK);
            }
        } catch {
            /* ignore */
        }
    }

    function cancelSunlightAnimation() {
        dayNightAnimToken += 1;
    }

    function writeSunlightStrengthValue(value) {
        const uniforms = getMapViewerUniforms();
        if (!uniforms?.sunlightStrength) {
            return false;
        }
        dayNightStrengthInternalSet = true;
        uniforms.sunlightStrength.value = value;
        dayNightStrengthInternalSet = false;
        return true;
    }

    function installSunlightStrengthGuard() {
        const uniform = getMapViewerUniforms()?.sunlightStrength;
        if (!uniform || uniform.__mcwwsDayNightGuard) {
            return;
        }
        let stored = uniform.value;
        Object.defineProperty(uniform, 'value', {
            get() {
                return stored;
            },
            set(next) {
                if (dayNightStrengthInternalSet) {
                    stored = next;
                    return;
                }
                if (isDayNightLocked()) {
                    stored = getDayNightLockStrength();
                    return;
                }
                stored = next;
            },
            configurable: true,
            enumerable: true
        });
        uniform.__mcwwsDayNightGuard = true;
    }

    function stopDayNightLockGuard() {
        if (dayNightLockRafId) {
            cancelAnimationFrame(dayNightLockRafId);
            dayNightLockRafId = 0;
        }
    }

    function startDayNightLockGuard() {
        stopDayNightLockGuard();
        if (!isDayNightLocked()) {
            return;
        }
        const tick = () => {
            if (!isDayNightLocked()) {
                dayNightLockRafId = 0;
                return;
            }
            const target = getDayNightLockStrength();
            if (Math.abs(getSunlightStrength() - target) > 0.001) {
                writeSunlightStrengthValue(target);
                redrawMapViewer();
            }
            dayNightLockRafId = requestAnimationFrame(tick);
        };
        dayNightLockRafId = requestAnimationFrame(tick);
    }

    function applyDayNightLockState(animate = true) {
        if (!isDayNightLocked()) {
            stopDayNightLockGuard();
            return;
        }
        cancelSunlightAnimation();
        const target = getDayNightLockStrength();
        if (animate) {
            animateSunlightStrength(target).then(() => updateMapControlsState());
        } else {
            writeSunlightStrengthValue(target);
            redrawMapViewer();
        }
        startDayNightLockGuard();
        updateMapControlsState();
    }

    function setSunlightStrengthImmediate(value) {
        if (!writeSunlightStrengthValue(value)) {
            return false;
        }
        redrawMapViewer();
        return true;
    }

    function animateSunlightStrength(target, durationMs = DAY_NIGHT_ANIM_MS) {
        const uniforms = getMapViewerUniforms();
        if (!uniforms?.sunlightStrength) return Promise.resolve(false);
        if (isDayNightLocked() && Math.abs(target - getDayNightLockStrength()) > 0.001) {
            return Promise.resolve(false);
        }
        const from = getSunlightStrength();
        if (Math.abs(from - target) < 0.002) {
            setSunlightStrengthImmediate(target);
            return Promise.resolve(true);
        }
        const token = ++dayNightAnimToken;
        const start = performance.now();
        return new Promise((resolve) => {
            function frame(now) {
                if (token !== dayNightAnimToken || isDayNightLocked()) {
                    if (isDayNightLocked()) {
                        applyDayNightLockState(false);
                    }
                    resolve(false);
                    return;
                }
                const progress = Math.min(1, (now - start) / durationMs);
                const eased = easeOutQuad(progress);
                writeSunlightStrengthValue(from + (target - from) * eased);
                redrawMapViewer();
                if (progress < 1) {
                    requestAnimationFrame(frame);
                    return;
                }
                writeSunlightStrengthValue(target);
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

    function isDayNightLocked() {
        return dayNightLock === 'day' || dayNightLock === 'night';
    }

    function getDayNightLockStrength() {
        return dayNightLock === 'night' ? DAY_NIGHT_STRENGTH_NIGHT : DAY_NIGHT_STRENGTH_DAY;
    }

    function isDayNightManualActive() {
        if (isDayNightLocked()) {
            return true;
        }
        return Date.now() < dayNightManualUntil;
    }

    function enforceDayNightLock() {
        if (!isDayNightLocked()) {
            return;
        }
        applyDayNightLockState(false);
    }

    function toggleMapDayNight() {
        if (isDayNightLocked()) {
            return;
        }
        const target = isMapDaylight() ? DAY_NIGHT_STRENGTH_NIGHT : DAY_NIGHT_STRENGTH_DAY;
        dayNightManualUntil = Date.now() + DAY_NIGHT_MANUAL_MS;
        animateSunlightStrength(target).then(() => updateMapControlsState());
        updateMapControlsState();
    }

    function onDayNightLongPress() {
        if (isDayNightLocked()) {
            dayNightLock = null;
            dayNightManualUntil = 0;
            saveDayNightLockToStorage();
            stopDayNightLockGuard();
            updateMapControlsState();
            void syncMapDayNightFromServer();
            return;
        }
        dayNightLock = isMapDaylight() ? 'day' : 'night';
        dayNightManualUntil = 0;
        saveDayNightLockToStorage();
        applyDayNightLockState(true);
    }

    function bindDayNightControl(btn) {
        if (!btn || btn.dataset.dayNightBound === '1') {
            return;
        }
        btn.dataset.dayNightBound = '1';
        migrateDayNightLockRing(btn);

        const clearLongPress = () => {
            if (dayNightLongPressTimer) {
                window.clearTimeout(dayNightLongPressTimer);
                dayNightLongPressTimer = 0;
            }
        };

        const endLongPress = (e) => {
            clearLongPress();
            try {
                if (btn.hasPointerCapture?.(e.pointerId)) {
                    btn.releasePointerCapture(e.pointerId);
                }
            } catch {
                /* ignore */
            }
        };

        btn.addEventListener('pointerdown', (e) => {
            if (e.button !== 0) {
                return;
            }
            dayNightLongPressHandled = false;
            clearLongPress();
            try {
                btn.setPointerCapture(e.pointerId);
            } catch {
                /* ignore */
            }
            dayNightLongPressTimer = window.setTimeout(() => {
                dayNightLongPressTimer = 0;
                dayNightLongPressHandled = true;
                onDayNightLongPress();
            }, DAY_NIGHT_LONG_PRESS_MS);
        });

        btn.addEventListener('pointerup', endLongPress);
        btn.addEventListener('pointercancel', endLongPress);

        btn.addEventListener('click', (e) => {
            if (dayNightLongPressHandled) {
                e.preventDefault();
                e.stopPropagation();
                dayNightLongPressHandled = false;
                return;
            }
            toggleMapDayNight();
        });
    }

    function migrateDayNightLockRing(btn) {
        if (!btn || btn.querySelector('.mcwws-ctrl-daynight-lock-ring')) {
            return;
        }
        const ring = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        ring.setAttribute('class', 'mcwws-ctrl-daynight-lock-ring');
        ring.setAttribute('viewBox', '0 0 40 40');
        ring.setAttribute('aria-hidden', 'true');
        ring.innerHTML = `
            <circle class="mcwws-daynight-lock-ring-track" cx="20" cy="20" r="17" pathLength="${DAY_NIGHT_LOCK_RING_LEN}" />
            <circle class="mcwws-daynight-lock-ring-bar" cx="20" cy="20" r="17" pathLength="${DAY_NIGHT_LOCK_RING_LEN}" />
        `;
        const icon = btn.querySelector('.mcwws-ctrl-daynight-icon');
        if (icon) {
            btn.insertBefore(ring, icon);
        } else {
            btn.prepend(ring);
        }
    }

    async function syncMapDayNightFromServer() {
        if (isDayNightLocked()) {
            enforceDayNightLock();
            return;
        }
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
            if (isDayNightLocked()) {
                enforceDayNightLock();
                return;
            }
            if (Date.now() < dayNightManualUntil) {
                return;
            }
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
        loadDayNightLockFromStorage();
        const tick = () => syncMapDayNightFromServer();
        const waitForBlueMap = () => {
            if (getMapViewerUniforms()?.sunlightStrength) {
                installSunlightStrengthGuard();
                if (isDayNightLocked()) {
                    applyDayNightLockState(false);
                } else {
                    tick();
                }
                setInterval(tick, WORLD_TIME_POLL_MS);
                return;
            }
            requestAnimationFrame(waitForBlueMap);
        };
        waitForBlueMap();
    }

    function isMobileSheetViewport() {
        return typeof window.matchMedia === 'function'
            && window.matchMedia('(max-width: 640px)').matches;
    }

    const MAP_LAYER_MENU_ID = 'mcwws-map-layer-menu';

    function getLayerMenuElement(root) {
        return document.getElementById(MAP_LAYER_MENU_ID)
            || root?.querySelector('.mcwws-layer-menu')
            || null;
    }

    function mountMobileSheetPanel(panel, homeParent) {
        if (!panel) {
            return;
        }
        if (isMobileSheetViewport()) {
            if (panel.parentElement !== document.body) {
                document.body.appendChild(panel);
            }
            return;
        }
        if (homeParent && panel.parentElement !== homeParent) {
            homeParent.appendChild(panel);
        }
    }

    function closeLayerMenu() {
        if (!layerMenuOpen) {
            return;
        }
        layerMenuOpen = false;
        renderLayerMenu();
    }

    function syncMobileSheetBodyLock() {
        const mobileOpen = isMobileSheetViewport()
            && (layerMenuOpen || document.body.classList.contains('mcwws-gis-layer-dialog-open'));
        document.body.classList.toggle('mcwws-mobile-sheet-open', mobileOpen);
        document.body.classList.toggle('mcwws-layer-menu-open', layerMenuOpen);
        const backdrop = document.getElementById('mcwws-mobile-sheet-backdrop');
        if (backdrop) {
            backdrop.hidden = !mobileOpen;
            backdrop.classList.toggle('is-visible', mobileOpen);
            backdrop.setAttribute('aria-hidden', mobileOpen ? 'false' : 'true');
        }
    }

    function renderLayerMenu() {
        const root = document.getElementById(MAP_CONTROLS_ID);
        const layerWrap = root?.querySelector('.mcwws-ctrl-layer-wrap');
        const menu = getLayerMenuElement(root);
        if (!menu) return;
        if (!menu.id) {
            menu.id = MAP_LAYER_MENU_ID;
        }
        menu.classList.toggle('is-sheet-open', layerMenuOpen);
        menu.hidden = !layerMenuOpen;
        menu.setAttribute('aria-hidden', layerMenuOpen ? 'false' : 'true');
        syncMobileSheetBodyLock();
        if (!layerMenuOpen) {
            return;
        }
        mountMobileSheetPanel(menu, layerWrap);
        const maps = getAvailableMaps();
        const current = parseHash()?.map || maps[0]?.id;
        menu.innerHTML = `
            <div class="mcwws-layer-menu-title">
                <span class="mcwws-layer-menu-title-text">维度</span>
                <button type="button" class="mcwws-layer-sheet-close" data-action="close-layer-menu" aria-label="关闭">×</button>
            </div>
            ${maps.map((entry) => {
                const active = entry.id === current;
                return `
                <button type="button" class="mcwws-layer-item${active ? ' is-active' : ''}" data-map-id="${escapeHtml(entry.id)}">
                    <span class="mcwws-layer-item-thumb" aria-hidden="true"></span>
                    <span class="mcwws-layer-item-name">${escapeHtml(entry.name)}</span>
                </button>
            `;
            }).join('')}
        `;
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
        const modeBtn = root.querySelector('.mcwws-ctrl-mode');
        if (modeLabel) {
            modeLabel.textContent = isFlat ? '2D' : '3D';
        }
        if (modeBtn) {
            modeBtn.title = isFlat
                ? '当前为 2D 俯视，点击切换到 3D'
                : '当前为 3D 透视，点击切换到 2D';
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
            const locked = isDayNightLocked();
            dayBtn.classList.toggle('is-night', !isDay);
            dayBtn.classList.toggle('is-locked', locked);
            const timeHint = dayNightDayTime != null ? `，游戏刻 ${dayNightDayTime}` : '';
            const periodHint = dayNightPeriod ? `（${dayNightPeriod}${timeHint}）` : (timeHint || '');
            if (locked) {
                dayBtn.title = isDay
                    ? `已锁定日景${periodHint}；长按取消锁定并跟随游戏时间`
                    : `已锁定夜景${periodHint}；长按取消锁定并跟随游戏时间`;
            } else {
                const manual = Date.now() < dayNightManualUntil;
                const manualMin = manual ? Math.max(1, Math.ceil((dayNightManualUntil - Date.now()) / 60000)) : 0;
                if (manual) {
                    dayBtn.title = isDay
                        ? `当前为日景${periodHint}；已手动切换，约 ${manualMin} 分钟后恢复跟随游戏时间；长按可锁定`
                        : `当前为夜景${periodHint}；已手动切换，约 ${manualMin} 分钟后恢复跟随游戏时间；长按可锁定`;
                } else {
                    dayBtn.title = isDay
                        ? `点击切换夜景${periodHint}（跟随主世界游戏时间）；长按锁定日景`
                        : `点击切换日景${periodHint}（跟随主世界游戏时间）；长按锁定夜景`;
                }
            }
        }
        const flyBtn = root.querySelector('.mcwws-ctrl-fly');
        if (flyBtn) {
            const flyAvailable = isFreeFlightViewEnabled();
            flyBtn.hidden = !flyAvailable;
            const isFree = getMapViewState() === 'free';
            flyBtn.classList.toggle('is-active', isFree);
            flyBtn.title = isFree
                ? '正在自由漫游（WASD 移动，空格/Shift 升降，右键环视）— 点击退出'
                : '自由漫游：第一人称飞行浏览地图';
        }
        const locateBtn = root.querySelector('.mcwws-ctrl-locate');
        if (locateBtn) {
            locateBtn.classList.toggle('is-active', playerFollowActive);
            if (!mapAuthUser) {
                locateBtn.title = '请先在左下角登录后再定位玩家';
            } else if (playerFollowActive) {
                const offline = playerFollowTarget && !playerFollowTarget.online;
                const modeLabel = offline
                    ? '离线 · 上次离开位置'
                    : (playerFollowSource === 'live' ? '实时跟踪' : '已保存位置');
                locateBtn.title = `正在定位 ${mapAuthUser.playerId}（${modeLabel}，仅 2D）— 点击取消；可滚轮缩放；拖拽松手回弹，按住拖拽约 2 秒退出`;
            } else {
                locateBtn.title = `定位到 ${mapAuthUser.playerId}（仅 2D 俯视；在线实时 / 离线用存档位置）`;
            }
        }
        updatePlayerOfflineLocateBanner();
        document.body.classList.toggle('mcwws-player-follow-active', playerFollowActive);
    }

    function refreshAuthPlayerMarkerAfterLocate(pos, attemptsLeft = 10) {
        if (!playerFollowActive || !pos) {
            return;
        }
        ensureAuthPlayerMarkerVisible(pos);
        syncAuthPlayerMarkerPosition(pos);
        getBlueMapApp()?.mapViewer?.redraw?.();
        if (attemptsLeft > 1) {
            requestAnimationFrame(() => refreshAuthPlayerMarkerAfterLocate(pos, attemptsLeft - 1));
        }
    }

    function ensureMapControls() {
        let root = document.getElementById(MAP_CONTROLS_ID);
        if (root?.dataset.ready === '1') {
            migrateMapControlsLayout(root);
            migrateCompassDom(root);
            migrateLayerMenuDom(root);
            migrateLocateProgressRing(root);
            migrateFreeFlightButton(root);
            migrateDayNightLockRing(root.querySelector('.mcwws-ctrl-daynight'));
            bindDayNightControl(root.querySelector('.mcwws-ctrl-daynight'));
            bindFreeFlightControl(root);
            updateMapControlsState();
            return root;
        }
        root = document.createElement('div');
        root.id = MAP_CONTROLS_ID;
        root.className = 'mcwws-map-controls';
        root.dataset.ready = '1';
        root.innerHTML = `
            <div class="mcwws-map-controls-stack">
                <div class="mcwws-ctrl-main-row">
                    <div class="mcwws-ctrl-dimension-column">
                        <div class="mcwws-ctrl-layer-wrap">
                            <button type="button" class="mcwws-ctrl-layer" title="维度选择">
                                <span class="mcwws-ctrl-layer-thumb" aria-hidden="true"></span>
                                <span class="mcwws-ctrl-layer-text">
                                    <svg class="mcwws-ctrl-layer-icon" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true"><path fill="currentColor" d="M12 2 2 7l10 5 10-5-10-5zm0 8.5L2 6v2.5l10 5 10-5V6l-10 4.5zm0 4.5L2 10.5V13l10 5 10-5v-2.5L12 15z"/></svg>
                                    维度
                                </span>
                            </button>
                            <div id="mcwws-map-layer-menu" class="mcwws-layer-menu" hidden></div>
                        </div>
                    </div>
                    <div class="mcwws-ctrl-tools-cluster">
                        <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-compass" title="复位朝北（2D 俯视，上北下南）">
                            <span class="mcwws-compass-shell" aria-hidden="true">
                                <span class="mcwws-compass-ellipse">
                                    <span class="mcwws-compass-dial">
                                        <span class="mcwws-compass-needle"></span>
                                    </span>
                                </span>
                            </span>
                        </button>
                        <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-mode" title="当前为 3D 透视，点击切换到 2D">
                            <span class="mcwws-ctrl-mode-label">3D</span>
                        </button>
                        <div class="mcwws-ctrl-zoom">
                            <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-zoom-in" title="放大">+</button>
                            <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-zoom-out" title="缩小">−</button>
                        </div>
                        <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-daynight" title="切换日景 / 夜景">
                            <svg class="mcwws-ctrl-daynight-lock-ring" viewBox="0 0 40 40" aria-hidden="true">
                                <circle class="mcwws-daynight-lock-ring-track" cx="20" cy="20" r="17" pathLength="100" />
                                <circle class="mcwws-daynight-lock-ring-bar" cx="20" cy="20" r="17" pathLength="100" />
                            </svg>
                            <svg class="mcwws-ctrl-daynight-icon" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                                <circle class="mcwws-daynight-sun" cx="12" cy="12" r="4" fill="currentColor"/>
                                <g class="mcwws-daynight-moon" fill="currentColor">
                                    <path d="M14.5 3.2a7.5 7.5 0 1 0 7.3 11.8A6.5 6.5 0 1 1 14.5 3.2z"/>
                                </g>
                            </svg>
                        </button>
                        <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-locate" title="定位到已登录玩家（需先登录）" disabled>
                            <svg class="mcwws-ctrl-locate-progress" viewBox="0 0 40 40" aria-hidden="true">
                                <circle class="mcwws-ctrl-locate-progress-track" cx="20" cy="20" r="17" pathLength="100" />
                                <circle class="mcwws-ctrl-locate-progress-bar" cx="20" cy="20" r="17" pathLength="100" />
                            </svg>
                            <svg class="mcwws-ctrl-locate-icon" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                                <path fill="currentColor" d="M12 2a7 7 0 0 0-7 7c0 5.25 7 13 7 13s7-7.75 7-13a7 7 0 0 0-7-7zm0 9.5A2.5 2.5 0 1 1 12 6a2.5 2.5 0 0 1 0 5.5z"/>
                            </svg>
                        </button>
                        <button type="button" class="mcwws-ctrl-btn mcwws-ctrl-fly" title="自由漫游：WASD 移动，空格/Shift 升降，右键环视">
                            <svg class="mcwws-ctrl-fly-icon" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                                <path fill="currentColor" d="M19.2 8.4c-.2-.4-.7-.7-1.2-.7h-1.6c-.4 0-.6-.3-.3-.6 0 0 .6-.7.6-1.6 0-1.7-1.4-3-3-3s-3 1.3-3 3c0 .9.6 1.6.6 1.6.3.3.1.6-.3.6l-1.6 0c-.4 0-.9.3-1.1.7l-.7.9c-.3.3-.8.4-1.2.2l-1.5-1c-.4-.2-.5-.8-.3-1.2l3.8-2.4c.3-.2.7-.2 1 0l3.8 2.4c.2.4.1 1-.3 1.2l-1.5 1c-.4.2-.9.1-1.2-.2l-.7-.9z"/>
                            </svg>
                        </button>
                        <button type="button" class="mcwws-ctrl-fullscreen" title="全屏，隐藏所有功能模块">
                            <svg class="mcwws-ctrl-fs-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path fill="currentColor" d="M4 9V4h5V6H6v3H4zm0 11v-5h2v3h3v2H4zm16-11V6h-3V4h5v5h-2zm0 16h-5v-2h3v-3h2v5z"/></svg>
                            <span class="mcwws-ctrl-fs-label">全屏</span>
                        </button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(root);
        bindMapControlEvents(root);
        migrateMapControlsLayout(root);
        migrateLayerMenuDom(root);
        migrateLocateProgressRing(root);
        migrateFreeFlightButton(root);
        renderLayerMenu();
        bindFreeFlightControl(root);
        updateMapControlsState();
        return root;
    }

    function bindMapControlEvents(root) {
        if (mapControlsBound) return;
        mapControlsBound = true;

        root.querySelector('.mcwws-ctrl-compass')?.addEventListener('click', resetCompassNorth);
        root.querySelector('.mcwws-ctrl-mode')?.addEventListener('click', () => {
            toggleMapViewMode();
        });
        root.querySelector('.mcwws-ctrl-zoom-in')?.addEventListener('click', () => adjustMapZoom(1));
        root.querySelector('.mcwws-ctrl-zoom-out')?.addEventListener('click', () => adjustMapZoom(-1));
        bindDayNightControl(root.querySelector('.mcwws-ctrl-daynight'));
        root.querySelector('.mcwws-ctrl-locate')?.addEventListener('click', () => {
            void togglePlayerLocate();
        });
        bindFreeFlightControl(root);
        root.querySelector('.mcwws-ctrl-fullscreen')?.addEventListener('click', toggleCleanMode);
        let layerBtnLock = 0;
        const onLayerButtonActivate = (event) => {
            if (event.type === 'pointerup' && event.pointerType === 'mouse' && event.button !== 0) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            const now = Date.now();
            if (now - layerBtnLock < 450) {
                return;
            }
            layerBtnLock = now;
            if (layerMenuOpen) {
                closeLayerMenu();
                return;
            }
            layerMenuOpen = true;
            window.dispatchEvent(new CustomEvent('mcwws-close-gis-layer-dialog'));
            renderLayerMenu();
        };
        const layerBtn = root.querySelector('.mcwws-ctrl-layer');
        layerBtn?.addEventListener('pointerup', onLayerButtonActivate);
        layerBtn?.addEventListener('click', (event) => {
            if (!isMobileSheetViewport()) {
                onLayerButtonActivate(event);
            }
        });

        document.addEventListener('click', (e) => {
            const item = e.target.closest(`#${MAP_LAYER_MENU_ID} .mcwws-layer-item, .mcwws-layer-menu .mcwws-layer-item`);
            if (!item) return;
            e.stopPropagation();
            switchMapLayer(item.dataset.mapId);
            closeLayerMenu();
            updateMapControlsState();
        });

        document.addEventListener('click', (e) => {
            if (!layerMenuOpen || isMobileSheetViewport()) return;
            if (e.target.closest('.mcwws-ctrl-layer') || e.target.closest('.mcwws-layer-menu')) return;
            closeLayerMenu();
        });

        window.addEventListener('mcwws-close-layer-menu', () => {
            closeLayerMenu();
        });

        if (typeof window.matchMedia === 'function') {
            const mq = window.matchMedia('(max-width: 640px)');
            const onMq = () => {
                const root = document.getElementById(MAP_CONTROLS_ID);
                const layerWrap = root?.querySelector('.mcwws-ctrl-layer-wrap');
                const menu = getLayerMenuElement(root);
                mountMobileSheetPanel(menu, layerWrap);
                syncMobileSheetBodyLock();
            };
            if (typeof mq.addEventListener === 'function') {
                mq.addEventListener('change', onMq);
            } else if (typeof mq.addListener === 'function') {
                mq.addListener(onMq);
            }
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
        const view = getViewForPinProjection();
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
        if (playerFollowActive) {
            tickPlayerFollow();
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
