/**
 * Litematica 投影粘贴：上传 .litematic → 报价 → 付款 → 游戏内 /build 粘贴
 */
(function () {
    let modalOpen = false;
    let lastQuote = null;
    let lastCheckout = null;
    let lastClientPlacement = null;

    function formatMoney(value) {
        const n = Number(value);
        if (!Number.isFinite(n)) return '￥0.00';
        return `￥${n.toFixed(2)}`;
    }

    function getDeps() {
        return window.MCWWS_BuildPasteDeps || window.MCWWS_LitematicaDeps || {};
    }

    function getAuth() {
        return window.MCWWS_AUTH;
    }

    function fileToBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                const result = String(reader.result || '');
                const comma = result.indexOf(',');
                resolve(comma >= 0 ? result.slice(comma + 1) : result);
            };
            reader.onerror = () => reject(reader.error || new Error('读取文件失败'));
            reader.readAsDataURL(file);
        });
    }

    function mountQuoteIcons(body, ctx) {
        body.querySelectorAll('.litematica-quote-icon').forEach((el) => {
            const itemId = el.getAttribute('data-item-id');
            const itemName = el.getAttribute('data-item-name');
            if (!itemId) return;
            el.innerHTML = ctx.getItemIconHtml(itemId, itemName || itemId);
        });
        ctx.mountItemIconsInContainer?.(body);
        window.McTextureAnim?.initInContainer?.(body);
        window.McEnchantGlint?.initInContainer?.(body);
    }

    function openModal() {
        const modal = document.getElementById('buildPasteModal');
        if (!modal) return;
        modalOpen = true;
        modal.classList.add('active');
        modal.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
        resetModal();
    }

    function closeModal() {
        const modal = document.getElementById('buildPasteModal');
        if (!modal) return;
        modalOpen = false;
        modal.classList.remove('active');
        modal.setAttribute('aria-hidden', 'true');
        if (!document.getElementById('cartDrawer')?.classList.contains('is-open')) {
            document.body.style.overflow = '';
        }
        lastQuote = null;
        lastCheckout = null;
        lastClientPlacement = null;
    }

    function resetModal() {
        const body = document.getElementById('buildPasteBody');
        const footer = document.getElementById('buildPasteFooter');
        const fileInput = document.getElementById('buildPasteFileInput');
        if (fileInput) fileInput.value = '';
        if (body) {
            body.innerHTML = `
                <div class="litematica-dropzone" id="buildPasteDropzone">
                    <p class="litematica-dropzone-title">拖放 .litematic 投影文件到此处</p>
                    <p class="litematica-dropzone-hint">须与游戏内 Litematica 加载的同一文件；上传后显示 contentHash 供核对</p>
                    <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost" id="buildPastePickBtn">选择 .litematic</button>
                </div>
                <p class="litematica-import-note">未上架材料（含基岩）当前免费；仅商店可购材料计费。付款后站到粘贴原点执行 <code>/build go &lt;订单号&gt;</code> 即可一步完成锚点与粘贴。</p>
            `;
            bindDropzone(body.querySelector('#buildPasteDropzone'));
        }
        if (footer) footer.hidden = true;
    }

    function bindDropzone(zone) {
        if (!zone) return;
        zone.querySelector('#buildPastePickBtn')?.addEventListener('click', () => {
            document.getElementById('buildPasteFileInput')?.click();
        });
        zone.addEventListener('dragover', (e) => {
            e.preventDefault();
            zone.classList.add('is-dragover');
        });
        zone.addEventListener('dragleave', () => zone.classList.remove('is-dragover'));
        zone.addEventListener('drop', (e) => {
            e.preventDefault();
            zone.classList.remove('is-dragover');
            const file = e.dataTransfer?.files?.[0];
            if (file) void handleFile(file);
        });
    }

    async function handleFile(file, options = {}) {
        lastClientPlacement = options.clientPlacement || null;
        const deps = getDeps();
        const showToast = deps.showToast || (() => {});
        const escapeHtml = deps.escapeHtml || ((s) => String(s));
        const getItemIconHtml = deps.getItemIconHtml || (() => '');
        const mountItemIconsInContainer = deps.mountItemIconsInContainer || (() => {});

        if (!file) return;
        const lower = String(file.name || '').toLowerCase();
        if (!lower.endsWith('.litematic')) {
            showToast('请选择 .litematic 文件', false);
            return;
        }

        const body = document.getElementById('buildPasteBody');
        if (body) {
            body.innerHTML = '<div class="loading-spinner"></div><p style="text-align:center;color:#94a3b8;margin-top:12px;">正在解析投影并计算报价…</p>';
        }

        try {
            const dataBase64 = await fileToBase64(file);
            const res = await fetch('/api/build/quote', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ dataBase64, fileName: file.name })
            });
            const quote = await res.json();
            if (!res.ok) {
                throw new Error(quote.error || '报价失败');
            }
            lastQuote = quote;
            lastCheckout = null;
            renderQuote(quote, {
                escapeHtml,
                getItemIconHtml,
                mountItemIconsInContainer,
                fileName: file.name,
                clientPlacement: lastClientPlacement
            });
        } catch (error) {
            if (body) {
                body.innerHTML = `<div class="litematica-import-error">⚠️ ${escapeHtml(error.message || '处理失败')}</div>`;
            }
            showToast(error.message || '处理失败', false);
        }
    }

    function renderQuote(quote, ctx) {
        const body = document.getElementById('buildPasteBody');
        const footer = document.getElementById('buildPasteFooter');
        if (!body) return;

        const lines = Array.isArray(quote.lines) ? quote.lines : [];
        const title = quote.listName || ctx.fileName || '投影粘贴';
        const hashShort = String(quote.contentHash || '').slice(0, 12);
        const sync = window.MCWWS_LitematicaClientSync;
        const placement = ctx.clientPlacement;
        const clientBlock = placement ? `
            <div class="build-paste-client-box">
                <p class="build-paste-client-title">客户端当前投影（Litematica）</p>
                <div class="build-paste-client-grid">
                    <span>名称 ${ctx.escapeHtml(placement.name || '—')}</span>
                    <span>原点 ${ctx.escapeHtml(sync?.formatPos?.(placement.origin) || '—')}</span>
                    <span>旋转 ${ctx.escapeHtml(sync?.labelRotation?.(placement.rotation) || placement.rotation || '无')}</span>
                    <span>镜像 ${ctx.escapeHtml(sync?.labelMirror?.(placement.mirror) || placement.mirror || '无')}</span>
                </div>
                ${(placement.rotation && placement.rotation !== 'NONE') || (placement.mirror && placement.mirror !== 'NONE')
                    ? '<p class="build-paste-client-warn">已记录旋转/镜像；付款后服务器粘贴将自动应用相同变换，请站在 Litematica 放置原点执行 /build go。</p>'
                    : ''}
            </div>
        ` : '';

        body.innerHTML = `
            <div class="litematica-quote-head">
                <h3 class="litematica-quote-title">${ctx.escapeHtml(title)}</h3>
                ${clientBlock}
                <p class="litematica-quote-meta">
                    非空气方块 ${quote.blockCount || 0} · 区域 ${quote.regionCount || 0}
                    · 可购 ${ctx.escapeHtml(formatMoney(quote.purchasableTotal))}
                    · 免费材料 ${quote.freeUnlistedCount || 0} 种
                </p>
                <div class="build-paste-hash-box">
                    <span class="build-paste-hash-label">contentHash</span>
                    <code class="build-paste-hash-value" id="buildPasteHashValue" title="${ctx.escapeHtml(quote.contentHash || '')}">${ctx.escapeHtml(quote.contentHash || '')}</code>
                    <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost build-paste-copy-hash" id="buildPasteCopyHash">复制</button>
                </div>
                <p class="build-paste-hash-hint">本地核对：<code>node plugins/Skript/scripts/web/tools/litematic-hash.js 你的文件.litematic</code></p>
            </div>
            <div class="litematica-quote-table-wrap">
                <table class="litematica-quote-table">
                    <thead>
                        <tr><th>物品</th><th>数量</th><th>计费</th><th>小计</th></tr>
                    </thead>
                    <tbody>
                        ${lines.map((line) => {
                            const iconId = line.itemId || '';
                            const iconName = line.displayName || line.label || iconId;
                            const billing = line.billing === 'free'
                                ? '<span class="litematica-status litematica-status--ok">免费</span>'
                                : ctx.escapeHtml(formatMoney(line.checkoutLineTotal ?? line.lineTotal ?? 0));
                            return `
                            <tr class="litematica-quote-row">
                                <td>
                                    <div class="litematica-quote-item">
                                        <div class="litematica-quote-icon" data-item-id="${ctx.escapeHtml(iconId)}" data-item-name="${ctx.escapeHtml(iconName)}"></div>
                                        <div class="litematica-quote-text">
                                            <div class="litematica-quote-name">${ctx.escapeHtml(line.displayName || line.label || '未知')}</div>
                                            <div class="litematica-quote-id">${ctx.escapeHtml(line.itemId || line.label || '')}</div>
                                        </div>
                                    </div>
                                </td>
                                <td>${line.materialCount || 0}</td>
                                <td>${line.billing === 'free' ? '免费' : '商店'}</td>
                                <td>${billing}</td>
                            </tr>`;
                        }).join('')}
                    </tbody>
                </table>
            </div>
            <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost litematica-repick-btn" id="buildPasteRepickBtn">重新选择文件</button>
        `;

        document.getElementById('buildPasteCopyHash')?.addEventListener('click', () => {
            const hash = quote.contentHash || '';
            navigator.clipboard?.writeText(hash).then(() => {
                getDeps().showToast?.('已复制 contentHash', true);
            }).catch(() => {
                getDeps().showToast?.('复制失败', false);
            });
        });
        document.getElementById('buildPasteRepickBtn')?.addEventListener('click', () => {
            document.getElementById('buildPasteFileInput')?.click();
        });

        if (footer) {
            footer.hidden = false;
            const totalEl = document.getElementById('buildPasteTotal');
            const payBtn = document.getElementById('buildPastePayBtn');
            if (totalEl) totalEl.textContent = formatMoney(quote.purchasableTotal);
            if (payBtn) {
                payBtn.disabled = false;
                payBtn.textContent = Number(quote.purchasableTotal) > 0
                    ? `付款 ${formatMoney(quote.purchasableTotal)} 并生成粘贴订单`
                    : '确认并生成粘贴订单（免费）';
            }
        }

        mountQuoteIcons(body, ctx);
    }

    function renderCheckoutResult(data, ctx) {
        const body = document.getElementById('buildPasteBody');
        const footer = document.getElementById('buildPasteFooter');
        if (!body) return;
        const sync = window.MCWWS_LitematicaClientSync;
        const rotation = data.rotation || 'NONE';
        const mirror = data.mirror || 'NONE';
        const hasTransform = rotation !== 'NONE' || mirror !== 'NONE';
        const transformBlock = hasTransform ? `
            <div class="build-paste-client-box">
                <p class="build-paste-client-title">粘贴变换（与 Litematica 一致）</p>
                <div class="build-paste-client-grid">
                    <span>旋转 ${ctx.escapeHtml(sync?.labelRotation?.(rotation) || rotation)}</span>
                    <span>镜像 ${ctx.escapeHtml(sync?.labelMirror?.(mirror) || mirror)}</span>
                </div>
            </div>
        ` : '';
        const anchorHint = hasTransform
            ? '站到 Litematica <strong>放置原点</strong>（变换中心，见客户端同步中的原点坐标）'
            : '站到建筑<strong>粘贴原点</strong>（投影最小角对应位置）';
        body.innerHTML = `
            <div class="litematica-quote-head">
                <h3 class="litematica-quote-title">粘贴订单已创建</h3>
                <p class="litematica-quote-meta">订单 #${ctx.escapeHtml(String(data.pasteOrderId))} · 金额 ${ctx.escapeHtml(formatMoney(data.total))}</p>
                ${transformBlock}
                <div class="build-paste-hash-box">
                    <span class="build-paste-hash-label">contentHash</span>
                    <code class="build-paste-hash-value">${ctx.escapeHtml(data.contentHash || '')}</code>
                </div>
            </div>
            <ol class="build-paste-steps">
                <li>确认 Litematica 加载的投影与上传文件一致（哈希相同）</li>
                <li>${anchorHint}</li>
                <li>执行 <code>/build go ${ctx.escapeHtml(String(data.pasteOrderId))}</code></li>
            </ol>
            <p class="litematica-import-note">也可分步执行 <code>/build anchor ${ctx.escapeHtml(String(data.pasteOrderId))}</code> 再 <code>/build paste ${ctx.escapeHtml(String(data.pasteOrderId))}</code>。粘贴凭证 ${ctx.escapeHtml(String(data.pasteTokenExpiresAt || ''))} 前有效；全程 contentHash 不变。</p>
        `;
        if (footer) footer.hidden = true;
    }

    async function checkoutQuote() {
        const auth = getAuth();
        const deps = getDeps();
        const showToast = deps.showToast || (() => {});
        if (!lastQuote) {
            showToast('请先上传投影文件', false);
            return;
        }
        if (!auth?.getToken?.()) {
            auth?.openModal?.();
            showToast('请先登录', false);
            return;
        }

        const payBtn = document.getElementById('buildPastePayBtn');
        if (payBtn) payBtn.disabled = true;

        try {
            const placement = lastClientPlacement;
            const payload = {
                quoteId: lastQuote.quoteId,
                contentHash: lastQuote.contentHash
            };
            if (placement) {
                if (placement.rotation) payload.rotation = placement.rotation;
                if (placement.mirror) payload.mirror = placement.mirror;
            }
            const res = await fetch('/api/build/checkout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...auth.headers()
                },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.error || '付款失败');
            }
            lastCheckout = data;
            if (data.balance != null) {
                auth.applyEconomySnapshot?.({ balance: data.balance, balanceFormatted: data.balanceFormatted });
            } else {
                void auth.refreshEconomy?.(true);
            }
            renderCheckoutResult(data, {
                escapeHtml: deps.escapeHtml || ((s) => String(s))
            });
            showToast(data.message || '订单已创建，请进游戏粘贴', true);
        } catch (error) {
            showToast(error.message || '付款失败', false);
        } finally {
            if (payBtn) payBtn.disabled = false;
        }
    }

    function init() {
        document.getElementById('buildPasteBtn')?.addEventListener('click', openModal);
        document.getElementById('buildPasteClose')?.addEventListener('click', closeModal);
        document.getElementById('buildPasteModal')?.addEventListener('click', (e) => {
            if (e.target.id === 'buildPasteModal') closeModal();
        });
        document.getElementById('buildPastePayBtn')?.addEventListener('click', () => void checkoutQuote());
        document.getElementById('buildPasteFileInput')?.addEventListener('change', (e) => {
            const file = e.target.files?.[0];
            if (file) void handleFile(file);
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && modalOpen) closeModal();
        });
        resetModal();
    }

    window.MCWWS_BuildPasteImport = {
        init,
        open: openModal,
        importFile: (file, options) => handleFile(file, options || {})
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
