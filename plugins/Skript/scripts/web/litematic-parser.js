/**
 * Litematica (.litematic) 解析与语义内容哈希（mcwws:content-v1）
 *
 * contentHash 仅由方块布局决定，忽略 Metadata 时间戳、预览图、调色板顺序等。
 * 客户端本地文件与服务器上传文件只要方块一致，哈希即相同。
 */
const crypto = require('crypto');
const nbt = require('prismarine-nbt');

const CONTENT_HASH_VERSION = 'mcwws:content-v1';
const AIR_KEYS = new Set(['minecraft:air', 'air']);

/** NBT simplify 后可能是数组、带数字键的对象或 TypedArray */
function normalizeNbtArray(value) {
    if (value == null) {
        return [];
    }
    if (Array.isArray(value)) {
        return value;
    }
    if (ArrayBuffer.isView(value)) {
        return Array.from(value);
    }
    if (typeof value === 'object') {
        if (Array.isArray(value.value)) {
            return value.value;
        }
        const numericKeys = Object.keys(value)
            .filter((k) => /^\d+$/.test(k))
            .sort((a, b) => Number(a) - Number(b));
        if (numericKeys.length) {
            return numericKeys.map((k) => value[k]);
        }
    }
    if (typeof value === 'number' || typeof value === 'string') {
        const n = Number(value);
        return Number.isFinite(n) ? [n] : [];
    }
    return [];
}

function normalizeIntTriple(value) {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
        const hasAxis = 'x' in value || 'y' in value || 'z' in value
            || 'X' in value || 'Y' in value || 'Z' in value;
        if (hasAxis) {
            return [
                Math.trunc(Number(value.x ?? value.X ?? 0) || 0),
                Math.trunc(Number(value.y ?? value.Y ?? 0) || 0),
                Math.trunc(Number(value.z ?? value.Z ?? 0) || 0)
            ];
        }
    }
    const arr = normalizeNbtArray(value).map((v) => Math.trunc(Number(v) || 0));
    while (arr.length < 3) {
        arr.push(0);
    }
    return arr.slice(0, 3);
}

function normalizeRegionSize(region) {
    return normalizeIntTriple(
        region?.Size ?? region?.size ?? region?.Dimensions ?? region?.dimensions
    );
}

function normalizeRegionPosition(region) {
    return normalizeIntTriple(
        region?.Position ?? region?.position ?? region?.Pos ?? region?.pos
    );
}

function normalizeBlockStatePalette(region) {
    const raw = region?.BlockStatePalette ?? region?.blockStatePalette ?? region?.Palette;
    const arr = normalizeNbtArray(raw);
    if (arr.length) {
        return arr;
    }
    if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
        return Object.keys(raw)
            .filter((k) => /^\d+$/.test(k))
            .sort((a, b) => Number(a) - Number(b))
            .map((k) => raw[k]);
    }
    return [];
}

function normalizeBlockStatesLongs(region) {
    const raw = region?.BlockStates ?? region?.blockStates ?? region?.Blocks;
    const arr = normalizeNbtArray(raw);
    if (arr.length) {
        return arr;
    }
    return [];
}

function nbtLongToBigInt(pair) {
    if (!Array.isArray(pair) || pair.length < 2) {
        return 0n;
    }
    const hi = BigInt(pair[0]);
    const lo = BigInt(pair[1]) & 0xffffffffn;
    return (hi << 32n) | lo;
}

function bitsPerEntry(paletteSize) {
    const size = Math.max(1, Number(paletteSize) || 1);
    return Math.max(2, (size - 1).toString(2).length);
}

function readPaletteIndex(longs, bits, index) {
    const startOffset = index * bits;
    let result = 0n;
    for (let i = 0; i < bits; i += 1) {
        const bitPos = startOffset + i;
        const longIndex = Math.floor(bitPos / 64);
        const bitInLong = bitPos % 64;
        const longVal = nbtLongToBigInt(longs[longIndex] || [0, 0]);
        const bit = (longVal >> BigInt(bitInLong)) & 1n;
        result |= bit << BigInt(i);
    }
    return Number(result);
}

function normalizeBlockName(name) {
    let s = String(name || '').trim().toLowerCase().replace(/-/g, '_');
    if (!s) {
        return '';
    }
    if (!s.includes(':')) {
        s = `minecraft:${s}`;
    }
    return s;
}

function canonicalBlockStateKey(entry) {
    const name = normalizeBlockName(entry?.Name ?? entry?.name);
    const props = entry?.Properties ?? entry?.properties ?? {};
    const keys = Object.keys(props).sort();
    if (!keys.length) {
        return name;
    }
    const propStr = keys.map((k) => `${k}=${String(props[k])}`).join(',');
    return `${name}[${propStr}]`;
}

function blockStateToMaterialId(stateKey) {
    const base = String(stateKey || '').split('[')[0];
    const id = base.includes(':') ? base.split(':').pop() : base;
    return id || null;
}

function regionVolume(sizeArr) {
    const dims = (sizeArr || []).map((v) => Math.abs(Math.trunc(Number(v) || 0)));
    if (dims.length < 3 || dims.some((d) => d <= 0)) {
        return 0;
    }
    return dims[0] * dims[1] * dims[2];
}

function countRegionBlocks(region) {
    const size = normalizeRegionSize(region);
    const volume = regionVolume(size);
    const palette = normalizeBlockStatePalette(region);
    const longs = normalizeBlockStatesLongs(region);
    if (!volume || !palette.length || !longs.length) {
        return new Map();
    }

    const bits = bitsPerEntry(palette.length);
    const counts = new Map();
    for (let i = 0; i < volume; i += 1) {
        const paletteIndex = readPaletteIndex(longs, bits, i);
        const entry = palette[paletteIndex] || palette[0];
        const stateKey = canonicalBlockStateKey(entry);
        if (!stateKey || AIR_KEYS.has(stateKey) || stateKey.startsWith('minecraft:air[')) {
            continue;
        }
        counts.set(stateKey, (counts.get(stateKey) || 0) + 1);
    }
    return counts;
}

function sortedCountEntries(counts) {
    return [...counts.entries()]
        .sort((a, b) => a[0].localeCompare(b[0]))
        .map(([state, count]) => [state, count]);
}

function normalizeRegions(root) {
    const raw = root?.Regions ?? root?.regions;
    if (!raw || typeof raw !== 'object') {
        return {};
    }
    if (Array.isArray(raw)) {
        const out = {};
        raw.forEach((region, index) => {
            const name = String(region?.Name ?? region?.name ?? `region_${index}`).trim() || `region_${index}`;
            out[name] = region;
        });
        return out;
    }
    return raw;
}

function buildCanonicalPayload(root) {
    const regions = normalizeRegions(root);
    const regionNames = Object.keys(regions).sort();
    return {
        version: CONTENT_HASH_VERSION,
        regions: regionNames.map((name) => {
            const region = regions[name] || {};
            const size = normalizeRegionSize(region);
            const position = normalizeRegionPosition(region);
            const counts = countRegionBlocks(region);
            return {
                name,
                size,
                position,
                blocks: sortedCountEntries(counts)
            };
        })
    };
}

function computeContentHashFromPayload(payload) {
    const json = JSON.stringify(payload);
    return crypto.createHash('sha256').update(json, 'utf8').digest('hex');
}

function mergeMaterialsFromPayload(payload) {
    const merged = new Map();
    (payload.regions || []).forEach((region) => {
        (region.blocks || []).forEach(([stateKey, count]) => {
            const itemId = blockStateToMaterialId(stateKey);
            const key = itemId || stateKey;
            merged.set(key, (merged.get(key) || 0) + count);
        });
    });
    return [...merged.entries()]
        .sort((a, b) => b[1] - a[1])
        .map(([label, count]) => ({
            label,
            itemId: blockStateToMaterialId(label) || label,
            count
        }));
}

function readMetadataName(root) {
    const meta = root?.Metadata && typeof root.Metadata === 'object' ? root.Metadata : {};
    const name = meta.Name ?? meta.name ?? root?.Name ?? root?.name ?? '';
    return String(name || '').trim();
}

async function parseLitematicBuffer(buffer) {
    if (!buffer || !buffer.length) {
        throw new Error('Litematica 文件为空。');
    }
    const { parsed } = await nbt.parse(buffer);
    const root = nbt.simplify(parsed);
    if (!root || typeof root !== 'object') {
        throw new Error('无法解析 Litematica NBT 结构。');
    }
    const payload = buildCanonicalPayload(root);
    const contentHash = computeContentHashFromPayload(payload);
    const materials = mergeMaterialsFromPayload(payload);
    const blockCount = materials.reduce((sum, entry) => sum + entry.count, 0);
    const regionCount = payload.regions.length;
    const listName = readMetadataName(root);

    return {
        contentHash,
        contentHashVersion: CONTENT_HASH_VERSION,
        listName,
        materials,
        blockCount,
        regionCount,
        canonicalPayload: payload,
        litematicaVersion: Number(root.Version) || null,
        minecraftDataVersion: Number(root.MinecraftDataVersion) || null
    };
}

function computeContentHashFromBuffer(buffer) {
    return parseLitematicBuffer(buffer).then((result) => result.contentHash);
}

function verifyContentHash(buffer, expectedHash) {
    return parseLitematicBuffer(buffer).then((result) => ({
        ok: result.contentHash === String(expectedHash || '').toLowerCase(),
        contentHash: result.contentHash,
        expectedHash: String(expectedHash || '').toLowerCase()
    }));
}

module.exports = {
    CONTENT_HASH_VERSION,
    parseLitematicBuffer,
    computeContentHashFromBuffer,
    verifyContentHash,
    buildCanonicalPayload,
    computeContentHashFromPayload,
    normalizeIntTriple,
    normalizeRegionSize,
    normalizeRegionPosition,
    normalizeBlockStatePalette,
    normalizeBlockStatesLongs,
    normalizeNbtArray,
    normalizeRegions
};
