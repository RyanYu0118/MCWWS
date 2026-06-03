/**
 * 修复 pending_orders.yml 缩进并规范化 lines 结构（可单独运行）
 * 用法: node scripts/repair-pending-orders.js
 */
const fs = require('fs');
const path = require('path');

const WEB_DIR = path.join(__dirname, '..', 'plugins', 'Skript', 'scripts', 'web');
const yaml = require(path.join(WEB_DIR, 'node_modules', 'js-yaml'));

const PENDING_ORDERS_PATH = path.join(WEB_DIR, 'data', 'pending_orders.yml');

const ORDER_LINE_FIELD_NAMES = [
    'itemId', 'shopId', 'slot', 'quantity', 'unitBuyPrice',
    'lineTotal', 'productAmount', 'material', 'shopTitle', 'status'
];

function repairPendingOrdersYamlText(text) {
    return text.split('\n').map((line) => {
        for (const field of ORDER_LINE_FIELD_NAMES) {
            if (line.startsWith(`            ${field}:`)) {
                return `                ${line.slice(12)}`;
            }
        }
        return line;
    }).join('\n');
}

function linesToSkriptMap(lines) {
    if (!Array.isArray(lines)) return lines;
    const out = {};
    lines.forEach((line, i) => {
        if (line && typeof line === 'object') {
            out[String(i + 1)] = line;
        }
    });
    return out;
}

function linesToArray(lines) {
    if (!lines) return [];
    if (Array.isArray(lines)) return lines;
    return Object.keys(lines)
        .sort((a, b) => Number(a) - Number(b))
        .map((k) => lines[k])
        .filter((line) => line && typeof line === 'object');
}

function normalizeOrderLines(order) {
    if (!order?.lines || typeof order.lines !== 'object') return;
    const lines = order.lines;
    if (Array.isArray(lines)) {
        order.lines = linesToSkriptMap(lines);
    }
    const numericKeys = Object.keys(order.lines).filter((k) => /^\d+$/.test(k));
    const flatOnLines = ORDER_LINE_FIELD_NAMES.filter((f) => order.lines[f] != null);
    if (flatOnLines.length && numericKeys.length) {
        const firstKey = numericKeys.sort((a, b) => Number(a) - Number(b))[0];
        const merged = { ...(order.lines[firstKey] || {}) };
        flatOnLines.forEach((f) => {
            merged[f] = order.lines[f];
            delete order.lines[f];
        });
        order.lines[firstKey] = merged;
    }
    order.lineCount = linesToArray(order.lines).length;
}

function main() {
    if (!fs.existsSync(PENDING_ORDERS_PATH)) {
        console.error('文件不存在:', PENDING_ORDERS_PATH);
        process.exit(1);
    }
    const raw = fs.readFileSync(PENDING_ORDERS_PATH, 'utf8');
    const repairedText = repairPendingOrdersYamlText(raw);
    let data;
    try {
        data = yaml.load(repairedText);
    } catch (e) {
        console.error('解析失败:', e.message);
        process.exit(1);
    }
    Object.values(data.orders || {}).forEach(normalizeOrderLines);
    const out = yaml.dump(data, { lineWidth: 120, noRefs: true });
    fs.writeFileSync(PENDING_ORDERS_PATH, out, 'utf8');
    console.log('已修复并规范化:', PENDING_ORDERS_PATH);
}

main();
