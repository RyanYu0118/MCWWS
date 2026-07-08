/**
 * 建造工具 — 客户端 Litematica 配置同步面板
 */
(function () {
    const sync = () => window.MCWWS_LitematicaClientSync;

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
            return;
        }
        const body = document.getElementById('buildClientSyncBody');
        if (body && !sync()?.getSnapshot?.()?.connected) {
            body.insertAdjacentHTML('afterbegin',
                `<div class="litematica-import-error${ok ? ' build-client-sync-toast-ok' : ''}">${escapeHtml(message)}</div>`);
        }
    }

    function renderSupportBanner() {
        const api = sync();
        const el = document.getElementById('buildClientSyncUnsupported');
        if (!api || !el) return;
        const info = api.getSupportInfo();
        if (!info.canConnect) {
            el.hidden = false;
            el.textContent = info.hint;
            return;
        }
        el.hidden = false;
        el.classList.add('build-client-sync-support-hint');
        el.textContent = info.hint;
    }

    function renderPlacements(snapshot) {
        const body = document.getElementById('buildClientSyncBody');
        if (!body) return;

        if (!snapshot.connected) {
            const info = sync()?.getSupportInfo?.() || {};
            body.innerHTML = `
                <p class="build-client-sync-intro">
                    点击上方按钮选择本机 <code>.minecraft</code> 文件夹，同步 Litematica 的<strong>放置原点、旋转、镜像</strong>。
                </p>
                <p class="build-client-sync-note">
                    ${escapeHtml(info.hint || '请使用 Chrome / Edge 桌面版。')}
                    ${info.mode === 'fallback' ? ' 兼容模式下请在弹窗中选中 <strong>.minecraft</strong> 文件夹并确认。' : ''}
                </p>
            `;
            return;
        }

        if (snapshot.lastError) {
            body.innerHTML = `<div class="litematica-import-error">⚠️ ${escapeHtml(snapshot.lastError)}</div>`;
            return;
        }

        const placements = snapshot.placements || [];
        const selected = snapshot.selectedPlacement;
        const configFiles = snapshot.configFiles || [];
        const worldOptions = configFiles.map((file) => {
            const sel = file.path === snapshot.selectedWorldFile ? ' selected' : '';
            return `<option value="${escapeHtml(file.path)}"${sel}>${escapeHtml(file.label || file.path)}</option>`;
        }).join('');

        const emptyHint = placements.length ? '' : `
            <div class="build-client-sync-empty-help">
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
            </div>`;

        const rows = placements.length
            ? placements.map((p) => {
                const isSelected = selected && selected.index === p.index;
                const rowClass = isSelected ? 'build-client-sync-row is-selected' : 'build-client-sync-row';
                const transformWarn = (p.rotation !== 'NONE' || p.mirror !== 'NONE')
                    ? '<span class="build-client-sync-transform-tag">已变换</span>'
                    : '';
                return `
                    <tr class="${rowClass}" data-placement-index="${p.index}">
                        <td>${escapeHtml(p.name)}${transformWarn}</td>
                        <td><code>${escapeHtml(p.schematicFileName || '—')}</code></td>
                        <td>${escapeHtml(sync().formatPos(p.origin))}</td>
                        <td>${escapeHtml(sync().labelRotation(p.rotation))}</td>
                        <td>${escapeHtml(sync().labelMirror(p.mirror))}</td>
                        <td>${p.enabled ? '开启' : '关闭'}</td>
                    </tr>`;
            }).join('')
            : `<tr><td colspan="6" class="build-client-sync-empty">该配置中暂无投影放置记录</td></tr>`;

        const transformNote = snapshot.hasTransform
            ? `<p class="build-client-sync-warn">当前投影含旋转/镜像：Litematica 预览与服务器 <code>/build paste</code>（无旋转）可能不一致，请以游戏内对齐为准。</p>`
            : '';

        const modeNote = snapshot.connectionMode === 'files'
            ? '<p class="build-client-sync-note">兼容模式：游戏内修改配置后，请点「重新选择文件夹」刷新。</p>'
            : '';

        body.innerHTML = `
            ${modeNote}
            <div class="build-client-sync-toolbar">
                <label class="build-client-sync-field">
                    <span>配置文件</span>
                    <select id="buildClientSyncWorldSelect"${worldOptions ? '' : ' disabled'}>
                        ${worldOptions || '<option>未找到 Litematica JSON 配置</option>'}
                    </select>
                </label>
                <div class="build-client-sync-status">
                    <span class="build-client-sync-dot${snapshot.polling ? ' is-polling' : ''}"></span>
                    上次刷新 ${formatTime(snapshot.lastUpdatedAt)}${snapshot.polling ? ' · 读取中…' : ''}
                </div>
            </div>
            ${selected ? `
                <div class="build-client-sync-highlight glass">
                    <strong>${escapeHtml(selected.name)}</strong>
                    <span>原点 ${escapeHtml(sync().formatPos(selected.origin))}</span>
                    <span>旋转 ${escapeHtml(sync().labelRotation(selected.rotation))}</span>
                    <span>镜像 ${escapeHtml(sync().labelMirror(selected.mirror))}</span>
                </div>` : ''}
            ${transformNote}
            ${emptyHint}
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
                    <tbody>${rows}</tbody>
                </table>
            </div>
            <div class="build-client-sync-actions">
                <button type="button" class="cart-drawer-btn cart-drawer-btn--primary" id="buildClientSyncImportPasteBtn"${selected ? '' : ' disabled'}>
                    用当前投影导入「投影粘贴」
                </button>
                <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost" id="buildClientSyncRefreshBtn">立即刷新</button>
                <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost" id="buildClientSyncReselectBtn">重新选择文件夹</button>
            </div>
        `;

        document.getElementById('buildClientSyncWorldSelect')?.addEventListener('change', (e) => {
            void sync().setWorldFile(e.target.value);
        });
        document.getElementById('buildClientSyncRefreshBtn')?.addEventListener('click', () => {
            void sync().pollOnce();
        });
        document.getElementById('buildClientSyncReselectBtn')?.addEventListener('click', () => {
            void connect();
        });
        document.getElementById('buildClientSyncImportPasteBtn')?.addEventListener('click', () => {
            void importSelectedToPaste();
        });
        body.querySelectorAll('[data-placement-index]').forEach((row) => {
            row.addEventListener('click', () => {
                sync().setSelectedPlacementIndex(Number(row.getAttribute('data-placement-index')));
            });
        });
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
            statusEl.textContent = snapshot.connected
                ? `已连接${modeLabel} · ${snapshot.selectedWorldFile ? basename(snapshot.selectedWorldFile) : '等待配置'} · ${(snapshot.placements || []).length} 个投影`
                : '未连接';
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
