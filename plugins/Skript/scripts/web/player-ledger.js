const fs = require('fs');
const path = require('path');
const { randomUUID } = require('crypto');
const { setTimeout: delay } = require('timers/promises');
const io = fs.promises;
const yaml = require('js-yaml');

const CATEGORY_LABELS = {
    web_shop: '网页商城',
    web_build: '投影粘贴（已下线）',
    shop_buy: '商店购买',
    shop_sell: '商店出售',
    flight: '飞行消耗',
    worldedit: '创世神建造',
    menu: '菜单服务',
    other: '其他'
};

const MAX_ENTRIES = 5000;
const MAX_PER_PLAYER_LIST = 200;
/** 写入时：距上一次飞行更新不超过此时长则累加到同一行 */
const FLIGHT_MERGE_WINDOW_MS = 30000;
/** 列表展示：相邻飞行记录间隔不超过此时长则合成一行（含旧的每秒一条） */
const FLIGHT_DISPLAY_GAP_MS = 60000;
const WRITE_ATTEMPTS = 8;

function isRetryableFsError(error) {
    const code = String(error && error.code ? error.code : '');
    return code === 'UNKNOWN' || code === 'EPERM' || code === 'EBUSY'
        || code === 'EACCES' || code === 'EAGAIN' || code === 'EIO';
}

async function retryFileOperation(operation) {
    for (let attempt = 1; ; attempt++) {
        try {
            return await operation();
        } catch (error) {
            if (!isRetryableFsError(error) || attempt >= WRITE_ATTEMPTS) throw error;
            await delay(25 * attempt);
        }
    }
}

async function writeFileAtomic(filePath, content) {
    const tempPath = `${filePath}.${process.pid}.${randomUUID()}.tmp`;
    await io.mkdir(path.dirname(filePath), { recursive: true });
    try {
        await retryFileOperation(() => io.writeFile(tempPath, content, 'utf8'));
        await retryFileOperation(() => io.rename(tempPath, filePath));
    } finally {
        await io.unlink(tempPath).catch(() => {});
    }
}

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

    let snapshot;
    let loading;
    let writes = Promise.resolve();
    let queueWork;
    const queueDir = path.dirname(queuePath);
    const batchPrefix = path.basename(queuePath) + '.processing-';

    async function loadStore() {
        if (snapshot) return snapshot;
        if (!loading) {
            loading = (async () => {
                let content;
                try {
                    content = await io.readFile(ledgerPath, 'utf8');
                } catch (error) {
                    if (error.code === 'ENOENT') return snapshot = { next_id: 1, entries: {} };
                    throw error;
                }
                const doc = yaml.load(content);
                if (!doc?.entries || typeof doc.entries !== 'object' || Array.isArray(doc.entries)
                    || !Number.isSafeInteger(Number(doc.next_id)) || Number(doc.next_id) < 1) {
                    throw new Error('零钱账本格式无效，已保留原文件');
                }
                return snapshot = doc;
            })().finally(() => { loading = null; });
        }
        return loading;
    }

    function serializeWrite(task) {
        const result = writes.then(task);
        writes = result.catch(() => {});
        return result;
    }

    async function saveStore(store) {
        await writeFileAtomic(ledgerPath, yaml.dump(store, { lineWidth: 120, noRefs: true }));
        snapshot = store;
    }

    function draftStore(store) {
        return { ...store, entries: { ...store.entries } };
    }

    function pruneStore(store) {
        const keys = Object.keys(store.entries);
        if (keys.length <= MAX_ENTRIES) return;
        keys.sort((a, b) => (Number(store.entries[a]?.id) || 0) - (Number(store.entries[b]?.id) || 0))
            .slice(0, keys.length - MAX_ENTRIES)
            .forEach(key => delete store.entries[key]);
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

    function mergeFlightEntry(existing, amount, entry, createdAt) {
        const base = roundMoney(existing.amount);
        const add = roundMoney(amount);
        if (!Number.isFinite(base) || !Number.isFinite(add) || add <= 0) {
            return existing;
        }
        existing.amount = roundMoney(base + add);
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

    function addEntry(store, entry, refs) {
        const uuid = normalizeUuid(entry.uuid);
        if (!uuid) {
            return null;
        }
        const direction = entry.direction === 'credit' ? 'credit' : 'debit';
        const category = String(entry.category || 'other').trim() || 'other';
        const amount = roundMoney(entry.amount);
        if (!Number.isFinite(amount) || amount <= 0) {
            return null;
        }
        const createdAt = entry.createdAt || new Date().toISOString();
        const createdMs = Date.parse(createdAt) || Date.now();

        const refKey = safeText(entry.refId || '', 80);
        if (refKey && refs.has(refKey)) return null;

        if (category === 'flight' && direction === 'debit') {
            const mergeTarget = findMergeableFlightEntry(store, uuid, createdMs);
            if (mergeTarget) {
                const updated = mergeFlightEntry({ ...mergeTarget }, amount, entry, createdAt);
                store.entries[String(updated.id)] = updated;
                if (refKey) refs.add(refKey);
                return updated;
            }
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

        if (refKey) refs.add(refKey);
        return row;
    }

    function refIndex(store) {
        return new Set(Object.values(store.entries).map(row => row?.refId).filter(Boolean));
    }

    function appendEntry(entry) {
        return serializeWrite(async () => {
            const store = draftStore(await loadStore());
            const row = addEntry(store, entry, refIndex(store));
            if (row) {
                pruneStore(store);
                await saveStore(store);
            }
            return row;
        });
    }

    async function importQueueBatch() {
        let files;
        try {
            files = await io.readdir(queueDir);
        } catch (error) {
            if (error.code === 'ENOENT') return 0;
            throw error;
        }
        let batch = files.find(name => name.startsWith(batchPrefix));
        if (!batch) {
            try {
                if (!(await io.stat(queuePath)).size) return 0;
                batch = batchPrefix + randomUUID();
                await retryFileOperation(() => io.rename(queuePath, path.join(queueDir, batch)));
            } catch (error) {
                if (error.code === 'ENOENT') return 0;
                throw error;
            }
        }
        const file = path.join(queueDir, batch);
        const saved = await loadStore();
        let count = 0;
        if (saved.last_queue_batch !== batch) {
            const content = await retryFileOperation(() => io.readFile(file, 'utf8'));
            const store = draftStore(saved);
            const refs = refIndex(store);
            for (const line of content.split(/\r?\n/)) {
                const parts = line.trim().split('|');
                if (parts.length < 6) continue;
                const [uuid, playerId, direction, category, amount, balance, description, refId, createdAt] = parts;
                if (addEntry(store, {
                    uuid, playerId, direction, category, amount, description, refId,
                    balanceAfter: balance === '' ? null : Number(balance),
                    createdAt: createdAt || undefined
                }, refs)) count++;
            }
            pruneStore(store);
            // The receipt and the entries commit together. A failed cleanup cannot replay the batch.
            store.last_queue_batch = batch;
            await saveStore(store);
        }
        await retryFileOperation(() => io.unlink(file));
        return count;
    }

    function processQueue() {
        if (!queueWork) {
            queueWork = serializeWrite(importQueueBatch).finally(() => { queueWork = null; });
        }
        return queueWork;
    }

    async function backfillForUuid(norm) {
        const entriesToImport = [];
        let retry = false;
        const prices = loadPriceTables();

        if (pendingOrdersPath) {
            try {
                const doc = yaml.load(await io.readFile(pendingOrdersPath, 'utf8')) || {};
                const orders = doc.orders || {};
                Object.values(orders).forEach((order) => {
                    if (!order || normalizeUuid(order.playerUuid) !== norm) {
                        return;
                    }
                    const refId = `web-order-${order.numericId || order.id}`;
                    const total = roundMoney(order.total);
                    if (total <= 0) {
                        return;
                    }
                    entriesToImport.push({
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
                if (error.code !== 'ENOENT') {
                    retry = true;
                    console.warn('[player-ledger] 回填 pending_orders 失败:', error.message);
                }
            }
        }

        if (pendingPasteOrdersPath) {
            try {
                const doc = yaml.load(await io.readFile(pendingPasteOrdersPath, 'utf8')) || {};
                const orders = doc.orders || {};
                Object.values(orders).forEach((order) => {
                    if (!order || normalizeUuid(order.playerUuid) !== norm) {
                        return;
                    }
                    const refId = `paste-order-${order.numericId || order.id}`;
                    const total = roundMoney(order.total);
                    if (total <= 0) {
                        return;
                    }
                    entriesToImport.push({
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
                if (error.code !== 'ENOENT') {
                    retry = true;
                    console.warn('[player-ledger] 回填 pending_paste_orders 失败:', error.message);
                }
            }
        }

        if (transactionsYamlPath) {
            try {
                const doc = yaml.load(await io.readFile(transactionsYamlPath, 'utf8')) || {};
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
                    entriesToImport.push({
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
                if (error.code !== 'ENOENT') {
                    retry = true;
                    console.warn('[player-ledger] 回填 transactions_store 失败:', error.message);
                }
            }
        }
        await serializeWrite(async () => {
            const store = draftStore(await loadStore());
            const refs = refIndex(store);
            let changed = false;
            for (const entry of entriesToImport) {
                if (addEntry(store, entry, refs)) changed = true;
            }
            if (changed) {
                pruneStore(store);
                await saveStore(store);
            }
        });
        if (retry) backfillDone.delete(norm);
    }

    function scheduleBackfill(uuid) {
        if (backfillDone.has(uuid)) return;
        backfillDone.add(uuid);
        setImmediate(() => {
            backfillForUuid(uuid).catch(error => {
                backfillDone.delete(uuid);
                console.error('[player-ledger] 历史流水回填失败:', error.message);
            });
        });
    }

    function formatEntry(row) {
        const direction = row.direction === 'credit' ? 'credit' : 'debit';
        const amount = roundMoney(row.amount);
        if (!Number.isFinite(amount) || amount <= 0) {
            return null;
        }
        const signedAmount = direction === 'credit' ? amount : -amount;
        const absFormatted = formatBalance(Math.abs(signedAmount));
        const amountFormatted = absFormatted != null ? absFormatted : String(Math.abs(signedAmount));
        const signedAmountFormatted = `${signedAmount >= 0 ? '+' : '-'}${amountFormatted}`;
        const ticks = Number(row.flightTicks) || 0;
        let description = row.description || '';
        if (row.category === 'flight' && ticks > 1) {
            const base = description || CATEGORY_LABELS.flight;
            description = `${base} ×${ticks}`;
        }
        return {
            id: row.id,
            direction,
            category: row.category,
            categoryLabel: CATEGORY_LABELS[row.category] || row.category,
            amount,
            signedAmount,
            amountFormatted,
            signedAmountFormatted,
            balanceAfter: row.balanceAfter ?? null,
            balanceAfterFormatted: row.balanceAfter != null && Number.isFinite(Number(row.balanceAfter))
                ? formatBalance(row.balanceAfter)
                : null,
            description,
            refId: row.refId || '',
            createdAt: row.updatedAt || row.createdAt,
            isFlight: row.category === 'flight',
            flightTicks: ticks > 0 ? ticks : undefined
        };
    }

    function rowTimeMs(row, field) {
        return Date.parse(row?.[field] || row?.updatedAt || row?.createdAt || '') || 0;
    }

    function collapseFlightSessions(rows) {
        const sorted = rows
            .map((row) => ({ ...row }))
            .sort((a, b) => rowTimeMs(a, 'createdAt') - rowTimeMs(b, 'createdAt'));
        const out = [];
        sorted.forEach((row) => {
            const last = out[out.length - 1];
            const isFlightDebit = row.category === 'flight' && row.direction === 'debit';
            if (
                isFlightDebit
                && last
                && last.category === 'flight'
                && last.direction === 'debit'
            ) {
                const gap = rowTimeMs(row, 'createdAt') - rowTimeMs(last, 'updatedAt');
                if (gap >= 0 && gap <= FLIGHT_DISPLAY_GAP_MS) {
                    last.amount = roundMoney(Number(last.amount) + Number(row.amount));
                    last.updatedAt = row.updatedAt || row.createdAt || last.updatedAt;
                    if (row.balanceAfter != null && Number.isFinite(Number(row.balanceAfter))) {
                        last.balanceAfter = roundMoney(row.balanceAfter);
                    }
                    last.flightTicks = (Number(last.flightTicks) || 1) + (Number(row.flightTicks) || 1);
                    return;
                }
            }
            if (isFlightDebit) {
                row.flightTicks = Number(row.flightTicks) || 1;
            }
            out.push(row);
        });
        return out;
    }

    async function listForUuid(uuid, options = {}) {
        const norm = normalizeUuid(uuid);
        if (!norm) {
            return { entries: [], summary: emptySummary(), hasMore: false };
        }
        const store = await loadStore();
        scheduleBackfill(norm);
        const limit = Math.min(Math.max(Number(options.limit) || 40, 1), MAX_PER_PLAYER_LIST);
        const offset = Math.max(Number(options.offset) || 0, 0);
        const filter = String(options.filter || 'all');

        let rows = Object.values(store.entries)
            .filter((row) => row && normalizeUuid(row.uuid) === norm);

        const summaryAll = summarize(rows);
        if (filter === 'flight') {
            rows = rows.filter((row) => row.category === 'flight');
        } else if (filter === 'exclude_flight') {
            rows = rows.filter((row) => row.category !== 'flight');
        }

        rows = collapseFlightSessions(rows);
        rows.sort((a, b) => {
            const ta = Date.parse(a.updatedAt || a.createdAt || '') || 0;
            const tb = Date.parse(b.updatedAt || b.createdAt || '') || 0;
            return tb - ta;
        });

        const totalMatching = rows.length;
        const slice = rows.slice(offset, offset + limit).map(formatEntry).filter(Boolean);

        const summary = summarize(rows);
        return {
            entries: slice,
            summary,
            summaryAll,
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
            if (!Number.isFinite(amount) || amount <= 0) {
                return;
            }
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
        }).catch(error => {
            backfillDone.delete(normalizeUuid(uuid));
            throw error;
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
