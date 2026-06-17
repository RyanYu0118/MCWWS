/**
 * Litematica → WorldEdit Sponge Schematic v3 (.schem)
 */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const nbt = require('prismarine-nbt');
const litematicParser = require('./litematic-parser');

const DEFAULT_WE_SCHEM_DIR = path.join(__dirname, '..', '..', '..', 'WorldEdit', 'schematics');
const DEFAULT_DATA_VERSION = 3955;

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
    const regions = root?.Regions && typeof root.Regions === 'object' ? root.Regions : {};
    const placed = [];
    Object.keys(regions).forEach((regionName) => {
        const region = regions[regionName] || {};
        const size = (region.Size || []).map((v) => Math.trunc(Number(v) || 0));
        const pos = (region.Position || []).map((v) => Math.trunc(Number(v) || 0));
        const palette = Array.isArray(region.BlockStatePalette) ? region.BlockStatePalette : [];
        const longs = Array.isArray(region.BlockStates) ? region.BlockStates : [];
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
    return `mcwws_${String(contentHash || '').toLowerCase().slice(0, 16)}`;
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
    const blockMap = new Map();
    placed.forEach((block) => {
        const rx = block.x - minX;
        const ry = block.y - minY;
        const rz = block.z - minZ;
        blockMap.set(`${rx},${ry},${rz}`, block.state);
    });

    const paletteList = ['minecraft:air'];
    const paletteIndex = new Map([['minecraft:air', 0]]);
    function paletteIdFor(state) {
        if (!paletteIndex.has(state)) {
            paletteIndex.set(state, paletteList.length);
            paletteList.push(state);
        }
        return paletteIndex.get(state);
    }

    const dataIndices = [];
    for (let y = 0; y < height; y += 1) {
        for (let z = 0; z < length; z += 1) {
            for (let x = 0; x < width; x += 1) {
                const state = blockMap.get(`${x},${y},${z}`) || 'minecraft:air';
                dataIndices.push(paletteIdFor(state));
            }
        }
    }

    const paletteCompound = {};
    paletteList.forEach((state, idx) => {
        paletteCompound[String(idx)] = nbt.string(state);
    });

    const dataVersion = Number(root.MinecraftDataVersion) || options.dataVersion || DEFAULT_DATA_VERSION;
    const schematic = nbt.comp({
        Version: nbt.int(3),
        DataVersion: nbt.int(dataVersion),
        Width: nbt.short(width),
        Height: nbt.short(height),
        Length: nbt.short(length),
        Offset: nbt.intArray([0, 0, 0]),
        Metadata: nbt.comp({
            'WEOffsetX': nbt.int(0),
            'WEOffsetY': nbt.int(0),
            'WEOffsetZ': nbt.int(0)
        }),
        Blocks: nbt.comp({
            Palette: nbt.comp(paletteCompound),
            Data: nbt.byteArray(encodeVarints(dataIndices))
        }),
        BlockEntities: nbt.list([]),
        Entities: nbt.list([])
    });

    const named = nbt.comp({ Schematic: schematic }, 'Schematic');
    const raw = nbt.writeUncompressed(named, 'big');
    const gz = zlib.gzipSync(raw);

    const baseName = schemFileBaseName(contentHash);
    const outDir = options.outDir || DEFAULT_WE_SCHEM_DIR;
    if (!fs.existsSync(outDir)) {
        fs.mkdirSync(outDir, { recursive: true });
    }
    const outPath = path.join(outDir, `${baseName}.schem`);
    fs.writeFileSync(outPath, gz);

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
    DEFAULT_WE_SCHEM_DIR,
    schemFileBaseName,
    exportWorldEditSchemFromBuffer,
    collectMergedBlocks
};
