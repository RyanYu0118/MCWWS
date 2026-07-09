/**
 * Litematica 投影粘贴：上传 .litematic → 报价 → 付款 → 网页设锚点 → 在线时网页粘贴
 */
(function () {
    let modalOpen = false;
    let lastQuote = null;
    let lastCheckout = null;
    let lastClientPlacement = null;
    let pastePollTimer = null;

    function formatMoney(value) {
        const n = Number(value);
        if (!Number.isFinite(n)) return '￥0.00';
        return `￥${n.toFixed(2)}`;
    }

    function quoteCheckoutAmounts(quote) {
        const material = Number(quote?.materialTotal);
        const fee = Number(quote?.buildServiceFee);
        const total = Number(quote?.checkoutTotal ?? quote?.purchasableTotal);
        const materialTotal = Number.isFinite(material)
            ? material
            : (Number.isFinite(total) && Number.isFinite(fee) ? total - fee : total);
        const buildServiceFee = Number.isFinite(fee) ? fee : 0;
        const checkoutTotal = Number.isFinite(total) ? total : materialTotal + buildServiceFee;
        return {
            materialTotal: Math.max(0, materialTotal),
            buildServiceFee: Math.max(0, buildServiceFee),
            checkoutTotal: Math.max(0, checkoutTotal)
        };
    }

    function getDeps() {
        return window.MCWWS_BuildPasteDeps || window.MCWWS_LitematicaDeps || {};
    }

    function getAuth() {
        return window.MCWWS_AUTH;
    }

    function getSyncApi() {
        return window.MCWWS_LitematicaClientSync;
    }

    function defaultWorldName(placement) {
        return String(placement?.worldHint || 'world').trim() || 'world';
    }

    function buildAnchorPayload(placement, worldName) {
        const sync = getSyncApi();
        if (sync?.buildAnchorFromPlacement) {
            return sync.buildAnchorFromPlacement(placement, worldName);
        }
        if (!placement?.origin) return null;
        const world = String(worldName || placement.worldHint || 'world').trim();
        return {
            world,
            x: Math.trunc(Number(placement.origin.x) || 0),
            y: Math.trunc(Number(placement.origin.y) || 0),
            z: Math.trunc(Number(placement.origin.z) || 0),
            yaw: 0,
            pitch: 0
        };
    }

    async function postPasteAnchor({ pasteOrderId, contentHash, anchor, rotation, mirror, source }) {
        const auth = getAuth();
        if (!auth?.getToken?.()) {
            throw new Error('请先登录');
        }
        const res = await fetch('/api/build/paste/anchor', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...auth.headers()
            },
            body: JSON.stringify({
                pasteOrderId,
                contentHash,
                world: anchor.world,
                x: anchor.x,
                y: anchor.y,
                z: anchor.z,
                yaw: anchor.yaw ?? 0,
                pitch: anchor.pitch ?? 0,
                rotation,
                mirror,
                source: source || 'web-litematica-sync'
            })
        });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || '设置锚点失败');
        }
        return data;
    }

    async function postPasteTrigger({ pasteOrderId, contentHash }) {
        const auth = getAuth();
        if (!auth?.getToken?.()) {
            throw new Error('请先登录');
        }
        const res = await fetch('/api/build/paste/trigger', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...auth.headers()
            },
            body: JSON.stringify({ pasteOrderId, contentHash })
        });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || '网页粘贴入队失败');
        }
        return data;
    }

    async function fetchPasteOrder(pasteOrderId) {
        const auth = getAuth();
        if (!auth?.getToken?.()) return null;
        const res = await fetch('/api/build/paste/orders', { headers: auth.headers() });
        const data = await res.json();
        if (!res.ok) return null;
        return (data.orders || []).find((o) => String(o.pasteOrderId) === String(pasteOrderId)) || null;
    }

    function stopPastePoll() {
        if (pastePollTimer) {
            clearInterval(pastePollTimer);
            pastePollTimer = null;
        }
    }

    function startPastePoll(pasteOrderId, onUpdate) {
        stopPastePoll();
        pastePollTimer = setInterval(async () => {
            const order = await fetchPasteOrder(pasteOrderId);
            if (!order) return;
            onUpdate?.(order);
            const queue = order.webPasteQueue || '';
            if (order.status === 'completed') {
                stopPastePoll();
            } else if (order.status === 'failed' || queue === 'failed') {
                stopPastePoll();
            } else if (order.status !== 'pasting' && queue !== 'pending' && queue !== 'processing') {
                stopPastePoll();
            }
        }, 2000);
    }

    async function triggerWebPaste(pasteOrderId, contentHash, ctx) {
        const showToast = getDeps().showToast || (() => {});
        try {
            const result = await postPasteTrigger({ pasteOrderId, contentHash });
            showToast(result.message || '已加入粘贴队列', true);
            if (lastCheckout && String(lastCheckout.pasteOrderId) === String(pasteOrderId)) {
                lastCheckout = {
                    ...lastCheckout,
                    webPasteQueue: result.webPasteQueue || 'pending',
                    status: result.status
                };
                renderCheckoutResult(lastCheckout, ctx);
            }
            startPastePoll(pasteOrderId, (order) => {
                if (lastCheckout && String(lastCheckout.pasteOrderId) === String(pasteOrderId)) {
                    lastCheckout = { ...lastCheckout, ...order };
                    renderCheckoutResult(lastCheckout, ctx);
                }
            });
            return result;
        } catch (error) {
            showToast(error.message || '网页粘贴失败', false);
            throw error;
        }
    }

    async function setAnchorForOrder(pasteOrderId, contentHash, placement, worldName) {
        const anchor = buildAnchorPayload(placement, worldName);
        if (!anchor) {
            throw new Error('当前投影缺少放置原点坐标');
        }
        return postPasteAnchor({
            pasteOrderId,
            contentHash,
            anchor,
            rotation: placement?.rotation,
            mirror: placement?.mirror,
            source: 'web-litematica-sync'
        });
    }

    function formatAnchorLine(anchor, escapeHtml) {
        if (!anchor?.world) return '未设置';
        const esc = escapeHtml || ((s) => String(s));
        return `${esc(anchor.world)} ${anchor.x}, ${anchor.y}, ${anchor.z}`;
    }

    function bindCheckoutAnchorForm(data, ctx) {
        const form = document.getElementById('buildPasteAnchorForm');
        const btn = document.getElementById('buildPasteSetAnchorBtn');
        if (!form || !btn) return;
        btn.addEventListener('click', async () => {
            const showToast = getDeps().showToast || (() => {});
            const worldInput = document.getElementById('buildPasteAnchorWorld');
            const world = worldInput?.value?.trim() || defaultWorldName(lastClientPlacement);
            const placement = lastClientPlacement || getSyncApi()?.getSelectedPlacement?.();
            if (!placement) {
                showToast('请先在客户端同步中选择投影', false);
                return;
            }
            btn.disabled = true;
            try {
                const result = await setAnchorForOrder(data.pasteOrderId, data.contentHash, placement, world);
                lastCheckout = { ...data, ...result, status: result.status, anchor: result.anchor };
                renderCheckoutResult(lastCheckout, ctx);
                showToast('锚点已通过网页设置', true);
            } catch (error) {
                showToast(error.message || '设置锚点失败', false);
            } finally {
                btn.disabled = false;
            }
        });
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
        stopPastePoll();
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
                <p class="litematica-import-note">未上架材料（含基岩）当前免费；商店材料费另收 100% 自动建造费。付款后可用坐标设锚点并网页粘贴，无需站到位。</p>
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
        const sync = getSyncApi();
        const placement = ctx.clientPlacement;
        const defaultWorld = placement ? defaultWorldName(placement) : 'world';
        const clientBlock = placement ? `
            <div class="build-paste-client-box">
                <p class="build-paste-client-title">客户端当前投影（Litematica）</p>
                <div class="build-paste-client-grid">
                    <span>名称 ${ctx.escapeHtml(placement.name || '—')}</span>
                    <span>原点 ${ctx.escapeHtml(sync?.formatPos?.(placement.origin) || '—')}</span>
                    <span>旋转 ${ctx.escapeHtml(sync?.labelRotation?.(placement.rotation) || placement.rotation || '无')}</span>
                    <span>镜像 ${ctx.escapeHtml(sync?.labelMirror?.(placement.mirror) || placement.mirror || '无')}</span>
                </div>
                <label class="build-paste-anchor-world-field">
                    <span>服务器世界名（Bukkit）</span>
                    <input type="text" id="buildPasteQuoteWorld" class="build-paste-anchor-world-input" value="${ctx.escapeHtml(defaultWorld)}" placeholder="world">
                </label>
                <p class="build-paste-client-warn">付款时将自动把 Litematica 放置原点写入订单锚点，无需进游戏。</p>
            </div>
        ` : '';

        const amounts = quoteCheckoutAmounts(quote);

        body.innerHTML = `
            <div class="litematica-quote-head">
                <h3 class="litematica-quote-title">${ctx.escapeHtml(title)}</h3>
                ${clientBlock}
                <p class="litematica-quote-meta">
                    非空气方块 ${quote.blockCount || 0} · 区域 ${quote.regionCount || 0}
                    · 材料 ${ctx.escapeHtml(formatMoney(amounts.materialTotal))}
                    · 建造费 ${ctx.escapeHtml(formatMoney(amounts.buildServiceFee))}
                    · 合计 ${ctx.escapeHtml(formatMoney(amounts.checkoutTotal))}
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
            const materialEl = document.getElementById('buildPasteMaterialTotal');
            const feeEl = document.getElementById('buildPasteServiceFee');
            const totalEl = document.getElementById('buildPasteTotal');
            const payBtn = document.getElementById('buildPastePayBtn');
            if (materialEl) materialEl.textContent = formatMoney(amounts.materialTotal);
            if (feeEl) feeEl.textContent = formatMoney(amounts.buildServiceFee);
            if (totalEl) totalEl.textContent = formatMoney(amounts.checkoutTotal);
            if (payBtn) {
                payBtn.disabled = false;
                payBtn.textContent = amounts.checkoutTotal > 0
                    ? `付款 ${formatMoney(amounts.checkoutTotal)} 并生成粘贴订单`
                    : '确认并生成粘贴订单（免费）';
            }
        }

        mountQuoteIcons(body, ctx);
    }

    function renderCheckoutResult(data, ctx) {
        const body = document.getElementById('buildPasteBody');
        const footer = document.getElementById('buildPasteFooter');
        if (!body) return;
        const sync = getSyncApi();
        const rotation = data.rotation || 'NONE';
        const mirror = data.mirror || 'NONE';
        const hasTransform = rotation !== 'NONE' || mirror !== 'NONE';
        const hasAnchor = data.anchor && data.anchor.world;
        const transformBlock = hasTransform ? `
            <div class="build-paste-client-box">
                <p class="build-paste-client-title">粘贴变换（与 Litematica 一致）</p>
                <div class="build-paste-client-grid">
                    <span>旋转 ${ctx.escapeHtml(sync?.labelRotation?.(rotation) || rotation)}</span>
                    <span>镜像 ${ctx.escapeHtml(sync?.labelMirror?.(mirror) || mirror)}</span>
                </div>
            </div>
        ` : '';
        const anchorBlock = hasAnchor ? `
            <div class="build-paste-client-box build-paste-anchor-set">
                <p class="build-paste-client-title">锚点已设置（网页）</p>
                <p class="build-paste-anchor-line"><code>${ctx.escapeHtml(formatAnchorLine(data.anchor, ctx.escapeHtml))}</code></p>
            </div>
        ` : `
            <div class="build-paste-client-box" id="buildPasteAnchorForm">
                <p class="build-paste-client-title">设置粘贴锚点（无需进游戏）</p>
                <p class="litematica-import-note">连接客户端同步并选择投影，或手动填写坐标。</p>
                <label class="build-paste-anchor-world-field">
                    <span>世界名</span>
                    <input type="text" id="buildPasteAnchorWorld" class="build-paste-anchor-world-input" value="${ctx.escapeHtml(defaultWorldName(lastClientPlacement))}" placeholder="world">
                </label>
                <button type="button" class="cart-drawer-btn cart-drawer-btn--primary" id="buildPasteSetAnchorBtn">从 Litematica 同步设锚点</button>
            </div>
        `;
        const stepAnchor = hasAnchor
            ? `锚点已由网页设置：<strong>${ctx.escapeHtml(formatAnchorLine(data.anchor, ctx.escapeHtml))}</strong>`
            : '在上方点击「从 Litematica 同步设锚点」，或稍后在客户端同步面板绑定';
        const queue = data.webPasteQueue || '';
        const isPasting = data.status === 'pasting' || queue === 'pending' || queue === 'processing';
        const isDone = data.status === 'completed';
        const isFailed = data.status === 'failed' || queue === 'failed';
        let pasteActionBlock = '';
        if (hasAnchor && !isDone) {
            if (isPasting) {
                pasteActionBlock = `
                    <div class="build-paste-client-box">
                        <p class="build-paste-client-title">网页粘贴进行中…</p>
                        <p class="litematica-import-note">服务器正在以你的身份执行粘贴，请保持在线…</p>
                    </div>`;
            } else if (isFailed) {
                pasteActionBlock = `
                    <div class="build-paste-client-box">
                        <p class="build-paste-client-title">粘贴失败</p>
                        <p class="litematica-import-error">${ctx.escapeHtml(data.webPasteQueueError || data.failureReason || '未知错误')}</p>
                        <button type="button" class="cart-drawer-btn cart-drawer-btn--primary" id="buildPasteWebTriggerBtn">重试网页粘贴</button>
                    </div>`;
            } else {
                pasteActionBlock = `
                    <div class="build-paste-client-box">
                        <button type="button" class="cart-drawer-btn cart-drawer-btn--primary" id="buildPasteWebTriggerBtn">网页粘贴（需在线）</button>
                        <p class="litematica-import-note">需使用下单账号登录服务器；粘贴期间将短暂切换旁观。也可进游戏执行 <code>/build paste ${ctx.escapeHtml(String(data.pasteOrderId))}</code></p>
                    </div>`;
            }
        }
        if (isDone) {
            pasteActionBlock = `
                <div class="build-paste-client-box build-paste-anchor-set">
                    <p class="build-paste-client-title">粘贴已完成</p>
                </div>`;
        }
        const checkoutAmounts = quoteCheckoutAmounts(data);
        body.innerHTML = `
            <div class="litematica-quote-head">
                <h3 class="litematica-quote-title">粘贴订单已创建</h3>
                <p class="litematica-quote-meta">订单 #${ctx.escapeHtml(String(data.pasteOrderId))} · 材料 ${ctx.escapeHtml(formatMoney(checkoutAmounts.materialTotal))} · 建造费 ${ctx.escapeHtml(formatMoney(checkoutAmounts.buildServiceFee))} · 合计 ${ctx.escapeHtml(formatMoney(data.total ?? checkoutAmounts.checkoutTotal))} · 状态 ${ctx.escapeHtml(data.status || '')}</p>
                ${transformBlock}
                ${anchorBlock}
                ${pasteActionBlock}
                <div class="build-paste-hash-box">
                    <span class="build-paste-hash-label">contentHash</span>
                    <code class="build-paste-hash-value">${ctx.escapeHtml(data.contentHash || '')}</code>
                </div>
            </div>
            <ol class="build-paste-steps">
                <li>确认 Litematica 加载的投影与上传文件一致（哈希相同）</li>
                <li>${stepAnchor}</li>
                <li>点击「网页粘贴」或进游戏执行 <code>/build paste</code>（需订单所属玩家在线）</li>
            </ol>
            <p class="litematica-import-note">粘贴凭证 ${ctx.escapeHtml(String(data.pasteTokenExpiresAt || ''))} 前有效。</p>
        `;
        if (footer) footer.hidden = true;
        if (!hasAnchor) {
            bindCheckoutAnchorForm(data, ctx);
        }
        document.getElementById('buildPasteWebTriggerBtn')?.addEventListener('click', async () => {
            const btn = document.getElementById('buildPasteWebTriggerBtn');
            if (btn) btn.disabled = true;
            try {
                await triggerWebPaste(data.pasteOrderId, data.contentHash, ctx);
            } finally {
                if (btn && !isPasting) btn.disabled = false;
            }
        });
        if (isPasting && !pastePollTimer) {
            startPastePoll(data.pasteOrderId, (order) => {
                lastCheckout = { ...data, ...order };
                renderCheckoutResult(lastCheckout, ctx);
            });
        }
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
            const placement = lastClientPlacement || getSyncApi()?.getSelectedPlacement?.();
            const worldInput = document.getElementById('buildPasteQuoteWorld')
                || document.getElementById('buildPasteAnchorWorld');
            const worldName = worldInput?.value?.trim() || defaultWorldName(placement);
            const payload = {
                quoteId: lastQuote.quoteId,
                contentHash: lastQuote.contentHash
            };
            if (placement) {
                if (placement.rotation) payload.rotation = placement.rotation;
                if (placement.mirror) payload.mirror = placement.mirror;
                const anchor = buildAnchorPayload(placement, worldName);
                if (anchor) payload.anchor = anchor;
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
            showToast(data.message || '订单已创建', true);
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
        importFile: (file, options) => handleFile(file, options || {}),
        setAnchorForOrder,
        triggerWebPaste,
        buildAnchorPayload,
        getLastCheckout: () => lastCheckout
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
