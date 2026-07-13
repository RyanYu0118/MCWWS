/**
 * 投影文件存储、报价与粘贴订单；全链路以 contentHash 为唯一内容标识。
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const yaml = require('js-yaml');
const litematicParser = require('./litematic-parser');
const { exportWorldEditSchemFromBuffer, schemFileBaseName } = require('./litematic-to-schem');

const SCHEMATICS_DIR = path.join(__dirname, 'data', 'build_schematics');
const BUILD_QUOTES_PATH = path.join(__dirname, 'data', 'build_quotes.yml');
const PENDING_PASTE_ORDERS_PATH = path.join(__dirname, 'data', 'pending_paste_orders.yml');

const QUOTE_TTL_MS = 24 * 60 * 60 * 1000;
const PASTE_TOKEN_TTL_MS = 15 * 60 * 1000;

const PASTE_ROTATION_VALUES = new Set(['NONE', 'CLOCKWISE_90', 'CLOCKWISE_180', 'COUNTERCLOCKWISE_90']);
const PASTE_MIRROR_VALUES = new Set(['NONE', 'LEFT_RIGHT', 'FRONT_BACK']);

const PASTE_ROTATION_ALIASES = {
    CW90: 'CLOCKWISE_90',
    CW_90: 'CLOCKWISE_90',
    '90': 'CLOCKWISE_90',
    R90: 'CLOCKWISE_90',
    CW180: 'CLOCKWISE_180',
    CW_180: 'CLOCKWISE_180',
    '180': 'CLOCKWISE_180',
    R180: 'CLOCKWISE_180',
    CCW90: 'COUNTERCLOCKWISE_90',
    CCW_90: 'COUNTERCLOCKWISE_90',
    '270': 'COUNTERCLOCKWISE_90',
    R270: 'COUNTERCLOCKWISE_90',
    '-90': 'COUNTERCLOCKWISE_90'
};

const PASTE_MIRROR_ALIASES = {
    LR: 'LEFT_RIGHT',
    LEFTRIGHT: 'LEFT_RIGHT',
    LEFT: 'LEFT_RIGHT',
    FB: 'FRONT_BACK',
    FRONTBACK: 'FRONT_BACK',
    FRONT: 'FRONT_BACK',
    BACK: 'FRONT_BACK'
};

function normalizePasteRotation(value) {
    const raw = String(value || 'NONE').trim().toUpperCase().replace(/[\s-]+/g, '_');
    if (PASTE_ROTATION_VALUES.has(raw)) {
        return raw;
    }
    const alias = PASTE_ROTATION_ALIASES[raw.replace(/_/g, '')] || PASTE_ROTATION_ALIASES[raw];
    if (alias) {
        return alias;
    }
    return 'NONE';
}

function normalizePasteMirror(value) {
    const raw = String(value || 'NONE').trim().toUpperCase().replace(/[\s-]+/g, '_');
    if (PASTE_MIRROR_VALUES.has(raw)) {
        return raw;
    }
    const compact = raw.replace(/_/g, '');
    const alias = PASTE_MIRROR_ALIASES[compact] || PASTE_MIRROR_ALIASES[raw];
    if (alias) {
        return alias;
    }
    return 'NONE';
}

const PASTE_REPLACE_VALUES = new Set(['NONE', 'WITH_NON_AIR', 'ALL']);

function normalizePasteReplaceBehavior(value) {
    const raw = String(value || 'NONE').trim().toUpperCase().replace(/[\s-]+/g, '_');
    if (PASTE_REPLACE_VALUES.has(raw)) {
        return raw;
    }
    if (raw === 'WITH_NON_AIR_BLOCKS' || raw === 'WITHNONAIR' || raw === 'NON_AIR') {
        return 'WITH_NON_AIR';
    }
    return 'NONE';
}

function pasteTransformNeedsWorldEdit(rotation, mirror) {
    return normalizePasteRotation(rotation) !== 'NONE' || normalizePasteMirror(mirror) !== 'NONE';
}

function ensureDirs() {
    if (!fs.existsSync(SCHEMATICS_DIR)) {
        fs.mkdirSync(SCHEMATICS_DIR, { recursive: true });
    }
}

function loadYaml(filePath, fallback) {
    if (!fs.existsSync(filePath)) {
        return fallback;
    }
    try {
        return yaml.load(fs.readFileSync(filePath, 'utf8')) || fallback;
    } catch (error) {
        console.error(`[build-schematic] 读取失败 ${filePath}:`, error.message);
        return fallback;
    }
}

function saveYaml(filePath, data) {
    fs.writeFileSync(filePath, yaml.dump(data, { lineWidth: 120, noRefs: true }), 'utf8');
}

function schematicFilePath(contentHash) {
    return path.join(SCHEMATICS_DIR, `${contentHash}.litematic`);
}

function schematicMetaPath(contentHash) {
    return path.join(SCHEMATICS_DIR, `${contentHash}.meta.json`);
}

function readBufferFromRequestBody(body) {
    if (Buffer.isBuffer(body)) {
        return body;
    }
    if (body && typeof body === 'object' && typeof body.dataBase64 === 'string') {
        return Buffer.from(body.dataBase64, 'base64');
    }
    if (typeof body === 'string') {
        return Buffer.from(body, 'base64');
    }
    return null;
}

async function ingestSchematicBuffer(buffer, options = {}) {
    ensureDirs();
    const parsed = await litematicParser.parseLitematicBuffer(buffer);
    const { contentHash } = parsed;
    const filePath = schematicFilePath(contentHash);
    const metaPath = schematicMetaPath(contentHash);

    if (!fs.existsSync(filePath)) {
        fs.writeFileSync(filePath, buffer);
    } else if (!options.skipFileHashCheck) {
        const existing = fs.readFileSync(filePath);
        const existingParsed = await litematicParser.parseLitematicBuffer(existing);
        if (existingParsed.contentHash !== contentHash) {
            throw new Error('同名存储文件与解析哈希不一致，请联系管理员。');
        }
    }

    const meta = {
        contentHash,
        contentHashVersion: parsed.contentHashVersion,
        listName: parsed.listName,
        blockCount: parsed.blockCount,
        regionCount: parsed.regionCount,
        schemFileName: schemFileBaseName(contentHash),
        fileSha256: crypto.createHash('sha256').update(buffer).digest('hex'),
        originalFileName: String(options.fileName || '').trim(),
        uploadedAt: new Date().toISOString()
    };
    try {
        const schem = await exportWorldEditSchemFromBuffer(buffer, contentHash);
        meta.schemFileName = schem.schemFileName;
        meta.schemPath = schem.schemPath;
        meta.schemOriginOffset = schem.originOffset;
        meta.schemSize = { width: schem.width, height: schem.height, length: schem.length };
        meta.schemExportError = '';
    } catch (schemError) {
        console.warn('[build-schematic] WorldEdit 导出失败:', schemError.message);
        meta.schemExportError = schemError.message || String(schemError);
    }
    fs.writeFileSync(metaPath, JSON.stringify(meta, null, 2), 'utf8');

    return { ...parsed, meta, stored: true };
}

async function verifyStoredSchematicHash(contentHash) {
    const hash = String(contentHash || '').toLowerCase();
    const filePath = schematicFilePath(hash);
    if (!fs.existsSync(filePath)) {
        return { ok: false, reason: 'not_found', contentHash: hash };
    }
    const buffer = fs.readFileSync(filePath);
    const parsed = await litematicParser.parseLitematicBuffer(buffer);
    const ok = parsed.contentHash === hash;
    return {
        ok,
        contentHash: parsed.contentHash,
        expectedHash: hash,
        reason: ok ? 'match' : 'mismatch'
    };
}

function loadQuotesStore() {
    const data = loadYaml(BUILD_QUOTES_PATH, { next_id: 1, quotes: {} });
    if (!data.quotes || typeof data.quotes !== 'object') {
        data.quotes = {};
    }
    if (!Number.isFinite(Number(data.next_id))) {
        data.next_id = 1;
    }
    return data;
}

function saveQuotesStore(data) {
    saveYaml(BUILD_QUOTES_PATH, data);
}

function loadPasteOrdersStore() {
    const data = loadYaml(PENDING_PASTE_ORDERS_PATH, { next_id: 1, orders: {} });
    if (!data.orders || typeof data.orders !== 'object') {
        data.orders = {};
    }
    if (!Number.isFinite(Number(data.next_id))) {
        data.next_id = 1;
    }
    return data;
}

function savePasteOrdersStore(data) {
    saveYaml(PENDING_PASTE_ORDERS_PATH, data);
}

function isQuoteExpired(quote) {
    if (!quote || !quote.expiresAt) {
        return true;
    }
    return Date.now() > Date.parse(quote.expiresAt);
}

function createBuildQuote({
    contentHash,
    listName,
    materials,
    quoteLines,
    purchasableTotal,
    materialTotal,
    buildServiceFee,
    freeUnlistedTotal,
    playerUuid,
    playerId,
    username
}) {
    const store = loadQuotesStore();
    const quoteUuid = crypto.randomUUID();
    const numericId = Number(store.next_id) || 1;
    const now = new Date();
    const normCheckout = Math.round(Number(purchasableTotal) * 100) / 100;
    const normMaterial = Math.round(Number(materialTotal ?? purchasableTotal) * 100) / 100;
    const normFee = Math.round(Number(buildServiceFee ?? 0) * 100) / 100;
    const quote = {
        id: quoteUuid,
        numericId,
        contentHash: String(contentHash).toLowerCase(),
        contentHashVersion: litematicParser.CONTENT_HASH_VERSION,
        listName: String(listName || '').trim(),
        materials,
        quoteLines,
        materialTotal: normMaterial,
        buildServiceFee: normFee,
        purchasableTotal: normCheckout,
        freeUnlistedTotal,
        playerUuid: playerUuid || '',
        playerId: playerId || '',
        username: username || '',
        status: 'open',
        createdAt: now.toISOString(),
        expiresAt: new Date(now.getTime() + QUOTE_TTL_MS).toISOString()
    };
    store.quotes[String(numericId)] = quote;
    store.next_id = numericId + 1;
    saveQuotesStore(store);
    return quote;
}

function getBuildQuote(numericId) {
    const store = loadQuotesStore();
    const quote = store.quotes[String(numericId)];
    if (!quote) {
        return null;
    }
    if (isQuoteExpired(quote) && quote.status === 'open') {
        quote.status = 'expired';
    }
    return quote;
}

function markQuoteConsumed(numericId, pasteOrderId) {
    const store = loadQuotesStore();
    const quote = store.quotes[String(numericId)];
    if (!quote) {
        return null;
    }
    quote.status = 'consumed';
    quote.consumedAt = new Date().toISOString();
    quote.pasteOrderId = pasteOrderId;
    saveQuotesStore(store);
    return quote;
}

async function ensureWorldEditSchemExport(contentHash, options = {}) {
    const hash = String(contentHash || '').toLowerCase();
    const litematicPath = schematicFilePath(hash);
    if (!fs.existsSync(litematicPath)) {
        throw new Error('投影文件不存在，请重新上传。');
    }
    const buffer = fs.readFileSync(litematicPath);
    const rotation = normalizePasteRotation(options.rotation);
    const mirror = normalizePasteMirror(options.mirror);
    return exportWorldEditSchemFromBuffer(buffer, hash, { rotation, mirror });
}

function normalizePasteAnchor(anchor) {
    if (!anchor || typeof anchor !== 'object') {
        return null;
    }
    const world = String(anchor.world || '').trim();
    const x = Number(anchor.x);
    const y = Number(anchor.y);
    const z = Number(anchor.z);
    if (!world || !Number.isFinite(x) || !Number.isFinite(y) || !Number.isFinite(z)) {
        return null;
    }
    return {
        world,
        x: Math.floor(x),
        y: Math.floor(y),
        z: Math.floor(z),
        yaw: Number.isFinite(Number(anchor.yaw)) ? Number(anchor.yaw) : 0,
        pitch: Number.isFinite(Number(anchor.pitch)) ? Number(anchor.pitch) : 0
    };
}

function createPasteOrder({
    contentHash,
    quoteId,
    playerUuid,
    playerId,
    username,
    total,
    anchor,
    rotation,
    mirror,
    pasteReplaceBehavior
}) {
    return verifyStoredSchematicHash(contentHash).then(async (verify) => {
        if (!verify.ok) {
            throw new Error('粘贴订单创建失败：存储的投影文件与 contentHash 不一致。');
        }
        const normalizedRotation = normalizePasteRotation(rotation);
        const normalizedMirror = normalizePasteMirror(mirror);
        const normalizedReplaceBehavior = normalizePasteReplaceBehavior(pasteReplaceBehavior);
        let schemExport;
        try {
            schemExport = await ensureWorldEditSchemExport(contentHash, {
                rotation: normalizedRotation,
                mirror: normalizedMirror
            });
        } catch (exportError) {
            throw new Error(`WorldEdit 原理图导出失败：${exportError.message || exportError}`);
        }
        const schemPath = path.join(
            require('./litematic-to-schem').FAWE_SCHEM_DIR,
            `${schemExport.schemFileName}.schem`
        );
        if (!fs.existsSync(schemPath)) {
            throw new Error(`原理图文件未生成（FAWE 目录）：${schemPath}`);
        }
        const store = loadPasteOrdersStore();
        const orderUuid = crypto.randomUUID();
        const numericId = Number(store.next_id) || 1;
        const pasteToken = crypto.randomBytes(16).toString('hex');
        const now = new Date();
        const metaPath = schematicMetaPath(contentHash);
        let schemFileName = schemExport?.schemFileName || schemFileBaseName(contentHash);
        let schemOriginOffset = schemExport?.originOffset || null;
        if (fs.existsSync(metaPath)) {
            try {
                const meta = JSON.parse(fs.readFileSync(metaPath, 'utf8'));
                if (meta.schemOriginOffset && !schemOriginOffset) {
                    schemOriginOffset = meta.schemOriginOffset;
                }
            } catch (metaError) {
                console.warn('[build-schematic] 读取 meta 失败:', metaError.message);
            }
        }
        const normalizedAnchor = normalizePasteAnchor(anchor);
        const order = {
            id: orderUuid,
            numericId,
            pasteToken,
            contentHash: String(contentHash).toLowerCase(),
            contentHashVersion: litematicParser.CONTENT_HASH_VERSION,
            quoteId,
            playerUuid,
            playerId,
            username,
            total,
            status: normalizedAnchor ? 'ready' : 'awaiting_anchor',
            anchor: normalizedAnchor,
            anchorSetAt: normalizedAnchor ? now.toISOString() : '',
            rotation: normalizedRotation,
            mirror: normalizedMirror,
            pasteReplaceBehavior: normalizedReplaceBehavior,
            schemFileName,
            schemOriginOffset,
            schemSize: schemExport?.width ? {
                width: schemExport.width,
                height: schemExport.height,
                length: schemExport.length
            } : null,
            transformBaked: !!schemExport?.transformBaked,
            pasteAnchorAdjust: schemExport?.pasteAnchorAdjust || { x: 0, y: 0, z: 0 },
            schematicPath: schematicFilePath(contentHash),
            createdAt: now.toISOString(),
            pasteTokenExpiresAt: new Date(now.getTime() + PASTE_TOKEN_TTL_MS).toISOString(),
            pastedAt: '',
            failureReason: ''
        };
        store.orders[String(numericId)] = order;
        store.next_id = numericId + 1;
        savePasteOrdersStore(store);
        return order;
    });
}

function getPasteOrder(numericId) {
    const store = loadPasteOrdersStore();
    return store.orders[String(numericId)] || null;
}

function getPasteOrderByToken(pasteToken) {
    const store = loadPasteOrdersStore();
    return Object.values(store.orders).find((order) => order.pasteToken === pasteToken) || null;
}

function updatePasteOrderAnchor(numericId, anchor, playerUuid, options = {}) {
    const store = loadPasteOrdersStore();
    const order = store.orders[String(numericId)];
    if (!order) {
        return { error: '订单不存在。' };
    }
    if (order.playerUuid && playerUuid && order.playerUuid !== playerUuid) {
        return { error: '无权操作此订单。' };
    }
    if (order.status !== 'awaiting_anchor' && order.status !== 'ready') {
        return { error: `订单状态为 ${order.status}，无法设置锚点。` };
    }
    const normalizedAnchor = normalizePasteAnchor(anchor);
    if (!normalizedAnchor) {
        return { error: '锚点坐标或世界名无效。' };
    }
    order.anchor = normalizedAnchor;
    order.status = 'ready';
    order.anchorSetAt = new Date().toISOString();
    if (options.rotation != null) {
        order.rotation = normalizePasteRotation(options.rotation);
    }
    if (options.mirror != null) {
        order.mirror = normalizePasteMirror(options.mirror);
    }
    if (options.source != null) {
        order.anchorSource = String(options.source);
    }
    savePasteOrdersStore(store);
    return { order };
}

async function assertPasteOrderHash(order) {
    if (!order) {
        throw new Error('订单不存在。');
    }
    const verify = await verifyStoredSchematicHash(order.contentHash);
    if (!verify.ok) {
        throw new Error('粘贴前哈希校验失败：投影文件已被替换或损坏。');
    }
    return verify;
}

function consumePasteToken(pasteToken, playerUuid) {
    const store = loadPasteOrdersStore();
    const order = Object.values(store.orders).find((item) => item.pasteToken === pasteToken);
    if (!order) {
        return { error: '无效的粘贴凭证。' };
    }
    if (order.playerUuid && playerUuid && order.playerUuid !== playerUuid) {
        return { error: '粘贴凭证不属于当前玩家。' };
    }
    if (Date.now() > Date.parse(order.pasteTokenExpiresAt || 0)) {
        order.status = 'expired';
        savePasteOrdersStore(store);
        return { error: '粘贴凭证已过期，请重新购买。' };
    }
    if (order.status !== 'ready') {
        return { error: `订单尚未就绪（${order.status}），请先设置锚点。` };
    }
    return assertPasteOrderHash(order).then(() => {
        order.status = 'pasting';
        order.pasteStartedAt = new Date().toISOString();
        savePasteOrdersStore(store);
        return { order };
    }).catch((error) => ({ error: error.message }));
}

function completePasteOrder(numericId, success, failureReason) {
    const store = loadPasteOrdersStore();
    const order = store.orders[String(numericId)];
    if (!order) {
        return null;
    }
    order.status = success ? 'completed' : 'failed';
    order.pastedAt = new Date().toISOString();
    order.failureReason = failureReason || '';
    order.pasteToken = '';
    savePasteOrdersStore(store);
    return order;
}

function enqueueWebPaste(numericId, playerUuid) {
    const store = loadPasteOrdersStore();
    const order = store.orders[String(numericId)];
    if (!order) {
        return { error: '粘贴订单不存在。' };
    }
    const normUuid = String(playerUuid || '').trim().toLowerCase();
    if (!normUuid) {
        return { error: '无法识别玩家身份。' };
    }
    if (order.playerUuid && order.playerUuid.toLowerCase() !== normUuid) {
        return { error: '无权操作此订单。' };
    }
    if (order.status !== 'ready') {
        return { error: `订单状态为 ${order.status}，无法粘贴。` };
    }
    if (!order.anchor?.world) {
        return { error: '请先设置锚点。' };
    }
    const queue = String(order.webPasteQueue || '').trim();
    if (queue === 'pending' || queue === 'processing') {
        return { error: '粘贴已在队列中，请稍候。' };
    }
    if (order.status === 'pasting') {
        return { error: '粘贴正在进行中。' };
    }
    const nowIso = new Date().toISOString();
    order.webPasteQueue = 'pending';
    order.webPasteQueuedAt = nowIso;
    order.webPasteQueueError = '';
    order.webPasteProcessedAt = '';
    savePasteOrdersStore(store);
    return { order };
}

module.exports = {
    SCHEMATICS_DIR,
    BUILD_QUOTES_PATH,
    PENDING_PASTE_ORDERS_PATH,
    QUOTE_TTL_MS,
    PASTE_TOKEN_TTL_MS,
    PASTE_ROTATION_VALUES,
    PASTE_MIRROR_VALUES,
    normalizePasteRotation,
    normalizePasteMirror,
    normalizePasteReplaceBehavior,
    normalizePasteAnchor,
    pasteTransformNeedsWorldEdit,
    readBufferFromRequestBody,
    ingestSchematicBuffer,
    verifyStoredSchematicHash,
    createBuildQuote,
    getBuildQuote,
    markQuoteConsumed,
    createPasteOrder,
    getPasteOrder,
    getPasteOrderByToken,
    updatePasteOrderAnchor,
    assertPasteOrderHash,
    consumePasteToken,
    completePasteOrder,
    enqueueWebPaste,
    ensureWorldEditSchemExport,
    schematicFilePath,
    loadPasteOrdersStore
};
