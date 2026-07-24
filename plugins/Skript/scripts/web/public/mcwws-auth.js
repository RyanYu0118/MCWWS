/**
 * 流浪世界 Web 统一登录（商店 / 地图 / 管理共用 localStorage authToken）
 */
(function () {
    const TOKEN_KEY = 'authToken';
    let authToken = null;
    let currentUser = null;
    let authMode = 'login';
    let bound = false;
    const listeners = new Set();

    function notify() {
        listeners.forEach((fn) => {
            try {
                fn({ token: authToken, user: currentUser });
            } catch (e) {
                console.warn('[mcwws-auth] listener error', e);
            }
        });
    }

    function readStoredToken() {
        try {
            return localStorage.getItem(TOKEN_KEY);
        } catch {
            return null;
        }
    }

    function writeStoredToken(token) {
        try {
            if (token) {
                localStorage.setItem(TOKEN_KEY, token);
            } else {
                localStorage.removeItem(TOKEN_KEY);
            }
        } catch {
            /* ignore */
        }
    }

    function authHeaders() {
        return authToken ? { Authorization: `Bearer ${authToken}` } : {};
    }

    function showDialog(modal) {
        if (!modal) return;
        modal.classList.remove('closing');
        modal.classList.add('active');
    }

    function hideDialog(modal) {
        if (!modal) return;
        if (!modal.classList.contains('active')) return;
        modal.classList.add('closing');
        modal.classList.remove('active');
        window.setTimeout(() => modal.classList.remove('closing'), 190);
    }

    function playerAvatarUrl(playerId, size) {
        const id = String(playerId || '').trim();
        if (!id) return '';
        const s = Math.min(Math.max(Number(size) || 40, 16), 128);
        return `/api/player-avatar/${encodeURIComponent(id)}?size=${s}`;
    }

    function buildAuthWidgetHtml(buttonId, isMap) {
        const avatarId = isMap ? 'mapAuthAvatar' : 'authAvatar';
        const popoverId = isMap ? 'mapAuthPopover' : 'authPopover';
        const titleId = isMap ? 'mapAuthPopoverTitle' : 'authPopoverTitle';
        const metaId = isMap ? 'mapAuthPopoverMeta' : 'authPopoverMeta';
        const economyId = isMap ? 'mapAuthPopoverEconomy' : 'authPopoverEconomy';
        const actionId = isMap ? 'mapAuthPopoverAction' : 'authPopoverAction';
        return `
            <button type="button" id="${buttonId}" class="mcwws-auth-avatar-btn" aria-label="账户" aria-expanded="false" aria-haspopup="true">
                <img id="${avatarId}" class="mcwws-auth-avatar-img" width="36" height="36" alt="" decoding="async" referrerpolicy="no-referrer">
                <span class="mcwws-auth-avatar-fallback" aria-hidden="true">👤</span>
            </button>
            <div id="${popoverId}" class="mcwws-auth-popover" role="tooltip" aria-hidden="true">
                <div class="mcwws-auth-popover-body">
                    <p id="${titleId}" class="mcwws-auth-popover-title">未登录</p>
                    <p id="${metaId}" class="mcwws-auth-popover-meta"></p>
                    <div id="${economyId}" class="mcwws-auth-popover-economy" hidden>
                        <p class="mcwws-auth-popover-economy-heading">经济系统</p>
                        <ul class="mcwws-auth-popover-economy-list">
                            <li><span class="label">在线时长</span><span class="value" data-economy="playHours">-</span></li>
                            <li><span class="label">当前零钱</span><span class="value" data-economy="balance">-</span></li>
                            <li><span class="label">当前身份</span><span class="value" data-economy="role">-</span></li>
                            <li class="has-progress">
                                <span class="label">完成进度</span>
                                <span class="value" data-economy="progress">-</span>
                            </li>
                        </ul>
                        <div class="mcwws-auth-popover-progress" data-economy="progressBar" hidden>
                            <div class="mcwws-auth-popover-progress-fill"></div>
                        </div>
                        <button type="button" class="mcwws-auth-ledger-btn" data-action="ledger">零钱明细</button>
                    </div>
                    ${isMap ? '<div id="mapAuthThemeSlot" class="mcwws-auth-popover-theme-row"></div><a href="home.html" class="mcwws-auth-popover-services-link">🏠 更多服务</a>' : ''}
                    <button type="button" id="${actionId}" class="mcwws-auth-popover-action">登录 / 注册</button>
                </div>
            </div>
        `;
    }

    function getAuthWidgetRefs(widget) {
        if (!widget) return null;
        const isMap = widget.id === 'mapAuthWidget';
        const btn = widget.querySelector('.mcwws-auth-avatar-btn');
        return {
            widget,
            btn,
            avatarImg: widget.querySelector('.mcwws-auth-avatar-img'),
            popover: widget.querySelector('.mcwws-auth-popover'),
            popoverTitle: document.getElementById(isMap ? 'mapAuthPopoverTitle' : 'authPopoverTitle'),
            popoverMeta: document.getElementById(isMap ? 'mapAuthPopoverMeta' : 'authPopoverMeta'),
            popoverEconomy: document.getElementById(isMap ? 'mapAuthPopoverEconomy' : 'authPopoverEconomy')
                || widget.querySelector('.mcwws-auth-popover-economy'),
            popoverAction: document.getElementById(isMap ? 'mapAuthPopoverAction' : 'authPopoverAction')
        };
    }

    function ensureEconomyDom(widget) {
        if (!widget) return null;
        let economy = widget.querySelector('.mcwws-auth-popover-economy');
        if (economy) return economy;
        const body = widget.querySelector('.mcwws-auth-popover-body');
        const action = widget.querySelector('.mcwws-auth-popover-action');
        if (!body || !action) return null;
        economy = document.createElement('div');
        economy.className = 'mcwws-auth-popover-economy';
        economy.hidden = true;
        economy.innerHTML = `
            <p class="mcwws-auth-popover-economy-heading">经济系统</p>
            <ul class="mcwws-auth-popover-economy-list">
                <li><span class="label">在线时长</span><span class="value" data-economy="playHours">-</span></li>
                <li><span class="label">当前零钱</span><span class="value" data-economy="balance">-</span></li>
                <li><span class="label">当前身份</span><span class="value" data-economy="role">-</span></li>
                <li class="has-progress">
                    <span class="label">完成进度</span>
                    <span class="value" data-economy="progress">-</span>
                </li>
            </ul>
            <div class="mcwws-auth-popover-progress" data-economy="progressBar" hidden>
                <div class="mcwws-auth-popover-progress-fill"></div>
            </div>
            <button type="button" class="mcwws-auth-ledger-btn" data-action="ledger">零钱明细</button>
        `;
        body.insertBefore(economy, action);
        return economy;
    }

    function ensureAuthWidgetDom() {
        let widget = document.querySelector('.mcwws-auth-widget');
        if (widget) return getAuthWidgetRefs(widget);

        const status = document.getElementById('userStatus')
            || document.getElementById('mapUserStatus');
        const btn = document.getElementById('authButton')
            || document.getElementById('mapAuthButton');
        if (!btn) return null;

        const isMap = btn.id === 'mapAuthButton';
        widget = document.createElement('div');
        widget.className = 'mcwws-auth-widget';
        widget.id = isMap ? 'mapAuthWidget' : 'mcwwsAuthWidget';
        widget.innerHTML = buildAuthWidgetHtml(btn.id, isMap);

        if (isMap && window.MCWWS_COLOR_SCHEME?.mountSwitcher) {
            window.MCWWS_COLOR_SCHEME.mountSwitcher();
        }

        if (status) {
            status.replaceWith(widget);
            btn.remove();
        } else {
            btn.replaceWith(widget);
        }
        return getAuthWidgetRefs(widget);
    }

    function setAvatarImage(img, playerId) {
        if (!img) return;
        const btn = img.closest('.mcwws-auth-avatar-btn');
        const fallback = btn?.querySelector('.mcwws-auth-avatar-fallback');
        const clearFallbackState = () => {
            btn?.classList.remove('is-avatar-fallback');
            if (fallback) fallback.hidden = true;
        };
        const showFallback = () => {
            img.hidden = true;
            img.removeAttribute('src');
            btn?.classList.add('is-avatar-fallback');
            if (fallback) fallback.hidden = false;
        };
        const url = playerAvatarUrl(playerId, 40);
        img.onload = () => {
            clearFallbackState();
            img.hidden = false;
        };
        img.onerror = () => {
            showFallback();
        };
        if (url) {
            img.alt = playerId ? `${playerId} 的头像` : '';
            img.hidden = false;
            clearFallbackState();
            img.src = url;
        } else {
            img.alt = '';
            showFallback();
        }
    }

    const POPOVER_CLOSE_DELAY_MS = 320;
    /** 悬停弹层等非轮询场景下的短时缓存 */
    const ECONOMY_CACHE_MS = 8000;
    /** 登录后自动轮询零钱（与游戏内 eco 扣款/入账对齐，约 1 秒内可见） */
    const ECONOMY_POLL_MS = 500;
    /** 零钱明细弹窗打开时每秒刷新明细与余额 */
    const LEDGER_POLL_MS = 1000;
    const EMPTY_VALUE = '-';

    /** minecraftAE 未编码字符回退为 ASCII，避免显示乱码 */
    function sanitizeMcFontText(text) {
        return String(text ?? '')
            .replace(/\u00a5/g, '￥')
            .replace(/\u2014/g, '-')
            .replace(/\u2013/g, '-')
            .replace(/\u00b7/g, ' / ')
            .replace(/\u00d7/g, 'x');
    }
    let economyCache = null;
    let economyCacheAt = 0;
    let economyFetchPromise = null;
    let economyPollTimer = null;
    let lastRenderedBalanceText = '';
    let ledgerModalBound = false;
    let ledgerFetchPromise = null;
    let ledgerPollTimer = null;
    let ledgerFilter = 'exclude_flight';

    function ensureLedgerModal() {
        let modal = document.getElementById('mcwwsLedgerModal');
        if (modal) {
            return modal;
        }
        modal = document.createElement('div');
        modal.id = 'mcwwsLedgerModal';
        modal.className = 'mcwws-ledger-modal';
        modal.hidden = true;
        modal.innerHTML = `
            <div class="mcwws-ledger-backdrop" data-ledger-close="1"></div>
            <div class="mcwws-ledger-panel" role="dialog" aria-labelledby="mcwwsLedgerTitle" aria-modal="true">
                <header class="mcwws-ledger-header">
                    <h2 id="mcwwsLedgerTitle">零钱明细</h2>
                    <button type="button" class="mcwws-ledger-close" data-ledger-close="1" aria-label="关闭">×</button>
                </header>
                <div class="mcwws-ledger-summary" id="mcwwsLedgerSummary"></div>
                <div class="mcwws-ledger-filters" id="mcwwsLedgerFilters">
                    <button type="button" class="mcwws-ledger-filter is-active" data-filter="exclude_flight">不含飞行</button>
                    <button type="button" class="mcwws-ledger-filter" data-filter="all">全部</button>
                    <button type="button" class="mcwws-ledger-filter" data-filter="flight">仅飞行</button>
                </div>
                <div class="mcwws-ledger-table-wrap">
                    <table class="mcwws-ledger-table">
                        <thead>
                            <tr>
                                <th>时间</th>
                                <th>类型</th>
                                <th>说明</th>
                                <th>金额</th>
                                <th>余额</th>
                            </tr>
                        </thead>
                        <tbody id="mcwwsLedgerBody">
                            <tr><td colspan="5" class="mcwws-ledger-empty">加载中…</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        return modal;
    }

    function bindLedgerModalEvents() {
        if (ledgerModalBound) {
            return;
        }
        ledgerModalBound = true;
        const modal = ensureLedgerModal();
        modal.addEventListener('click', (e) => {
            if (e.target.closest('[data-ledger-close]')) {
                closeLedgerModal();
            }
        });
        modal.querySelector('#mcwwsLedgerFilters')?.addEventListener('click', (e) => {
            const btn = e.target.closest('[data-filter]');
            if (!btn) {
                return;
            }
            ledgerFilter = btn.dataset.filter || 'exclude_flight';
            modal.querySelectorAll('.mcwws-ledger-filter').forEach((el) => {
                el.classList.toggle('is-active', el === btn);
            });
            void refreshLedgerModal(true);
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                closeLedgerModal();
            }
        });
    }

    function formatLedgerTime(iso) {
        if (!iso) {
            return '-';
        }
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) {
            const raw = String(iso).trim().replace('T', ' ');
            if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(raw)) {
                return `${raw}.000`;
            }
            const dot = raw.indexOf('.');
            if (dot > 0) {
                const base = raw.slice(0, dot);
                const frac = raw.slice(dot + 1).replace(/\D/g, '').padEnd(3, '0').slice(0, 3);
                return `${base}.${frac}`;
            }
            return raw.slice(0, 23);
        }
        const pad = (n, width = 2) => String(n).padStart(width, '0');
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;
    }

    function renderLedgerSummary(summary, summaryAll, economyData) {
        const el = document.getElementById('mcwwsLedgerSummary');
        if (!el) {
            return;
        }
        const balanceText = economyData?.balanceFormatted
            || (economyData?.balance != null ? String(economyData.balance) : EMPTY_VALUE);
        const flight = summaryAll?.flightDebitTotal ?? summary?.flightDebitTotal ?? 0;
        const credit = summary?.creditTotal ?? 0;
        const debit = summary?.debitTotal ?? 0;
        el.innerHTML = `
            <div class="mcwws-ledger-stat is-balance"><span class="label">当前余额</span><span class="value is-balance">${sanitizeMcFontText(balanceText)}</span></div>
            <div class="mcwws-ledger-stat"><span class="label">本页收入</span><span class="value is-credit">+${sanitizeMcFontText(String(credit))}</span></div>
            <div class="mcwws-ledger-stat"><span class="label">本页支出</span><span class="value is-debit">-${sanitizeMcFontText(String(debit))}</span></div>
            <div class="mcwws-ledger-stat"><span class="label">飞行累计</span><span class="value is-flight">${sanitizeMcFontText(String(flight))}</span></div>
        `;
    }

    function renderLedgerRows(entries) {
        const body = document.getElementById('mcwwsLedgerBody');
        if (!body) {
            return;
        }
        if (!entries || !entries.length) {
            body.innerHTML = '<tr><td colspan="5" class="mcwws-ledger-empty">暂无明细记录</td></tr>';
            return;
        }
        body.innerHTML = entries.map((row) => {
            const signClass = row.direction === 'credit' ? 'is-credit' : 'is-debit';
            const amountText = sanitizeMcFontText(row.signedAmountFormatted || row.amountFormatted || String(row.amount));
            const balanceText = sanitizeMcFontText(row.balanceAfterFormatted || (row.balanceAfter != null ? String(row.balanceAfter) : '-'));
            const desc = sanitizeMcFontText(row.description || row.categoryLabel || '');
            const cat = sanitizeMcFontText(row.categoryLabel || row.category || '');
            return `<tr>
                <td>${formatLedgerTime(row.createdAt)}</td>
                <td>${cat}</td>
                <td>${desc}</td>
                <td class="mcwws-ledger-amount ${signClass}">${amountText}</td>
                <td>${balanceText}</td>
            </tr>`;
        }).join('');
    }

    async function fetchPlayerLedger(force) {
        if (!authToken) {
            return null;
        }
        if (!force && ledgerFetchPromise) {
            return ledgerFetchPromise;
        }
        ledgerFetchPromise = fetch(`/api/player-ledger?limit=40&filter=${encodeURIComponent(ledgerFilter)}`, {
            headers: authHeaders(),
            cache: 'no-store'
        }).then(async (response) => {
            if (!response.ok) {
                throw new Error('ledger fetch failed');
            }
            return response.json();
        }).catch(() => null).finally(() => {
            ledgerFetchPromise = null;
        });
        return ledgerFetchPromise;
    }

    async function refreshLedgerModal(force, silent) {
        bindLedgerModalEvents();
        const body = document.getElementById('mcwwsLedgerBody');
        if (!silent && body) {
            body.innerHTML = '<tr><td colspan="5" class="mcwws-ledger-empty">加载中…</td></tr>';
        }
        const [data, economyData] = await Promise.all([
            fetchPlayerLedger(force),
            fetchPlayerEconomy(true)
        ]);
        if (!data) {
            if (!silent && body) {
                body.innerHTML = '<tr><td colspan="5" class="mcwws-ledger-empty">加载失败，请稍后重试</td></tr>';
            }
            if (economyData) {
                renderLedgerSummary(null, null, economyData);
            }
            return;
        }
        renderLedgerSummary(data.summary, data.summaryAll, economyData);
        renderLedgerRows(data.entries);
    }

    function stopLedgerPolling() {
        if (ledgerPollTimer) {
            window.clearInterval(ledgerPollTimer);
            ledgerPollTimer = null;
        }
    }

    function startLedgerPolling() {
        stopLedgerPolling();
        ledgerPollTimer = window.setInterval(() => {
            const modal = document.getElementById('mcwwsLedgerModal');
            if (!modal || modal.hidden || !authToken || document.hidden) {
                return;
            }
            void refreshLedgerModal(true, true);
        }, LEDGER_POLL_MS);
    }

    function openLedgerModal() {
        if (!currentUser) {
            openAuthModal();
            return;
        }
        bindLedgerModalEvents();
        const modal = ensureLedgerModal();
        modal.hidden = false;
        document.body.classList.add('mcwws-ledger-open');
        void refreshLedgerModal(true, false);
        startLedgerPolling();
    }

    function closeLedgerModal() {
        stopLedgerPolling();
        const modal = document.getElementById('mcwwsLedgerModal');
        if (!modal) {
            return;
        }
        modal.hidden = true;
        document.body.classList.remove('mcwws-ledger-open');
    }

    function bindLedgerButtons(widget) {
        if (!widget || widget.dataset.ledgerBound === '1') {
            return;
        }
        widget.dataset.ledgerBound = '1';
        widget.addEventListener('click', (e) => {
            const btn = e.target.closest('[data-action="ledger"]');
            if (!btn) {
                return;
            }
            e.preventDefault();
            e.stopPropagation();
            openLedgerModal();
        });
    }

    function formatPlayHours(hours) {
        if (hours == null || !Number.isFinite(Number(hours))) return EMPTY_VALUE;
        return `${Math.floor(Number(hours))}h`;
    }

    function formatProgressText(progress, max) {
        if (progress == null || !Number.isFinite(Number(progress))) return EMPTY_VALUE;
        const limit = Number.isFinite(Number(max)) ? Number(max) : 100;
        return `${Math.max(0, Math.floor(Number(progress)))}/${limit}`;
    }

    function renderEconomyUi(refs, data) {
        const economy = refs?.popoverEconomy || ensureEconomyDom(refs?.widget);
        if (!economy) return;

        if (!currentUser) {
            economy.hidden = true;
            return;
        }

        economy.hidden = false;

        if (!data) {
            economy.querySelectorAll('.value[data-economy]').forEach((el) => {
                el.textContent = EMPTY_VALUE;
                el.classList.remove('is-op');
            });
            const progressBar = economy.querySelector('[data-economy="progressBar"]');
            const progressFill = progressBar?.querySelector('.mcwws-auth-popover-progress-fill');
            if (progressBar) progressBar.hidden = true;
            if (progressFill) progressFill.style.width = '0%';
            return;
        }
        const playHoursEl = economy.querySelector('[data-economy="playHours"]');
        const balanceEl = economy.querySelector('[data-economy="balance"]');
        const roleEl = economy.querySelector('[data-economy="role"]');
        const progressEl = economy.querySelector('[data-economy="progress"]');
        const progressBar = economy.querySelector('[data-economy="progressBar"]');
        const progressFill = progressBar?.querySelector('.mcwws-auth-popover-progress-fill');

        if (playHoursEl) playHoursEl.textContent = formatPlayHours(data.playHours);
        if (balanceEl) {
            const balanceText = data.balanceFormatted
                || (data.balance != null ? String(data.balance) : EMPTY_VALUE);
            const displayBalance = sanitizeMcFontText(balanceText);
            if (displayBalance !== lastRenderedBalanceText) {
                if (lastRenderedBalanceText && displayBalance !== EMPTY_VALUE) {
                    balanceEl.classList.add('is-balance-updated');
                    window.setTimeout(() => {
                        balanceEl.classList.remove('is-balance-updated');
                    }, 700);
                }
                lastRenderedBalanceText = displayBalance;
            }
            balanceEl.textContent = displayBalance;
        }
        if (roleEl) {
            roleEl.textContent = sanitizeMcFontText(
                data.role || (data.isOp ? '管理员/服主' : '普通玩家')
            );
            roleEl.classList.toggle('is-op', Boolean(data.isOp));
        }
        if (progressEl) {
            progressEl.textContent = formatProgressText(data.warningProgress, data.warningMax);
        }

        const hasProgress = data.warningProgress != null && Number.isFinite(Number(data.warningProgress));
        if (progressBar && progressFill) {
            progressBar.hidden = !hasProgress;
            if (hasProgress) {
                const max = Number.isFinite(Number(data.warningMax)) ? Number(data.warningMax) : 100;
                const pct = Math.max(0, Math.min(100, (Number(data.warningProgress) / max) * 100));
                progressFill.style.width = `${pct}%`;
            } else {
                progressFill.style.width = '0%';
            }
        }

        const meta = refs?.popoverMeta;
        if (meta && currentUser?.playerId) {
            const parts = [`游戏 ID: ${currentUser.playerId}`];
            if (data.online === true) parts.push('在线');
            else if (data.online === false) parts.push('离线');
            meta.textContent = parts.join(' / ');
            meta.hidden = false;
        }
    }

    function clearEconomyUi(refs) {
        economyCache = null;
        economyCacheAt = 0;
        lastRenderedBalanceText = '';
        const economy = refs?.popoverEconomy || ensureEconomyDom(refs?.widget);
        if (economy) economy.hidden = true;
    }

    function stopEconomyPolling() {
        if (economyPollTimer) {
            window.clearInterval(economyPollTimer);
            economyPollTimer = null;
        }
    }

    async function tickEconomyPoll() {
        if (!authToken || !currentUser?.playerId || document.hidden) {
            return;
        }
        const data = await fetchPlayerEconomy(true);
        if (!data) return;
        const refs = ensureAuthWidgetDom();
        renderEconomyUi(refs, data);
        if (refs?.popoverMeta && currentUser?.playerId) {
            const parts = [`游戏 ID: ${currentUser.playerId}`];
            if (data.online === true) parts.push('在线');
            else if (data.online === false) parts.push('离线');
            refs.popoverMeta.textContent = parts.join(' / ');
        }
    }

    function startEconomyPolling() {
        stopEconomyPolling();
        if (!authToken || !currentUser?.playerId) {
            return;
        }
        void tickEconomyPoll();
        economyPollTimer = window.setInterval(() => {
            void tickEconomyPoll();
        }, ECONOMY_POLL_MS);
    }

    async function fetchPlayerEconomy(force) {
        if (!authToken || !currentUser?.playerId) {
            return null;
        }
        const now = Date.now();
        if (!force && economyCache && now - economyCacheAt < ECONOMY_CACHE_MS) {
            return economyCache;
        }
        if (economyFetchPromise) {
            return economyFetchPromise;
        }
        economyFetchPromise = fetch('/api/player-economy', {
            headers: authHeaders(),
            cache: 'no-store'
        }).then(async (response) => {
            if (!response.ok) {
                throw new Error('economy fetch failed');
            }
            const data = await response.json();
            economyCache = data;
            economyCacheAt = Date.now();
            return data;
        }).catch(() => null).finally(() => {
            economyFetchPromise = null;
        });
        return economyFetchPromise;
    }

    function applyEconomySnapshot(patch) {
        if (!patch || (patch.balance == null && patch.balanceFormatted == null)) {
            return;
        }
        economyCache = {
            ...(economyCache || {}),
            ...patch
        };
        economyCacheAt = Date.now();
        const refs = ensureAuthWidgetDom();
        if (refs) {
            renderEconomyUi(refs, economyCache);
        }
    }

    async function refreshEconomyForPopover(refs, force) {
        if (!currentUser) {
            clearEconomyUi(refs);
            return;
        }
        ensureEconomyDom(refs?.widget);
        const cached = !force && economyCache && Date.now() - economyCacheAt < ECONOMY_CACHE_MS
            ? economyCache
            : null;
        if (cached) {
            renderEconomyUi(refs, cached);
            return;
        }
        renderEconomyUi(refs, economyCache);
        const data = await fetchPlayerEconomy(force);
        if (data) {
            renderEconomyUi(refs, data);
        }
    }

    function clearAuthPopoverCloseTimer(widget) {
        if (!widget) return;
        if (widget._popoverCloseTimer) {
            window.clearTimeout(widget._popoverCloseTimer);
            widget._popoverCloseTimer = 0;
        }
    }

    function showAuthPopover(refs) {
        if (!refs?.widget) return;
        clearAuthPopoverCloseTimer(refs.widget);
        refs.widget.classList.add('is-popover-hover');
        refs.btn?.setAttribute('aria-expanded', 'true');
        refs.popover?.setAttribute('aria-hidden', 'false');
        if (currentUser) {
            void refreshEconomyForPopover(refs);
        }
    }

    function closeAuthPopover(refs) {
        if (!refs?.widget) return;
        clearAuthPopoverCloseTimer(refs.widget);
        refs.widget.classList.remove('is-popover-open', 'is-popover-hover');
        refs.btn?.setAttribute('aria-expanded', 'false');
        refs.popover?.setAttribute('aria-hidden', 'true');
    }

    function scheduleCloseAuthPopover(refs) {
        if (!refs?.widget) return;
        clearAuthPopoverCloseTimer(refs.widget);
        refs.widget._popoverCloseTimer = window.setTimeout(() => {
            refs.widget._popoverCloseTimer = 0;
            if (refs.widget.classList.contains('is-popover-open')) return;
            refs.widget.classList.remove('is-popover-hover');
            refs.btn?.setAttribute('aria-expanded', 'false');
            refs.popover?.setAttribute('aria-hidden', 'true');
        }, POPOVER_CLOSE_DELAY_MS);
    }

    function updateStatusUi() {
        const refs = ensureAuthWidgetDom();
        if (!refs) return;

        const { widget, btn, avatarImg, popoverTitle, popoverMeta, popoverAction } = refs;
        ensureEconomyDom(widget);

        if (currentUser) {
            widget.classList.add('is-logged-in');
            setAvatarImage(avatarImg, currentUser.playerId);
            if (popoverTitle) {
                popoverTitle.textContent = currentUser.username || currentUser.playerId || '已登录';
            }
            if (popoverMeta) {
                const parts = [];
                if (currentUser.playerId) {
                    parts.push(`游戏 ID: ${currentUser.playerId}`);
                }
                if (economyCache?.online) {
                    parts.push('在线');
                } else if (economyCache && economyCache.online === false) {
                    parts.push('离线');
                }
                popoverMeta.textContent = parts.join(' / ');
                popoverMeta.hidden = parts.length === 0;
            }
            if (popoverAction) {
                popoverAction.textContent = '退出登录';
                popoverAction.dataset.action = 'logout';
            }
            if (btn) {
                btn.title = `${currentUser.username || currentUser.playerId} - 悬停查看账户`;
            }
            renderEconomyUi(refs, economyCache);
        } else {
            widget.classList.remove('is-logged-in');
            setAvatarImage(avatarImg, '');
            clearEconomyUi(refs);
            if (popoverTitle) popoverTitle.textContent = '未登录';
            if (popoverMeta) {
                popoverMeta.textContent = '登录后可定位玩家、使用商店等功能';
                popoverMeta.hidden = false;
            }
            if (popoverAction) {
                popoverAction.textContent = '登录 / 注册';
                popoverAction.dataset.action = 'login';
            }
            if (btn) {
                btn.title = '登录 / 注册（全站只需登录一次）';
            }
        }
        closeAuthPopover(refs);
    }

    function switchAuthMode(mode) {
        authMode = mode === 'register' ? 'register' : 'login';
        document.getElementById('authModeLogin')?.classList.toggle('active', authMode === 'login');
        document.getElementById('authModeRegister')?.classList.toggle('active', authMode === 'register');
        const title = document.getElementById('authModalTitle');
        if (title) {
            title.textContent = authMode === 'login' ? '登录' : '注册';
        }
        document.querySelectorAll('.auth-register-only').forEach((el) => {
            el.style.display = authMode === 'register' ? 'flex' : 'none';
        });
        const message = document.getElementById('authMessage');
        if (message) {
            message.textContent = '';
            message.style.color = '';
        }
    }

    function openAuthModal() {
        authMode = 'login';
        switchAuthMode('login');
        const form = document.getElementById('authForm');
        form?.reset();
        showDialog(document.getElementById('authModal'));
    }

    function closeAuthModal() {
        hideDialog(document.getElementById('authModal'));
        const message = document.getElementById('authMessage');
        if (message) {
            message.textContent = '';
            message.style.color = '';
        }
    }

    async function handleAuthSubmit(event) {
        event.preventDefault();
        const username = document.getElementById('authUsername')?.value.trim();
        const password = document.getElementById('authPassword')?.value;
        const playerId = document.getElementById('authPlayerId')?.value.trim();
        const message = document.getElementById('authMessage');

        if (!username || !password) {
            if (message) message.textContent = '请填写用户名和密码。';
            return;
        }
        if (authMode === 'register' && !playerId) {
            if (message) message.textContent = '注册时请填写游戏玩家 ID。';
            return;
        }

        const endpoint = authMode === 'register' ? '/api/register' : '/api/login';
        const payload = authMode === 'register'
            ? { username, password, playerId }
            : { username, password };

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await response.json();
            if (!response.ok) {
                if (message) {
                    message.textContent = result.error || '操作失败，请重试。';
                    message.style.color = 'var(--danger, #dc2626)';
                }
                return;
            }
            authToken = result.authToken;
            currentUser = { username: result.username, playerId: result.playerId };
            writeStoredToken(authToken);
            economyCache = null;
            economyCacheAt = 0;
            updateStatusUi();
            notify();
            closeAuthModal();
            startEconomyPolling();
            void fetchPlayerEconomy(true).then((data) => {
                if (!data) return;
                const refs = ensureAuthWidgetDom();
                renderEconomyUi(refs, data);
            });
        } catch (error) {
            if (message) {
                message.textContent = `网络错误：${error.message || '请检查服务器是否已启动。'}`;
                message.style.color = 'var(--danger, #dc2626)';
            }
        }
    }

    async function logout() {
        if (authToken) {
            try {
                await fetch('/api/logout', {
                    method: 'POST',
                    headers: authHeaders()
                });
            } catch {
                /* ignore */
            }
        }
        authToken = null;
        currentUser = null;
        economyCache = null;
        economyCacheAt = 0;
        stopEconomyPolling();
        writeStoredToken(null);
        updateStatusUi();
        notify();
    }

    async function loadProfile() {
        authToken = readStoredToken();
        if (!authToken) {
            currentUser = null;
            stopEconomyPolling();
            updateStatusUi();
            notify();
            return null;
        }
        try {
            const response = await fetch('/api/profile', {
                headers: authHeaders(),
                cache: 'no-store'
            });
            if (!response.ok) {
                throw new Error('未登录');
            }
            currentUser = await response.json();
            startEconomyPolling();
            void fetchPlayerEconomy(true).then((data) => {
                if (!data) return;
                const refs = ensureAuthWidgetDom();
                renderEconomyUi(refs, data);
            });
        } catch {
            authToken = null;
            currentUser = null;
            stopEconomyPolling();
            writeStoredToken(null);
        }
        updateStatusUi();
        notify();
        return currentUser;
    }

    function bindAuthWidgetEvents(refs) {
        if (!refs || refs.widget.dataset.bound === '1') return;
        refs.widget.dataset.bound = '1';

        const { widget, btn, popover, popoverAction } = refs;
        const canHover = () => window.matchMedia('(hover: hover)').matches;

        const bindHoverOpen = (el) => {
            el?.addEventListener('mouseenter', () => {
                if (!canHover()) return;
                showAuthPopover(refs);
            });
            el?.addEventListener('mouseleave', () => {
                if (!canHover()) return;
                scheduleCloseAuthPopover(refs);
            });
        };
        bindHoverOpen(widget);
        bindHoverOpen(popover);

        btn?.addEventListener('click', () => {
            if (widget.id === 'mapAuthWidget') {
                const open = widget.classList.toggle('is-popover-open');
                widget.classList.toggle('is-popover-hover', open);
                btn.setAttribute('aria-expanded', open ? 'true' : 'false');
                refs.popover?.setAttribute('aria-hidden', open ? 'false' : 'true');
                if (open && currentUser) {
                    void refreshEconomyForPopover(refs);
                }
                return;
            }
            if (currentUser) {
                if (!canHover()) {
                    widget.classList.toggle('is-popover-open');
                    const open = widget.classList.contains('is-popover-open');
                    btn.setAttribute('aria-expanded', open ? 'true' : 'false');
                    refs.popover?.setAttribute('aria-hidden', open ? 'false' : 'true');
                    if (open) {
                        void refreshEconomyForPopover(refs);
                    }
                }
                return;
            }
            openAuthModal();
        });

        popoverAction?.addEventListener('click', (e) => {
            e.stopPropagation();
            if (popoverAction.dataset.action === 'logout') {
                void logout();
            } else {
                openAuthModal();
            }
            closeAuthPopover(refs);
        });

        document.addEventListener('click', (e) => {
            if (!widget.classList.contains('is-popover-open')) return;
            if (widget.contains(e.target)) return;
            closeAuthPopover(refs);
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') closeAuthPopover(refs);
        });
    }

    function bindDom() {
        if (bound) return;
        bound = true;

        const refs = ensureAuthWidgetDom();
        bindAuthWidgetEvents(refs);
        bindLedgerButtons(refs?.widget);

        document.getElementById('authModeLogin')?.addEventListener('click', () => switchAuthMode('login'));
        document.getElementById('authModeRegister')?.addEventListener('click', () => switchAuthMode('register'));
        document.getElementById('authForm')?.addEventListener('submit', handleAuthSubmit);
        document.querySelector('#authModal .modal-close')?.addEventListener('click', closeAuthModal);
        document.querySelector('#authModal .btn-ghost')?.addEventListener('click', closeAuthModal);
        document.getElementById('authModal')?.addEventListener('click', (e) => {
            if (e.target.id === 'authModal') closeAuthModal();
        });
    }

    window.openAuthModal = openAuthModal;
    window.closeAuthModal = closeAuthModal;

    window.MCWWS_AUTH = {
        TOKEN_KEY,
        init() {
            bindDom();
            return loadProfile();
        },
        getToken: () => authToken,
        getUser: () => currentUser,
        headers: authHeaders,
        openModal: openAuthModal,
        closeModal: closeAuthModal,
        logout,
        refresh: loadProfile,
        applyEconomySnapshot,
        refreshEconomy(force) {
            const refs = ensureAuthWidgetDom();
            return refreshEconomyForPopover(refs, Boolean(force));
        },
        openLedgerModal,
        closeLedgerModal,
        refreshLedger: refreshLedgerModal,
        startEconomyPolling,
        stopEconomyPolling,
        onChange(fn) {
            listeners.add(fn);
            return () => listeners.delete(fn);
        }
    };

    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            return;
        }
        if (authToken && currentUser?.playerId) {
            void tickEconomyPoll();
            startEconomyPolling();
        }
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            if (document.getElementById('authModal')) {
                bindDom();
            }
        }, { once: true });
    }
})();
