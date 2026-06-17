#!/usr/bin/env node
/**
 * 本地核对 Litematica 投影 contentHash（与服务器算法一致）
 * 用法: node tools/litematic-hash.js path/to/build.litematic
 */
const fs = require('fs');
const path = require('path');
const parser = require('../litematic-parser');

async function main() {
    const filePath = process.argv[2];
    if (!filePath) {
        console.error('用法: node tools/litematic-hash.js <file.litematic>');
        process.exit(1);
    }
    const abs = path.resolve(filePath);
    if (!fs.existsSync(abs)) {
        console.error('文件不存在:', abs);
        process.exit(1);
    }
    const buffer = fs.readFileSync(abs);
    const result = await parser.parseLitematicBuffer(buffer);
    console.log('文件:', path.basename(abs));
    console.log('名称:', result.listName || '(未命名)');
    console.log('contentHash:', result.contentHash);
    console.log('算法版本:', result.contentHashVersion);
    console.log('区域数:', result.regionCount);
    console.log('非空气方块:', result.blockCount);
    console.log('材料种类:', result.materials.length);
    console.log('');
    console.log('请确认此哈希与网页报价单上的 contentHash 完全一致后再付款。');
}

main().catch((error) => {
    console.error('解析失败:', error.message || error);
    process.exit(1);
});
