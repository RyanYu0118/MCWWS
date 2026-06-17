#!/usr/bin/env node
/**
 * 从已存储的 .litematic 重新导出 WorldEdit .schem
 * 用法: node tools/reexport-schem.js <contentHash>
 */
const fs = require('fs');
const path = require('path');
const { exportWorldEditSchemFromBuffer } = require('../litematic-to-schem');
const { parseLitematicBuffer } = require('../litematic-parser');

async function main() {
    const hash = String(process.argv[2] || '').trim().toLowerCase();
    if (!hash) {
        console.error('用法: node tools/reexport-schem.js <contentHash>');
        process.exit(1);
    }
    const filePath = path.join(__dirname, '..', 'data', 'build_schematics', `${hash}.litematic`);
    if (!fs.existsSync(filePath)) {
        console.error('找不到文件:', filePath);
        process.exit(1);
    }
    const buffer = fs.readFileSync(filePath);
    const parsed = await parseLitematicBuffer(buffer);
    console.log('方块数:', parsed.blockCount, '材料种类:', parsed.materials.length);
    const schem = await exportWorldEditSchemFromBuffer(buffer, hash);
    console.log('已导出:', schem.schemPath);
}

main().catch((error) => {
    console.error('导出失败:', error.message || error);
    process.exit(1);
});
