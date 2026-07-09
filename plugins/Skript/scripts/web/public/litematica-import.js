/**
 * Litematica 材料清单导入：解析 JSON/TXT，请求服务端计价，加入购物车
 */
(function () {
    const Parser = window.MCWWSMaterialListParser;
    if (!Parser) {
        console.warn('[Litematica] material-list-parser.js 未加载');
        return;
    }

    let importModalOpen = false;
    let lastQuote = null;

    function formatMoney(value) {
        const n = Number(value);
        if (!Number.isFinite(n)) return '￥0.00';
        return `￥${n.toFixed(2)}`;
    }

    function getDeps() {
        return window.MCWWS_LitematicaDeps || {};
    }

    function resolveLineUnitPrice(line) {
        if (line.status === 'ok') {
            return { value: line.unitBuyPrice, reference: false };
        }
        if (line.catalogBuyPrice != null) {
            return { value: line.catalogBuyPrice, reference: true };
        }
        return { value: null, reference: false };
    }

    function resolveLineSubtotal(line) {
        if (line.status === 'ok') {
            return { value: line.lineTotal, reference: false };
        }
        if (line.estimatedLineTotal != null) {
            return { value: line.estimatedLineTotal, reference: true };
        }
        if (line.catalogBuyPrice != null && line.materialCount) {
            return {
                value: Math.round(line.catalogBuyPrice * line.materialCount * 100) / 100,
                reference: true
            };
        }
        return { value: null, reference: false };
    }

    function mountQuoteIcons(body, ctx) {
        body.querySelectorAll('.litematica-quote-icon').forEach((el) => {
            const itemId = el.getAttribute('data-item-id');
            const itemName = el.getAttribute('data-item-name');
            if (!itemId) return;
            el.innerHTML = ctx.getItemIconHtml(itemId, itemName || itemId);
        });
        ctx.mountItemIconsInContainer?.(body);
        if (window.McTextureAnim) {
            window.McTextureAnim.initInContainer(body);
        }
        if (window.McEnchantGlint) {
            window.McEnchantGlint.initInContainer(body);
        }
    }

    function openImportModal() {
        const modal = document.getElementById('litematicaImportModal');
        if (!modal) return;
        importModalOpen = true;
        modal.classList.add('active');
        modal.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
        resetImportModal();
    }

    function closeImportModal() {
        const modal = document.getElementById('litematicaImportModal');
        if (!modal) return;
        importModalOpen = false;
        modal.classList.remove('active');
        modal.setAttribute('aria-hidden', 'true');
        if (!document.getElementById('cartDrawer')?.classList.contains('is-open')) {
            document.body.style.overflow = '';
        }
        lastQuote = null;
    }

    function resetImportModal() {
        const body = document.getElementById('litematicaImportBody');
        const fileInput = document.getElementById('litematicaFileInput');
        if (fileInput) fileInput.value = '';
        if (body) {
            body.innerHTML = `
                <div class="litematica-dropzone" id="litematicaDropzone">
                    <p class="litematica-dropzone-title">拖放 Litematica 材料清单到此处</p>
                    <p class="litematica-dropzone-hint">支持 JSON（Raw Material List / Stormatica）与 TXT 表格导出</p>
                    <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost" id="litematicaPickFileBtn">选择文件</button>
                </div>
                <p class="litematica-import-note">将按当前商店「系统买入价」估算；不可网购的物品也会完整列出并显示参考价。</p>
            `;
            bindDropzone(body.querySelector('#litematicaDropzone'));
        }
        const footer = document.getElementById('litematicaImportFooter');
        if (footer) footer.hidden = true;
    }

    function bindDropzone(zone) {
        if (!zone) return;
        const pickBtn = zone.querySelector('#litematicaPickFileBtn');
        const fileInput = document.getElementById('litematicaFileInput');
        pickBtn?.addEventListener('click', () => fileInput?.click());
        zone.addEventListener('dragover', (e) => {
            e.preventDefault();
            zone.classList.add('is-dragover');
        });
        zone.addEventListener('dragleave', () => zone.classList.remove('is-dragover'));
        zone.addEventListener('drop', (e) => {
            e.preventDefault();
            zone.classList.remove('is-dragover');
            const file = e.dataTransfer?.files?.[0];
            if (file) void handleImportFile(file);
        });
    }

    async function handleImportFile(file) {
        const deps = getDeps();
        const showToast = deps.showToast || (() => {});
        const escapeHtml = deps.escapeHtml || ((s) => String(s));
        const getItemIconHtml = deps.getItemIconHtml || (() => '');
        const mountItemIconsInContainer = deps.mountItemIconsInContainer || (() => {});

        if (!file) return;
        const body = document.getElementById('litematicaImportBody');
        if (body) {
            body.innerHTML = '<div class="loading-spinner"></div><p style="text-align:center;color:var(--text-muted);margin-top:12px;">正在解析并计算价格…</p>';
        }

        try {
            const text = await file.text();
            const parsed = Parser.parseLitematicaMaterialFile(text, file.name);
            if (!parsed.materials.length) {
                throw new Error('未识别到任何材料条目，请确认导出格式为 JSON 或 Litematica 表格 TXT。');
            }

            const res = await fetch('/api/shop/material-quote', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    listName: parsed.listName || file.name,
                    materials: parsed.materials
                })
            });
            const quote = await res.json();
            if (!res.ok) {
                throw new Error(quote.error || '计价失败');
            }

            lastQuote = quote;
            renderQuote(quote, {
                escapeHtml,
                getItemIconHtml,
                mountItemIconsInContainer,
                listName: parsed.listName || file.name,
                fileName: file.name
            });
        } catch (error) {
            if (body) {
                body.innerHTML = `<div class="litematica-import-error">⚠️ ${escapeHtml(error.message || '导入失败')}</div>`;
            }
            showToast(error.message || '导入失败', false);
        }
    }

    function renderQuote(quote, ctx) {
        const body = document.getElementById('litematicaImportBody');
        const footer = document.getElementById('litematicaImportFooter');
        if (!body) return;

        const title = quote.listName || ctx.listName || ctx.fileName || '材料清单';
        const lines = Array.isArray(quote.lines) ? quote.lines : [];
        const purchasable = lines.filter((line) => line.status === 'ok');
        const unavailable = lines.length - purchasable.length;

        body.innerHTML = `
            <div class="litematica-quote-head">
                <h3 class="litematica-quote-title">${ctx.escapeHtml(title)}</h3>
                <p class="litematica-quote-meta">
                    共 ${lines.length} 种材料 · 可购买 ${purchasable.length} 种 · 不可购买 ${unavailable} 种
                    · 可购合计 ${ctx.escapeHtml(formatMoney(quote.purchasableTotal))}
                    ${quote.referenceTotal > 0 ? ` · 参考合计 ${ctx.escapeHtml(formatMoney(quote.referenceTotal))}` : ''}
                </p>
            </div>
            <div class="litematica-quote-table-wrap">
                <table class="litematica-quote-table">
                    <thead>
                        <tr>
                            <th>物品</th>
                            <th>数量</th>
                            <th>单价</th>
                            <th>小计</th>
                            <th>状态</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${lines.map((line) => {
                            const unit = resolveLineUnitPrice(line);
                            const subtotal = resolveLineSubtotal(line);
                            const iconId = line.itemId
                                || (Parser.normalizeMaterialId(line.label) || '');
                            const iconName = line.displayName || line.label || iconId;
                            const qty = line.materialCount || line.count || 0;
                            const packNote = line.status === 'ok' && line.productAmount > 1
                                ? `<div class="litematica-quote-pack">×${line.purchaseQuantity} 组（每组 ${line.productAmount}）</div>`
                                : '';
                            const unitHtml = unit.value != null
                                ? `<span class="${unit.reference ? 'litematica-price-ref' : ''}">${ctx.escapeHtml(formatMoney(unit.value))}${unit.reference ? '<small>参考</small>' : ''}</span>`
                                : '—';
                            const subtotalHtml = subtotal.value != null
                                ? `<span class="${subtotal.reference ? 'litematica-price-ref' : ''}">${ctx.escapeHtml(formatMoney(subtotal.value))}${subtotal.reference ? '<small>参考</small>' : ''}</span>`
                                : '—';
                            return `
                            <tr class="litematica-quote-row litematica-quote-row--${ctx.escapeHtml(line.status)}">
                                <td>
                                    <div class="litematica-quote-item">
                                        <div class="litematica-quote-icon" data-item-id="${ctx.escapeHtml(iconId)}" data-item-name="${ctx.escapeHtml(iconName)}"></div>
                                        <div class="litematica-quote-text">
                                            <div class="litematica-quote-name">${ctx.escapeHtml(line.displayName || line.label || line.itemId || '未知')}</div>
                                            ${line.itemId ? `<div class="litematica-quote-id">${ctx.escapeHtml(line.itemId)}</div>` : `<div class="litematica-quote-id">${ctx.escapeHtml(line.label || '')}</div>`}
                                        </div>
                                    </div>
                                </td>
                                <td>${qty}${packNote}</td>
                                <td>${unitHtml}</td>
                                <td>${subtotalHtml}</td>
                                <td><span class="litematica-status litematica-status--${ctx.escapeHtml(line.status)}">${ctx.escapeHtml(line.statusLabel || line.status)}</span></td>
                            </tr>`;
                        }).join('')}
                    </tbody>
                </table>
            </div>
            <div class="litematica-quote-summary">
                <div><span>可购材料合计</span><strong>${ctx.escapeHtml(formatMoney(quote.purchasableTotal))}</strong></div>
                ${quote.referenceTotal > 0 ? `<div><span>不可购材料参考合计</span><strong class="litematica-price-ref">${ctx.escapeHtml(formatMoney(quote.referenceTotal))}</strong></div>` : ''}
            </div>
            <button type="button" class="cart-drawer-btn cart-drawer-btn--ghost litematica-repick-btn" id="litematicaRepickBtn">重新选择文件</button>
        `;

        if (footer) {
            footer.hidden = false;
            const totalEl = document.getElementById('litematicaImportTotal');
            const addBtn = document.getElementById('litematicaAddCartBtn');
            if (totalEl) totalEl.textContent = formatMoney(quote.purchasableTotal);
            if (addBtn) {
                addBtn.disabled = purchasable.length === 0;
                addBtn.textContent = purchasable.length
                    ? `将 ${purchasable.length} 种可购材料加入购物车`
                    : '没有可加入购物车的材料';
            }
        }

        document.getElementById('litematicaRepickBtn')?.addEventListener('click', () => {
            document.getElementById('litematicaFileInput')?.click();
        });

        mountQuoteIcons(body, ctx);
    }

    function addQuotedMaterialsToCart() {
        const deps = getDeps();
        const showToast = deps.showToast || (() => {});
        const addToCart = deps.addToCart;
        const allItems = typeof deps.allItems === 'function' ? deps.allItems() : (deps.allItems || []);
        if (!lastQuote || !addToCart) return;

        let added = 0;
        (lastQuote.lines || []).forEach((line) => {
            if (line.status !== 'ok' || !line.itemId || !line.shopId || !line.slot) return;
            const item = allItems.find((i) => i.id === line.itemId);
            if (!item) return;
            const offer = (item.ultimateShopOffers || []).find((o) =>
                o.shopId === line.shopId && String(o.slot) === String(line.slot));
            if (!offer) return;
            addToCart(item, offer, line.purchaseQuantity || 1);
            added += 1;
        });

        if (added > 0) {
            showToast(`已将 ${added} 种材料加入购物车`, true);
            deps.openCartDrawer?.();
            closeImportModal();
        } else {
            showToast('没有可加入购物车的材料', false);
        }
    }

    function initLitematicaImport() {
        document.getElementById('litematicaImportBtn')?.addEventListener('click', openImportModal);
        document.getElementById('litematicaImportClose')?.addEventListener('click', closeImportModal);
        document.getElementById('litematicaImportModal')?.addEventListener('click', (e) => {
            if (e.target.id === 'litematicaImportModal') closeImportModal();
        });
        document.getElementById('litematicaAddCartBtn')?.addEventListener('click', addQuotedMaterialsToCart);
        document.getElementById('litematicaFileInput')?.addEventListener('change', (e) => {
            const file = e.target.files?.[0];
            if (file) void handleImportFile(file);
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && importModalOpen) closeImportModal();
        });
        resetImportModal();
    }

    window.MCWWS_LitematicaImport = {
        init: initLitematicaImport,
        open: openImportModal
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initLitematicaImport);
    } else {
        initLitematicaImport();
    }
})();
