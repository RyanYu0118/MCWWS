/**
 * 建造工具 — 客户端 Litematica 配置同步面板（增量更新，避免轮询闪烁）
 */
(function () {
    const sync = () => window.MCWWS_LitematicaClientSync;

    let shellMounted = false;
    let lastDataKey = '';
    let lastSelectedIndex = -1;
    let cachedPasteOrders = [];
    let pasteOrdersLoadedAt = 0;

    async function fetchPasteOrders(force = false) {
        const auth = window.MCWWS_AUTH;
        if (!auth?.getToken?.()) {
            cachedPasteOrders = [];
            return cachedPasteOrders;
        }
        if (!force && Date.now() - pasteOrdersLoadedAt < 8000 && cachedPasteOrders.length) {
            return cachedPasteOrders;
        }
        try {
            const url = '/api/build/paste/orders';
            const res = await fetch(url, { headers: auth.headers() });
            const data = window.MCWWS_readJsonResponse
                ? await window.MCWWS_readJsonResponse(res, url)
                : await res.json();
            if (!res.ok && !window.MCWWS_readJsonResponse) {
                throw new Error(data.error || '读取订单失败');
            }
            cachedPasteOrders = (data.orders || []).filter((o) =>
                o.status === 'awaiting_anchor'
                || o.status === 'ready'
                || o.status === 'pasting'
                || o.webPasteQueue === 'pending'
                || o.webPasteQueue === 'processing'
            );
            pasteOrdersLoadedAt = Date.now();
        } catch (_) {
            cachedPasteOrders = [];
        }
        return cachedPasteOrders;
    }

    async function bindAnchorToOrder(pasteOrderId, contentHash) {
        const placement = sync()?.getSelectedPlacement();
        if (!placement) {
            showToast('请先在下方表格中选择投影', false);
            return;
        }
        const worldInput = document.getElementById('buildClientSyncAnchorWorld');
        const world = worldInput?.value?.trim() || placement.worldHint || 'world';
        const pasteApi = window.MCWWS_BuildPasteImport;
        if (!pasteApi?.setAnchorForOrder) {
            showToast('投影粘贴模块未加载', false);
            return;
        }
        try {
            await pasteApi.setAnchorForOrder(pasteOrderId, contentHash, placement, world);
            showToast(`订单 #${pasteOrderId} 锚点已设置`, true);
            await fetchPasteOrders(true);
            renderPasteOrdersPanel();
        } catch (error) {
            showToast(error.message || '设锚点失败', false);
        }
    }

    async function triggerWebPasteForOrder(pasteOrderId, contentHash) {
        const pasteApi = window.MCWWS_BuildPasteImport;
        if (!pasteApi?.triggerWebPaste) {
            showToast('投影粘贴模块未加载', false);
            return;
        }
        try {
            await pasteApi.triggerWebPaste(pasteOrderId, contentHash, {
                escapeHtml
            });
            await fetchPasteOrders(true);
            renderPasteOrdersPanel();
        } catch (_) {
            await fetchPasteOrders(true);
            renderPasteOrdersPanel();
        }
    }

    function renderPasteOrdersPanel() {
        const panel = document.getElementById('buildClientSyncPasteOrders');
        if (!panel) return;
        const auth = window.MCWWS_AUTH;
        if (!auth?.getToken?.()) {
            panel.hidden = true;
            return;
        }
        const orders = cachedPasteOrders.filter((o) =>
            o.status === 'awaiting_anchor'
            || o.status === 'ready'
            || o.status === 'pasting'
            || o.webPasteQueue === 'pending'
            || o.webPasteQueue === 'processing'
        );
        if (!orders.length) {
            panel.hidden = true;
            panel.innerHTML = '';
            return;
        }
        const placement = sync()?.getSelectedPlacement();
        const defaultWorld = placement?.worldHint || 'world';
        panel.hidden = false;
        panel.innerHTML = `
            <div class="build-client-sync-paste-orders">
                <p class="build-paste-client-title">待粘贴订单（需下单账号在线）</p>
                <label class="build-paste-anchor-world-field">
                    <span>默认世界名</span>
                    <input type="text" id="buildClientSyncAnchorWorld" class="build-paste-anchor-world-input" value="${escapeHtml(defaultWorld)}" placeholder="world">
                </label>
                ${orders.map((order) => {
                    const anchorText = order.anchor?.world
                        ? `${order.anchor.world} ${order.anchor.x}, ${order.anchor.y}, ${order.anchor.z}`
                        : '未设锚点';
                    const queue = order.webPasteQueue || '';
                    const isPasting = order.status === 'pasting' || queue === 'pending' || queue === 'processing';
                    let actionBtn = '';
                    if (order.status === 'ready' && !isPasting) {
                        actionBtn = `<button type="button" class="cart-drawer-btn cart-drawer-btn--primary build-client-sync-web-paste" data-order-id="${order.pasteOrderId}" data-content-hash="${escapeHtml(order.contentHash)}">网页粘贴（需在线）</button>`;
                    } else if (order.status === 'awaiting_anchor') {
                        actionBtn = `<button type="button" class="cart-drawer-btn cart-drawer-btn--ghost build-client-sync-bind-anchor" data-order-id="${order.pasteOrderId}" data-content-hash="${escapeHtml(order.contentHash)}">用当前投影设锚点</button>`;
                    } else if (isPasting) {
                        actionBtn = '<span class="litematica-status">粘贴中…</span>';
                    } else if (order.status === 'ready') {
                        actionBtn = '<span class="litematica-status litematica-status--ok">已就绪</span>';
                    }
                    return `
                        <div class="build-client-sync-paste-order">
                            <span>#${order.pasteOrderId} · ${escapeHtml(order.status)}</span>
                            <code>${escapeHtml(anchorText)}</code>
                            ${actionBtn}
                        </div>`;
                }).join('')}
            </div>
        `;
        panel.querySelectorAll('.build-client-sync-bind-anchor').forEach((btn) => {
            btn.addEventListener('click', () => {
                void bindAnchorToOrder(
                    btn.getAttribute('data-order-id'),
                    btn.getAttribute('data-content-hash')
                );
            });
        });
        panel.querySelectorAll('.build-client-sync-web-paste').forEach((btn) => {
            btn.addEventListener('click', () => {
                void triggerWebPasteForOrder(
                    btn.getAttribute('data-order-id'),
                    btn.getAttribute('data-content-hash')
                );
            });
        });
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function formatTime(ts) {
        if (!ts) return '—';
        try {
            return new Date(ts).toLocaleTimeString('zh-CN', { hour12: false });
        } catch (_) {
            return '—';
        }
    }

    function showToast(message, ok) {
        if (window.MCWWS_ShopCart?.showToast) {
            window.MCWWS_ShopCart.showToast(message, ok);
        }
    }

    function dataKey(snapshot) {
        return JSON.stringify({
            connected: snapshot.connected,
            connectionMode: snapshot.connectionMode,
            lastError: snapshot.lastError,
            selectedWorldFile: snapshot.selectedWorldFile,
            lastUpdatedAt: snapshot.lastUpdatedAt,
            placements: snapshot.placements,
            configFiles: (snapshot.configFiles || []).map((f) => ({
                path: f.path,
                label: f.label,
                placementCount: f.placementCount,
                modifiedAt: f.modifiedAt
            })),
            parseHint: snapshot.parseHint,
            hasTransform: snapshot.hasTransform
        });
    }

    function updateConnectButtonLabel() {
        const connectBtn = document.getElementById('buildClientSyncConnectBtn');
        const api = sync();
        if (!connectBtn || !api) return;
        const info = api.getSupportInfo();
        if (info.mode === 'picker') {
            connectBtn.textContent = '选择 .minecraft 文件夹';
            connectBtn.title = '使用系统文件夹选择器（支持自动刷新）';
        } else if (info.mode === 'fallback') {
            connectBtn.textContent = '选择 .minecraft 文件夹';
            connectBtn.title = '兼容模式：系统弹窗可能显示「上传」，请选中 .minecraft 文件夹后点确定/上传';
        } else {
            connectBtn.textContent = '当前浏览器不支持';
            connectBtn.title = info.hint || '';
        }
    }

    function renderSupportBanner() {
        const api = sync();
        const el = document.getElementById('buildClientSyncUnsupported');
        if (!api || !el) return;
        const info = api.getSupportInfo();
        el.hidden = false;
        el.classList.add('build-client-sync-support-hint');
        if (info.mode === 'fallback') {
            el.textContent = `${info.hint} 弹窗里若看到「上传」按钮，这是浏览器兼容模式的正常显示，并非上传文件到网站；请进入 .minecraft 文件夹后点确定。`;
        } else {
            el.textContent = info.canConnect ? info.hint : info.hint;
        }
        updateConnectButtonLabel();
    }

    function bindShellEvents(body) {
        body.querySelector('#buildClientSyncWorldSelect')?.addEventListener('change', (e) => {
            void sync().setWorldFile(e.target.value);
        });
        body.querySelector('#buildClientSyncRefreshBtn')?.addEventListener('click', () => {
            void sync().pollOnce();
        });
        body.querySelector('#buildClientSyncReselectBtn')?.addEventListener('click', () => {
            void connect();
        });
        body.querySelector('#buildClientSyncImportPasteBtn')?.addEventListener('click', () => {
            void importSelectedToPaste();
        });
        body.querySelector('#buildClientSyncPasteManualToggle')?.addEventListener('change', (e) => {
            const api = sync();
            if (!api) return;
            const mode = body.querySelector('input[name="buildClientSyncPasteReplaceMode"]:checked')?.value || 'NONE';
            api.setPasteReplaceManual(e.target.checked, mode);
        });
        body.querySelectorAll('input[name="buildClientSyncPasteReplaceMode"]').forEach((input) => {
            input.addEventListener('change', () => {
                const toggle = body.querySelector('#buildClientSyncPasteManualToggle');
                if (!toggle?.checked) return;
                sync()?.setPasteReplaceManual(true, input.value);
            });
        });
        body.addEventListener('click', (e) => {
            const row = e.target.closest('[data-placement-index]');
            if (!row) return;
            sync().setSelectedPlacementIndex(Number(row.getAttribute('data-placement-index')));
        });
    }

    function mountConnectedShell(snapshot) {
        const body = document.getElementById('buildClientSyncBody');
        if (!body) return;

        const modeNote = snapshot.connectionMode === 'files'
            ? '<p class="build-client-sync-note" id="buildClientSyncModeNote">兼容模式：游戏内修改配置后，请点「重新选择文件夹」刷新。</p>'
            : '<p class="build-client-sync-note" id="buildClientSyncModeNote" hidden></p>';

        body.innerHTML = `
            ${modeNote}
            <div class="build-client-sync-toolbar">
                <label class="build-client-sync-field">
                    <span>配置文件</span>
                    <select id="buildClientSyncWorldSelect"></select>
                </label>
                <div class="build-client-sync-status">
                    <span class="build-client-sync-dot" id="buildClientSyncDot" aria-hidden="true"></span>
                    <span id="buildClientSyncRefreshTime">上次刷新 —</span>
                </div>
            </div>
            <div class="build-client-sync-paste-settings" id="buildClientSyncPasteSettings" hidden>
                <div class="build-client-sync-paste-settings-head">
                    <span class="build-client-sync-paste-settings-label">Litematica 粘贴替换</span>
                    <span class="build-client-sync-paste-settings-read" id="buildClientSyncPasteReadValue" title="从 litematica.json 读取">—</span>
                </div>
                <label class="build-client-sync-paste-manual-toggle">
                    <input type="checkbox" id="buildClientSyncPasteManualToggle">
                    <span>手动指定（覆盖读取值）</span>
                </label>
                <div class="build-client-sync-paste-mode-options" id="buildClientSyncPasteModeOptions" hidden>
                    ${(sync()?.PASTE_REPLACE_MODE_OPTIONS || [
                        ['ALL', '全部替换'],
                        ['NONE', '仅在空气放置'],
                        ['WITH_NON_AIR', '放置非空气方块']
                    ]).map(([value, label]) => `
                        <label class="build-client-sync-paste-mode-option">
                            <input type="radio" name="buildClientSyncPasteReplaceMode" value="${escapeHtml(value)}">
                            <span>${escapeHtml(label)}</span>
                        </label>
                    `).join('')}
                </div>
                <p class="build-client-sync-paste-effective" id="buildClientSyncPasteEffective" hidden></p>
            </div>
            <div class="build-client-sync-highlight glass" id="buildClientSyncHighlight" hidden></div>
            <p class="build-client-sync-warn" id="buildClientSyncTransformNote" hidden></p>
            <div id="buildClientSyncEmptyHelp" hidden></div>
            <div class="build-client-sync-table-wrap">
                <table class="build-client-sync-table">
                    <thead>
                        <tr>
                            <th>名称</th>
                            <th>原理图文件</th>
                            <th>放置原点</th>
                            <th>旋转</th>
                            <th>镜像</th>
                            <th>放置</th>
                        </tr>
                    </thead>
                    <tbody id="buildClientSyncTableBody"></tbody>
                </table>
            </div>
            <div class="build-client-sync-actions">
                <button type="button" class="cart-drawer-btn cart-drawer-btn--primary" id="buildClientSyncImportPasteBtn" disabled>
                    用当前投影导入「投影粘贴」
                </button>
                <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost" id="buildClientSyncRefreshBtn">立即刷新</button>
                <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost" id="buildClientSyncReselectBtn">重新选择文件夹</button>
            </div>
            <div id="buildClientSyncPasteOrders" hidden></div>
        `;
        bindShellEvents(body);
        shellMounted = true;
        patchConnectedView(snapshot);
    }

    function updateConfigSelect(snapshot) {
        const select = document.getElementById('buildClientSyncWorldSelect');
        if (!select) return;

        const configFiles = snapshot.configFiles || [];
        const isFocused = document.activeElement === select;
        const optionsKey = configFiles.map((f) => `${f.path}|${f.label}`).join('\n');

        if (select.dataset.optionsKey !== optionsKey) {
            if (isFocused) return;
            select.innerHTML = configFiles.length
                ? configFiles.map((file) => {
                    const sel = file.path === snapshot.selectedWorldFile ? ' selected' : '';
                    return `<option value="${escapeHtml(file.path)}"${sel}>${escapeHtml(file.label || file.path)}</option>`;
                }).join('')
                : '<option>未找到 Litematica JSON 配置</option>';
            select.disabled = !configFiles.length;
            select.dataset.optionsKey = optionsKey;
        } else if (select.value !== snapshot.selectedWorldFile) {
            select.value = snapshot.selectedWorldFile || '';
        }
    }

    function buildPlacementRows(snapshot) {
        const placements = snapshot.placements || [];
        const selected = snapshot.selectedPlacement;
        const api = sync();

        if (!placements.length) {
            return '<tr><td colspan="6" class="build-client-sync-empty">该配置中暂无投影放置记录</td></tr>';
        }

        return placements.map((p) => {
            const isSelected = selected && selected.index === p.index;
            const rowClass = isSelected ? 'build-client-sync-row is-selected' : 'build-client-sync-row';
            const transformWarn = (p.rotation !== 'NONE' || p.mirror !== 'NONE')
                ? '<span class="build-client-sync-transform-tag">已变换</span>'
                : '';
            return `
                <tr class="${rowClass}" data-placement-index="${p.index}">
                    <td>${escapeHtml(p.name)}${transformWarn}</td>
                    <td><code>${escapeHtml(p.schematicFileName || '—')}</code></td>
                    <td>${escapeHtml(api.formatPos(p.origin))}</td>
                    <td>${escapeHtml(api.labelRotation(p.rotation))}</td>
                    <td>${escapeHtml(api.labelMirror(p.mirror))}</td>
                    <td>${p.enabled ? '开启' : '关闭'}</td>
                </tr>`;
        }).join('');
    }

    function patchSelectionOnly(snapshot) {
        const tbody = document.getElementById('buildClientSyncTableBody');
        if (!tbody) return;
        const selectedIndex = snapshot.selectedPlacement?.index ?? -1;
        tbody.querySelectorAll('[data-placement-index]').forEach((row) => {
            const idx = Number(row.getAttribute('data-placement-index'));
            row.classList.toggle('is-selected', idx === selectedIndex);
            row.classList.toggle('build-client-sync-row', true);
        });
        patchHighlight(snapshot);
        const importBtn = document.getElementById('buildClientSyncImportPasteBtn');
        if (importBtn) importBtn.disabled = !snapshot.selectedPlacement;
    }

    function patchPasteSettings(snapshot) {
        const panel = document.getElementById('buildClientSyncPasteSettings');
        const api = sync();
        if (!panel) return;
        if (!snapshot.connected) {
            panel.hidden = true;
            return;
        }
        panel.hidden = false;

        const readEl = document.getElementById('buildClientSyncPasteReadValue');
        const toggle = document.getElementById('buildClientSyncPasteManualToggle');
        const optionsEl = document.getElementById('buildClientSyncPasteModeOptions');
        const effectiveEl = document.getElementById('buildClientSyncPasteEffective');

        if (readEl) {
            const readLabel = snapshot.pasteReplaceBehavior
                ? (api?.labelReplaceBehavior?.(snapshot.pasteReplaceBehavior) || snapshot.pasteReplaceBehavior)
                : '未读取到（可在下方手动指定）';
            readEl.textContent = `读取：${readLabel}`;
        }

        if (toggle && document.activeElement !== toggle) {
            toggle.checked = !!snapshot.pasteReplaceManual;
        }
        if (optionsEl) {
            optionsEl.hidden = !snapshot.pasteReplaceManual;
        }

        const manualMode = snapshot.pasteReplaceManualMode || 'NONE';
        panel.querySelectorAll('input[name="buildClientSyncPasteReplaceMode"]').forEach((input) => {
            const shouldCheck = input.value === manualMode;
            if (document.activeElement !== input) {
                input.checked = shouldCheck;
            }
            input.disabled = !snapshot.pasteReplaceManual;
        });

        if (effectiveEl) {
            const effective = snapshot.effectivePasteReplaceBehavior || api?.getEffectivePasteReplaceBehavior?.() || 'NONE';
            const effectiveLabel = api?.labelReplaceBehavior?.(effective) || effective;
            const sourceNote = snapshot.pasteReplaceManual
                ? '当前使用手动指定'
                : (snapshot.pasteReplaceBehavior ? '当前跟随 Litematica 读取' : '当前使用默认');
            effectiveEl.hidden = false;
            effectiveEl.textContent = `${sourceNote} · 生效：${effectiveLabel}`;
        }
    }

    function patchHighlight(snapshot) {
        const el = document.getElementById('buildClientSyncHighlight');
        const selected = snapshot.selectedPlacement;
        const api = sync();
        if (!el) return;
        if (!selected) {
            el.hidden = true;
            return;
        }
        el.hidden = false;
        el.innerHTML = `
            <strong>${escapeHtml(selected.name)}</strong>
            <span>原点 ${escapeHtml(api.formatPos(selected.origin))}</span>
            <span>世界 ${escapeHtml(selected.worldHint || 'world')}</span>
            <span>旋转 ${escapeHtml(api.labelRotation(selected.rotation))}</span>
            <span>镜像 ${escapeHtml(api.labelMirror(selected.mirror))}</span>
            ${snapshot.effectivePasteReplaceBehavior ? `<span>粘贴替换 ${escapeHtml(api.labelReplaceBehavior(snapshot.effectivePasteReplaceBehavior))}${snapshot.pasteReplaceManual ? '（手动）' : ''}</span>` : ''}
        `;
    }

    function patchEmptyHelp(snapshot) {
        const el = document.getElementById('buildClientSyncEmptyHelp');
        if (!el) return;
        if ((snapshot.placements || []).length) {
            el.hidden = true;
            el.innerHTML = '';
            return;
        }
        const configFiles = snapshot.configFiles || [];
        el.hidden = false;
        el.className = 'build-client-sync-empty-help';
        el.innerHTML = `
            <p><strong>当前配置文件里没有投影记录，常见原因：</strong></p>
            <ul>
                <li>选错了配置文件 — 多人服务器会按<strong>服务器名</strong>分文件，请在下拉框里找带服务器 IP/名称、且显示「N 个投影」的项</li>
                <li>游戏内尚未写入磁盘 — 加载投影后，请<strong>切换维度</strong>或<strong>退出世界</strong>一次</li>
                <li>兼容模式下改了游戏配置 — 需点<strong>重新选择文件夹</strong>刷新</li>
            </ul>
            ${snapshot.parseHint ? `<p class="build-client-sync-parse-hint">解析提示：${escapeHtml(snapshot.parseHint)}</p>` : ''}
            ${configFiles.some((f) => f.placementCount > 0)
                ? '<p>检测到有其它配置文件含投影，请从下拉框切换。</p>'
                : '<p>已扫描全部路径，目前未发现含投影的配置文件。</p>'}
        `;
    }

    function patchConnectedView(snapshot) {
        updateConfigSelect(snapshot);

        const timeEl = document.getElementById('buildClientSyncRefreshTime');
        if (timeEl) {
            timeEl.textContent = `上次刷新 ${formatTime(snapshot.lastUpdatedAt)}`;
        }

        const transformNote = document.getElementById('buildClientSyncTransformNote');
        if (transformNote) {
            if (snapshot.hasTransform) {
                transformNote.hidden = false;
                transformNote.innerHTML = '当前投影含旋转/镜像：结账导入投影粘贴后，服务器将按相同变换粘贴；可在网页一键粘贴。';
            } else {
                transformNote.hidden = true;
            }
        }

        const tbody = document.getElementById('buildClientSyncTableBody');
        if (tbody) {
            const rowsHtml = buildPlacementRows(snapshot);
            if (tbody.dataset.rowsKey !== rowsHtml) {
                tbody.innerHTML = rowsHtml;
                tbody.dataset.rowsKey = rowsHtml;
            } else {
                patchSelectionOnly(snapshot);
            }
        }

        patchHighlight(snapshot);
        patchPasteSettings(snapshot);
        patchEmptyHelp(snapshot);
        void fetchPasteOrders().then(() => renderPasteOrdersPanel());

        const importBtn = document.getElementById('buildClientSyncImportPasteBtn');
        if (importBtn) importBtn.disabled = !snapshot.selectedPlacement;
    }

    function renderDisconnected(snapshot) {
        const body = document.getElementById('buildClientSyncBody');
        if (!body) return;
        shellMounted = false;
        lastDataKey = '';
        lastSelectedIndex = -1;
        const info = sync()?.getSupportInfo?.() || {};
        body.innerHTML = `
            <p class="build-client-sync-intro">
                点击上方按钮选择本机 <code>.minecraft</code> 文件夹，同步 Litematica 的<strong>放置原点、旋转、镜像</strong>。
            </p>
            <p class="build-client-sync-note">
                ${escapeHtml(info.hint || '请使用 Chrome / Edge 桌面版。')}
                ${info.mode === 'fallback' ? ' 兼容模式下系统弹窗可能显示「<strong>上传</strong>」，请进入 <strong>.minecraft</strong> 文件夹（能看到 <strong>config</strong> 子文件夹）后点确定。' : ''}
            </p>
        `;
    }

    function renderError(snapshot) {
        const body = document.getElementById('buildClientSyncBody');
        if (!body) return;
        shellMounted = false;
        lastDataKey = '';
        body.innerHTML = `<div class="litematica-import-error">⚠️ ${escapeHtml(snapshot.lastError)}</div>`;
    }

    function renderPlacements(snapshot) {
        if (!snapshot.connected) {
            renderDisconnected(snapshot);
            return;
        }
        if (snapshot.lastError) {
            renderError(snapshot);
            return;
        }

        const key = dataKey(snapshot);
        const selectedIndex = snapshot.selectedPlacement?.index ?? -1;

        if (!shellMounted) {
            mountConnectedShell(snapshot);
            lastDataKey = key;
            lastSelectedIndex = selectedIndex;
            return;
        }

        if (key === lastDataKey) {
            if (selectedIndex !== lastSelectedIndex) {
                patchSelectionOnly(snapshot);
                lastSelectedIndex = selectedIndex;
            }
            patchPasteSettings(snapshot);
            patchHighlight(snapshot);
            return;
        }

        patchConnectedView(snapshot);
        lastDataKey = key;
        lastSelectedIndex = selectedIndex;
    }

    async function importSelectedToPaste() {
        const api = sync();
        const placement = api.getSelectedPlacement();
        if (!placement) return;
        const file = await api.readSchematicAsFile(placement);
        if (!file) {
            showToast('无法读取原理图文件，请确认 .minecraft/schematics 可访问', false);
            return;
        }
        window.MCWWS_BuildPasteImport?.open?.();
        await window.MCWWS_BuildPasteImport?.importFile?.(file, {
            clientPlacement: placement
        });
    }

    function basename(path) {
        const normalized = String(path || '').replace(/\\/g, '/');
        const parts = normalized.split('/').filter(Boolean);
        return parts[parts.length - 1] || path || '';
    }

    function updateHeader(snapshot) {
        const statusEl = document.getElementById('buildClientSyncStatus');
        const connectBtn = document.getElementById('buildClientSyncConnectBtn');
        const disconnectBtn = document.getElementById('buildClientSyncDisconnectBtn');
        const modeLabel = snapshot.connectionMode === 'files' ? ' · 兼容模式' : '';
        if (statusEl) {
            const next = snapshot.connected
                ? `已连接${modeLabel} · ${snapshot.selectedWorldFile ? basename(snapshot.selectedWorldFile) : '等待配置'} · ${(snapshot.placements || []).length} 个投影`
                : '未连接';
            if (statusEl.textContent !== next) statusEl.textContent = next;
        }
        if (connectBtn) connectBtn.hidden = !!snapshot.connected;
        if (disconnectBtn) disconnectBtn.hidden = !snapshot.connected;
    }

    function render(snapshot) {
        updateHeader(snapshot);
        renderPlacements(snapshot);
    }

    async function connect() {
        const api = sync();
        if (!api) return;
        const connectBtn = document.getElementById('buildClientSyncConnectBtn');
        if (connectBtn) connectBtn.disabled = true;
        try {
            await api.openFolderDialog();
            showToast('已连接 .minecraft 文件夹', true);
        } catch (error) {
            if (error?.name === 'AbortError') return;
            showToast(error.message || '连接失败', false);
        } finally {
            if (connectBtn && !api.getSnapshot().connected) {
                connectBtn.disabled = false;
            }
        }
    }

    async function init() {
        const api = sync();
        const section = document.getElementById('buildClientSyncSection');
        if (!api || !section) return;

        renderSupportBanner();
        api.subscribe(render);

        document.getElementById('buildClientSyncConnectBtn')?.addEventListener('click', () => { void connect(); });
        document.getElementById('buildClientSyncDisconnectBtn')?.addEventListener('click', () => {
            void api.disconnect();
            const connectBtn = document.getElementById('buildClientSyncConnectBtn');
            if (connectBtn) connectBtn.disabled = false;
            showToast('已断开客户端连接', true);
        });

        render(api.getSnapshot());
        await api.tryRestoreConnection();
    }

    window.MCWWS_BuildClientSyncUI = { init, render, importSelectedToPaste, connect };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        void init();
    }
})();
