const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

const CATEGORY_LABELS = {
    web_shop: '网页商城',
    web_build: '投影粘贴',
    shop_buy: '商店购买',
    shop_sell: '商店出售',
    flight: '飞行消耗',
    worldedit: '创世神建造',
    menu: '菜单服务',
    other: '其他'
};

const MAX_ENTRIES = 5000;
const MAX_PER_PLAYER_LIST = 200;
const FLIGHT_MERGE_WINDOW_MS = 2000;

function normalizeUuid(uuid) {
    return String(uuid || '').trim().toLowerCase();
}

function roundMoney(value) {
    return Math.round(Number(value) * 100) / 100;
}

function safeText(input, maxLen = 120) {
    return String(input ?? '')
        .replace(/\|/g, '/')
        .replace(/\r?\n/g, ' ')
        .trim()
        .slice(0, maxLen);
}

/**
 * @param {object} opts
 * @param {string} opts.ledgerPath
 * @param {string} opts.queuePath
 * @param {string} [opts.transactionsYamlPath]
 * @param {string} [opts.pendingOrdersPath]
 * @param {string} [opts.pendingPasteOrdersPath]
 * @param {function} [opts.formatBalance]
 * @param {function} [opts.loadPriceTables]
 */
function createPlayerLedgerService(opts) {
    const {
        ledgerPath,
        queuePath,
        transactionsYamlPath,
        pendingOrdersPath,
        pendingPasteOrdersPath,
        formatBalance = (n) => String(n),
        loadPriceTables = () => ({})
    } = opts;

    const backfillDone = new Set();

    function ensureLedgerFile() {
        const dir = path.dirname(ledgerPath);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
        if (!fs.existsSync(ledgerPath)) {
            fs.writeFileSync(ledgerPath, 'next_id: 1\nentries: {}\n', 'utf8');
        }
    }

    function loadStore() {
        ensureLedgerFile();
        try {
            const doc = yaml.load(fs.readFileSync(ledgerPath, 'utf8')) || {};
            if (!doc.entries || typeof doc.entries !== 'object') {
                doc.entries = {};
            }
            if (!Number.isFinite(Number(doc.next_id))) {
                doc.next_id = 1;
            }
            return doc;
        } catch (error) {
            console.error('[player-ledger] 读取失败，已重置:', error.message);
            return { next_id: 1, entries: {} };
        }
    }

    function saveStore(store) {
        ensureLedgerFile();
        fs.writeFileSync(ledgerPath, yaml.dump(store, { lineWidth: 120, noRefs: true }), 'utf8');
    }

    function flightAnchorMs(row) {
        return Date.parse(row.updatedAt || row.createdAt || '') || 0;
    }

    function findMergeableFlightEntry(store, uuid, createdMs) {
        let candidate = null;
        let anchorMs = 0;
        Object.values(store.entries).forEach((row) => {
            if (!row || normalizeUuid(row.uuid) !== uuid) {
                return;
            }
            if (row.category !== 'flight' || row.direction !== 'debit') {
                return;
            }
            const rowAnchor = flightAnchorMs(row);
            if (rowAnchor > anchorMs) {
                anchorMs = rowAnchor;
                candidate = row;
            }
        });
        if (!candidate || createdMs - anchorMs >= FLIGHT_MERGE_WINDOW_MS) {
            return null;
        }
        return candidate;
    }

    function mergeFlightEntry(existing, entry, createdAt, createdMs) {
        const mergedAmount = roundMoney(existing.amount + entry.amount);
        existing.amount = mergedAmount;
        existing.updatedAt = createdAt;
        existing.playerId = safeText(entry.playerId || existing.playerId || '', 32);
        existing.description = safeText(
            entry.description || existing.description || CATEGORY_LABELS.flight,
            160
        );
        if (entry.balanceAfter != null && Number.isFinite(Number(entry.balanceAfter))) {
            existing.balanceAfter = roundMoney(entry.balanceAfter);
        }
        return existing;
    }

    function appendEntry(entry) {
        const uuid = normalizeUuid(entry.uuid);
        if (!uuid) {
            return null;
        }
        const store = loadStore();
        const direction = entry.direction === 'credit' ? 'credit' : 'debit';
        const category = String(entry.category || 'other').trim() || 'other';
        const amount = roundMoney(entry.amount);
        if (!Number.isFinite(amount) || amount <= 0) {
            return null;
        }
        const createdAt = entry.createdAt || new Date().toISOString();
        const createdMs = Date.parse(createdAt) || Date.now();

        if (category === 'flight' && direction === 'debit') {
            const mergeTarget = findMergeableFlightEntry(store, uuid, createdMs);
            if (mergeTarget) {
                mergeFlightEntry(mergeTarget, entry, createdAt, createdMs);
                saveStore(store);
                return mergeTarget;
            }
        }

        const refKey = safeText(entry.refId || '', 80);
        if (refKey && hasRef(store, refKey)) {
            return null;
        }
        const dedupWindowMs = 4000;
        const isNearDuplicate = category !== 'flight' && Object.values(store.entries).some((row) => {
            if (!row || normalizeUuid(row.uuid) !== uuid) {
                return false;
            }
            if (row.direction !== direction || roundMoney(row.amount) !== amount) {
                return false;
            }
            const rowMs = Date.parse(row.createdAt || '') || 0;
            return Math.abs(createdMs - rowMs) <= dedupWindowMs;
        });
        if (isNearDuplicate) {
            return null;
        }
        const id = Number(store.next_id) || 1;
        const row = {
            id,
            uuid,
            playerId: safeText(entry.playerId || '', 32),
            direction,
            category,
            amount,
            description: safeText(entry.description || CATEGORY_LABELS[category] || category, 160),
            refId: refKey,
            createdAt
        };
        if (entry.balanceAfter != null && Number.isFinite(Number(entry.balanceAfter))) {
            row.balanceAfter = roundMoney(entry.balanceAfter);
        }
        store.entries[String(id)] = row;
        store.next_id = id + 1;

        const keys = Object.keys(store.entries);
        if (keys.length > MAX_ENTRIES) {
            keys
                .map((k) => ({ k, id: Number(store.entries[k]?.id) || 0 }))
                .sort((a, b) => a.id - b.id)
                .slice(0, keys.length - MAX_ENTRIES)
                .forEach(({ k }) => delete store.entries[k]);
        }
        saveStore(store);
        return row;
    }

    function processQueue() {
        if (!fs.existsSync(queuePath)) {
            return 0;
        }
        const size = fs.statSync(queuePath).size;
        if (size <= 0) {
            return 0;
        }
        let text;
        try {
            text = fs.readFileSync(queuePath, 'utf8');
            fs.writeFileSync(queuePath, '', 'utf8');
        } catch (error) {
            console.error('[player-ledger] 处理队列失败:', error.message);
            return 0;
        }
        let count = 0;
        text.split(/\r?\n/).forEach((line) => {
            const trimmed = line.trim();
            if (!trimmed) {
                return;
            }
            const parts = trimmed.split('|');
            if (parts.length < 6) {
                return;
            }
            const [
                uuid,
                playerId,
                direction,
                category,
                amountRaw,
                balanceAfterRaw,
                description,
                refId,
                createdAt
            ] = parts;
            const appended = appendEntry({
                uuid,
                playerId,
                direction,
                category,
                amount: amountRaw,
                balanceAfter: balanceAfterRaw ? Number(balanceAfterRaw) : null,
                description,
                refId,
                createdAt: createdAt || undefined
            });
            if (appended) {
                count += 1;
            }
        });
        return count;
    }

    function hasRef(store, refId) {
        const key = safeText(refId, 80);
        if (!key) {
            return false;
        }
        return Object.values(store.entries).some((row) => row && row.refId === key);
    }

    function backfillForUuid(uuid) {
        const norm = normalizeUuid(uuid);
        if (!norm || backfillDone.has(norm)) {
            return;
        }
        backfillDone.add(norm);
        const store = loadStore();
        const prices = loadPriceTables();

        if (pendingOrdersPath && fs.existsSync(pendingOrdersPath)) {
            try {
                const doc = yaml.load(fs.readFileSync(pendingOrdersPath, 'utf8')) || {};
                const orders = doc.orders || {};
                Object.values(orders).forEach((order) => {
                    if (!order || normalizeUuid(order.playerUuid) !== norm) {
                        return;
                    }
                    const refId = `web-order-${order.numericId || order.id}`;
                    if (hasRef(store, refId)) {
                        return;
                    }
                    const total = roundMoney(order.total);
                    if (total <= 0) {
                        return;
                    }
                    appendEntry({
                        uuid: norm,
                        playerId: order.playerId,
                        direction: 'debit',
                        category: 'web_shop',
                        amount: total,
                        balanceAfter: order.balanceAfter,
                        description: `网页商城订单 #${order.numericId || ''}`,
                        refId,
                        createdAt: order.createdAt || order.updatedAt
                    });
                });
            } catch (error) {
                console.warn('[player-ledger] 回填 pending_orders 失败:', error.message);
            }
        }

        if (pendingPasteOrdersPath && fs.existsSync(pendingPasteOrdersPath)) {
            try {
                const doc = yaml.load(fs.readFileSync(pendingPasteOrdersPath, 'utf8')) || {};
                const orders = doc.orders || {};
                Object.values(orders).forEach((order) => {
                    if (!order || normalizeUuid(order.playerUuid) !== norm) {
                        return;
                    }
                    const refId = `paste-order-${order.numericId || order.id}`;
                    if (hasRef(store, refId)) {
                        return;
                    }
                    const total = roundMoney(order.total);
                    if (total <= 0) {
                        return;
                    }
                    appendEntry({
                        uuid: norm,
                        playerId: order.playerId,
                        direction: 'debit',
                        category: 'web_build',
                        amount: total,
                        description: `投影粘贴订单 #${order.numericId || ''}`,
                        refId,
                        createdAt: order.createdAt || order.updatedAt
                    });
                });
            } catch (error) {
                console.warn('[player-ledger] 回填 pending_paste_orders 失败:', error.message);
            }
        }

        if (transactionsYamlPath && fs.existsSync(transactionsYamlPath)) {
            try {
                const doc = yaml.load(fs.readFileSync(transactionsYamlPath, 'utf8')) || {};
                const entries = doc.entry || {};
                Object.values(entries).forEach((raw) => {
                    if (!raw || typeof raw !== 'string') {
                        return;
                    }
                    const parts = raw.split(',');
                    if (parts.length < 8) {
                        return;
                    }
                    const [txId, txUuid, playerName, type, shopId, productId, amountRaw, timeRaw] = parts;
                    if (normalizeUuid(txUuid) !== norm) {
                        return;
                    }
                    const refId = `us-${txId}`;
                    if (hasRef(store, refId)) {
                        return;
                    }
                    const qty = Math.max(1, Number(amountRaw) || 1);
                    const material = String(productId || '').trim().toLowerCase().replace(/-/g, '_');
                    const priceRow = prices[material] || {};
                    const unit = type === 'SELL'
                        ? roundMoney(priceRow.sell || 0)
                        : roundMoney(priceRow.buy || 0);
                    const total = roundMoney(unit * qty);
                    if (total <= 0) {
                        return;
                    }
                    const isSell = String(type).toUpperCase() === 'SELL';
                    appendEntry({
                        uuid: norm,
                        playerId: playerName,
                        direction: isSell ? 'credit' : 'debit',
                        category: isSell ? 'shop_sell' : 'shop_buy',
                        amount: total,
                        description: `${isSell ? '出售' : '购买'} ${material} x${qty}`,
                        refId,
                        createdAt: timeRaw ? new Date(timeRaw.replace(' ', 'T') + '+08:00').toISOString() : undefined
                    });
                });
            } catch (error) {
                console.warn('[player-ledger] 回填 transactions_store 失败:', error.message);
            }
        }
    }

    function formatEntry(row) {
        const direction = row.direction === 'credit' ? 'credit' : 'debit';
        const amount = roundMoney(row.amount);
        const signedAmount = direction === 'credit' ? amount : -amount;
        return {
            id: row.id,
            direction,
            category: row.category,
            categoryLabel: CATEGORY_LABELS[row.category] || row.category,
            amount,
            signedAmount,
            amountFormatted: formatBalance(amount),
            signedAmountFormatted: `${signedAmount >= 0 ? '+' : '-'}${formatBalance(Math.abs(signedAmount))}`,
            balanceAfter: row.balanceAfter ?? null,
            balanceAfterFormatted: row.balanceAfter != null ? formatBalance(row.balanceAfter) : null,
            description: row.description || '',
            refId: row.refId || '',
            createdAt: row.updatedAt || row.createdAt,
            isFlight: row.category === 'flight'
        };
    }

    function listForUuid(uuid, options = {}) {
        processQueue();
        const norm = normalizeUuid(uuid);
        if (!norm) {
            return { entries: [], summary: emptySummary(), hasMore: false };
        }
        backfillForUuid(norm);
        const store = loadStore();
        const limit = Math.min(Math.max(Number(options.limit) || 40, 1), MAX_PER_PLAYER_LIST);
        const offset = Math.max(Number(options.offset) || 0, 0);
        const filter = String(options.filter || 'all');

        let rows = Object.values(store.entries)
            .filter((row) => row && normalizeUuid(row.uuid) === norm);

        if (filter === 'flight') {
            rows = rows.filter((row) => row.category === 'flight');
        } else if (filter === 'exclude_flight') {
            rows = rows.filter((row) => row.category !== 'flight');
        }

        rows.sort((a, b) => {
            const ta = Date.parse(a.updatedAt || a.createdAt || '') || 0;
            const tb = Date.parse(b.updatedAt || b.createdAt || '') || 0;
            return tb - ta;
        });

        const totalMatching = rows.length;
        const slice = rows.slice(offset, offset + limit).map(formatEntry);

        const summary = summarize(rows);
        return {
            entries: slice,
            summary,
            total: totalMatching,
            hasMore: offset + limit < totalMatching,
            offset,
            limit,
            filter
        };
    }

    function emptySummary() {
        return {
            creditTotal: 0,
            debitTotal: 0,
            flightDebitTotal: 0,
            otherDebitTotal: 0,
            netTotal: 0
        };
    }

    function summarize(rows) {
        const summary = emptySummary();
        rows.forEach((row) => {
            const amount = roundMoney(row.amount);
            if (row.direction === 'credit') {
                summary.creditTotal = roundMoney(summary.creditTotal + amount);
                summary.netTotal = roundMoney(summary.netTotal + amount);
            } else {
                summary.debitTotal = roundMoney(summary.debitTotal + amount);
                summary.netTotal = roundMoney(summary.netTotal - amount);
                if (row.category === 'flight') {
                    summary.flightDebitTotal = roundMoney(summary.flightDebitTotal + amount);
                } else {
                    summary.otherDebitTotal = roundMoney(summary.otherDebitTotal + amount);
                }
            }
        });
        return summary;
    }

    function logCheckout({
        uuid,
        playerId,
        category,
        amount,
        balanceAfter,
        description,
        refId
    }) {
        return appendEntry({
            uuid,
            playerId,
            direction: 'debit',
            category: category || 'web_shop',
            amount,
            balanceAfter,
            description,
            refId
        });
    }

    return {
        appendEntry,
        processQueue,
        listForUuid,
        logCheckout,
        CATEGORY_LABELS
    };
}

module.exports = { createPlayerLedgerService, CATEGORY_LABELS };
