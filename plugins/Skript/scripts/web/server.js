const express = require('express');
const fs = require('fs');
const zlib = require('zlib');
const https = require('https');
const yaml = require('js-yaml');
const cors = require('cors');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { createAnalyticsService } = require('./analytics');
const nbt = require('prismarine-nbt');

const app = express();
const PORT = 8002;
const HOST = process.env.HOST || '0.0.0.0';

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

// 价格表：原版物品与自定义物品分开存放，接口层合并输出。
const VANILLA_PRICES_PATH = path.join(__dirname, 'mcwws', 'economy', 'vanilla_prices.yml');
const CUSTOM_PRICES_PATH = path.join(__dirname, 'mcwws', 'economy', 'custom_prices.yml');
const PRICE_TABLE_PATHS = [
    { path: VANILLA_PRICES_PATH, source: 'vanilla', custom: false },
    { path: CUSTOM_PRICES_PATH, source: 'custom', custom: true }
];
const MAPPING_PATH = path.join(__dirname, 'mcwws', 'ultimateshop_mappings.yml');
const ULTIMATE_SHOP_SHOPS_DIR = path.join(__dirname, '..', '..', '..', 'UltimateShop', 'shops');
const ULTIMATE_SHOP_LANG_FILE = path.join(__dirname, '..', '..', '..', 'UltimateShop', 'languages', 'zh_CN.yml');
const DB_DIR = path.join(__dirname, 'data');
const USER_DB_FILE = path.join(DB_DIR, 'users.json');
const TRANSACTIONS_CSV = path.join(DB_DIR, 'transactions.csv');
const TRANSACTIONS_YAML = path.join(DB_DIR, 'transactions_store.yml');
const LEGACY_TRANSACTIONS_CSV = path.join(__dirname, '..', '..', '..', 'DynamicShop', 'transactions', 'transactions.csv');
const ITEMS_DB_PATH = path.join(__dirname, '..', 'mcwws', 'economy', 'database', 'items.yml');
const OPS_PATH = path.join(__dirname, '..', '..', '..', '..', 'ops.json');
const ADMIN_ACCESS_PATH = path.join(DB_DIR, 'admin_access.yml');
const SHOP_LOCATIONS_PATH = path.join(__dirname, 'mcwws', 'shop_locations.yml');
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

const analytics = createAnalyticsService({
    transactionsCsvPath: TRANSACTIONS_CSV,
    transactionsYamlPath: TRANSACTIONS_YAML,
    legacyCsvPath: LEGACY_TRANSACTIONS_CSV,
    webPricePaths: PRICE_TABLE_PATHS,
    itemsDbPath: ITEMS_DB_PATH,
    ultimateShopShopsDir: ULTIMATE_SHOP_SHOPS_DIR
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
        return yaml.load(fs.readFileSync(filePath, 'utf8')) || {};
    } catch (error) {
        console.error(`读取 YAML 文件失败: ${filePath}`, error);
        return {};
    }
}

function saveYamlFile(filePath, data) {
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, yaml.dump(data || {}, { lineWidth: 120, noRefs: true }), 'utf8');
}

function loadPriceTables() {
    const merged = {};
    PRICE_TABLE_PATHS.forEach((table) => {
        const data = loadYamlFile(table.path);
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

function userMatchesPlayerEntry(user, entryKey, entry = {}) {
    const userKeys = new Set([
        normalizePlayerKey(user && user.username),
        normalizePlayerKey(user && user.playerId)
    ].filter(Boolean));
    const entryKeys = [
        entryKey,
        entry.name,
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
    return entry.ultimateshopEditor === true
        || entry.ultimateshop_editor === true
        || entry['ultimateshop.editor'] === true
        || permissions['ultimateshop.editor'] === true;
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
    const balance = readEssentialsBalance(uuid);
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
    for (const mapId of BLUEMAP_LIVE_MAP_IDS) {
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
app.get('/api/prices', (req, res) => {
    try {
        if (!PRICE_TABLE_PATHS.some((table) => fs.existsSync(table.path))) {
            return res.status(404).json({ error: '暂无数据' });
        }

        const rawData = loadPriceTables();
        const mappings = loadYamlFile(MAPPING_PATH);
        const usCatalog = buildUltimateShopCatalogByMaterial(rawData);

        const responseData = {};
        Object.keys(rawData).forEach(key => {
            const mapping = mappings[key] || {};
            const normKey = normalizeMaterialId(key) || key;
            const ultimateShopOffers = usCatalog[normKey] || [];
            responseData[key] = {
                buy: rawData[key].buy,
                sell: rawData[key].sell,
                source: rawData[key].source || 'vanilla',
                custom: rawData[key].custom === true,
                shop: mapping.shop || null,
                item: mapping.item || null,
                amount: mapping.amount || 1,
                displayName: mapping.displayName || null,
                customDisplayName: rawData[key].displayName || null,
                loreLine: rawData[key].loreLine || rawData[key].description || rawData[key].lore || null,
                ultimateShopOffers
            };
        });

        res.json(responseData);
    } catch (error) {
        console.error('读取价格数据出错:', error);
        res.status(500).json({ error: '读取错误' });
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
        const hours = Math.min(Number(req.query.hours) || 168, 24 * 30);
        const item = normalizeMaterialId(req.params.item) || req.params.item;
        res.json(analytics.getPriceHistory(item, hours));
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

app.post('/api/buy', (req, res) => {
    try {
        const user = authenticate(req);
        const { itemId, amount } = req.body || {};
        if (!itemId) {
            return res.status(400).json({ error: '缺少 itemId 参数' });
        }

        if (!PRICE_TABLE_PATHS.some((table) => fs.existsSync(table.path))) {
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
    purgeBlueMapShopMarkers();
    console.log(`✅ 高级版 UI 服务已启动！访问: ${protocol}://${HOST}:${PORT}`);
    localIpv4Addresses().forEach((ip) => {
        console.log(`📱 局域网访问: ${protocol}://${ip}:${PORT}`);
    });
    console.log(`📊 仪表板交易记录: ${TRANSACTIONS_YAML}`);
}

if (HTTPS_ENABLED && fs.existsSync(HTTPS_KEY_PATH) && fs.existsSync(HTTPS_CERT_PATH)) {
    https.createServer({
        key: fs.readFileSync(HTTPS_KEY_PATH),
        cert: fs.readFileSync(HTTPS_CERT_PATH)
    }, app).listen(PORT, HOST, () => logServerStart('https'));
} else {
    app.listen(PORT, HOST, () => {
        logServerStart('http');
        if (!HTTPS_ENABLED) {
            console.log('ℹ️ 当前为 HTTP 模式；如需 HTTPS，可设置 HTTPS=1 后重启服务。');
        } else {
            console.log('ℹ️ 未找到 HTTPS 证书；运行 npm run generate-cert 后重启服务即可启用 HTTPS。');
        }
    });
}