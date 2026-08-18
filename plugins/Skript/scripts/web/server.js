const express = require('express');
const fs = require('fs');
const zlib = require('zlib');
const http = require('http');
const https = require('https');
const yaml = require('js-yaml');

const SKRIPT_DATE_TAG = new yaml.Type('!skriptdate', {
    kind: 'scalar',
    construct: (data) => String(data)
});

function decodeSkriptTextComponent(dataField) {
    if (!dataField) {
        return '';
    }
    try {
        const buf = Buffer.from(String(dataField), 'base64');
        const text = buf.toString('latin1');
        const matches = [...text.matchAll(/"((?:[^"\\]|\\.)*)"/g)];
        for (let i = matches.length - 1; i >= 0; i -= 1) {
            const candidate = matches[i][1];
            if (candidate && !/json/i.test(candidate) && candidate.length <= 64) {
                return candidate.replace(/\\"/g, '"').replace(/\\\\/g, '\\');
            }
        }
    } catch (error) {
        // ignore malformed skript serialization
    }
    return '';
}

function decodeSkriptClassValue(raw) {
    if (raw == null) {
        return raw;
    }
    if (typeof raw === 'string') {
        return raw;
    }
    if (typeof raw !== 'object') {
        return String(raw);
    }
    const type = String(raw.type || '').trim().toLowerCase();
    if (type === 'textcomponent' && raw.data) {
        const decoded = decodeSkriptTextComponent(raw.data);
        if (decoded) {
            return decoded;
        }
    }
    if (raw.text != null) {
        return String(raw.text);
    }
    if (raw.value != null) {
        return String(raw.value);
    }
    return raw;
}

const SKRIPT_CLASS_TAG = new yaml.Type('!skriptclass', {
    kind: 'mapping',
    construct: (data) => decodeSkriptClassValue(data)
});

const YAML_SCHEMA = yaml.DEFAULT_SCHEMA.extend([SKRIPT_DATE_TAG, SKRIPT_CLASS_TAG]);
const yamlFileCache = new Map();

const cors = require('cors');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { createAnalyticsService } = require('./analytics');
const { createPlayerLedgerService } = require('./player-ledger');
const nbt = require('prismarine-nbt');

const app = express();
const PORT = Number(process.env.PORT || process.env.MCWWS_WEB_PORT) || 8002;
const HOST = process.env.HOST || '0.0.0.0';
const materialListParser = require('./public/material-list-parser');

app.use(cors());
app.use(express.json());

const PUBLIC_DIR = path.join(__dirname, 'public');
const BLUEMAP_WEB_PORT = Number(process.env.BLUEMAP_PORT) || 8100;

// 须在 express.static 之前：否则 / 会先落到 public/index.html（经济仪表板）
app.get('/', (req, res) => {
    res.sendFile(path.join(PUBLIC_DIR, 'home.html'));
});

app.use(express.static(PUBLIC_DIR));

app.get('/api/services-config', (req, res) => {
    const host = req.hostname || '127.0.0.1';
    const protocol = req.protocol || 'http';
    res.json({
        bluemapPort: BLUEMAP_WEB_PORT,
        bluemapUrl: `${protocol}://${host}:${BLUEMAP_WEB_PORT}/`
    });
});

const AVATAR_FETCH_TIMEOUT_MS = 8000;
const AVATAR_PLAYER_ID_RE = /^[a-zA-Z0-9_]{1,16}$/;

function fetchRemoteAvatar(url, redirectCount = 0) {
    return new Promise((resolve, reject) => {
        if (redirectCount > 4) {
            reject(new Error('too many redirects'));
            return;
        }
        const client = url.startsWith('https:') ? https : http;
        const req = client.get(
            url,
            { headers: { 'User-Agent': 'MCWWS-Avatar-Proxy/1.0', Accept: 'image/*' } },
            (response) => {
                const { statusCode, headers } = response;
                if (statusCode >= 300 && statusCode < 400 && headers.location) {
                    const next = headers.location.startsWith('http')
                        ? headers.location
                        : new URL(headers.location, url).toString();
                    response.resume();
                    fetchRemoteAvatar(next, redirectCount + 1).then(resolve).catch(reject);
                    return;
                }
                const chunks = [];
                response.on('data', (chunk) => chunks.push(chunk));
                response.on('end', () => {
                    resolve({
                        statusCode: statusCode || 0,
                        contentType: headers['content-type'] || 'image/png',
                        body: Buffer.concat(chunks)
                    });
                });
            }
        );
        req.on('error', reject);
        req.setTimeout(AVATAR_FETCH_TIMEOUT_MS, () => {
            req.destroy(new Error('avatar fetch timeout'));
        });
    });
}

function buildAvatarSourceUrls(playerId, size) {
    const enc = encodeURIComponent(playerId);
    return [
        `https://minotar.net/helm/${enc}/${size}.png`,
        `https://mc-heads.net/avatar/${enc}/${size}`,
        `https://mc-heads.net/head/${enc}/${size}`
    ];
}

app.get('/api/player-avatar/:playerId', async (req, res) => {
    const playerId = String(req.params.playerId || '').trim();
    const size = Math.min(Math.max(Number(req.query.size) || 40, 16), 128);
    if (!playerId || !AVATAR_PLAYER_ID_RE.test(playerId)) {
        return res.status(400).send('invalid player id');
    }
    for (const url of buildAvatarSourceUrls(playerId, size)) {
        try {
            const result = await fetchRemoteAvatar(url);
            if (result.statusCode === 200 && result.body.length > 64) {
                res.set('Content-Type', result.contentType.split(';')[0] || 'image/png');
                res.set('Cache-Control', 'public, max-age=3600');
                return res.send(result.body);
            }
        } catch (error) {
            console.warn(`头像拉取失败 (${playerId} @ ${url}):`, error.message);
        }
    }
    return res.status(404).send('avatar not found');
});

// 旧 /manage/* 商店页 → 根目录商店系统
app.get('/manage/items.html', (req, res) => {
    const q = req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : '';
    res.redirect(301, `/items.html${q}`);
});
app.get('/manage/dashboard.html', (req, res) => {
    const q = req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : '';
    res.redirect(301, `/index.html${q}`);
});
app.get('/manage/index.html', (req, res) => {
    res.redirect(301, '/manage/shop-locations.html');
});

// 价格表：Skript 每分钟导出 web_prices.yml；custom_prices 为自定义物品覆盖层。
const WEB_PRICES_PATH = path.join(__dirname, 'mcwws', 'economy', 'web_prices.yml');
const VANILLA_PRICES_PATH = path.join(__dirname, 'mcwws', 'economy', 'vanilla_prices.yml');
const CUSTOM_PRICES_PATH = path.join(__dirname, 'mcwws', 'economy', 'custom_prices.yml');
const PRICE_TABLE_PATHS = [
    { path: WEB_PRICES_PATH, fallback: VANILLA_PRICES_PATH, source: 'vanilla', custom: false },
    { path: CUSTOM_PRICES_PATH, source: 'custom', custom: true }
];
const MAPPING_PATH = path.join(__dirname, 'mcwws', 'ultimateshop_mappings.yml');
const ULTIMATE_SHOP_SHOPS_DIR = path.join(__dirname, '..', '..', '..', 'UltimateShop', 'shops');
const ULTIMATE_SHOP_MAIN_MENU_PATH = path.join(__dirname, '..', '..', '..', 'UltimateShop', 'menus', 'main.yml');
const ULTIMATE_SHOP_LANG_FILE = path.join(__dirname, '..', '..', '..', 'UltimateShop', 'languages', 'zh_CN.yml');
const DB_DIR = path.join(__dirname, 'data');
const USER_DB_FILE = path.join(DB_DIR, 'users.json');
const TRANSACTIONS_CSV = path.join(DB_DIR, 'transactions.csv');
const TRANSACTIONS_YAML = path.join(DB_DIR, 'transactions_store.yml');
const PRICE_HISTORY_CSV = path.join(DB_DIR, 'price_history.csv');
const PENDING_ORDERS_PATH = path.join(DB_DIR, 'pending_orders.yml');
const ECONOMY_DEDUCTIONS_PATH = path.join(DB_DIR, 'economy_deductions.yml');
const ECO_TAKE_QUEUE_PATH = path.join(DB_DIR, 'eco_take_queue.txt');
const ONLINE_PLAYERS_PATH = path.join(DB_DIR, 'online_players.txt');
const PLAYER_LEDGER_PATH = path.join(DB_DIR, 'player_ledger.yml');
const LEDGER_QUEUE_PATH = path.join(DB_DIR, 'ledger_queue.txt');
/** 历史投影粘贴订单（只读，供零钱明细展示；网页建造工具已下线） */
const PENDING_PASTE_ORDERS_PATH = path.join(DB_DIR, 'pending_paste_orders.yml');
const LEGACY_TRANSACTIONS_CSV = path.join(__dirname, '..', '..', '..', 'DynamicShop', 'transactions', 'transactions.csv');
const ITEMS_DB_PATH = path.join(__dirname, '..', 'mcwws', 'economy', 'database', 'items.yml');
const OPS_PATH = path.join(__dirname, '..', '..', '..', '..', 'ops.json');
const ADMIN_ACCESS_PATH = path.join(DB_DIR, 'admin_access.yml');
const SHOP_LOCATIONS_PATH = path.join(__dirname, 'mcwws', 'shop_locations.yml');
const GIS_PROJECT_PATH = path.join(__dirname, 'mcwws', 'gis', 'project.json');
const GIS_MAX_FEATURES = 5000;
const GIS_MAX_LAYERS = 32;
const GIS_ALLOWED_MAPS = new Set(['world', 'world_nether', 'world_the_end', 'dimensionalhome']);
const BLUEMAP_WEB_MAPS_DIR = path.join(__dirname, '..', '..', '..', '..', 'bluemap', 'web', 'maps');
const BLUEMAP_LIVE_MAP_IDS = ['world', 'world_nether', 'world_the_end', 'dimensionalhome'];
const SERVER_ROOT = path.join(__dirname, '..', '..', '..', '..');
const OVERWORLD_LEVEL_DAT = path.join(SERVER_ROOT, 'world', 'level.dat');
const MC_DAY_TICKS = 24000;
const ESSENTIALS_USERDATA_DIR = path.join(SERVER_ROOT, 'plugins', 'Essentials', 'userdata');
const ESSENTIALS_CONFIG_PATH = path.join(SERVER_ROOT, 'plugins', 'Essentials', 'config.yml');
const USERCACHE_PATH = path.join(SERVER_ROOT, 'usercache.json');
const OPS_JSON_PATH = path.join(SERVER_ROOT, 'ops.json');
const SCOREBOARD_DAT_PATH = path.join(SERVER_ROOT, 'world', 'data', 'scoreboard.dat');
const WORLD_STATS_DIR = path.join(SERVER_ROOT, 'world', 'stats');
const MC_PLAY_TIME_TICKS_PER_HOUR = 72000;
const WARNING_OBJECTIVE = 'Warning';
const WARNING_MAX = 100;
const HTTPS_KEY_PATH = path.join(__dirname, 'certs', 'server.key');
const HTTPS_CERT_PATH = path.join(__dirname, 'certs', 'server.crt');
const HTTPS_ENABLED = process.env.HTTPS === '1';

if (!fs.existsSync(DB_DIR)) {
    fs.mkdirSync(DB_DIR, { recursive: true });
}

if (!fs.existsSync(TRANSACTIONS_CSV)) {
    fs.writeFileSync(
        TRANSACTIONS_CSV,
        'id,playerUuid,playerName,type,shopId,productId,amount,timestamp\n',
        'utf8'
    );
}

if (!fs.existsSync(PENDING_ORDERS_PATH)) {
    fs.writeFileSync(PENDING_ORDERS_PATH, 'next_id: 1\norders: {}\n', 'utf8');
}

if (!fs.existsSync(PRICE_HISTORY_CSV)) {
    fs.writeFileSync(
        PRICE_HISTORY_CSV,
        'timestamp,item,buy,sell\n',
        'utf8'
    );
}

const analytics = createAnalyticsService({
    transactionsCsvPath: TRANSACTIONS_CSV,
    transactionsYamlPath: TRANSACTIONS_YAML,
    legacyCsvPath: LEGACY_TRANSACTIONS_CSV,
    webPricePaths: PRICE_TABLE_PATHS,
    priceHistoryCsvPath: PRICE_HISTORY_CSV,
    itemsDbPath: ITEMS_DB_PATH,
    ultimateShopShopsDir: ULTIMATE_SHOP_SHOPS_DIR
});

const playerLedger = createPlayerLedgerService({
    ledgerPath: PLAYER_LEDGER_PATH,
    queuePath: LEDGER_QUEUE_PATH,
    transactionsYamlPath: TRANSACTIONS_YAML,
    pendingOrdersPath: PENDING_ORDERS_PATH,
    pendingPasteOrdersPath: PENDING_PASTE_ORDERS_PATH,
    formatBalance: formatEssentialsBalance,
    loadPriceTables
});

if (!fs.existsSync(USER_DB_FILE)) {
    fs.writeFileSync(USER_DB_FILE, JSON.stringify([]), 'utf8');
}

function readUsers() {
    try {
        return JSON.parse(fs.readFileSync(USER_DB_FILE, 'utf8') || '[]');
    } catch (error) {
        console.error('读取用户数据库失败', error);
        return [];
    }
}

function saveUsers(users) {
    fs.writeFileSync(USER_DB_FILE, JSON.stringify(users, null, 2), 'utf8');
}

function loadYamlFile(filePath) {
    if (!fs.existsSync(filePath)) {
        return {};
    }

    try {
        const stat = fs.statSync(filePath);
        const cached = yamlFileCache.get(filePath);
        if (cached && cached.mtimeMs === stat.mtimeMs) {
            return cached.data;
        }
        const data = yaml.load(fs.readFileSync(filePath, 'utf8'), { schema: YAML_SCHEMA }) || {};
        yamlFileCache.set(filePath, { mtimeMs: stat.mtimeMs, data });
        return data;
    } catch (error) {
        console.error(`读取 YAML 文件失败: ${filePath}`, error.message || error);
        return {};
    }
}

function saveYamlFile(filePath, data) {
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    const payload = yaml.dump(data || {}, { lineWidth: 120, noRefs: true });
    const tempPath = `${filePath}.tmp`;
    fs.writeFileSync(tempPath, payload, 'utf8');
    fs.renameSync(tempPath, filePath);
    yamlFileCache.delete(filePath);
}

function resolvePriceTablePath(table) {
    if (fs.existsSync(table.path)) {
        return table.path;
    }
    if (table.fallback && fs.existsSync(table.fallback)) {
        return table.fallback;
    }
    return null;
}

function loadPriceTables() {
    const merged = {};
    PRICE_TABLE_PATHS.forEach((table) => {
        const filePath = resolvePriceTablePath(table);
        if (!filePath) return;
        const data = loadYamlFile(filePath);
        Object.keys(data).forEach((key) => {
            const row = data[key];
            if (!row || typeof row !== 'object') return;
            merged[key] = {
                ...row,
                source: table.source,
                custom: table.custom
            };
        });
    });
    return merged;
}

function loadBasePriceLookup() {
    const db = loadYamlFile(ITEMS_DB_PATH);
    const map = new Map();
    Object.keys(db || {}).forEach((key) => {
        const itemId = normalizeMaterialId(key) || key;
        const row = db[key];
        if (!itemId || !row || typeof row !== 'object') return;
        map.set(itemId, {
            buy: Number(row.unit_buy) || 0,
            sell: Number(row.unit_sell) || 0
        });
    });
    return map;
}

function loadLatestPriceHistoryState() {
    const state = new Map();
    if (!fs.existsSync(PRICE_HISTORY_CSV)) {
        return state;
    }
    try {
        const content = fs.readFileSync(PRICE_HISTORY_CSV, 'utf8');
        content.split(/\r?\n/).forEach((line) => {
            const trimmed = line.trim();
            if (!trimmed || trimmed.startsWith('timestamp,item,')) return;
            const [timestamp, rawItem, rawBuy, rawSell] = trimmed.split(',');
            const itemId = normalizeMaterialId(rawItem);
            if (!itemId || !timestamp) return;
            state.set(itemId, {
                buy: Number(rawBuy) || 0,
                sell: Number(rawSell) || 0
            });
        });
    } catch (error) {
        console.error('读取价格历史状态失败:', error);
    }
    return state;
}

const basePriceLookup = loadBasePriceLookup();
const latestPriceHistoryState = loadLatestPriceHistoryState();

function appendPriceHistorySnapshotRows(rows) {
    if (!rows.length) return;
    fs.appendFileSync(PRICE_HISTORY_CSV, rows.join(''), 'utf8');
}

function pollAndRecordPriceHistoryChanges() {
    try {
        const rawPrices = loadPriceTables();
        const nowIso = new Date().toISOString();
        const lines = [];
        Object.keys(rawPrices).forEach((rawKey) => {
            const itemId = normalizeMaterialId(rawKey) || rawKey;
            const row = rawPrices[rawKey];
            if (!row || typeof row !== 'object') return;
            const buy = Number(row.buy) || 0;
            const sell = Number(row.sell) || 0;
            const last = latestPriceHistoryState.get(itemId);
            if (!last) {
                const base = basePriceLookup.get(itemId);
                const baseBuy = Number(base?.buy) || 0;
                const baseSell = Number(base?.sell) || 0;
                if (buy !== baseBuy || sell !== baseSell) {
                    lines.push(`${nowIso},${itemId},${buy},${sell}\n`);
                }
                latestPriceHistoryState.set(itemId, { buy, sell });
                return;
            }
            if (last.buy !== buy || last.sell !== sell) {
                lines.push(`${nowIso},${itemId},${buy},${sell}\n`);
                latestPriceHistoryState.set(itemId, { buy, sell });
            }
        });
        appendPriceHistorySnapshotRows(lines);
    } catch (error) {
        console.error('记录价格历史失败:', error);
    }
}

pollAndRecordPriceHistoryChanges();
setInterval(pollAndRecordPriceHistoryChanges, 1000);

let materialNameIndexCache = null;
let materialNameIndexMtime = 0;
let chineseNameIndexCache = null;
let chineseDisplayNameCache = null;

function loadChineseDisplayNames() {
    if (chineseDisplayNameCache) {
        return chineseDisplayNameCache;
    }
    const map = {};
    const langPath = path.join(PUBLIC_DIR, '26.1.2', 'assets', 'minecraft', 'lang', 'zh_cn.json');
    const lang = loadJsonFile(langPath, {});
    Object.keys(lang).forEach((key) => {
        if (!key.startsWith('item.minecraft.') && !key.startsWith('block.minecraft.')) {
            return;
        }
        const itemId = normalizeMaterialId(key.replace(/^item\.minecraft\./, '').replace(/^block\.minecraft\./, ''));
        if (itemId) {
            map[itemId] = lang[key];
        }
    });
    chineseDisplayNameCache = map;
    return map;
}

function loadChineseNameIndex() {
    if (chineseNameIndexCache) {
        return chineseNameIndexCache;
    }
    const index = new Map();
    const langPath = path.join(PUBLIC_DIR, '26.1.2', 'assets', 'minecraft', 'lang', 'zh_cn.json');
    const lang = loadJsonFile(langPath, {});
    Object.keys(lang).forEach((key) => {
        if (!key.startsWith('item.minecraft.') && !key.startsWith('block.minecraft.')) {
            return;
        }
        const itemId = normalizeMaterialId(key.replace(/^item\.minecraft\./, '').replace(/^block\.minecraft\./, ''));
        const lookup = materialListParser.normalizeLookupKey(lang[key]);
        if (itemId && lookup && !index.has(lookup)) {
            index.set(lookup, itemId);
        }
    });
    chineseNameIndexCache = index;
    return index;
}

function resolveItemDisplayName(itemId, label, mappings, priceData) {
    if (itemId) {
        const zhName = loadChineseDisplayNames()[itemId];
        if (zhName) return zhName;
        if (mappings[itemId] && mappings[itemId].displayName) {
            return mappings[itemId].displayName;
        }
        if (priceData[itemId] && priceData[itemId].customDisplayName) {
            return priceData[itemId].customDisplayName;
        }
    }
    return label || itemId || '未知';
}

function getCatalogBuyPrice(itemId, priceData) {
    const buy = itemId && priceData[itemId] ? Number(priceData[itemId].buy) : NaN;
    return Number.isFinite(buy) && buy >= 0 ? buy : null;
}

function loadMaterialNameIndex() {
    if (!fs.existsSync(ITEMS_DB_PATH)) {
        return new Map();
    }
    const stat = fs.statSync(ITEMS_DB_PATH);
    if (materialNameIndexCache && materialNameIndexMtime === stat.mtimeMs) {
        return materialNameIndexCache;
    }
    const index = new Map();
    const add = (label, itemId) => {
        const key = materialListParser.normalizeLookupKey(label);
        if (!key || index.has(key)) return;
        index.set(key, itemId);
    };
    const items = loadYamlFile(ITEMS_DB_PATH);
    Object.keys(items).forEach((itemId) => {
        const row = items[itemId];
        const normId = normalizeMaterialId(itemId);
        if (!normId) return;
        add(normId, normId);
        add(normId.replace(/_/g, ' '), normId);
        if (row && typeof row === 'object' && row.name) {
            add(row.name, normId);
            add(String(row.name).replace(/\s+/g, '_'), normId);
        }
    });
    materialNameIndexCache = index;
    materialNameIndexMtime = stat.mtimeMs;
    return index;
}

function resolveMaterialItemId(label, explicitId) {
    const fromId = normalizeMaterialId(explicitId || label);
    if (fromId && loadPriceTables()[fromId]) {
        return fromId;
    }
    if (fromId) {
        const catalog = buildUltimateShopCatalogByMaterial(loadPriceTables());
        if (catalog[fromId]) return fromId;
    }
    const index = loadMaterialNameIndex();
    const key = materialListParser.normalizeLookupKey(label || explicitId);
    if (key && index.has(key)) {
        return index.get(key);
    }
    const zhIndex = loadChineseNameIndex();
    if (key && zhIndex.has(key)) {
        return zhIndex.get(key);
    }
    return fromId;
}

function pickCheapestBuyOffer(catalog, itemId, priceData) {
    const offers = catalog[itemId] || [];
    let best = null;
    let bestPrice = Infinity;
    offers.forEach((offer) => {
        const hasBuy = offer.buyAmount != null && offer.buyAmount !== '';
        if (!hasBuy) return;
        const unit = offer.buyAmountResolved != null
            ? Number(offer.buyAmountResolved)
            : resolveMcwwsPricePlaceholder(offer.buyAmount, priceData);
        if (!Number.isFinite(unit) || unit < 0) return;
        if (unit < bestPrice) {
            bestPrice = unit;
            best = offer;
        }
    });
    return best;
}

function quoteMaterialLines(materials, listName) {
    const priceData = loadPriceTables();
    const catalog = buildUltimateShopCatalogByMaterial(priceData);
    const mappings = loadYamlFile(MAPPING_PATH);
    const lines = [];
    let purchasableTotal = 0;
    let referenceTotal = 0;
    let purchasableCount = 0;
    let unavailableCount = 0;

    (materials || []).forEach((entry) => {
        const materialCount = Math.floor(Number(entry.count) || 0);
        const label = entry.label || entry.name || entry.itemId || '';
        const itemId = resolveMaterialItemId(label, entry.itemId);
        const displayName = resolveItemDisplayName(itemId, label, mappings, priceData);
        const catalogBuyPrice = itemId ? getCatalogBuyPrice(itemId, priceData) : null;
        const base = {
            label,
            itemId: itemId || null,
            materialCount,
            displayName,
            catalogBuyPrice
        };

        if (!itemId || materialCount <= 0) {
            unavailableCount += 1;
            lines.push({
                ...base,
                itemId: itemId || normalizeMaterialId(label),
                status: 'unresolved',
                statusLabel: '无法识别物品'
            });
            return;
        }

        const offer = pickCheapestBuyOffer(catalog, itemId, priceData);
        if (!offer) {
            unavailableCount += 1;
            const estimatedLineTotal = catalogBuyPrice != null
                ? Math.round(catalogBuyPrice * materialCount * 100) / 100
                : null;
            if (estimatedLineTotal != null) {
                referenceTotal = Math.round((referenceTotal + estimatedLineTotal) * 100) / 100;
            }
            lines.push({
                ...base,
                status: 'not_listed',
                statusLabel: '未上架',
                estimatedLineTotal
            });
            return;
        }

        const product = getShopProductDetails(offer.shopId, offer.slot);
        const productAmount = Math.max(1, Number(product && product.productAmount) || 1);
        const unitBuyPrice = offer.buyAmountResolved != null
            ? Number(offer.buyAmountResolved)
            : resolveMcwwsPricePlaceholder(offer.buyAmount, priceData);
        if (!Number.isFinite(unitBuyPrice) || unitBuyPrice < 0) {
            unavailableCount += 1;
            const estimatedLineTotal = catalogBuyPrice != null
                ? Math.round(catalogBuyPrice * materialCount * 100) / 100
                : null;
            if (estimatedLineTotal != null) {
                referenceTotal = Math.round((referenceTotal + estimatedLineTotal) * 100) / 100;
            }
            lines.push({
                ...base,
                status: 'no_price',
                statusLabel: '无买入价',
                estimatedLineTotal
            });
            return;
        }

        const purchaseQuantity = Math.ceil(materialCount / productAmount);
        const lineTotal = Math.round(unitBuyPrice * purchaseQuantity * 100) / 100;
        purchasableTotal = Math.round((purchasableTotal + lineTotal) * 100) / 100;
        purchasableCount += 1;
        lines.push({
            ...base,
            status: 'ok',
            statusLabel: '可购买',
            shopId: offer.shopId,
            slot: String(offer.slot),
            shopTitle: offer.shopTitleResolved || offer.shopTitle || offer.shopId,
            unitBuyPrice,
            purchaseQuantity,
            productAmount,
            lineTotal
        });
    });

    lines.sort((a, b) => (b.materialCount || 0) - (a.materialCount || 0));

    return {
        listName: String(listName || '').trim(),
        lines,
        purchasableTotal,
        referenceTotal,
        purchasableCount,
        unavailableCount,
        lineCount: lines.length
    };
}

function normalizeMaterialId(material) {
    if (material == null || material === '') {
        return null;
    }
    return String(material).trim().toLowerCase().replace(/-/g, '_');
}

function normalizePlayerKey(value) {
    return String(value == null ? '' : value).trim().toLowerCase();
}

function loadJsonFile(filePath, fallback) {
    if (!fs.existsSync(filePath)) {
        return fallback;
    }
    try {
        return JSON.parse(fs.readFileSync(filePath, 'utf8'));
    } catch (error) {
        console.error(`读取 JSON 文件失败: ${filePath}`, error);
        return fallback;
    }
}

function normalizePlayerNameField(value) {
    if (value == null) {
        return '';
    }
    if (typeof value === 'string') {
        return value.trim();
    }
    if (typeof value === 'object') {
        const decoded = decodeSkriptClassValue(value);
        return typeof decoded === 'string' ? decoded.trim() : '';
    }
    return String(value).trim();
}

function userMatchesPlayerEntry(user, entryKey, entry = {}) {
    const userKeys = new Set([
        normalizePlayerKey(user && user.username),
        normalizePlayerKey(user && user.playerId)
    ].filter(Boolean));
    const entryKeys = [
        entryKey,
        normalizePlayerNameField(entry.name),
        entry.username,
        entry.playerId,
        entry.uuid
    ].map(normalizePlayerKey).filter(Boolean);
    return entryKeys.some((key) => userKeys.has(key));
}

function userIsOp(user) {
    const ops = loadJsonFile(OPS_PATH, []);
    if (!Array.isArray(ops)) {
        return false;
    }
    return ops.some((entry) => userMatchesPlayerEntry(user, entry.uuid || entry.name, entry));
}

function entryHasEditorPermission(entry = {}) {
    const permissions = entry.permissions && typeof entry.permissions === 'object' ? entry.permissions : {};
    const nestedUltimateShop = permissions.ultimateshop && typeof permissions.ultimateshop === 'object'
        ? permissions.ultimateshop
        : {};
    return entry.ultimateshopEditor === true
        || entry.ultimateshop_editor === true
        || entry['ultimateshop.editor'] === true
        || permissions['ultimateshop.editor'] === true
        || nestedUltimateShop.editor === true;
}

function userHasEditorPermission(user) {
    const accessData = loadYamlFile(ADMIN_ACCESS_PATH);
    const players = accessData.players && typeof accessData.players === 'object'
        ? accessData.players
        : accessData;
    if (!players || typeof players !== 'object') {
        return false;
    }
    return Object.keys(players).some((key) => {
        const entry = players[key];
        return entry
            && typeof entry === 'object'
            && userMatchesPlayerEntry(user, key, entry)
            && entryHasEditorPermission(entry);
    });
}

function getAdminAccess(user) {
    const isOp = userIsOp(user);
    const hasEditorPermission = userHasEditorPermission(user);
    return {
        allowed: isOp || hasEditorPermission,
        isOp,
        hasEditorPermission
    };
}

function requireAdmin(req, res) {
    const user = authenticate(req);
    if (!user) {
        res.status(401).json({ error: '需要登录。' });
        return null;
    }
    const access = getAdminAccess(user);
    if (!access.allowed) {
        res.status(403).json({
            ...access,
            error: '你没有进入管理系统的权限。需要 OP 或 ultimateshop.editor 权限。'
        });
        return null;
    }
    return { user, access };
}

function normalizeShopLocation(raw = {}) {
    const out = {};
    if (raw.displayName != null) out.displayName = String(raw.displayName).trim();
    if (raw.world != null) out.world = String(raw.world).trim();
    if (raw.map != null) out.map = String(raw.map).trim();
    ['x', 'y', 'z'].forEach((key) => {
        if (raw[key] !== '' && raw[key] != null) {
            const num = Number(raw[key]);
            if (Number.isFinite(num)) out[key] = num;
        }
    });
    ['yaw', 'pitch', 'zoom'].forEach((key) => {
        if (raw[key] !== '' && raw[key] != null) {
            const num = Number(raw[key]);
            if (Number.isFinite(num)) out[key] = num;
        }
    });
    if (raw.viewUrl != null) out.viewUrl = normalizeMapViewUrl(String(raw.viewUrl).trim());
    if (raw.description != null) out.description = String(raw.description).trim();
    if (raw.enabled != null) out.enabled = raw.enabled === true || raw.enabled === 'true';
    return out;
}

function normalizeMapViewUrl(url) {
    const raw = String(url || '').trim();
    if (!raw) return raw;
    try {
        const parsed = new URL(raw);
        parsed.searchParams.delete('mcwws');
        let out = parsed.toString();
        if (out.endsWith('?')) out = out.slice(0, -1);
        return out;
    } catch {
        return raw
            .replace(/([?&])mcwws=[^&]*(?=&|$)/g, '$1')
            .replace(/[?&]$/, '');
    }
}

function parseBlueMapViewUrl(viewUrl) {
    const raw = String(viewUrl || '').trim();
    const hash = raw.includes('#') ? raw.slice(raw.indexOf('#') + 1) : raw;
    const parts = hash.split(':');
    if (parts.length < 4) {
        return null;
    }
    const x = Number(parts[1]);
    const y = Number(parts[2]);
    const z = Number(parts[3]);
    if (![x, y, z].every(Number.isFinite)) {
        return null;
    }
    return {
        map: parts[0],
        x,
        y,
        z,
        yaw: parts[4] != null ? Number(parts[4]) : null,
        pitch: parts[5] != null ? Number(parts[5]) : null,
        zoom: parts[6] != null ? Number(parts[6]) : null,
        mode: parts[9] || null
    };
}

function loadShopLocations() {
    const data = loadYamlFile(SHOP_LOCATIONS_PATH);
    return data && typeof data === 'object' ? data : {};
}

function listUltimateShopShops() {
    if (!fs.existsSync(ULTIMATE_SHOP_SHOPS_DIR)) {
        return [];
    }
    const langMap = loadUltimateShopLangMap();
    const locations = loadShopLocations();
    return fs.readdirSync(ULTIMATE_SHOP_SHOPS_DIR)
        .filter((file) => file.endsWith('.yml'))
        .sort((a, b) => a.localeCompare(b))
        .map((file) => {
            const shopId = path.basename(file, '.yml');
            const doc = loadYamlFile(path.join(ULTIMATE_SHOP_SHOPS_DIR, file));
            const settings = doc.settings && typeof doc.settings === 'object' ? doc.settings : {};
            const items = doc.items && typeof doc.items === 'object' ? doc.items : {};
            const rawTitle = settings['shop-name'] != null ? settings['shop-name'] : shopId;
            const location = normalizeShopLocation(locations[shopId] || {});
            return {
                id: shopId,
                file,
                title: rawTitle,
                titleResolved: resolveUltimateShopLangText(rawTitle, langMap) || shopId,
                menu: settings.menu || null,
                itemCount: Object.keys(items).length,
                location
            };
        });
}

function listShopMapMarkers() {
    const mappings = loadYamlFile(MAPPING_PATH) || {};
    const priceData = loadPriceTables();
    const zhLabels = loadMinecraftZhLabels();

    return listUltimateShopShops()
        .map((shop) => {
            const location = shop.location || {};
            const parsed = parseBlueMapViewUrl(location.viewUrl);
            if (!location.viewUrl || !parsed || location.enabled === false) {
                return null;
            }
            const shopFile = path.join(ULTIMATE_SHOP_SHOPS_DIR, shop.file || `${shop.id}.yml`);
            const shopDoc = fs.existsSync(shopFile) ? loadYamlFile(shopFile) : {};
            const shopItems = shopDoc.items && typeof shopDoc.items === 'object' ? shopDoc.items : {};
            const trade = collectShopTradeCatalog(shopItems, mappings, priceData, zhLabels);
            return {
                id: shop.id,
                label: shop.titleResolved || shop.id,
                shopId: shop.id,
                itemCount: shop.itemCount,
                description: location.description || '',
                viewUrl: location.viewUrl,
                tradeItemIds: trade.tradeItemIds,
                tradeLabels: trade.tradeLabels,
                map: parsed.map,
                position: {
                    x: parsed.x,
                    y: parsed.y,
                    z: parsed.z
                },
                view: {
                    yaw: parsed.yaw,
                    pitch: parsed.pitch,
                    zoom: parsed.zoom,
                    mode: parsed.mode
                }
            };
        })
        .filter(Boolean);
}

/** 移除 BlueMap 原生 markers.json 中的商店图层，避免普通访客看到商店 POI */
function purgeBlueMapShopMarkers() {
    if (!fs.existsSync(BLUEMAP_WEB_MAPS_DIR)) {
        return;
    }
    fs.readdirSync(BLUEMAP_WEB_MAPS_DIR, { withFileTypes: true })
        .filter((entry) => entry.isDirectory())
        .forEach((entry) => {
            const markersFile = path.join(BLUEMAP_WEB_MAPS_DIR, entry.name, 'live', 'markers.json');
            if (!fs.existsSync(markersFile)) return;
            const doc = loadJsonFile(markersFile, {});
            if (!doc['mcwws-shops']) return;
            delete doc['mcwws-shops'];
            fs.writeFileSync(markersFile, JSON.stringify(doc), 'utf8');
        });
}

function firstEconomyPriceAmount(priceSection) {
    if (!priceSection || typeof priceSection !== 'object') {
        return null;
    }
    const keys = Object.keys(priceSection).sort((a, b) => Number(a) - Number(b));
    for (let i = 0; i < keys.length; i += 1) {
        const entry = priceSection[keys[i]];
        if (entry && typeof entry === 'object' && Object.prototype.hasOwnProperty.call(entry, 'amount')) {
            return entry.amount;
        }
    }
    return null;
}

function collectProductMaterials(products) {
    const out = [];
    if (!products || typeof products !== 'object') {
        return out;
    }
    Object.keys(products).forEach((k) => {
        const slot = products[k];
        if (slot && typeof slot === 'object' && slot.material != null) {
            const norm = normalizeMaterialId(slot.material);
            if (norm) {
                out.push(norm);
            }
        }
    });
    return out;
}

let minecraftZhLabelCache = null;

function loadMinecraftZhLabels() {
    if (minecraftZhLabelCache) {
        return minecraftZhLabelCache;
    }
    minecraftZhLabelCache = {};
    const candidates = [
        path.join(__dirname, '..', '..', '..', 'UltimateShop', 'zh_cn.json'),
        path.join(__dirname, '..', '..', '..', 'Geyser-Spigot', 'locales', 'zh_cn.json')
    ];
    candidates.forEach((filePath) => {
        if (!fs.existsSync(filePath)) {
            return;
        }
        try {
            const doc = JSON.parse(fs.readFileSync(filePath, 'utf8'));
            Object.keys(doc).forEach((key) => {
                if (!key.startsWith('block.minecraft.')) {
                    return;
                }
                const id = key.slice('block.minecraft.'.length);
                if (doc[key]) {
                    minecraftZhLabelCache[id] = String(doc[key]);
                }
            });
        } catch {
            /* ignore */
        }
    });
    return minecraftZhLabelCache;
}

function resolveTradeItemLabel(materialId, mappings, priceRow, zhLabels) {
    const norm = normalizeMaterialId(materialId);
    if (!norm) {
        return '';
    }
    const mapping = mappings && mappings[norm];
    if (mapping && mapping.displayName) {
        return String(mapping.displayName).trim();
    }
    if (priceRow) {
        const custom = priceRow.customDisplayName || priceRow.displayName;
        if (custom) {
            return String(custom).trim();
        }
    }
    if (zhLabels[norm]) {
        return zhLabels[norm];
    }
    return norm.replace(/_/g, ' ');
}

function collectShopTradeCatalog(shopItems, mappings, priceData, zhLabels) {
    const ids = new Set();
    if (!shopItems || typeof shopItems !== 'object') {
        return { tradeItemIds: [], tradeLabels: [] };
    }
    Object.keys(shopItems).forEach((slot) => {
        const def = shopItems[slot];
        if (!def || typeof def !== 'object') {
            return;
        }
        collectProductMaterials(def.products).forEach((mat) => ids.add(mat));
    });
    const tradeItemIds = [...ids].sort();
    const tradeLabels = tradeItemIds.map((id) => resolveTradeItemLabel(id, mappings, priceData[id], zhLabels));
    return { tradeItemIds, tradeLabels };
}

function stripMinecraftColorCodes(value) {
    return String(value || '')
        .replace(/&#[0-9a-fA-F]{6}/g, '')
        .replace(/[&§][0-9a-fk-orA-FK-OR]/g, '')
        .trim();
}

function flattenObject(obj, prefix = '', out = {}) {
    if (!obj || typeof obj !== 'object') {
        return out;
    }
    Object.keys(obj).forEach((key) => {
        const fullKey = prefix ? `${prefix}.${key}` : key;
        const val = obj[key];
        if (val && typeof val === 'object' && !Array.isArray(val)) {
            flattenObject(val, fullKey, out);
        } else {
            out[fullKey] = val;
            out[key] = val;
        }
    });
    return out;
}

function loadUltimateShopLangMap() {
    return flattenObject(loadYamlFile(ULTIMATE_SHOP_LANG_FILE));
}

function resolveUltimateShopLangText(value, langMap) {
    const raw = String(value == null ? '' : value);
    const resolved = raw.replace(/\{lang:([^}]+)\}/g, (match, key) => {
        const text = langMap[key];
        return text == null ? match : String(text);
    });
    return stripMinecraftColorCodes(resolved);
}

function resolveMcwwsPricePlaceholder(value, priceData) {
    if (typeof value === 'number') {
        return value;
    }
    const raw = String(value == null ? '' : value).trim();
    const match = raw.match(/^%mcwws\.price_(buy|sell)_(.+)%$/i);
    if (!match) {
        const num = Number(raw);
        return Number.isFinite(num) ? num : null;
    }
    const kind = match[1].toLowerCase();
    const itemId = normalizeMaterialId(match[2]);
    const item = itemId && priceData ? priceData[itemId] : null;
    const resolved = item && kind === 'buy' ? item.buy : item && item.sell;
    const num = Number(resolved);
    return Number.isFinite(num) ? num : null;
}

/**
 * 扫描 UltimateShop/shops/*.yml，按物品 material（与网页 itemId 小写一致）建立可交易报价列表。
 * @returns {Record<string, Array<{shopId: string, shopTitle: string|null, slot: string, buyAmount: *, sellAmount: *}>>}
 */
function buildUltimateShopCatalogByMaterial(priceData = {}) {
    const catalog = {};
    if (!fs.existsSync(ULTIMATE_SHOP_SHOPS_DIR)) {
        return catalog;
    }

    const langMap = loadUltimateShopLangMap();
    const shopLocations = loadShopLocations();
    const files = fs.readdirSync(ULTIMATE_SHOP_SHOPS_DIR).filter((f) => f.endsWith('.yml'));
    files.forEach((file) => {
        const shopId = path.basename(file, '.yml');
        const doc = loadYamlFile(path.join(ULTIMATE_SHOP_SHOPS_DIR, file));
        const items = doc.items;
        if (!items || typeof items !== 'object') {
            return;
        }
        const shopTitle = doc.settings && doc.settings['shop-name'] != null ? doc.settings['shop-name'] : null;

        Object.keys(items).forEach((slot) => {
            const def = items[slot];
            if (!def || typeof def !== 'object') {
                return;
            }
            const materials = [...new Set(collectProductMaterials(def.products))];
            if (materials.length === 0) {
                return;
            }
            const buyAmount = firstEconomyPriceAmount(def['buy-prices']);
            const sellAmount = firstEconomyPriceAmount(def['sell-prices']);
            const offer = {
                shopId,
                shopTitle,
                shopTitleResolved: shopTitle == null ? null : resolveUltimateShopLangText(shopTitle, langMap),
                slot,
                buyAmount,
                sellAmount,
                buyAmountResolved: resolveMcwwsPricePlaceholder(buyAmount, priceData),
                sellAmountResolved: resolveMcwwsPricePlaceholder(sellAmount, priceData),
                location: normalizeShopLocation(shopLocations[shopId] || {})
            };
            materials.forEach((mat) => {
                if (!catalog[mat]) {
                    catalog[mat] = [];
                }
                catalog[mat].push(offer);
            });
        });
    });

    return catalog;
}

const ORPHAN_SHOP_CATEGORIES = [
    { id: 'daily', name: '每日商店', icon: 'clock', langNameKey: 'daily-shop-name' }
];

let ultimateShopRegistryCache = null;

function buildMaterialToShopsMap() {
    const map = {};
    if (!fs.existsSync(ULTIMATE_SHOP_SHOPS_DIR)) {
        return map;
    }
    const files = fs.readdirSync(ULTIMATE_SHOP_SHOPS_DIR).filter((f) => f.endsWith('.yml'));
    files.forEach((file) => {
        const shopId = path.basename(file, '.yml');
        const doc = loadYamlFile(path.join(ULTIMATE_SHOP_SHOPS_DIR, file));
        const items = doc.items;
        if (!items || typeof items !== 'object') {
            return;
        }
        Object.keys(items).forEach((slot) => {
            const def = items[slot];
            if (!def || typeof def !== 'object') {
                return;
            }
            collectProductMaterials(def.products).forEach((mat) => {
                if (!map[mat]) {
                    map[mat] = new Set();
                }
                map[mat].add(shopId);
            });
        });
    });
    const plain = {};
    Object.keys(map).forEach((mat) => {
        plain[mat] = [...map[mat]].sort();
    });
    return plain;
}

function loadUltimateShopCategoryRegistry() {
    if (ultimateShopRegistryCache) {
        return ultimateShopRegistryCache;
    }

    const langMap = loadUltimateShopLangMap();
    const mainMenu = loadYamlFile(ULTIMATE_SHOP_MAIN_MENU_PATH);
    const categories = [];
    const knownShopIds = new Set();

    const pushCategory = (entry) => {
        if (!entry || !entry.id || knownShopIds.has(entry.id)) {
            return;
        }
        knownShopIds.add(entry.id);
        categories.push(entry);
    };

    if (mainMenu && mainMenu.buttons && typeof mainMenu.buttons === 'object') {
        Object.keys(mainMenu.buttons).forEach((menuKey) => {
            const btn = mainMenu.buttons[menuKey];
            if (!btn || typeof btn !== 'object') {
                return;
            }
            const actions = btn.actions || {};
            Object.values(actions).forEach((action) => {
                if (!action || action.type !== 'shop_menu' || !action.shop) {
                    return;
                }
                const shopId = String(action.shop).trim();
                const displayItem = btn['display-item'] || btn.displayItem || {};
                const resolvedName = resolveUltimateShopLangText(displayItem.name || '', langMap) || shopId;
                pushCategory({
                    id: shopId,
                    menuKey,
                    name: stripMinecraftColorCodes(resolvedName) || shopId,
                    icon: normalizeMaterialId(displayItem.material) || 'chest',
                    order: categories.length
                });
            });
        });
    }

    ORPHAN_SHOP_CATEGORIES.forEach((orphan) => {
        const shopFile = path.join(ULTIMATE_SHOP_SHOPS_DIR, `${orphan.id}.yml`);
        if (!fs.existsSync(shopFile)) {
            return;
        }
        let name = orphan.name;
        if (orphan.langNameKey && langMap[orphan.langNameKey]) {
            name = stripMinecraftColorCodes(resolveUltimateShopLangText(`{lang:${orphan.langNameKey}}`, langMap));
        }
        const entry = {
            id: orphan.id,
            menuKey: null,
            name: name || orphan.id,
            icon: orphan.icon || 'chest',
            order: categories.length
        };
        if (orphan.insertAfter) {
            const idx = categories.findIndex((cat) => cat.id === orphan.insertAfter);
            if (idx >= 0) {
                entry.order = idx + 1;
                categories.splice(idx + 1, 0, entry);
                categories.forEach((cat, i) => {
                    cat.order = i;
                });
                knownShopIds.add(entry.id);
                return;
            }
        }
        pushCategory(entry);
    });

    const materialToShops = buildMaterialToShopsMap();
    const shopItemCounts = {};
    categories.forEach((cat) => {
        shopItemCounts[cat.id] = 0;
    });
    Object.values(materialToShops).forEach((shopIds) => {
        shopIds.forEach((shopId) => {
            if (shopItemCounts[shopId] == null) {
                shopItemCounts[shopId] = 0;
            }
            shopItemCounts[shopId] += 1;
        });
    });

    ultimateShopRegistryCache = {
        categories,
        materialToShops,
        shopItemCounts
    };
    return ultimateShopRegistryCache;
}

function pickPrimaryUltimateShopOffer(offers, priceData) {
    if (!Array.isArray(offers) || !offers.length) {
        return null;
    }
    let best = offers[0];
    let bestPrice = Infinity;
    offers.forEach((offer) => {
        const hasBuy = offer.buyAmount != null && offer.buyAmount !== '';
        if (!hasBuy) {
            return;
        }
        const unit = offer.buyAmountResolved != null
            ? Number(offer.buyAmountResolved)
            : resolveMcwwsPricePlaceholder(offer.buyAmount, priceData);
        if (!Number.isFinite(unit) || unit < 0) {
            return;
        }
        if (unit < bestPrice) {
            bestPrice = unit;
            best = offer;
        }
    });
    return best;
}

function buildUltimateShopMappingsFromCatalog(priceData) {
    const catalog = buildUltimateShopCatalogByMaterial(priceData);
    const mappings = {};
    Object.keys(catalog).forEach((itemId) => {
        const offer = pickPrimaryUltimateShopOffer(catalog[itemId], priceData);
        if (!offer) {
            return;
        }
        mappings[itemId] = {
            shop: offer.shopId,
            item: String(offer.slot),
            amount: 1
        };
    });
    return mappings;
}

function syncUltimateShopMappingsFile() {
    try {
        const priceData = loadPriceTables();
        const generated = buildUltimateShopMappingsFromCatalog(priceData);
        const header = `# UltimateShop 映射（由 server.js 根据 shops/*.yml 自动生成）\n# shop = 商店 ID（如 blocks、farming），item = 该商店 yml 中的槽位字母\n\n`;
        const body = yaml.dump(generated, { lineWidth: 120, noRefs: true });
        fs.writeFileSync(MAPPING_PATH, header + body, 'utf8');
        yamlFileCache.delete(MAPPING_PATH);
        return Object.keys(generated).length;
    } catch (error) {
        console.error('同步 UltimateShop 映射失败:', error.message || error);
        return 0;
    }
}

const ORDER_LINE_FIELD_NAMES = [
    'itemId', 'shopId', 'slot', 'quantity', 'unitBuyPrice',
    'lineTotal', 'productAmount', 'material', 'shopTitle', 'status'
];

/** Node 数组 → Skript/skript-yaml 可读的 lines.1、lines.2 … */
function linesToSkriptMap(lines) {
    if (!lines) return {};
    if (!Array.isArray(lines)) return lines;
    const out = {};
    lines.forEach((line, i) => {
        if (line && typeof line === 'object') {
            out[String(i + 1)] = line;
        }
    });
    return out;
}

/** Skript 映射或历史数组 → API 用数组 */
function linesToArray(lines) {
    if (!lines) return [];
    if (Array.isArray(lines)) return lines;
    return Object.keys(lines)
        .sort((a, b) => Number(a) - Number(b))
        .map((k) => lines[k])
        .filter((line) => line && typeof line === 'object');
}

/**
 * 修复 skript-yaml 误写导致的缩进：商品字段应在 lines.'1' 下（16 空格），而非与 '1' 同级（12 空格）。
 */
function repairPendingOrdersYamlText(text) {
    if (!text || typeof text !== 'string') {
        return text;
    }
    return text.split('\n').map((line) => {
        for (const field of ORDER_LINE_FIELD_NAMES) {
            if (line.startsWith(`            ${field}:`)) {
                return `                ${line.slice(12)}`;
            }
        }
        return line;
    }).join('\n');
}

/** 将误挂在 lines 根上的字段合并进 lines.'1'，并统一为 Skript 映射格式 */
function normalizeOrderLines(order) {
    if (!order || typeof order !== 'object' || !order.lines || typeof order.lines !== 'object') {
        return false;
    }
    let changed = false;
    const raw = order.lines;

    if (Array.isArray(raw)) {
        order.lines = linesToSkriptMap(raw);
        changed = true;
    }

    const lines = order.lines;
    const numericKeys = Object.keys(lines).filter((k) => /^\d+$/.test(k));
    const flatOnLines = ORDER_LINE_FIELD_NAMES.filter((f) => lines[f] != null);

    if (flatOnLines.length && numericKeys.length) {
        const firstKey = numericKeys.sort((a, b) => Number(a) - Number(b))[0];
        const merged = { ...(lines[firstKey] && typeof lines[firstKey] === 'object' ? lines[firstKey] : {}) };
        flatOnLines.forEach((f) => {
            merged[f] = lines[f];
            delete lines[f];
        });
        lines[firstKey] = merged;
        changed = true;
    }

    const arr = linesToArray(lines);
    if (order.lineCount !== arr.length) {
        order.lineCount = arr.length;
        changed = true;
    }

    return changed;
}

function normalizePendingOrdersStore(store) {
    let changed = false;
    Object.values(store.orders || {}).forEach((order) => {
        if (normalizeOrderLines(order)) {
            changed = true;
        }
    });
    return changed;
}

function migratePendingOrdersLines(store) {
    let changed = false;
    Object.values(store.orders || {}).forEach((order) => {
        if (!order || typeof order !== 'object' || !Array.isArray(order.lines)) {
            return;
        }
        order.lines = linesToSkriptMap(order.lines);
        changed = true;
    });
    return changed;
}

function loadPendingOrdersYaml() {
    if (!fs.existsSync(PENDING_ORDERS_PATH)) {
        return { data: { next_id: 1, orders: {} }, repaired: false };
    }
    const rawText = fs.readFileSync(PENDING_ORDERS_PATH, 'utf8');
    try {
        return { data: yaml.load(rawText, { schema: YAML_SCHEMA }) || {}, repaired: false };
    } catch (error) {
        console.error('[MCWWS] pending_orders.yml 解析失败，尝试自动修复缩进:', error.message);
        const repairedText = repairPendingOrdersYamlText(rawText);
        try {
            const data = yaml.load(repairedText, { schema: YAML_SCHEMA }) || {};
            fs.writeFileSync(PENDING_ORDERS_PATH, repairedText, 'utf8');
            console.warn('[MCWWS] pending_orders.yml 已自动修复缩进并写回');
            return { data, repaired: true };
        } catch (error2) {
            console.error('[MCWWS] 修复后仍无法解析 pending_orders.yml', error2);
            return { data: null, repaired: false };
        }
    }
}

function loadPendingOrdersStore() {
    const { data: loaded, repaired } = loadPendingOrdersYaml();
    const data = loaded && typeof loaded === 'object' ? loaded : { next_id: 1, orders: {} };
    if (!data.orders || typeof data.orders !== 'object') {
        data.orders = {};
    }
    if (!Number.isFinite(Number(data.next_id))) {
        data.next_id = 1;
    }
    let changed = repaired;
    if (migratePendingOrdersLines(data)) {
        changed = true;
    }
    if (normalizePendingOrdersStore(data)) {
        changed = true;
    }
    if (changed) {
        savePendingOrdersStore(data);
    }
    return data;
}

function savePendingOrdersStore(data) {
    saveYamlFile(PENDING_ORDERS_PATH, data);
}

function getShopProductDetails(shopId, slot) {
    const file = path.join(ULTIMATE_SHOP_SHOPS_DIR, `${shopId}.yml`);
    if (!fs.existsSync(file)) {
        return null;
    }
    const doc = loadYamlFile(file);
    const def = doc.items && doc.items[slot];
    if (!def || typeof def !== 'object') {
        return null;
    }
    const materials = collectProductMaterials(def.products);
    let productAmount = 1;
    if (def.products && typeof def.products === 'object') {
        const firstKey = Object.keys(def.products).sort((a, b) => Number(a) - Number(b))[0];
        const product = firstKey ? def.products[firstKey] : null;
        const amount = Number(product && product.amount);
        if (Number.isFinite(amount) && amount > 0) {
            productAmount = amount;
        }
    }
    const buyRaw = firstEconomyPriceAmount(def['buy-prices']);
    return {
        material: materials[0] || null,
        productAmount,
        buyRaw,
        hasBuyPrice: buyRaw != null && buyRaw !== ''
    };
}

function findUltimateShopOffer(catalog, itemId, shopId, slot) {
    const normItemId = normalizeMaterialId(itemId);
    const offers = normItemId ? (catalog[normItemId] || []) : [];
    const normShopId = String(shopId || '').trim();
    const normSlot = String(slot || '').trim();
    return offers.find((offer) => offer.shopId === normShopId && String(offer.slot) === normSlot) || null;
}

function resolveCheckoutLine(rawLine, priceData, catalog) {
    const itemId = normalizeMaterialId(rawLine && rawLine.itemId);
    const shopId = String(rawLine && rawLine.shopId || '').trim();
    const slot = String(rawLine && rawLine.slot || '').trim();
    const quantity = Math.floor(Number(rawLine && rawLine.quantity));
    if (!itemId || !shopId || !slot) {
        return { error: '订单行缺少 itemId、shopId 或 slot。' };
    }
    if (!Number.isFinite(quantity) || quantity < 1 || quantity > 10000) {
        return { error: `无效数量：${rawLine && rawLine.itemId}` };
    }
    const offer = findUltimateShopOffer(catalog, itemId, shopId, slot);
    if (!offer) {
        return { error: `未找到上架位置：${itemId} / ${shopId} / ${slot}` };
    }
    const product = getShopProductDetails(shopId, slot);
    if (!product || !product.hasBuyPrice) {
        return { error: `该商品不可购买：${shopId} / ${slot}` };
    }
    if (product.material && product.material !== itemId) {
        return { error: `物品与商店槽位不匹配：${itemId}` };
    }
    const unitBuyPrice = offer.buyAmountResolved != null
        ? Number(offer.buyAmountResolved)
        : resolveMcwwsPricePlaceholder(offer.buyAmount, priceData);
    if (!Number.isFinite(unitBuyPrice) || unitBuyPrice < 0) {
        return { error: `无法解析买入价：${itemId}` };
    }
    const lineTotal = Math.round(unitBuyPrice * quantity * 100) / 100;
    return {
        line: {
            itemId,
            shopId,
            slot,
            quantity,
            unitBuyPrice,
            lineTotal,
            productAmount: product.productAmount,
            material: product.material,
            shopTitle: offer.shopTitleResolved || offer.shopTitle || shopId,
            status: 'pending'
        }
    };
}

function listOrdersForUser(user, limit = 30) {
    const store = loadPendingOrdersStore();
    const playerId = String(user.playerId || '').trim().toLowerCase();
    const username = String(user.username || '').trim().toLowerCase();
    const uuid = resolvePlayerUuid(user.playerId);
    const uuidKey = uuid ? String(uuid).trim().toLowerCase() : '';
    const orders = Object.values(store.orders || {})
        .filter((order) => {
            const oid = String(order.playerId || '').trim().toLowerCase();
            const ouser = String(order.username || '').trim().toLowerCase();
            const ouuid = String(order.playerUuid || '').trim().toLowerCase();
            return (playerId && oid === playerId)
                || (username && ouser === username)
                || (uuidKey && ouuid === uuidKey);
        })
        .sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
        .slice(0, limit);
    return orders;
}

function generateToken() {
    return crypto.randomBytes(32).toString('hex');
}

function hashPassword(password, salt) {
    return crypto.scryptSync(password, salt, 64).toString('hex');
}

function authenticate(req) {
    const authHeader = req.headers.authorization || '';
    if (!authHeader.startsWith('Bearer ')) {
        return null;
    }
    const token = authHeader.substring(7);
    if (!token) {
        return null;
    }
    const users = readUsers();
    return users.find(user => user.authToken === token) || null;
}

app.post('/api/register', (req, res) => {
    try {
        const { username, password, playerId } = req.body || {};
        if (!username || !password || !playerId) {
            return res.status(400).json({ error: '请填写用户名、密码和游戏玩家ID。' });
        }

        const users = readUsers();
        if (users.some(user => user.username === username)) {
            return res.status(409).json({ error: '用户名已存在，请换一个用户名。' });
        }

        const salt = crypto.randomBytes(16).toString('hex');
        const passwordHash = hashPassword(password, salt);
        const authToken = generateToken();
        const createdAt = new Date().toISOString();

        const newUser = {
            id: users.length > 0 ? Math.max(...users.map(user => user.id || 0)) + 1 : 1,
            username,
            passwordHash,
            passwordSalt: salt,
            playerId,
            authToken,
            createdAt
        };

        users.push(newUser);
        saveUsers(users);

        res.json({
            status: 'ok',
            username,
            playerId,
            authToken,
            message: '注册成功，已自动登录。'
        });
    } catch (error) {
        console.error('注册失败:', error);
        res.status(500).json({ error: '注册失败，请稍后重试。' });
    }
});

app.post('/api/login', (req, res) => {
    try {
        const { username, password } = req.body || {};
        if (!username || !password) {
            return res.status(400).json({ error: '请填写用户名和密码。' });
        }

        const users = readUsers();
        const user = users.find(item => item.username === username);
        if (!user) {
            return res.status(401).json({ error: '用户名或密码错误。' });
        }

        const passwordHash = hashPassword(password, user.passwordSalt);
        if (passwordHash !== user.passwordHash) {
            return res.status(401).json({ error: '用户名或密码错误。' });
        }

        const authToken = generateToken();
        user.authToken = authToken;
        saveUsers(users);

        res.json({
            status: 'ok',
            username: user.username,
            playerId: user.playerId,
            authToken,
            message: '登录成功。'
        });
    } catch (error) {
        console.error('登录失败:', error);
        res.status(500).json({ error: '登录失败，请稍后重试。' });
    }
});

app.post('/api/logout', (req, res) => {
    try {
        const authHeader = req.headers.authorization || '';
        if (authHeader.startsWith('Bearer ')) {
            const token = authHeader.substring(7);
            const users = readUsers();
            const user = users.find(item => item.authToken === token);
            if (user) {
                user.authToken = null;
                saveUsers(users);
            }
        }
        res.json({ status: 'ok', message: '已退出登录。' });
    } catch (error) {
        console.error('注销失败:', error);
        res.status(500).json({ error: '注销失败，请稍后重试。' });
    }
});

app.get('/api/profile', (req, res) => {
    try {
        const user = authenticate(req);
        if (!user) {
            return res.status(401).json({ error: '需要登录。' });
        }
        res.json({
            username: user.username,
            playerId: user.playerId,
            createdAt: user.createdAt
        });
    } catch (error) {
        console.error('获取用户信息失败:', error);
        res.status(500).json({ error: '获取用户信息失败。' });
    }
});

let essentialsCurrencySymbol = '￥';
let essentialsCurrencySuffix = false;
let essentialsCurrencyLoadedAt = 0;

function normalizeWebCurrencySymbol(symbol) {
    const value = String(symbol ?? '￥').trim();
    if (!value || value === '¥' || value === '\u00a5') {
        return '￥';
    }
    return value;
}

function loadEssentialsCurrencyConfig() {
    const now = Date.now();
    if (essentialsCurrencyLoadedAt && now - essentialsCurrencyLoadedAt < 300000) {
        return;
    }
    essentialsCurrencyLoadedAt = now;
    if (!fs.existsSync(ESSENTIALS_CONFIG_PATH)) {
        return;
    }
    try {
        const config = yaml.load(fs.readFileSync(ESSENTIALS_CONFIG_PATH, 'utf8'));
        essentialsCurrencySymbol = normalizeWebCurrencySymbol(config?.['currency-symbol']);
        essentialsCurrencySuffix = Boolean(config?.['currency-symbol-suffix']);
    } catch (error) {
        console.warn('读取 Essentials 货币配置失败:', error.message);
    }
}

function formatEssentialsBalance(amount) {
    loadEssentialsCurrencyConfig();
    const num = Number(amount);
    if (!Number.isFinite(num)) {
        return null;
    }
    const formatted = num.toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const symbol = normalizeWebCurrencySymbol(essentialsCurrencySymbol);
    if (essentialsCurrencySuffix) {
        return `${formatted}${symbol}`;
    }
    return `${symbol}${formatted}`;
}

function readEssentialsBalance(uuid) {
    const file = path.join(ESSENTIALS_USERDATA_DIR, `${uuid}.yml`);
    if (!fs.existsSync(file)) {
        return null;
    }
    try {
        const data = yaml.load(fs.readFileSync(file, 'utf8'));
        const money = data?.money;
        if (money == null || money === '') {
            return 0;
        }
        const num = Number(money);
        return Number.isFinite(num) ? num : null;
    } catch {
        return null;
    }
}

/**
 * 将已 eco take 但 YAML 仍标 pending 的记录纠正为 done（避免网页多扣显示）
 */
function reconcileStuckEconomyDeductions() {
    const store = loadEconomyDeductionsStore();
    const orders = loadPendingOrdersStore();
    let changed = false;
    for (const entry of Object.values(store.pending || {})) {
        if (!entry || entry.status !== 'pending') {
            continue;
        }
        const orderKey = String(entry.orderId ?? '');
        if (!orderKey) {
            continue;
        }
        const order = orders.orders?.[orderKey];
        if (order?.economyChargedInGame === true) {
            entry.status = 'done';
            entry.mode = 'eco-take';
            entry.processedAt = entry.processedAt || new Date().toISOString();
            changed = true;
        }
    }
    if (changed) {
        saveEconomyDeductionsStore(store);
    }
}

/** 尚未由 Skript 执行的 eco take（status=pending），仅用于结账前短时校验 */
function sumPendingEssentialsDeductions(uuid) {
    const normUuid = String(uuid || '').trim().toLowerCase();
    if (!normUuid) {
        return 0;
    }
    const store = loadEconomyDeductionsStore();
    let sum = 0;
    for (const entry of Object.values(store.pending || {})) {
        if (!entry || entry.status !== 'pending') {
            continue;
        }
        if (String(entry.uuid || '').trim().toLowerCase() !== normUuid) {
            continue;
        }
        const amount = Number(entry.amount);
        if (Number.isFinite(amount) && amount > 0) {
            sum += amount;
        }
    }
    return Math.round(sum * 100) / 100;
}

/** 结账校验：文件余额减去仍在排队、且尚未扣款的金额 */
function readEffectiveEssentialsBalance(uuid) {
    const fileBalance = readEssentialsBalance(uuid);
    if (fileBalance == null) {
        return null;
    }
    const pending = sumPendingEssentialsDeductions(uuid);
    return Math.round((fileBalance - pending) * 100) / 100;
}

/** 展示用余额：与游戏 /bal 一致，读 Essentials userdata */
function readDisplayEssentialsBalance(uuid) {
    reconcileStuckEconomyDeductions();
    return readEssentialsBalance(uuid);
}

function formatEssentialsMoneyValue(amount) {
    const rounded = Math.round(Number(amount) * 100) / 100;
    if (!Number.isFinite(rounded)) {
        return '0.00';
    }
    return rounded.toFixed(2);
}

function writeEssentialsBalance(uuid, amount) {
    const file = path.join(ESSENTIALS_USERDATA_DIR, `${uuid}.yml`);
    if (!fs.existsSync(file)) {
        return false;
    }
    try {
        const formatted = formatEssentialsMoneyValue(amount);
        let text = fs.readFileSync(file, 'utf8');
        const moneyLine = `money: '${formatted}'`;
        if (/^money:\s*.+$/m.test(text)) {
            text = text.replace(/^money:\s*.+$/m, moneyLine);
        } else {
            text = `${text.trimEnd()}\n${moneyLine}\n`;
        }
        fs.writeFileSync(file, text, 'utf8');
        return true;
    } catch (error) {
        console.error(`写入 Essentials 零钱失败 (${uuid}):`, error);
        return false;
    }
}

function isPlayerOnlineForEconomy(uuid, playerId) {
    return isPlayerOnlineInGame(uuid, playerId);
}

function readSkriptOnlinePlayerRegistry() {
    if (!fs.existsSync(ONLINE_PLAYERS_PATH)) {
        return null;
    }
    try {
        const text = fs.readFileSync(ONLINE_PLAYERS_PATH, 'utf8');
        const players = [];
        for (const line of text.split(/\r?\n/)) {
            const trimmed = line.trim();
            if (!trimmed) {
                continue;
            }
            const sep = trimmed.indexOf('|');
            if (sep <= 0) {
                continue;
            }
            const uuid = trimmed.slice(0, sep).trim().toLowerCase();
            const name = trimmed.slice(sep + 1).trim();
            if (uuid) {
                players.push({ uuid, name });
            }
        }
        return players;
    } catch {
        return null;
    }
}

/** 以 Skript 写入的 Bukkit 在线列表为准，BlueMap 仅作备用 */
function isPlayerOnlineInGame(uuid, playerId) {
    const normUuid = String(uuid || '').trim().toLowerCase();
    const normName = String(playerId || '').trim().toLowerCase();
    if (!normUuid && !normName) {
        return false;
    }
    const registry = readSkriptOnlinePlayerRegistry();
    if (registry) {
        for (const entry of registry) {
            if (normUuid && entry.uuid === normUuid) {
                return true;
            }
            if (normName && String(entry.name || '').trim().toLowerCase() === normName) {
                return true;
            }
        }
    }
    return Boolean(readBlueMapLivePlayerLocation(uuid, playerId));
}

function loadEconomyDeductionsStore() {
    const data = loadYamlFile(ECONOMY_DEDUCTIONS_PATH);
    if (!data || typeof data !== 'object') {
        return { next_id: 1, pending: {} };
    }
    if (!data.pending || typeof data.pending !== 'object') {
        data.pending = {};
    }
    if (!Number.isFinite(Number(data.next_id))) {
        data.next_id = 1;
    }
    return data;
}

function saveEconomyDeductionsStore(data) {
    saveYamlFile(ECONOMY_DEDUCTIONS_PATH, data);
}

/** Skript 每秒读取并执行：eco take <玩家> <金额> <订单号> */
function appendEcoTakeQueueLine(playerId, amount, orderId) {
    const pid = String(playerId || '').trim();
    const amt = Math.round(Number(amount) * 100) / 100;
    const oid = String(orderId || '').trim();
    if (!pid || !Number.isFinite(amt) || amt <= 0) {
        return;
    }
    const line = `${pid}|${amt}|${oid}\n`;
    try {
        fs.appendFileSync(ECO_TAKE_QUEUE_PATH, line, 'utf8');
    } catch (error) {
        console.error('写入 eco_take_queue.txt 失败:', error);
    }
}

function enqueueEssentialsDeduction({
    uuid,
    playerId,
    amount,
    orderId,
    balanceAfter
}) {
    const store = loadEconomyDeductionsStore();
    const numericId = Number(store.next_id) || 1;
    const nowIso = new Date().toISOString();
    const entry = {
        id: numericId,
        uuid,
        playerId,
        amount: Math.round(Number(amount) * 100) / 100,
        orderId,
        status: 'pending',
        createdAt: nowIso,
        mode: 'eco-take'
    };
    if (balanceAfter != null && Number.isFinite(Number(balanceAfter))) {
        entry.balanceAfter = Math.round(Number(balanceAfter) * 100) / 100;
    }
    store.pending[String(numericId)] = entry;
    store.next_id = numericId + 1;
    saveEconomyDeductionsStore(store);
    return numericId;
}

/** 结账时立即写入 Essentials userdata（在线/离线统一） */
function applyCheckoutFileDeduction(uuid, amount) {
    const normAmount = Math.round(Number(amount) * 100) / 100;
    const fileBalance = readEssentialsBalance(uuid);
    if (fileBalance == null) {
        return { ok: false, error: '未找到 Essentials 经济存档。' };
    }
    if (fileBalance < normAmount) {
        return { ok: false, error: '零钱不足。' };
    }
    const newBalance = Math.round((fileBalance - normAmount) * 100) / 100;
    if (!writeEssentialsBalance(uuid, newBalance)) {
        return { ok: false, error: '写入 Essentials 零钱失败。' };
    }
    return { ok: true, newBalance, mode: 'file-checkout' };
}

/**
 * 扣除 Essentials 零钱：结账时 Node 写 userdata；Skript 对在线玩家执行 eco set 同步内存。
 */
function deductEssentialsBalance(uuid, playerId, amount) {
    const normAmount = Math.round(Number(amount) * 100) / 100;
    if (!Number.isFinite(normAmount) || normAmount <= 0) {
        return { ok: false, error: '扣款金额无效。' };
    }
    const balance = readEssentialsBalance(uuid);
    if (balance == null) {
        return { ok: false, error: '未找到 Essentials 经济存档。' };
    }
    if (balance < normAmount) {
        return { ok: false, error: '零钱不足。' };
    }
    const newBalance = Math.round((balance - normAmount) * 100) / 100;
    if (isPlayerOnlineForEconomy(uuid, playerId)) {
        return { ok: true, newBalance, mode: 'queue' };
    }
    if (!writeEssentialsBalance(uuid, newBalance)) {
        return { ok: false, error: '写入 Essentials 零钱失败。' };
    }
    return { ok: true, newBalance, mode: 'file' };
}

function readPlayerPlayHours(uuid) {
    const file = path.join(WORLD_STATS_DIR, `${uuid}.json`);
    if (!fs.existsSync(file)) {
        return null;
    }
    try {
        const stats = JSON.parse(fs.readFileSync(file, 'utf8'));
        const ticks = stats?.stats?.['minecraft:custom']?.['minecraft:play_time'];
        if (ticks == null) {
            return null;
        }
        const hours = Number(ticks) / MC_PLAY_TIME_TICKS_PER_HOUR;
        return Number.isFinite(hours) ? Math.floor(hours) : null;
    } catch {
        return null;
    }
}

let opsUuidSet = null;
let opsNameSet = null;
let opsLoadedAt = 0;

function loadOpsIndex() {
    const now = Date.now();
    if (opsUuidSet && now - opsLoadedAt < 60000) {
        return;
    }
    const uuids = new Set();
    const names = new Set();
    if (fs.existsSync(OPS_JSON_PATH)) {
        try {
            const entries = JSON.parse(fs.readFileSync(OPS_JSON_PATH, 'utf8'));
            if (Array.isArray(entries)) {
                entries.forEach((entry) => {
                    const uuid = String(entry?.uuid || '').trim().toLowerCase();
                    const name = String(entry?.name || '').trim().toLowerCase();
                    if (uuid) uuids.add(uuid);
                    if (name) names.add(name);
                });
            }
        } catch (error) {
            console.warn('读取 ops.json 失败:', error.message);
        }
    }
    opsUuidSet = uuids;
    opsNameSet = names;
    opsLoadedAt = now;
}

function isPlayerOp(uuid, playerName) {
    loadOpsIndex();
    const normUuid = String(uuid || '').trim().toLowerCase();
    const normName = String(playerName || '').trim().toLowerCase();
    return (normUuid && opsUuidSet.has(normUuid)) || (normName && opsNameSet.has(normName));
}

let scoreboardScoresCache = null;
let scoreboardLoadedAt = 0;

async function loadScoreboardScoresIndex() {
    const now = Date.now();
    if (scoreboardScoresCache && now - scoreboardLoadedAt < 30000) {
        return scoreboardScoresCache;
    }
    const index = new Map();
    if (fs.existsSync(SCOREBOARD_DAT_PATH)) {
        try {
            const { parsed } = await nbt.parse(fs.readFileSync(SCOREBOARD_DAT_PATH));
            const scores = parsed?.value?.data?.value?.PlayerScores?.value?.value || [];
            scores.forEach((entry) => {
                const objective = String(entry.Objective?.value || '');
                const name = String(entry.Name?.value || '').trim().toLowerCase();
                const score = entry.Score?.value;
                if (objective && name && score != null) {
                    index.set(`${objective}:${name}`, Number(score));
                }
            });
        } catch (error) {
            console.warn('读取 scoreboard.dat 失败:', error.message);
        }
    }
    scoreboardScoresCache = index;
    scoreboardLoadedAt = now;
    return index;
}

function lookupScoreboardScore(index, objective, playerName, uuid) {
    const keys = [
        `${objective}:${String(playerName || '').trim().toLowerCase()}`,
        `${objective}:${String(uuid || '').trim().toLowerCase()}`
    ];
    for (const key of keys) {
        if (index.has(key)) {
            return index.get(key);
        }
    }
    return null;
}

async function readPlayerEconomySnapshot(playerId, uuid) {
    const scoreIndex = await loadScoreboardScoresIndex();
    const balance = readDisplayEssentialsBalance(uuid);
    const playHours = readPlayerPlayHours(uuid);
    const op = isPlayerOp(uuid, playerId);
    const warningProgress = lookupScoreboardScore(scoreIndex, WARNING_OBJECTIVE, playerId, uuid);
    const online = Boolean(readBlueMapLivePlayerLocation(uuid, playerId));

    return {
        playerId,
        uuid,
        online,
        playHours,
        balance,
        balanceFormatted: balance == null ? null : formatEssentialsBalance(balance),
        role: op ? '管理员/服主' : '普通玩家',
        isOp: op,
        warningProgress,
        warningMax: WARNING_MAX
    };
}

app.get('/api/player-economy', async (req, res) => {
    try {
        const user = authenticate(req);
        if (!user) {
            return res.status(401).json({ error: '需要登录。' });
        }
        const playerId = String(user.playerId || '').trim();
        if (!playerId) {
            return res.status(400).json({ error: '账号未绑定游戏玩家 ID。' });
        }
        const uuid = resolvePlayerUuid(playerId);
        if (!uuid) {
            return res.status(404).json({ error: '未找到该玩家在服务器的存档。' });
        }
        const snapshot = await readPlayerEconomySnapshot(playerId, uuid);
        res.json({
            ...snapshot,
            updatedAt: new Date().toISOString()
        });
    } catch (error) {
        console.error('读取玩家经济信息失败:', error);
        res.status(500).json({ error: '读取玩家经济信息失败。' });
    }
});

app.get('/api/player-ledger', (req, res) => {
    try {
        const user = authenticate(req);
        if (!user) {
            return res.status(401).json({ error: '需要登录。' });
        }
        const playerId = String(user.playerId || '').trim();
        if (!playerId) {
            return res.status(400).json({ error: '账号未绑定游戏玩家 ID。' });
        }
        const uuid = resolvePlayerUuid(playerId);
        if (!uuid) {
            return res.status(404).json({ error: '未找到该玩家在服务器的存档。' });
        }
        const filterRaw = String(req.query.filter || 'exclude_flight').trim().toLowerCase();
        const filter = ['all', 'flight', 'exclude_flight'].includes(filterRaw) ? filterRaw : 'exclude_flight';
        const limit = Math.min(Number(req.query.limit) || 30, 100);
        const offset = Math.max(Number(req.query.offset) || 0, 0);
        const result = playerLedger.listForUuid(uuid, { limit, offset, filter });
        const summaryAll = playerLedger.listForUuid(uuid, { limit: 1, offset: 0, filter: 'all' }).summary;
        res.json({
            playerId,
            uuid,
            ...result,
            summaryAll,
            updatedAt: new Date().toISOString()
        });
    } catch (error) {
        console.error('读取零钱明细失败:', error);
        res.status(500).json({ error: '读取零钱明细失败。' });
    }
});

let usercacheByName = null;
let usercacheLoadedAt = 0;

function loadUsercacheByName() {
    const now = Date.now();
    if (usercacheByName && now - usercacheLoadedAt < 60000) {
        return usercacheByName;
    }
    const map = new Map();
    if (fs.existsSync(USERCACHE_PATH)) {
        try {
            const entries = JSON.parse(fs.readFileSync(USERCACHE_PATH, 'utf8'));
            if (Array.isArray(entries)) {
                entries.forEach((entry) => {
                    const name = String(entry?.name || '').trim().toLowerCase();
                    const uuid = String(entry?.uuid || '').trim().toLowerCase();
                    if (name && uuid) {
                        map.set(name, uuid);
                    }
                });
            }
        } catch (error) {
            console.warn('读取 usercache.json 失败:', error.message);
        }
    }
    usercacheByName = map;
    usercacheLoadedAt = now;
    return map;
}

function resolvePlayerUuid(playerName) {
    const norm = String(playerName || '').trim().toLowerCase();
    if (!norm) {
        return null;
    }
    const cached = loadUsercacheByName().get(norm);
    if (cached) {
        return cached;
    }
    if (!fs.existsSync(ESSENTIALS_USERDATA_DIR)) {
        return null;
    }
    const files = fs.readdirSync(ESSENTIALS_USERDATA_DIR).filter((name) => name.endsWith('.yml'));
    for (const file of files) {
        try {
            const data = yaml.load(fs.readFileSync(path.join(ESSENTIALS_USERDATA_DIR, file), 'utf8'));
            const account = String(data?.['last-account-name'] || '').trim().toLowerCase();
            if (account === norm) {
                return file.replace(/\.yml$/i, '').toLowerCase();
            }
        } catch {
            /* ignore */
        }
    }
    return null;
}

function normalizeEssentialsWorldName(worldName) {
    const value = String(worldName || 'world').trim();
    if (!value) {
        return 'world';
    }
    const lower = value.toLowerCase();
    if (lower === 'overworld' || lower === 'minecraft:overworld') return 'world';
    if (lower === 'the_nether' || lower === 'minecraft:the_nether' || lower === 'nether') return 'world_nether';
    if (lower === 'the_end' || lower === 'minecraft:the_end' || lower === 'end') return 'world_the_end';
    if (lower === 'dim-1' || lower === 'dim_1') return 'world_nether';
    if (lower === 'dim1') return 'world_the_end';
    if (lower.endsWith('_nether') && !lower.startsWith('world_')) return `world_${lower}`;
    return value;
}

function readEssentialsLocationBlock(block) {
    if (!block || typeof block !== 'object') {
        return null;
    }
    const x = Number(block.x);
    const y = Number(block.y);
    const z = Number(block.z);
    if (![x, y, z].every(Number.isFinite)) {
        return null;
    }
    return {
        map: normalizeEssentialsWorldName(block['world-name'] || block.world),
        x,
        y,
        z,
        yaw: Number.isFinite(Number(block.yaw)) ? Number(block.yaw) : null,
        pitch: Number.isFinite(Number(block.pitch)) ? Number(block.pitch) : null
    };
}

/** 离线/未在 BlueMap 实时列表时，使用 Essentials 保存的位置 */
function readEssentialsSavedPlayerLocation(uuid) {
    const file = path.join(ESSENTIALS_USERDATA_DIR, `${uuid}.yml`);
    if (!fs.existsSync(file)) {
        return null;
    }
    let data;
    try {
        data = yaml.load(fs.readFileSync(file, 'utf8'));
    } catch {
        return null;
    }
    const logout = readEssentialsLocationBlock(data?.logoutlocation);
    const last = readEssentialsLocationBlock(data?.lastlocation);
    const tsLogout = Number(data?.timestamps?.logout) || 0;
    const tsLogin = Number(data?.timestamps?.login) || 0;
    const offline = tsLogout >= tsLogin;
    const chosen = offline && logout ? logout : (last || logout);
    if (!chosen) {
        return null;
    }
    return {
        ...chosen,
        source: offline && logout ? 'logout' : 'lastlocation',
        accountName: String(data?.['last-account-name'] || '').trim() || null
    };
}

/** BlueMap 写入的在线玩家坐标（与网页地图一致），避免误读 Three.js 动画中的 marker.position */
function readBlueMapLivePlayerLocation(uuid, playerName) {
    const normUuid = String(uuid || '').trim().toLowerCase();
    const normName = String(playerName || '').trim().toLowerCase();
    if (!normUuid && !normName) {
        return null;
    }
    for (const mapId of (fs.existsSync(BLUEMAP_WEB_MAPS_DIR)
        ? fs.readdirSync(BLUEMAP_WEB_MAPS_DIR, { withFileTypes: true })
            .filter((entry) => entry.isDirectory())
            .map((entry) => entry.name)
        : BLUEMAP_LIVE_MAP_IDS)) {
        const file = path.join(BLUEMAP_WEB_MAPS_DIR, mapId, 'live', 'players.json');
        if (!fs.existsSync(file)) {
            continue;
        }
        try {
            const raw = JSON.parse(fs.readFileSync(file, 'utf8'));
            const list = Array.isArray(raw?.players) ? raw.players : [];
            const hit = list.find((entry) => {
                const u = String(entry?.uuid || '').toLowerCase();
                const n = String(entry?.name || '').toLowerCase();
                return (normUuid && u === normUuid) || (normName && n === normName);
            });
            if (!hit?.position) {
                continue;
            }
            const x = Number(hit.position.x);
            const y = Number(hit.position.y);
            const z = Number(hit.position.z);
            if (![x, y, z].every(Number.isFinite)) {
                continue;
            }
            return {
                map: mapId,
                x,
                y,
                z,
                yaw: Number.isFinite(Number(hit.rotation?.yaw)) ? Number(hit.rotation.yaw) : null,
                pitch: Number.isFinite(Number(hit.rotation?.pitch)) ? Number(hit.rotation.pitch) : null,
                source: 'bluemap-live'
            };
        } catch {
            /* ignore */
        }
    }
    return null;
}

app.get('/api/player-location', (req, res) => {
    try {
        const user = authenticate(req);
        if (!user) {
            return res.status(401).json({ error: '需要登录后才能定位玩家。' });
        }
        const playerId = String(user.playerId || '').trim();
        if (!playerId) {
            return res.status(400).json({ error: '账号未绑定游戏玩家 ID。' });
        }
        const uuid = resolvePlayerUuid(playerId);
        if (!uuid) {
            return res.status(404).json({ error: '未找到该玩家在服务器的存档。' });
        }
        const live = readBlueMapLivePlayerLocation(uuid, playerId);
        if (live) {
            return res.json({
                playerId,
                uuid,
                username: user.username,
                online: true,
                source: live.source,
                map: live.map,
                x: live.x,
                y: live.y,
                z: live.z,
                yaw: live.yaw,
                pitch: live.pitch,
                updatedAt: new Date().toISOString()
            });
        }
        const saved = readEssentialsSavedPlayerLocation(uuid);
        if (!saved) {
            return res.status(404).json({ error: '未找到该玩家的已保存位置。' });
        }
        res.json({
            playerId,
            uuid,
            username: user.username,
            online: false,
            source: saved.source,
            map: saved.map,
            x: saved.x,
            y: saved.y,
            z: saved.z,
            yaw: saved.yaw,
            pitch: saved.pitch,
            updatedAt: new Date().toISOString()
        });
    } catch (error) {
        console.error('读取玩家位置失败:', error);
        res.status(500).json({ error: '读取玩家位置失败。' });
    }
});

app.get('/api/admin/access', (req, res) => {
    try {
        const user = authenticate(req);
        if (!user) {
            return res.status(401).json({ error: '需要登录。' });
        }
        const access = getAdminAccess(user);
        if (!access.allowed) {
            return res.status(403).json({
                ...access,
                error: '你没有进入管理系统的权限。需要 OP 或 ultimateshop.editor 权限。'
            });
        }
        res.json(access);
    } catch (error) {
        console.error('检查管理权限失败:', error);
        res.status(500).json({ error: '检查管理权限失败。' });
    }
});

app.get('/api/admin/shops', (req, res) => {
    try {
        if (!requireAdmin(req, res)) return;
        res.json(listUltimateShopShops());
    } catch (error) {
        console.error('读取商店列表失败:', error);
        res.status(500).json({ error: '读取商店列表失败。' });
    }
});

/** 从 world/level.dat 读取主世界 DayTime（0–23999 为一日内刻） */
function readOverworldDayTime() {
    if (!fs.existsSync(OVERWORLD_LEVEL_DAT)) {
        return null;
    }
    const raw = zlib.gunzipSync(fs.readFileSync(OVERWORLD_LEVEL_DAT));
    for (let offset = 0; offset < raw.length - 18; offset++) {
        if (raw[offset] !== 4) {
            continue;
        }
        if (raw.readUInt16BE(offset + 1) !== 7) {
            continue;
        }
        if (raw.slice(offset + 3, offset + 10).toString('utf8') !== 'DayTime') {
            continue;
        }
        const ticks = Number(raw.readBigInt64BE(offset + 10) % BigInt(MC_DAY_TICKS));
        return ((ticks % MC_DAY_TICKS) + MC_DAY_TICKS) % MC_DAY_TICKS;
    }
    return null;
}

function sunlightStrengthFromDayTime(dayTime) {
    const tick = ((Number(dayTime) % MC_DAY_TICKS) + MC_DAY_TICKS) % MC_DAY_TICKS;
    return 0.625 + 0.375 * Math.cos((2 * Math.PI * (tick - 6000)) / MC_DAY_TICKS);
}

function describeDayPeriod(dayTime) {
    const t = ((Number(dayTime) % MC_DAY_TICKS) + MC_DAY_TICKS) % MC_DAY_TICKS;
    if (t < 1000 || t >= 23000) return '日出';
    if (t < 11000) return '白天';
    if (t < 13000) return '日落';
    if (t < 23000) return '夜晚';
    return '日出';
}

function defaultGisProject() {
    return {
        version: 1,
        updatedAt: null,
        layers: [
            {
                id: 'roads',
                name: '道路',
                color: '#f59e0b',
                visible: true,
                features: []
            },
            {
                id: 'labels',
                name: '标注',
                color: '#3b82f6',
                visible: true,
                features: []
            }
        ]
    };
}

function loadGisProject() {
    const fallback = defaultGisProject();
    const raw = loadJsonFile(GIS_PROJECT_PATH, fallback);
    if (!raw || typeof raw !== 'object') {
        return fallback;
    }
    return normalizeGisProject(raw);
}

function saveGisProject(project) {
    const normalized = normalizeGisProject(project);
    normalized.updatedAt = new Date().toISOString();
    fs.mkdirSync(path.dirname(GIS_PROJECT_PATH), { recursive: true });
    fs.writeFileSync(GIS_PROJECT_PATH, JSON.stringify(normalized, null, 2), 'utf8');
    return normalized;
}

function normalizeGisCoord(value) {
    const num = Number(value);
    if (!Number.isFinite(num)) {
        return null;
    }
    return Math.round(num * 1000) / 1000;
}

function normalizeGisPoint(raw) {
    if (!raw || typeof raw !== 'object') {
        return null;
    }
    const x = normalizeGisCoord(raw.x);
    const y = normalizeGisCoord(raw.y);
    const z = normalizeGisCoord(raw.z);
    if (x == null || y == null || z == null) {
        return null;
    }
    return { x, y, z };
}

function normalizeGisFeature(raw, layerId) {
    if (!raw || typeof raw !== 'object') {
        return null;
    }
    const type = String(raw.type || '').trim();
    if (!['Point', 'LineString', 'Polygon', 'Label'].includes(type)) {
        return null;
    }
    const map = String(raw.map || 'world').trim();
    if (!GIS_ALLOWED_MAPS.has(map)) {
        return null;
    }
    let coordinates = [];
    if (type === 'Point' || type === 'Label') {
        const point = normalizeGisPoint(raw.coordinates || raw.position);
        if (!point) {
            return null;
        }
        coordinates = point;
    } else {
        const list = Array.isArray(raw.coordinates) ? raw.coordinates : [];
        coordinates = list.map(normalizeGisPoint).filter(Boolean);
        if (type === 'LineString' && coordinates.length < 2) {
            return null;
        }
        if (type === 'Polygon' && coordinates.length < 3) {
            const volShape = String(props.volume3d?.shape || '').trim().toLowerCase();
            const cylinderOk = volShape === 'cylinder' && coordinates.length >= 2;
            const hexaOk = (volShape === 'hexahedron' || volShape === 'hex') && coordinates.length >= 8;
            if (!cylinderOk && !hexaOk) {
                return null;
            }
        }
    }
    const props = raw.properties && typeof raw.properties === 'object' ? raw.properties : {};
    const name = String(props.name || raw.name || '').trim().slice(0, 120);
    const description = String(props.description || raw.description || '').trim().slice(0, 2000);
    const color = String(props.color || raw.color || '').trim();
    const safeColor = /^#[0-9a-fA-F]{3,8}$/.test(color) ? color : '';
    const featureProps = {
        name,
        description,
        ...(safeColor ? { color: safeColor } : {})
    };
    if (type === 'LineString' || type === 'Polygon') {
        const vertexIdsRaw = props.vertexIds;
        if (Array.isArray(vertexIdsRaw)) {
            const vertexIds = vertexIdsRaw
                .slice(0, 512)
                .map((id) => String(id || '').trim().slice(0, 80))
                .filter((id) => id.length > 0);
            if (vertexIds.length) {
                featureProps.vertexIds = vertexIds;
            }
        }
        const strokeStyle = String(props.strokeStyle || '').trim().toLowerCase();
        if (['solid', 'dashed', 'dotted', 'dashdot', 'dash-dot'].includes(strokeStyle)) {
            featureProps.strokeStyle = strokeStyle === 'dash-dot' ? 'dashdot' : strokeStyle;
        }
        const strokeWidth = Number(props.strokeWidth);
        if (Number.isFinite(strokeWidth) && strokeWidth > 0) {
            featureProps.strokeWidth = Math.min(24, Math.max(1, strokeWidth));
        }
        if (type === 'LineString') {
            const travelRaw = String(props.travelDirection || '').trim().toLowerCase();
            if (travelRaw === 'dir1' || travelRaw === 'direction1' || travelRaw === '1') {
                featureProps.travelDirection = 'dir1';
            } else if (travelRaw === 'dir2' || travelRaw === 'direction2' || travelRaw === '2') {
                featureProps.travelDirection = 'dir2';
            }
            if (props.showRoadName === false) {
                featureProps.showRoadName = false;
            }
            const segRaw = props.roadNameSegments;
            if (Array.isArray(segRaw) && segRaw.length) {
                const roadNameSegments = [];
                segRaw.slice(0, 64).forEach((entry) => {
                    if (!entry || typeof entry !== 'object') {
                        return;
                    }
                    const segName = String(entry.name || '').trim().slice(0, 120);
                    if (!segName) {
                        return;
                    }
                    const fromVertexId = String(entry.fromVertexId || '').trim().slice(0, 80);
                    const toVertexId = String(entry.toVertexId || '').trim().slice(0, 80);
                    if (!fromVertexId || !toVertexId || fromVertexId === toVertexId) {
                        return;
                    }
                    roadNameSegments.push({ fromVertexId, toVertexId, name: segName });
                });
                if (roadNameSegments.length) {
                    featureProps.roadNameSegments = roadNameSegments;
                    if (roadNameSegments.length === 1) {
                        featureProps.name = roadNameSegments[0].name;
                    }
                }
            }
        }
        const vertCount = coordinates.length;
        const visRaw = props.vertexVisibility;
        const vertexVisibility = [];
        if (Array.isArray(visRaw)) {
            visRaw.slice(0, Math.min(512, vertCount)).forEach((entry) => {
                if (!entry || typeof entry !== 'object') {
                    vertexVisibility.push({});
                    return;
                }
                const out = {};
                const min = Number(entry.min);
                const max = Number(entry.max);
                if (Number.isFinite(min)) {
                    out.min = min;
                }
                if (Number.isFinite(max)) {
                    out.max = max;
                }
                const lo = out.min == null ? -Infinity : out.min;
                const hi = out.max == null ? Infinity : out.max;
                if (lo < hi) {
                    vertexVisibility.push(out);
                } else {
                    vertexVisibility.push({});
                }
            });
        }
        while (vertexVisibility.length < vertCount) {
            vertexVisibility.push({});
        }
        if (vertexVisibility.length > vertCount) {
            vertexVisibility.length = vertCount;
        }
        if (vertCount > 0) {
            featureProps.vertexVisibility = vertexVisibility;
        }
        if (type === 'Polygon') {
            const volRaw = props.volume3d;
            if (volRaw && typeof volRaw === 'object') {
                let shape = String(volRaw.shape || 'flat').trim().toLowerCase();
                if (shape === 'cuboid' || shape === 'cube') {
                    shape = 'box';
                }
                if (shape === 'cyl') {
                    shape = 'cylinder';
                }
                if (shape === 'hex' || shape === '6face') {
                    shape = 'hexahedron';
                }
                if (['box', 'cylinder', 'hexahedron'].includes(shape)) {
                    const volume3d = { shape };
                    const minY = Number(volRaw.minY);
                    const maxY = Number(volRaw.maxY);
                    const radius = Number(volRaw.radius);
                    const segments = Number(volRaw.segments);
                    if (Number.isFinite(minY)) {
                        volume3d.minY = minY;
                    }
                    if (Number.isFinite(maxY)) {
                        volume3d.maxY = maxY;
                    }
                    if (Number.isFinite(radius) && radius > 0) {
                        volume3d.radius = Math.min(4096, radius);
                    }
                    if (Number.isFinite(segments)) {
                        volume3d.segments = Math.min(64, Math.max(8, Math.round(segments)));
                    }
                    featureProps.volume3d = volume3d;
                }
            }
        }
    }
    if (type === 'Point' || type === 'Label') {
        const vertexIdsRaw = props.vertexIds;
        if (Array.isArray(vertexIdsRaw) && vertexIdsRaw.length) {
            const id0 = String(vertexIdsRaw[0] || '').trim().slice(0, 80);
            if (id0) {
                featureProps.vertexIds = [id0];
            }
        }
    }
    const id = String(raw.id || '').trim().slice(0, 64)
        || `f_${crypto.randomBytes(8).toString('hex')}`;
    return {
        id,
        type,
        map,
        layerId: String(raw.layerId || layerId || '').trim().slice(0, 64),
        coordinates,
        properties: featureProps
    };
}

function normalizeGisProject(raw) {
    const base = defaultGisProject();
    const layersIn = Array.isArray(raw.layers) ? raw.layers.slice(0, GIS_MAX_LAYERS) : base.layers;
    let featureCount = 0;
    const layers = layersIn.map((layerRaw, index) => {
        const layerId = String(layerRaw?.id || `layer_${index}`).trim().slice(0, 64)
            || `layer_${index}`;
        const name = String(layerRaw?.name || layerId).trim().slice(0, 80) || layerId;
        const colorRaw = String(layerRaw?.color || '#3b82f6').trim();
        const color = /^#[0-9a-fA-F]{3,8}$/.test(colorRaw) ? colorRaw : '#3b82f6';
        const visible = layerRaw?.visible !== false;
        const featuresIn = Array.isArray(layerRaw?.features) ? layerRaw.features : [];
        const features = [];
        featuresIn.forEach((featureRaw) => {
            if (featureCount >= GIS_MAX_FEATURES) {
                return;
            }
            const feature = normalizeGisFeature(featureRaw, layerId);
            if (!feature) {
                return;
            }
            feature.layerId = layerId;
            features.push(feature);
            featureCount += 1;
        });
        return { id: layerId, name, color, visible, features };
    });
    if (!layers.length) {
        return base;
    }
    return {
        version: 1,
        updatedAt: raw.updatedAt || null,
        layers
    };
}

app.get('/api/gis', (req, res) => {
    try {
        const project = loadGisProject();
        res.json({
            updatedAt: project.updatedAt,
            project
        });
    } catch (error) {
        console.error('读取 GIS 数据失败:', error);
        res.status(500).json({ error: '读取 GIS 数据失败。' });
    }
});

app.put('/api/gis', (req, res) => {
    try {
        if (!requireAdmin(req, res)) {
            return;
        }
        const body = req.body || {};
        const project = body.project && typeof body.project === 'object' ? body.project : body;
        const saved = saveGisProject(project);
        res.json({ status: 'ok', updatedAt: saved.updatedAt, project: saved });
    } catch (error) {
        console.error('保存 GIS 数据失败:', error);
        res.status(500).json({ error: '保存 GIS 数据失败。' });
    }
});

app.get('/api/world-time', (req, res) => {
    try {
        const dayTime = readOverworldDayTime();
        if (dayTime == null) {
            return res.status(503).json({ error: '无法读取主世界 level.dat 中的 DayTime。' });
        }
        const sunlightStrength = sunlightStrengthFromDayTime(dayTime);
        res.json({
            world: 'world',
            dayTime,
            dayTicks: MC_DAY_TICKS,
            sunlightStrength: Math.round(sunlightStrength * 1000) / 1000,
            isDay: sunlightStrength > 0.6,
            period: describeDayPeriod(dayTime),
            updatedAt: new Date().toISOString()
        });
    } catch (error) {
        console.error('读取世界时间失败:', error);
        res.status(500).json({ error: '读取世界时间失败。' });
    }
});

app.get('/api/shop-map-markers', (req, res) => {
    try {
        purgeBlueMapShopMarkers();
        res.json({
            updatedAt: new Date().toISOString(),
            markers: listShopMapMarkers()
        });
    } catch (error) {
        console.error('读取商店地图标记失败:', error);
        res.status(500).json({ error: '读取商店地图标记失败。' });
    }
});

app.post('/api/admin/shop-locations/:shopId', (req, res) => {
    try {
        if (!requireAdmin(req, res)) return;
        const shopId = String(req.params.shopId || '').trim();
        if (!/^[A-Za-z0-9_.-]+$/.test(shopId)) {
            return res.status(400).json({ error: '商店 ID 无效。' });
        }
        const shopFile = path.join(ULTIMATE_SHOP_SHOPS_DIR, `${shopId}.yml`);
        if (!fs.existsSync(shopFile)) {
            return res.status(404).json({ error: '未找到该 UltimateShop 商店。' });
        }

        const locations = loadShopLocations();
        const location = normalizeShopLocation(req.body || {});
        locations[shopId] = location;
        saveYamlFile(SHOP_LOCATIONS_PATH, locations);
        purgeBlueMapShopMarkers();
        res.json({ status: 'ok', shopId, location });
    } catch (error) {
        console.error('保存商店位置失败:', error);
        res.status(500).json({ error: '保存商店位置失败。' });
    }
});

// 提供数据接口，同时返回 UltimateShop 映射配置
app.get('/api/shop/categories', (req, res) => {
    try {
        const registry = loadUltimateShopCategoryRegistry();
        res.json({
            categories: registry.categories,
            shopItemCounts: registry.shopItemCounts,
            shopMaterialCount: Object.keys(registry.materialToShops).length
        });
    } catch (error) {
        console.error('读取商店分类失败:', error);
        res.status(500).json({ error: '读取商店分类失败' });
    }
});

app.get('/api/prices', (req, res) => {
    try {
        if (!PRICE_TABLE_PATHS.some((table) => resolvePriceTablePath(table))) {
            return res.status(404).json({ error: '暂无数据' });
        }

        const rawData = loadPriceTables();
        const mappings = loadYamlFile(MAPPING_PATH);
        const usCatalog = buildUltimateShopCatalogByMaterial(rawData);
        const registry = loadUltimateShopCategoryRegistry();

        const responseData = {};
        Object.keys(rawData).forEach(key => {
            const mapping = mappings[key] || {};
            const normKey = normalizeMaterialId(key) || key;
            const ultimateShopOffers = usCatalog[normKey] || [];
            const shopCategories = registry.materialToShops[normKey] || [];
            const primaryOffer = pickPrimaryUltimateShopOffer(ultimateShopOffers, rawData);
            responseData[key] = {
                buy: rawData[key].buy,
                sell: rawData[key].sell,
                source: rawData[key].source || 'vanilla',
                custom: rawData[key].custom === true,
                shop: primaryOffer ? primaryOffer.shopId : (mapping.shop || null),
                item: primaryOffer ? String(primaryOffer.slot) : (mapping.item || null),
                amount: mapping.amount || 1,
                displayName: mapping.displayName || null,
                customDisplayName: rawData[key].displayName || null,
                loreLine: rawData[key].loreLine || rawData[key].description || rawData[key].lore || null,
                shopCategories,
                inShop: ultimateShopOffers.length > 0,
                ultimateShopOffers
            };
        });

        res.json(responseData);
    } catch (error) {
        console.error('读取价格数据出错:', error);
        res.status(500).json({ error: '读取错误' });
    }
});

// 网页建造工具（投影粘贴）已下线：Axiom 游戏内粘贴替代
app.all('/api/build', (req, res) => {
    res.status(410).json({ error: '网页建造工具已下线，请使用游戏内 Axiom 粘贴原理图。' });
});
app.all(/^\/api\/build(\/.*)?$/, (req, res) => {
    res.status(410).json({ error: '网页建造工具已下线，请使用游戏内 Axiom 粘贴原理图。' });
});

// Litematica 材料清单：解析 JSON/合并条目后按商店买入价计价
app.post('/api/shop/material-quote', (req, res) => {
    try {
        if (!PRICE_TABLE_PATHS.some((table) => resolvePriceTablePath(table))) {
            return res.status(404).json({ error: '暂无价格数据' });
        }

        const body = req.body || {};
        let materials = [];
        let listName = String(body.listName || '').trim();

        if (Array.isArray(body.materials) && body.materials.length) {
            materials = body.materials;
        } else if (typeof body.raw === 'string' && body.raw.trim()) {
            const parsed = materialListParser.parseLitematicaMaterialFile(body.raw, body.fileName || 'import.json');
            materials = parsed.materials;
            if (!listName) listName = parsed.listName;
        } else if (body && typeof body === 'object' && !Array.isArray(body)) {
            const payload = body.data && typeof body.data === 'object' ? body.data : body;
            const parsed = materialListParser.parseLitematicaMaterialJson(payload);
            materials = parsed.materials;
            if (!listName) listName = parsed.listName;
        }

        if (!materials.length) {
            return res.status(400).json({ error: '材料清单为空或格式无法识别。' });
        }
        if (materials.length > 500) {
            return res.status(400).json({ error: '单次最多导入 500 种材料。' });
        }

        res.json(quoteMaterialLines(materials, listName));
    } catch (error) {
        console.error('材料清单计价失败:', error);
        res.status(400).json({ error: error.message || '材料清单计价失败' });
    }
});

// ── 仪表板：交易与分析（UltimateShop 日志 + 历史 DynamicShop CSV）──
app.get('/api/recent', (req, res) => {
    try {
        const limit = Math.min(Number(req.query.limit) || 500, 5000);
        res.json(analytics.getTransactions(limit));
    } catch (error) {
        console.error('读取最近交易失败:', error);
        res.status(500).json({ error: '读取失败' });
    }
});

app.get('/api/stats', (req, res) => {
    try {
        res.json(analytics.getStats());
    } catch (error) {
        console.error('读取统计失败:', error);
        res.status(500).json({ error: '读取失败' });
    }
});

app.get('/api/analytics/economy', (req, res) => {
    try {
        res.json(analytics.getEconomyHealth());
    } catch (error) {
        console.error('读取经济健康指标失败:', error);
        res.status(500).json({ error: '读取失败' });
    }
});

app.get('/api/analytics/leaderboard', (req, res) => {
    try {
        const type = req.query.type || 'buyers';
        const limit = Math.min(Number(req.query.limit) || 5, 50);
        res.json(analytics.getLeaderboard(type, limit));
    } catch (error) {
        console.error('读取排行榜失败:', error);
        res.status(500).json({ error: '读取失败' });
    }
});

app.get('/api/analytics/trends', (req, res) => {
    try {
        const limit = Math.min(Number(req.query.limit) || 10, 50);
        res.json(analytics.getTrends(limit));
    } catch (error) {
        console.error('读取市场趋势失败:', error);
        res.status(500).json({ error: '读取失败' });
    }
});

app.get('/api/analytics/price-history/:item', (req, res) => {
    try {
        const range = String(req.query.range || '').trim().toLowerCase();
        const hoursRaw = Number(req.query.hours);
        const rangeOrHours = range || (Number.isFinite(hoursRaw) && hoursRaw > 0 ? hoursRaw : '7d');
        const item = normalizeMaterialId(req.params.item) || req.params.item;
        res.json(analytics.getPriceHistory(item, rangeOrHours));
    } catch (error) {
        console.error('读取价格历史失败:', error);
        res.status(500).json({ error: '读取失败' });
    }
});

app.get('/api/shop/item/:item', (req, res) => {
    try {
        const item = normalizeMaterialId(req.params.item) || req.params.item;
        res.json(analytics.getItemDetails(item));
    } catch (error) {
        console.error('读取物品详情失败:', error);
        res.status(500).json({ error: '读取失败' });
    }
});

app.get('/api/shop/item-snapshot/:item', (req, res) => {
    try {
        const range = String(req.query.range || '').trim().toLowerCase();
        const hoursRaw = Number(req.query.hours);
        const viewportRange = range || (Number.isFinite(hoursRaw) && hoursRaw > 0 ? hoursRaw : '7d');
        const item = normalizeMaterialId(req.params.item) || req.params.item;
        res.json({
            item: analytics.getItemDetails(item),
            priceHistory: analytics.getPriceHistory(item, 'full'),
            range: viewportRange,
            serverTime: new Date().toISOString()
        });
    } catch (error) {
        console.error('读取物品详情快照失败:', error);
        res.status(500).json({ error: '读取失败' });
    }
});

app.post('/api/shop/checkout', (req, res) => {
    try {
        const user = authenticate(req);
        if (!user) {
            return res.status(401).json({ error: '需要登录后才能提交订单。' });
        }
        const playerId = String(user.playerId || '').trim();
        if (!playerId) {
            return res.status(400).json({ error: '账号未绑定游戏玩家 ID。' });
        }
        const uuid = resolvePlayerUuid(playerId);
        if (!uuid) {
            return res.status(404).json({ error: '未找到该玩家在服务器的存档。' });
        }

        const rawLines = Array.isArray(req.body && req.body.lines) ? req.body.lines : [];
        if (!rawLines.length) {
            return res.status(400).json({ error: '购物车为空。' });
        }
        if (rawLines.length > 50) {
            return res.status(400).json({ error: '单次最多提交 50 种商品。' });
        }

        const priceData = loadPriceTables();
        const catalog = buildUltimateShopCatalogByMaterial(priceData);
        const resolvedLines = [];
        for (let i = 0; i < rawLines.length; i += 1) {
            const result = resolveCheckoutLine(rawLines[i], priceData, catalog);
            if (result.error) {
                return res.status(422).json({ error: result.error });
            }
            resolvedLines.push(result.line);
        }

        const total = Math.round(resolvedLines.reduce((sum, line) => sum + line.lineTotal, 0) * 100) / 100;
        reconcileStuckEconomyDeductions();
        const balance = readEffectiveEssentialsBalance(uuid);
        if (balance == null) {
            return res.status(404).json({ error: '未找到该玩家的 Essentials 经济存档。' });
        }
        if (balance < total) {
            return res.status(402).json({
                error: '零钱不足，无法提交订单。',
                balance,
                balanceFormatted: formatEssentialsBalance(balance),
                total,
                totalFormatted: formatEssentialsBalance(total)
            });
        }

        const fileBalance = readEssentialsBalance(uuid);
        const balanceAfter = Math.round((fileBalance - total) * 100) / 100;
        // 结账响应中的余额与游戏内一致（已扣款后由 eco take 更新 userdata）

        const store = loadPendingOrdersStore();
        const numericId = Number(store.next_id) || 1;
        const orderId = crypto.randomUUID();
        const now = new Date().toISOString();
        store.orders[String(numericId)] = {
            id: orderId,
            numericId,
            playerUuid: uuid,
            playerId,
            username: user.username,
            status: 'pending',
            total,
            balanceAfter,
            economyChargedInGame: false,
            createdAt: now,
            updatedAt: now,
            failureReason: '',
            deliveringLine: 0,
            deliveringStartedAt: '',
            lineCount: resolvedLines.length,
            lines: linesToSkriptMap(resolvedLines)
        };
        store.next_id = numericId + 1;
        savePendingOrdersStore(store);

        const linePreview = resolvedLines.slice(0, 3).map((line) => `${line.itemId}×${line.quantity}`).join('、');
        const lineSuffix = resolvedLines.length > 3 ? ` 等${resolvedLines.length}项` : '';
        playerLedger.logCheckout({
            uuid,
            playerId,
            category: 'web_shop',
            amount: total,
            balanceAfter,
            description: `网页商城：${linePreview}${lineSuffix}`,
            refId: `web-order-${numericId}`
        });

        enqueueEssentialsDeduction({
            uuid,
            playerId,
            amount: total,
            orderId: numericId,
            balanceAfter
        });
        appendEcoTakeQueueLine(playerId, total, numericId);
        res.json({
            orderId: numericId,
            orderUuid: orderId,
            status: 'pending',
            total,
            totalFormatted: formatEssentialsBalance(total),
            balance: balanceAfter,
            balanceFormatted: formatEssentialsBalance(balanceAfter),
            lineCount: resolvedLines.length,
            deductionMode: 'eco-take',
            message: '订单已提交，服务器将执行 eco take 扣款；进游戏后物品将发放至 BetterBags。',
            createdAt: now
        });
    } catch (error) {
        console.error('提交网页订单失败:', error);
        res.status(500).json({ error: '提交订单失败，请稍后重试。' });
    }
});

app.get('/api/shop/orders', (req, res) => {
    try {
        const user = authenticate(req);
        if (!user) {
            return res.status(401).json({ error: '需要登录。' });
        }
        const limit = Math.min(Number(req.query.limit) || 20, 50);
        const orders = listOrdersForUser(user, limit).map((order) => ({
            orderId: order.numericId,
            orderUuid: order.id,
            status: order.status,
            total: order.total,
            totalFormatted: formatEssentialsBalance(order.total),
            createdAt: order.createdAt,
            updatedAt: order.updatedAt,
            failureReason: order.failureReason || '',
            lineCount: linesToArray(order.lines).length,
            lines: linesToArray(order.lines).map((line) => ({
                itemId: line.itemId,
                shopId: line.shopId,
                slot: line.slot,
                quantity: line.quantity,
                unitBuyPrice: line.unitBuyPrice,
                lineTotal: line.lineTotal,
                shopTitle: line.shopTitle,
                status: line.status || 'pending'
            }))
        }));
        const pendingCount = orders.filter((o) => o.status === 'pending' || o.status === 'delivering').length;
        res.json({ orders, pendingCount, updatedAt: new Date().toISOString() });
    } catch (error) {
        console.error('读取网页订单失败:', error);
        res.status(500).json({ error: '读取订单失败。' });
    }
});

app.post('/api/buy', (req, res) => {
    try {
        const user = authenticate(req);
        const { itemId, amount } = req.body || {};
        if (!itemId) {
            return res.status(400).json({ error: '缺少 itemId 参数' });
        }

        if (!PRICE_TABLE_PATHS.some((table) => resolvePriceTablePath(table))) {
            return res.status(404).json({ error: '暂无数据' });
        }

        const rawData = loadPriceTables();
        const itemData = rawData[itemId];
        if (!itemData) {
            return res.status(404).json({ error: '未找到该商品' });
        }

        const mappings = loadYamlFile(MAPPING_PATH);
        const mapping = mappings[itemId];
        if (!mapping || !mapping.shop || !mapping.item) {
            return res.status(422).json({ error: '该商品未配置 UltimateShop 映射' });
        }

        const buyAmount = Number(amount) || mapping.amount || 1;
        const command = `/shop quickbuy ${mapping.shop} ${mapping.item}${buyAmount > 1 ? ` ${buyAmount}` : ''}`;

        // 如果你有 Minecraft 服务器桥接，可以在这里执行 command。
        // 目前返回给前端用于提示或后续桥接处理。
        res.json({
            status: 'ok',
            itemId,
            shop: mapping.shop,
            shopItem: mapping.item,
            amount: buyAmount,
            command,
            user: user ? { username: user.username, playerId: user.playerId } : null,
            message: user ? `已为玩家 ${user.username} 生成购买指令。` : '已生成购买指令，可在 Minecraft 中由玩家执行或通过服务器桥接调用。'
        });
    } catch (error) {
        console.error('处理购买请求出错:', error);
        res.status(500).json({ error: '购买失败' });
    }
});

app.use('/api', (req, res) => {
    res.status(404).json({
        error: '接口不存在',
        path: req.originalUrl
    });
});

function localIpv4Addresses() {
    const addresses = [];
    Object.values(os.networkInterfaces()).forEach((entries) => {
        (entries || []).forEach((entry) => {
            if (entry.family === 'IPv4' && !entry.internal) {
                addresses.push(entry.address);
            }
        });
    });
    return addresses;
}

function logServerStart(protocol) {
    analytics.reload();
    try {
        playerLedger.processQueue();
    } catch (error) {
        console.error('[player-ledger] 启动时处理队列失败:', error.message || error);
    }
    purgeBlueMapShopMarkers();
    const mappingCount = syncUltimateShopMappingsFile();
    if (mappingCount > 0) {
        console.log(`🛒 UltimateShop 映射已同步：${mappingCount} 个可购物品 → ${MAPPING_PATH}`);
    }
    console.log(`✅ 高级版 UI 服务已启动！访问: ${protocol}://${HOST}:${PORT}`);
    localIpv4Addresses().forEach((ip) => {
        console.log(`📱 局域网访问: ${protocol}://${ip}:${PORT}`);
    });
    console.log(`📊 仪表板交易记录: ${TRANSACTIONS_YAML}`);
}

let httpServer = null;
let shuttingDown = false;

if (HTTPS_ENABLED && fs.existsSync(HTTPS_KEY_PATH) && fs.existsSync(HTTPS_CERT_PATH)) {
    httpServer = https.createServer({
        key: fs.readFileSync(HTTPS_KEY_PATH),
        cert: fs.readFileSync(HTTPS_CERT_PATH)
    }, app);
    httpServer.listen(PORT, HOST, () => {
        logServerStart('https');
        loadPendingOrdersStore();
    });
} else {
    httpServer = app.listen(PORT, HOST, () => {
        logServerStart('http');
        loadPendingOrdersStore();
        if (!HTTPS_ENABLED) {
            console.log('ℹ️ 当前为 HTTP 模式；如需 HTTPS，可设置 HTTPS=1 后重启服务。');
        } else {
            console.log('ℹ️ 未找到 HTTPS 证书；运行 npm run generate-cert 后重启服务即可启用 HTTPS。');
        }
    });
}

function shutdownWebServer(signal) {
    if (shuttingDown) {
        return;
    }
    shuttingDown = true;
    console.log(`ℹ️ 收到 ${signal}，正在关闭 MCWWS 网页服务…`);
    if (!httpServer) {
        process.exit(0);
        return;
    }
    httpServer.close((err) => {
        if (err) {
            console.error('关闭 HTTP 服务失败:', err.message);
            process.exit(1);
            return;
        }
        process.exit(0);
    });
    setTimeout(() => process.exit(0), 5000).unref();
}

process.on('SIGTERM', () => shutdownWebServer('SIGTERM'));
process.on('SIGINT', () => shutdownWebServer('SIGINT'));