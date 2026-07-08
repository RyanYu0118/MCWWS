/**
 * Litematica → WorldEdit Sponge Schematic v3 (.schem)
 */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const nbt = require('prismarine-nbt');
const litematicParser = require('./litematic-parser');
const {
    normalizeRegionSize,
    normalizeRegionPosition,
    normalizeBlockStatePalette,
    normalizeBlockStatesLongs,
    normalizeRegions
} = litematicParser;

const SERVER_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
/** FAWE 实际读取的原理图目录（worldedit-config.yml → saving.dir） */
const FAWE_SCHEM_DIR = path.join(SERVER_ROOT, 'plugins', 'FastAsyncWorldEdit', 'schematics');
/** 兼容：纯 WorldEdit 时的目录 */
const LEGACY_WE_SCHEM_DIR = path.join(SERVER_ROOT, 'plugins', 'WorldEdit', 'schematics');
const DEFAULT_WE_SCHEM_DIR = FAWE_SCHEM_DIR;
const SCHEM_EXPORT_DIRS = [FAWE_SCHEM_DIR, LEGACY_WE_SCHEM_DIR];
const DEFAULT_DATA_VERSION = 3955;

function blockIndexV3(x, z, y, width, length) {
    return x + z * width + y * width * length;
}

function indexToLocalXYZ(i, size) {
    const w = Math.abs(Math.trunc(Number(size[0]) || 0));
    const h = Math.abs(Math.trunc(Number(size[1]) || 0));
    const l = Math.abs(Math.trunc(Number(size[2]) || 0));
    const x = i % w;
    const z = Math.floor(i / w) % l;
    const y = Math.floor(i / (w * l));
    const lx = size[0] < 0 ? size[0] + 1 + x : x;
    const ly = size[1] < 0 ? size[1] + 1 + y : y;
    const lz = size[2] < 0 ? size[2] + 1 + z : z;
    return { x: lx, y: ly, z: lz };
}

function blockStateToWeString(entry) {
    const name = String(entry?.Name ?? entry?.name ?? 'minecraft:air').toLowerCase().replace(/-/g, '_');
    const props = entry?.Properties ?? entry?.properties ?? {};
    const keys = Object.keys(props).sort();
    if (!keys.length) {
        return name;
    }
    const propStr = keys.map((k) => `${k}=${String(props[k])}`).join(',');
    return `${name}[${propStr}]`;
}

function isAirState(entry) {
    const name = String(entry?.Name ?? entry?.name ?? '').toLowerCase();
    return name === 'minecraft:air' || name === 'air';
}

function collectMergedBlocks(root) {
    const regions = normalizeRegions(root);
    const placed = [];
    Object.keys(regions).forEach((regionName) => {
        const region = regions[regionName] || {};
        const size = normalizeRegionSize(region);
        const pos = normalizeRegionPosition(region);
        const palette = normalizeBlockStatePalette(region);
        const longs = normalizeBlockStatesLongs(region);
        const volume = Math.abs(size[0]) * Math.abs(size[1]) * Math.abs(size[2]);
        if (!volume || !palette.length || !longs.length) {
            return;
        }
        const bits = Math.max(2, (palette.length - 1).toString(2).length);
        for (let i = 0; i < volume; i += 1) {
            const local = indexToLocalXYZ(i, size);
            const paletteIndex = readPaletteIndexFromRegion(longs, bits, i);
            const entry = palette[paletteIndex] || palette[0];
            if (!entry || isAirState(entry)) {
                continue;
            }
            placed.push({
                x: pos[0] + local.x,
                y: pos[1] + local.y,
                z: pos[2] + local.z,
                state: blockStateToWeString(entry)
            });
        }
    });
    return placed;
}

function readPaletteIndexFromRegion(longs, bits, index) {
    const startOffset = index * bits;
    let result = 0n;
    for (let i = 0; i < bits; i += 1) {
        const bitPos = startOffset + i;
        const longIndex = Math.floor(bitPos / 64);
        const bitInLong = bitPos % 64;
        const pair = longs[longIndex] || [0, 0];
        const hi = BigInt(pair[0]);
        const lo = BigInt(pair[1]) & 0xffffffffn;
        const longVal = (hi << 32n) | lo;
        const bit = (longVal >> BigInt(bitInLong)) & 1n;
        result |= bit << BigInt(i);
    }
    return Number(result);
}

function encodeVarints(indices) {
    const bytes = [];
    indices.forEach((value) => {
        let v = value >>> 0;
        while (true) {
            if ((v & ~0x7f) === 0) {
                bytes.push(v);
                break;
            }
            bytes.push((v & 0x7f) | 0x80);
            v >>>= 7;
        }
    });
    return bytes;
}

function schemFileBaseName(contentHash) {
    const hash = String(contentHash || '').toLowerCase();
    return `mcwws_${hash.slice(0, 16)}`;
}

/** 与 Litematica PositionUtils.getTransformedBlockPos 一致（先镜像后旋转，绕放置原点） */
function transformLocalPos(x, y, z, mirror, rotation) {
    let lx = Math.trunc(Number(x) || 0);
    let ly = Math.trunc(Number(y) || 0);
    let lz = Math.trunc(Number(z) || 0);

    if (mirror === 'LEFT_RIGHT') {
        lz = -lz;
    } else if (mirror === 'FRONT_BACK') {
        lx = -lx;
    }

    switch (rotation) {
        case 'CLOCKWISE_90':
            return { x: -lz, y: ly, z: lx };
        case 'COUNTERCLOCKWISE_90':
            return { x: lz, y: ly, z: -lx };
        case 'CLOCKWISE_180':
            return { x: -lx, y: ly, z: -lz };
        default:
            return { x: lx, y: ly, z: lz };
    }
}

function normalizeExportRotation(value) {
    const raw = String(value || 'NONE').trim().toUpperCase().replace(/[\s-]+/g, '_');
    if (raw === 'CLOCKWISE_90' || raw === 'CW90' || raw === 'CW_90' || raw === '90' || raw === 'R90') {
        return 'CLOCKWISE_90';
    }
    if (raw === 'CLOCKWISE_180' || raw === 'CW180' || raw === 'CW_180' || raw === '180' || raw === 'R180') {
        return 'CLOCKWISE_180';
    }
    if (raw === 'COUNTERCLOCKWISE_90' || raw === 'CCW90' || raw === 'CCW_90' || raw === '270' || raw === 'R270') {
        return 'COUNTERCLOCKWISE_90';
    }
    return 'NONE';
}

function normalizeExportMirror(value) {
    const raw = String(value || 'NONE').trim().toUpperCase().replace(/[\s-]+/g, '_');
    if (raw === 'LEFT_RIGHT' || raw === 'LEFTRIGHT' || raw === 'LR' || raw === 'LEFT') {
        return 'LEFT_RIGHT';
    }
    if (raw === 'FRONT_BACK' || raw === 'FRONTBACK' || raw === 'FB' || raw === 'FRONT' || raw === 'BACK') {
        return 'FRONT_BACK';
    }
    return 'NONE';
}

function exportTransformNeedsBake(rotation, mirror) {
    return normalizeExportRotation(rotation) !== 'NONE' || normalizeExportMirror(mirror) !== 'NONE';
}

function schemTransformSuffix(rotation, mirror) {
    const rot = normalizeExportRotation(rotation);
    const mir = normalizeExportMirror(mirror);
    if (!exportTransformNeedsBake(rot, mir)) {
        return '';
    }
    const parts = [];
    if (rot === 'CLOCKWISE_90') parts.push('r90');
    else if (rot === 'CLOCKWISE_180') parts.push('r180');
    else if (rot === 'COUNTERCLOCKWISE_90') parts.push('r270');
    if (mir === 'LEFT_RIGHT') parts.push('mlr');
    else if (mir === 'FRONT_BACK') parts.push('mfb');
    return `_${parts.join('_')}`;
}

function schemFileNameForTransform(contentHash, rotation, mirror) {
    const base = schemFileBaseName(contentHash);
    const suffix = schemTransformSuffix(rotation, mirror);
    return suffix ? `${base}${suffix}` : base;
}

async function exportWorldEditSchemFromBuffer(buffer, contentHash, options = {}) {
    const rotation = normalizeExportRotation(options.rotation);
    const mirror = normalizeExportMirror(options.mirror);
    const transformBaked = exportTransformNeedsBake(rotation, mirror);
    const { parsed } = await nbt.parse(buffer);
    const root = nbt.simplify(parsed);
    const placed = collectMergedBlocks(root);
    if (!placed.length) {
        throw new Error('投影中没有可粘贴的非空气方块。');
    }

    let minX = Infinity;
    let minY = Infinity;
    let minZ = Infinity;
    let maxX = -Infinity;
    let maxY = -Infinity;
    let maxZ = -Infinity;
    placed.forEach((block) => {
        minX = Math.min(minX, block.x);
        minY = Math.min(minY, block.y);
        minZ = Math.min(minZ, block.z);
        maxX = Math.max(maxX, block.x);
        maxY = Math.max(maxY, block.y);
        maxZ = Math.max(maxZ, block.z);
    });

    const width = maxX - minX + 1;
    const height = maxY - minY + 1;
    const length = maxZ - minZ + 1;

    let transformedBlocks = placed.map((block) => ({
        x: block.x - minX,
        y: block.y - minY,
        z: block.z - minZ,
        state: block.state
    }));

    let pasteAnchorAdjust = { x: 0, y: 0, z: 0 };
    let outWidth = width;
    let outHeight = height;
    let outLength = length;

    if (transformBaked) {
        transformedBlocks = transformedBlocks.map((block) => {
            const t = transformLocalPos(block.x, block.y, block.z, mirror, rotation);
            return { x: t.x, y: t.y, z: t.z, state: block.state };
        });
        let tMinX = Infinity;
        let tMinY = Infinity;
        let tMinZ = Infinity;
        let tMaxX = -Infinity;
        let tMaxY = -Infinity;
        let tMaxZ = -Infinity;
        transformedBlocks.forEach((block) => {
            tMinX = Math.min(tMinX, block.x);
            tMinY = Math.min(tMinY, block.y);
            tMinZ = Math.min(tMinZ, block.z);
            tMaxX = Math.max(tMaxX, block.x);
            tMaxY = Math.max(tMaxY, block.y);
            tMaxZ = Math.max(tMaxZ, block.z);
        });
        pasteAnchorAdjust = { x: tMinX, y: tMinY, z: tMinZ };
        transformedBlocks = transformedBlocks.map((block) => ({
            x: block.x - tMinX,
            y: block.y - tMinY,
            z: block.z - tMinZ,
            state: block.state
        }));
        outWidth = tMaxX - tMinX + 1;
        outHeight = tMaxY - tMinY + 1;
        outLength = tMaxZ - tMinZ + 1;
    }

    const paletteList = ['minecraft:air'];
    const paletteIndex = new Map([['minecraft:air', 0]]);
    function paletteIdFor(state) {
        if (!paletteIndex.has(state)) {
            paletteIndex.set(state, paletteList.length);
            paletteList.push(state);
        }
        return paletteIndex.get(state);
    }

    transformedBlocks.forEach((block) => {
        paletteIdFor(block.state);
    });

    const volume = outWidth * outHeight * outLength;
    const dataIndices = new Array(volume).fill(0);
    transformedBlocks.forEach((block) => {
        const index = blockIndexV3(block.x, block.z, block.y, outWidth, outLength);
        dataIndices[index] = paletteIdFor(block.state);
    });

    const paletteCompound = {};
    paletteList.forEach((state, idx) => {
        paletteCompound[state] = nbt.int(idx);
    });

    const dataVersion = Number(root.MinecraftDataVersion) || options.dataVersion || DEFAULT_DATA_VERSION;
    const schematicBody = nbt.comp({
        Version: nbt.int(3),
        DataVersion: nbt.int(dataVersion),
        Width: nbt.short(outWidth),
        Height: nbt.short(outHeight),
        Length: nbt.short(outLength),
        Offset: nbt.intArray([0, 0, 0]),
        Metadata: nbt.comp({
            WorldEdit: nbt.comp({
                Origin: nbt.intArray([0, 0, 0])
            })
        }),
        Blocks: nbt.comp({
            Palette: nbt.comp(paletteCompound),
            Data: nbt.byteArray(encodeVarints(dataIndices)),
            BlockEntities: nbt.list(nbt.comp([]))
        }),
        Entities: nbt.list(nbt.comp([]))
    });

    const schemRoot = nbt.comp({
        Schematic: schematicBody
    }, '');

    const raw = nbt.writeUncompressed(schemRoot, 'big');
    const gz = zlib.gzipSync(raw);

    const baseName = schemFileNameForTransform(contentHash, rotation, mirror);
    const dirs = options.outDirs || (options.outDir ? [options.outDir] : SCHEM_EXPORT_DIRS);
    let outPath = '';
    dirs.forEach((dir) => {
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
        const target = path.join(dir, `${baseName}.schem`);
        fs.writeFileSync(target, gz);
        if (!outPath || dir === FAWE_SCHEM_DIR) {
            outPath = target;
        }
    });

    return {
        schemFileName: baseName,
        schemPath: outPath,
        width: outWidth,
        height: outHeight,
        length: outLength,
        blockCount: transformedBlocks.length,
        originOffset: { x: minX, y: minY, z: minZ },
        rotation,
        mirror,
        transformBaked,
        pasteAnchorAdjust
    };
}

module.exports = {
    SERVER_ROOT,
    FAWE_SCHEM_DIR,
    LEGACY_WE_SCHEM_DIR,
    DEFAULT_WE_SCHEM_DIR,
    SCHEM_EXPORT_DIRS,
    schemFileBaseName,
    schemFileNameForTransform,
    schemTransformSuffix,
    normalizeExportRotation,
    normalizeExportMirror,
    exportTransformNeedsBake,
    transformLocalPos,
    exportWorldEditSchemFromBuffer,
    collectMergedBlocks
};
