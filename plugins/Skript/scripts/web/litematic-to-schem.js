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

async function exportWorldEditSchemFromBuffer(buffer, contentHash, options = {}) {
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

    const paletteList = ['minecraft:air'];
    const paletteIndex = new Map([['minecraft:air', 0]]);
    function paletteIdFor(state) {
        if (!paletteIndex.has(state)) {
            paletteIndex.set(state, paletteList.length);
            paletteList.push(state);
        }
        return paletteIndex.get(state);
    }

    placed.forEach((block) => {
        paletteIdFor(block.state);
    });

    const volume = width * height * length;
    const dataIndices = new Array(volume).fill(0);
    placed.forEach((block) => {
        const rx = block.x - minX;
        const ry = block.y - minY;
        const rz = block.z - minZ;
        const index = blockIndexV3(rx, rz, ry, width, length);
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
        Width: nbt.short(width),
        Height: nbt.short(height),
        Length: nbt.short(length),
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

    const baseName = schemFileBaseName(contentHash);
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
        width,
        height,
        length,
        blockCount: placed.length,
        originOffset: { x: minX, y: minY, z: minZ }
    };
}

module.exports = {
    SERVER_ROOT,
    FAWE_SCHEM_DIR,
    LEGACY_WE_SCHEM_DIR,
    DEFAULT_WE_SCHEM_DIR,
    SCHEM_EXPORT_DIRS,
    schemFileBaseName,
    exportWorldEditSchemFromBuffer,
    collectMergedBlocks
};
