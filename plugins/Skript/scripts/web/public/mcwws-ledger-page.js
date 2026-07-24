/**
 * 零钱明细独立页（/ledger.html）
 */
(function () {
    const LEDGER_POLL_MS = 1000;
    const LEDGER_LIMIT = 60;
    const EMPTY_VALUE = '-';

    let ledgerFilter = 'exclude_flight';
    let ledgerFetchPromise = null;
    let pollTimer = null;
    let filtersBound = false;

    function sanitizeMcFontText(text) {
        return String(text ?? '')
            .replace(/\u00a5/g, '￥')
            .replace(/\u2014/g, '-')
            .replace(/\u2013/g, '-')
            .replace(/\u00b7/g, ' / ')
            .replace(/\u00d7/g, 'x');
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

    function readFilterFromUrl() {
        const param = new URLSearchParams(window.location.search).get('filter');
        if (param === 'all' || param === 'flight' || param === 'exclude_flight') {
            ledgerFilter = param;
        }
        document.querySelectorAll('.mcwws-ledger-filter[data-filter]').forEach((el) => {
            el.classList.toggle('is-active', el.dataset.filter === ledgerFilter);
        });
    }

    function syncFilterToUrl() {
        const url = new URL(window.location.href);
        url.searchParams.set('filter', ledgerFilter);
        window.history.replaceState(null, '', url.pathname + url.search);
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
        const auth = window.MCWWS_AUTH;
        if (!auth?.getToken?.()) {
            return null;
        }
        if (!force && ledgerFetchPromise) {
            return ledgerFetchPromise;
        }
        ledgerFetchPromise = fetch(`/api/player-ledger?limit=${LEDGER_LIMIT}&filter=${encodeURIComponent(ledgerFilter)}`, {
            headers: auth.headers(),
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

    function setLoginGate(needsLogin) {
        const gate = document.getElementById('mcwwsLedgerLoginGate');
        const card = document.getElementById('mcwwsLedgerPageCard');
        if (gate) {
            gate.hidden = !needsLogin;
        }
        if (card) {
            card.hidden = needsLogin;
        }
    }

    async function refreshLedger(force, silent) {
        const auth = window.MCWWS_AUTH;
        if (!auth?.getUser?.()) {
            setLoginGate(true);
            return;
        }
        setLoginGate(false);

        const body = document.getElementById('mcwwsLedgerBody');
        if (!silent && body) {
            body.innerHTML = '<tr><td colspan="5" class="mcwws-ledger-empty">加载中…</td></tr>';
        }
        const [data, economyData] = await Promise.all([
            fetchPlayerLedger(force),
            auth.fetchEconomy ? auth.fetchEconomy(true) : null
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

    function stopPolling() {
        if (pollTimer) {
            window.clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function startPolling() {
        stopPolling();
        pollTimer = window.setInterval(() => {
            if (!window.MCWWS_AUTH?.getToken?.() || document.hidden) {
                return;
            }
            void refreshLedger(true, true);
        }, LEDGER_POLL_MS);
    }

    function bindFilters() {
        if (filtersBound) {
            return;
        }
        filtersBound = true;
        document.getElementById('mcwwsLedgerFilters')?.addEventListener('click', (e) => {
            const btn = e.target.closest('[data-filter]');
            if (!btn) {
                return;
            }
            ledgerFilter = btn.dataset.filter || 'exclude_flight';
            document.querySelectorAll('.mcwws-ledger-filter').forEach((el) => {
                el.classList.toggle('is-active', el === btn);
            });
            syncFilterToUrl();
            void refreshLedger(true, false);
        });
        document.getElementById('mcwwsLedgerLoginBtn')?.addEventListener('click', () => {
            window.MCWWS_AUTH?.openModal?.();
        });
    }

    function init() {
        if (!document.getElementById('mcwwsLedgerPage')) {
            return;
        }
        readFilterFromUrl();
        bindFilters();
        void refreshLedger(true, false);
        startPolling();

        window.MCWWS_AUTH?.onChange?.(({ user }) => {
            if (user) {
                void refreshLedger(true, false);
                startPolling();
            } else {
                stopPolling();
                setLoginGate(true);
            }
        });
    }

    window.MCWWS_LEDGER_PAGE = { init, refresh: refreshLedger, stopPolling, startPolling };
})();
