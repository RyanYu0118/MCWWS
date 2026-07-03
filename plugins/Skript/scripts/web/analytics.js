const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');
const crypto = require('crypto');

const MAX_TRANSACTIONS = 10000;
const PRICE_HISTORY_FALLBACK_START = new Date('2026-07-01T00:00:00+08:00');

/**
 * @param {object} opts
 * @param {string} opts.transactionsCsvPath
 * @param {string} opts.transactionsYamlPath
 * @param {string} opts.legacyCsvPath
 * @param {string} [opts.webPricesPath]
 * @param {Array<{path:string, source?:string, custom?:boolean}>} [opts.webPricePaths]
 * @param {string} opts.itemsDbPath
 * @param {string} opts.ultimateShopShopsDir
 */
function createAnalyticsService(opts) {
    const {
        transactionsCsvPath,
        transactionsYamlPath,
        legacyCsvPath,
        webPricesPath,
        webPricePaths,
        itemsDbPath,
        ultimateShopShopsDir
    } = opts;

    let cache = {
        loadedAt: 0,
        transactions: [],
        shopProductIndex: {},
        itemMeta: {},
        prices: {}
    };

    function loadYamlFile(filePath) {
        if (!fs.existsSync(filePath)) {
            return {};
        }
        try {
            return yaml.load(fs.readFileSync(filePath, 'utf8')) || {};
        } catch (error) {
            console.error(`读取 YAML 失败: ${filePath}`, error);
            return {};
        }
    }

    function loadPriceTables() {
        const tables = Array.isArray(webPricePaths) && webPricePaths.length
            ? webPricePaths
            : [{ path: webPricesPath, source: 'vanilla', custom: false }];
        const merged = {};
        tables.forEach((table) => {
            if (!table || !table.path) return;
            const data = loadYamlFile(table.path);
            Object.keys(data).forEach((key) => {
                const row = data[key];
                if (!row || typeof row !== 'object') return;
                merged[key] = {
                    ...row,
                    source: table.source || row.source || 'vanilla',
                    custom: table.custom === true || row.custom === true
                };
            });
        });
        return merged;
    }

    function normalizeMaterialId(material) {
        if (material == null || material === '') {
            return null;
        }
        return String(material).trim().toLowerCase().replace(/-/g, '_');
    }

    function buildShopProductIndex() {
        const index = {};
        if (!fs.existsSync(ultimateShopShopsDir)) {
            return index;
        }
        const files = fs.readdirSync(ultimateShopShopsDir).filter((f) => f.endsWith('.yml'));
        files.forEach((file) => {
            const shopId = path.basename(file, '.yml');
            const doc = loadYamlFile(path.join(ultimateShopShopsDir, file));
            const items = doc.items;
            if (!items || typeof items !== 'object') {
                return;
            }
            Object.keys(items).forEach((productId) => {
                const def = items[productId];
                if (!def || typeof def !== 'object' || !def.products) {
                    return;
                }
                const materials = [];
                Object.keys(def.products).forEach((k) => {
                    const slot = def.products[k];
                    if (slot && slot.material != null) {
                        const norm = normalizeMaterialId(slot.material);
                        if (norm) {
                            materials.push(norm);
                        }
                    }
                });
                if (materials.length > 0) {
                    const key = `${shopId}::${productId}`;
                    index[key] = materials[0];
                }
            });
        });
        return index;
    }

    function loadItemMeta() {
        const db = loadYamlFile(itemsDbPath);
        const meta = {};
        Object.keys(db).forEach((itemId) => {
            const row = db[itemId];
            if (row && typeof row === 'object') {
                meta[normalizeMaterialId(itemId) || itemId] = {
                    category: row.category || 'unknown',
                    displayName: row.name || itemId,
                    baseBuyPrice: Number(row.unit_buy) || 0,
                    baseSellPrice: Number(row.unit_sell) || 0
                };
            }
        });
        return meta;
    }

    function parseTimestamp(raw) {
        if (!raw) {
            return new Date(0);
        }
        const trimmed = String(raw).trim();
        const iso = trimmed.includes('T') ? trimmed : trimmed.replace(' ', 'T');
        const date = new Date(iso);
        return Number.isNaN(date.getTime()) ? new Date(0) : date;
    }

    function formatTimestamp(date) {
        const pad = (n) => String(n).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    }

    function prettifyItem(item) {
        return String(item)
            .split('_')
            .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
            .join(' ');
    }

    function parseLegacyCsvLine(line) {
        const parts = line.split(',');
        if (parts.length < 10) {
            return null;
        }
        const [
            id,
            playerUuid,
            playerName,
            type,
            item,
            displayName,
            amount,
            price,
            unitPrice,
            timestamp
        ] = parts;
        const itemNorm = normalizeMaterialId(item);
        if (!itemNorm) {
            return null;
        }
        return {
            id: id || crypto.randomUUID(),
            playerUuid,
            playerName,
            type: type === 'SELL' ? 'SELL' : 'BUY',
            item: itemNorm,
            displayName: displayName || prettifyItem(itemNorm),
            amount: Number(amount) || 0,
            price: Number(price) || 0,
            unitPrice: Number(unitPrice) || 0,
            timestamp: timestamp || '',
            shopId: null,
            productId: null,
            source: 'dynamicshop'
        };
    }

    function parseMcwwsCsvLine(line, shopProductIndex, prices, itemMeta) {
        const parts = line.split(',');
        if (parts.length < 8) {
            return null;
        }
        const headerLike = parts[0] === 'id' && parts[1] === 'playerUuid';
        if (headerLike) {
            return null;
        }

        let id;
        let playerUuid;
        let playerName;
        let type;
        let shopId;
        let productId;
        let amount;
        let timestamp;

        if (parts.length >= 8 && parts[1] && parts[1].includes('-')) {
            [id, playerUuid, playerName, type, shopId, productId, amount, timestamp] = parts;
        } else {
            return null;
        }

        const lookupKey = `${shopId}::${productId}`;
        const item = shopProductIndex[lookupKey];
        if (!item) {
            return null;
        }

        const txType = type === 'SELL' ? 'SELL' : 'BUY';
        const qty = Number(amount) || 1;
        const unit = txType === 'BUY'
            ? Number(prices[item]?.buy) || 0
            : Number(prices[item]?.sell) || 0;
        const total = unit * qty;
        const meta = itemMeta[item] || {};

        return {
            id: id || crypto.randomUUID(),
            playerUuid,
            playerName,
            type: txType,
            item,
            displayName: meta.displayName || prettifyItem(item),
            amount: qty,
            price: total,
            unitPrice: unit,
            timestamp: timestamp || '',
            shopId,
            productId,
            category: meta.category || 'unknown',
            source: 'ultimateshop'
        };
    }

    function enrichTransaction(row, shopProductIndex, prices, itemMeta) {
        if (!row) {
            return null;
        }
        const item = row.item;
        const meta = itemMeta[item] || {};
        if (!row.category) {
            row.category = meta.category || 'unknown';
        }
        if (!row.displayName) {
            row.displayName = meta.displayName || prettifyItem(item);
        }
        if ((!row.price || row.price === 0) && row.amount > 0) {
            const unit = row.type === 'BUY'
                ? Number(prices[item]?.buy) || row.unitPrice || 0
                : Number(prices[item]?.sell) || row.unitPrice || 0;
            row.unitPrice = unit;
            row.price = unit * row.amount;
        }
        return row;
    }

    function loadTransactions() {
        const shopProductIndex = buildShopProductIndex();
        const itemMeta = loadItemMeta();
        const prices = loadPriceTables();
        const rows = [];
        const seen = new Set();

        function pushRow(row) {
            if (!row || !row.item) {
                return;
            }
            const enriched = enrichTransaction(row, shopProductIndex, prices, itemMeta);
            const key = enriched.id || `${enriched.timestamp}|${enriched.playerUuid}|${enriched.type}|${enriched.item}|${enriched.amount}`;
            if (seen.has(key)) {
                return;
            }
            seen.add(key);
            rows.push(enriched);
        }

        if (fs.existsSync(transactionsCsvPath)) {
            const content = fs.readFileSync(transactionsCsvPath, 'utf8');
            content.split(/\r?\n/).forEach((line) => {
                const trimmed = line.trim();
                if (!trimmed || trimmed.startsWith('id,playerUuid')) {
                    return;
                }
                pushRow(parseMcwwsCsvLine(trimmed, shopProductIndex, prices, itemMeta));
            });
        }

        if (transactionsYamlPath && fs.existsSync(transactionsYamlPath)) {
            const store = loadYamlFile(transactionsYamlPath);
            const entryBlock = store.entry;
            if (entryBlock && typeof entryBlock === 'object') {
                Object.keys(entryBlock).forEach((key) => {
                    const line = entryBlock[key];
                    if (typeof line === 'string' && line.trim()) {
                        pushRow(parseMcwwsCsvLine(line.trim(), shopProductIndex, prices, itemMeta));
                    }
                });
            }
            Object.keys(store).forEach((key) => {
                if (!key.startsWith('entry.')) {
                    return;
                }
                const line = store[key];
                if (typeof line === 'string' && line.trim()) {
                    pushRow(parseMcwwsCsvLine(line.trim(), shopProductIndex, prices, itemMeta));
                }
            });
        }

        if (fs.existsSync(legacyCsvPath)) {
            const content = fs.readFileSync(legacyCsvPath, 'utf8');
            content.split(/\r?\n/).forEach((line) => {
                const trimmed = line.trim();
                if (!trimmed) {
                    return;
                }
                const legacy = parseLegacyCsvLine(trimmed);
                pushRow(enrichTransaction(legacy, shopProductIndex, prices, itemMeta));
            });
        }

        rows.sort((a, b) => parseTimestamp(b.timestamp) - parseTimestamp(a.timestamp));
        const limited = rows.slice(0, MAX_TRANSACTIONS);

        cache = {
            loadedAt: Date.now(),
            transactions: limited,
            shopProductIndex,
            itemMeta,
            prices
        };
        return cache;
    }

    function getCache(forceReload = false) {
        if (forceReload || !cache.transactions.length && (
            fs.existsSync(transactionsCsvPath)
            || (transactionsYamlPath && fs.existsSync(transactionsYamlPath))
            || fs.existsSync(legacyCsvPath)
        )) {
            return loadTransactions();
        }
        if (Date.now() - cache.loadedAt > 5000) {
            return loadTransactions();
        }
        return cache;
    }

    function filterBySince(transactions, sinceMs) {
        if (!sinceMs) {
            return transactions;
        }
        const cutoff = Date.now() - sinceMs;
        return transactions.filter((tx) => parseTimestamp(tx.timestamp).getTime() >= cutoff);
    }

    function computeStats(transactions) {
        let totalMoney = 0;
        let buys = 0;
        let sells = 0;
        transactions.forEach((tx) => {
            totalMoney += tx.price || 0;
            if (tx.type === 'BUY') {
                buys += 1;
            } else {
                sells += 1;
            }
        });
        return {
            total: transactions.length,
            totalMoney,
            buys,
            sells
        };
    }

    function computeEconomyHealth(transactions) {
        const stats = computeStats(transactions);
        const buyRatio = stats.total > 0 ? stats.buys / stats.total : 0.5;
        const uniqueItems = new Set(transactions.map((tx) => tx.item)).size;
        const uniquePlayers = new Set(transactions.map((tx) => tx.playerName)).size;

        let buyVolume = 0;
        let sellVolume = 0;
        transactions.forEach((tx) => {
            if (tx.type === 'BUY') {
                buyVolume += tx.price || 0;
            } else {
                sellVolume += tx.price || 0;
            }
        });
        const netFlow = sellVolume - buyVolume;
        const avgTransaction = stats.total > 0 ? stats.totalMoney / stats.total : 0;

        const oneHourAgo = Date.now() - 60 * 60 * 1000;
        const recentCount = transactions.filter(
            (tx) => parseTimestamp(tx.timestamp).getTime() >= oneHourAgo
        ).length;
        const velocity = recentCount;

        return {
            buyRatio,
            velocity,
            netFlow,
            avgTransaction,
            uniqueItems,
            uniquePlayers
        };
    }

    function computeLeaderboard(transactions, type, limit) {
        const players = {};
        transactions.forEach((tx) => {
            const name = tx.playerName || 'Unknown';
            if (!players[name]) {
                players[name] = { player: name, trades: 0, spent: 0, earned: 0, netProfit: 0 };
            }
            players[name].trades += 1;
            if (tx.type === 'BUY') {
                players[name].spent += tx.price || 0;
            } else {
                players[name].earned += tx.price || 0;
            }
            players[name].netProfit = players[name].earned - players[name].spent;
        });

        let sortKey = 'spent';
        if (type === 'recyclers' || type === 'sellers') {
            sortKey = 'earned';
        } else if (type === 'traders') {
            sortKey = 'trades';
        } else if (type === 'buyers') {
            sortKey = 'spent';
        } else if (type === 'earners') {
            sortKey = 'netProfit';
        } else if (type === 'spenders') {
            sortKey = 'spent';
        }

        return Object.values(players)
            .sort((a, b) => b[sortKey] - a[sortKey])
            .slice(0, limit);
    }

    function computeTrends(transactions, limit) {
        const now = Date.now();
        const day = 24 * 60 * 60 * 1000;
        const recent = transactions.filter((tx) => parseTimestamp(tx.timestamp).getTime() >= now - day);
        const previous = transactions.filter((tx) => {
            const t = parseTimestamp(tx.timestamp).getTime();
            return t >= now - 2 * day && t < now - day;
        });

        const itemStats = {};
        function bump(list, sign) {
            list.forEach((tx) => {
                if (!itemStats[tx.item]) {
                    itemStats[tx.item] = {
                        item: tx.item,
                        recentCount: 0,
                        previousCount: 0,
                        recentVolume: 0,
                        avgPrice: 0
                    };
                }
                if (sign > 0) {
                    itemStats[tx.item].recentCount += 1;
                    itemStats[tx.item].recentVolume += tx.price || 0;
                } else {
                    itemStats[tx.item].previousCount += 1;
                }
            });
        }
        bump(recent, 1);
        bump(previous, -1);

        const items = Object.values(itemStats).map((row) => {
            const avgPrice = row.recentCount > 0 ? row.recentVolume / row.recentCount : 0;
            const base = row.previousCount > 0 ? row.previousCount : 1;
            const changePercent = ((row.recentCount - row.previousCount) / base) * 100;
            return {
                item: row.item,
                recentCount: row.recentCount,
                avgPrice,
                changePercent
            };
        });

        const hot = [...items].sort((a, b) => b.recentCount - a.recentCount).slice(0, limit);
        const rising = [...items]
            .filter((i) => i.recentCount > 0)
            .sort((a, b) => b.changePercent - a.changePercent)
            .slice(0, limit);
        const falling = [...items]
            .filter((i) => i.recentCount > 0 || i.changePercent < 0)
            .sort((a, b) => a.changePercent - b.changePercent)
            .slice(0, limit);

        return { hot, rising, falling };
    }

    const HISTORY_RANGE_PRESETS = {
        '10m': { sinceMs: 10 * 60 * 1000, bucketMs: 1 * 1000 },
        '30m': { sinceMs: 30 * 60 * 1000, bucketMs: 1 * 1000 },
        '1h': { sinceMs: 1 * 60 * 60 * 1000, bucketMs: 1 * 1000 },
        '6h': { sinceMs: 6 * 60 * 60 * 1000, bucketMs: 10 * 1000 },
        '24h': { sinceMs: 24 * 60 * 60 * 1000, bucketMs: 30 * 1000 },
        '7d': { sinceMs: 7 * 24 * 60 * 60 * 1000, bucketMs: 10 * 60 * 1000 },
        '1mo': { sinceMs: 30 * 24 * 60 * 60 * 1000, bucketMs: 1 * 60 * 60 * 1000 },
        '1y': { sinceMs: 365 * 24 * 60 * 60 * 1000, bucketMs: 24 * 60 * 60 * 1000 },
        '3y': { sinceMs: 3 * 365 * 24 * 60 * 60 * 1000, bucketMs: 7 * 24 * 60 * 60 * 1000 },
        all: { sinceMs: null, bucketMs: 30 * 24 * 60 * 60 * 1000 }
    };

    function resolveHistoryRangeConfig(rangeOrHours) {
        if (typeof rangeOrHours === 'number' && Number.isFinite(rangeOrHours) && rangeOrHours > 0) {
            const sinceMs = rangeOrHours * 60 * 60 * 1000;
            let bucketMs = 60 * 60 * 1000;
            if (rangeOrHours <= 1) {
                bucketMs = 5 * 60 * 1000;
            } else if (rangeOrHours <= 6) {
                bucketMs = 30 * 60 * 1000;
            } else if (rangeOrHours <= 24) {
                bucketMs = 60 * 60 * 1000;
            } else if (rangeOrHours <= 24 * 30) {
                bucketMs = 24 * 60 * 60 * 1000;
            } else if (rangeOrHours <= 24 * 365) {
                bucketMs = 7 * 24 * 60 * 60 * 1000;
            } else {
                bucketMs = 30 * 24 * 60 * 60 * 1000;
            }
            return { sinceMs, bucketMs };
        }
        const key = String(rangeOrHours || '7d').trim().toLowerCase();
        return HISTORY_RANGE_PRESETS[key] || HISTORY_RANGE_PRESETS['7d'];
    }

    function computePriceHistory(transactions, item, rangeOrHours) {
        const { sinceMs, bucketMs } = resolveHistoryRangeConfig(rangeOrHours);
        const { itemMeta, prices } = getCache();
        const meta = itemMeta[item] || {};
        const fallbackBuy = meta.baseBuyPrice > 0 ? meta.baseBuyPrice : (Number(prices[item]?.buy) || 0);
        const fallbackSell = meta.baseSellPrice > 0 ? meta.baseSellPrice : (Number(prices[item]?.sell) || 0);
        const currentBuy = Number(prices[item]?.buy) || fallbackBuy;
        const currentSell = Number(prices[item]?.sell) || fallbackSell;
        const fallbackStartMs = PRICE_HISTORY_FALLBACK_START.getTime();
        const nowMs = Date.now();
        const rangeStartMs = Math.max(sinceMs == null ? fallbackStartMs : (nowMs - sinceMs), fallbackStartMs);
        const related = transactions
            .filter((tx) => tx.item === item)
            .map((tx) => ({ ...tx, _time: parseTimestamp(tx.timestamp).getTime() }))
            .filter((tx) => tx._time > 0)
            .sort((a, b) => a._time - b._time);
        const inRange = related.filter((tx) => tx._time >= rangeStartMs);

        if (!inRange.length) {
            return [
                {
                    timestamp: formatTimestamp(PRICE_HISTORY_FALLBACK_START),
                    avgBuyPrice: fallbackBuy,
                    avgSellPrice: fallbackSell
                },
                {
                    timestamp: formatTimestamp(new Date(nowMs)),
                    avgBuyPrice: currentBuy,
                    avgSellPrice: currentSell
                }
            ];
        }

        let buyPrice = fallbackBuy;
        let sellPrice = fallbackSell;
        related.forEach((tx) => {
            if (tx._time < rangeStartMs) {
                if (tx.type === 'BUY' && tx.unitPrice > 0) buyPrice = tx.unitPrice;
                if (tx.type === 'SELL' && tx.unitPrice > 0) sellPrice = tx.unitPrice;
            }
        });

        const rows = [];
        const pushRow = (timeMs) => {
            const last = rows[rows.length - 1];
            if (last && last.timestamp === formatTimestamp(new Date(timeMs)) && last.avgBuyPrice === buyPrice && last.avgSellPrice === sellPrice) {
                return;
            }
            rows.push({
                timestamp: formatTimestamp(new Date(timeMs)),
                avgBuyPrice: buyPrice,
                avgSellPrice: sellPrice
            });
        };

        pushRow(rangeStartMs);

        let txIndex = 0;
        while (txIndex < related.length && related[txIndex]._time < rangeStartMs) {
            txIndex += 1;
        }

        let cursorMs = Math.ceil(rangeStartMs / bucketMs) * bucketMs;
        while (cursorMs <= nowMs) {
            while (txIndex < related.length && related[txIndex]._time <= cursorMs) {
                const tx = related[txIndex];
                if (tx.type === 'BUY' && tx.unitPrice > 0) buyPrice = tx.unitPrice;
                if (tx.type === 'SELL' && tx.unitPrice > 0) sellPrice = tx.unitPrice;
                txIndex += 1;
            }
            pushRow(cursorMs);
            cursorMs += bucketMs;
        }

        buyPrice = currentBuy;
        sellPrice = currentSell;
        pushRow(nowMs);
        return rows;
    }

    function getItemDetails(item, transactions) {
        const { prices, itemMeta } = getCache();
        const meta = itemMeta[item] || {};
        const related = transactions.filter((tx) => tx.item === item);
        let totalBuys = 0;
        let totalSells = 0;
        related.forEach((tx) => {
            if (tx.type === 'BUY') {
                totalBuys += tx.amount;
            } else {
                totalSells += tx.amount;
            }
        });

        return {
            item,
            displayName: meta.displayName || prettifyItem(item),
            category: meta.category || 'unknown',
            buyPrice: Number(prices[item]?.buy) || 0,
            sellPrice: Number(prices[item]?.sell) || 0,
            stock: 0,
            totalBuys,
            totalSells,
            imageUrl: `https://mc-heads.net/icon/${item}`,
            recentTransactions: related.slice(0, 20)
        };
    }

    return {
        reload: () => loadTransactions(),
        getTransactions: (limit) => getCache().transactions.slice(0, limit),
        getStats: () => computeStats(getCache().transactions),
        getEconomyHealth: () => computeEconomyHealth(getCache().transactions),
        getLeaderboard: (type, limit) => computeLeaderboard(getCache().transactions, type, limit),
        getTrends: (limit) => computeTrends(getCache().transactions, limit),
        getPriceHistory: (item, rangeOrHours) => computePriceHistory(getCache().transactions, item, rangeOrHours),
        getItemDetails: (item) => getItemDetails(item, getCache().transactions)
    };
}

module.exports = { createAnalyticsService };
