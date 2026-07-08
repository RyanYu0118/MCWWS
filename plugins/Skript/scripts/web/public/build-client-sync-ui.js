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

    function renderPlacements(snapshot) {
        const body = document.getElementById('buildClientSyncBody');
        if (!body) return;

        if (!snapshot.connected) {
            body.innerHTML = `
                <p class="build-client-sync-intro">
                    选择本机 <code>.minecraft</code> 文件夹后，网页将每 2 秒读取
                    <code>config/litematica/*.json</code>，同步游戏内投影的<strong>放置原点、旋转、镜像</strong>等设置。
                </p>
                <p class="build-client-sync-note">需使用 Chrome / Edge 等支持「文件夹访问」的桌面浏览器；数据仅在本地读取，不会上传配置文件。</p>
            `;
            return;
        }

        if (snapshot.lastError) {
            body.innerHTML = `<div class="litematica-import-error">⚠️ ${escapeHtml(snapshot.lastError)}</div>`;
            return;
        }

        const placements = snapshot.placements || [];
        const selected = snapshot.selectedPlacement;
        const worldOptions = (snapshot.worldFiles || []).map((name) => {
            const sel = name === snapshot.selectedWorldFile ? ' selected' : '';
            return `<option value="${escapeHtml(name)}"${sel}>${escapeHtml(name)}</option>`;
        }).join('');

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
            : `<tr><td colspan="6" class="build-client-sync-empty">该世界配置中暂无投影放置记录</td></tr>`;

        const transformNote = snapshot.hasTransform
            ? `<p class="build-client-sync-warn">当前投影含旋转/镜像：Litematica 预览与服务器 <code>/build paste</code>（无旋转）可能不一致，请以游戏内对齐为准。</p>`
            : '';

        body.innerHTML = `
            <div class="build-client-sync-toolbar">
                <label class="build-client-sync-field">
                    <span>世界配置</span>
                    <select id="buildClientSyncWorldSelect"${worldOptions ? '' : ' disabled'}>
                        ${worldOptions || '<option>未找到 config/litematica/*.json</option>'}
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
            </div>
        `;

        document.getElementById('buildClientSyncWorldSelect')?.addEventListener('change', (e) => {
            void sync().setWorldFile(e.target.value);
        });
        document.getElementById('buildClientSyncRefreshBtn')?.addEventListener('click', () => {
            void sync().pollOnce();
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
            window.MCWWS_ShopCart?.showToast?.('无法读取原理图文件，请确认 .minecraft/schematics 可访问', false);
            return;
        }
        window.MCWWS_BuildPasteImport?.open?.();
        await window.MCWWS_BuildPasteImport?.importFile?.(file, {
            clientPlacement: placement
        });
    }

    function updateHeader(snapshot) {
        const statusEl = document.getElementById('buildClientSyncStatus');
        const connectBtn = document.getElementById('buildClientSyncConnectBtn');
        const disconnectBtn = document.getElementById('buildClientSyncDisconnectBtn');
        if (statusEl) {
            statusEl.textContent = snapshot.connected
                ? `已连接 · ${snapshot.worldFileName || '等待配置'} · ${(snapshot.placements || []).length} 个投影`
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
        try {
            await api.connectDirectory();
            window.MCWWS_ShopCart?.showToast?.('已连接 .minecraft 文件夹', true);
        } catch (error) {
            window.MCWWS_ShopCart?.showToast?.(error.message || '连接失败', false);
        }
    }

    async function init() {
        const api = sync();
        const section = document.getElementById('buildClientSyncSection');
        if (!api || !section) return;

        if (!api.isSupported()) {
            section.querySelector('.build-client-sync-unsupported')?.removeAttribute('hidden');
            document.getElementById('buildClientSyncConnectBtn')?.setAttribute('disabled', 'disabled');
        }

        api.subscribe(render);

        document.getElementById('buildClientSyncConnectBtn')?.addEventListener('click', () => { void connect(); });
        document.getElementById('buildClientSyncDisconnectBtn')?.addEventListener('click', () => {
            void api.disconnect();
            window.MCWWS_ShopCart?.showToast?.('已断开客户端连接', true);
        });

        render(api.getSnapshot());
        await api.tryRestoreConnection();
    }

    window.MCWWS_BuildClientSyncUI = { init, render, importSelectedToPaste };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        void init();
    }
})();
