/**
 * 把 Price Guide 26.3 JSON 导入经济基准价。
 * 只收录 Paper 26.2 已注册的物品；蓄风药水沿用商店里的 wind_charging ID。
 */
const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

const JSON_PATH = 'd:/下载/prices_26_3_2026-09-27T15-20-43.json';
const ROOT = path.join(__dirname, '..', '..', '..', '..', '..');
const ITEMS_DB = path.join(ROOT, 'plugins/Skript/scripts/mcwws/economy/database/items.yml');
const ARCHIVE = path.join(ROOT, 'plugins/Skript/scripts/mcwws/economy/database/prices_26_3_2026-09-27T15-20-43.yml');
const MARKET_STATE = path.join(ROOT, 'plugins/Skript/scripts/mcwws/economy/market_state.yml');
const WEB_PRICES = path.join(ROOT, 'plugins/Skript/scripts/web/mcwws/economy/web_prices.yml');
const VANILLA_PRICES = path.join(ROOT, 'plugins/Skript/scripts/web/mcwws/economy/vanilla_prices.yml');

const ID_ALIAS = {
    potion_of_wind_charged_1: 'potion_of_wind_charging_1',
    lingering_potion_of_wind_charged_1: 'lingering_potion_of_wind_charging_1',
    splash_potion_of_wind_charged_1: 'splash_potion_of_wind_charging_1',
    arrow_of_wind_charged_1: 'arrow_of_wind_charging_1'
};

/** 在 versions/26.2/paper-26.2.jar 里按完整 ID 命中、且旧价表没有的物品 */
const NEW_IN_26_2 = new Set([
    'chiseled_cinnabar', 'chiseled_sulfur', 'cinnabar', 'cinnabar_brick_slab', 'cinnabar_brick_stairs',
    'cinnabar_brick_wall', 'cinnabar_bricks', 'cinnabar_slab', 'cinnabar_stairs', 'cinnabar_wall',
    'golden_dandelion', 'music_disc_bounce', 'polished_cinnabar', 'polished_cinnabar_slab',
    'polished_cinnabar_stairs', 'polished_cinnabar_wall', 'polished_sulfur', 'polished_sulfur_slab',
    'polished_sulfur_stairs', 'polished_sulfur_wall', 'potent_sulfur', 'sulfur', 'sulfur_brick_slab',
    'sulfur_brick_stairs', 'sulfur_brick_wall', 'sulfur_bricks', 'sulfur_cube_bucket', 'sulfur_slab',
    'sulfur_spike', 'sulfur_stairs', 'sulfur_wall'
]);

function fmtItem(n) {
    const x = Math.round(Number(n) * 100000) / 100000;
    if (!Number.isFinite(x)) return '0';
    if (Object.is(x, -0)) return '0';
    return String(x);
}

function fmtWeb(n) {
    const x = Math.round(Number(n) * 100) / 100;
    if (!Number.isFinite(x)) return '0.0';
    if (Number.isInteger(x)) return x.toFixed(1);
    return String(x);
}

function quoteName(name) {
    const text = String(name);
    if (/[:#&*!|>%@`]/.test(text) || text.includes('"') || text.includes("'")) {
        return JSON.stringify(text);
    }
    return text;
}

function dumpItems(doc) {
    const lines = [];
    Object.keys(doc).forEach((id) => {
        const row = doc[id];
        lines.push(`${id}:`);
        lines.push(`  name: ${quoteName(row.name)}`);
        lines.push(`  category: ${row.category}`);
        lines.push(`  stack: ${fmtItem(row.stack)}`);
        lines.push(`  unit_buy: ${fmtItem(row.unit_buy)}`);
        lines.push(`  unit_sell: ${fmtItem(row.unit_sell)}`);
        lines.push(`  stack_buy: ${fmtItem(row.stack_buy)}`);
        lines.push(`  stack_sell: ${fmtItem(row.stack_sell)}`);
        lines.push('');
    });
    return lines.join('\n');
}

function dumpPrices(doc) {
    const lines = [];
    Object.keys(doc).forEach((id) => {
        lines.push(`${id}:`);
        lines.push(`    buy: ${fmtWeb(doc[id].buy)}`);
        lines.push(`    sell: ${fmtWeb(doc[id].sell)}`);
        lines.push('');
    });
    return lines.join('\n');
}

function categoryMultiplier(category) {
    if (category === 'archaeology') return 1.1;
    if (category === 'wood') return 0.9;
    return 1;
}

function dynamicFactor(itemId, unitBuy, category, state) {
    if (category === 'mcwws') return 1;
    const row = state && state[itemId];
    if (!row) return 1;
    const buy = Number(unitBuy);
    const eq = buy > 0 ? 10000 / buy : 512;
    const stock = Number(row.stock);
    const buyP = Number(row.buy_pressure) || 0;
    const sellP = Number(row.sell_pressure) || 0;
    let factor = 1;
    if (eq > 0 && Number.isFinite(stock)) {
        factor += 0.35 * (eq - stock) / eq;
    }
    factor += 0.18 * buyP / 256;
    factor -= 0.18 * sellP / 256;
    return Math.min(2.5, Math.max(0.5, factor));
}

function pricePair(row, itemId, state) {
    const cat = categoryMultiplier(row.category);
    const dyn = dynamicFactor(itemId, row.unit_buy, row.category, state);
    const mult = cat * dyn;
    let buy = Number(row.unit_buy) * mult;
    let sell = Number(row.unit_sell) * mult;
    if (sell > buy) sell = buy * 0.99;
    return {
        buy: Math.round(buy * 100) / 100,
        sell: Math.round(sell * 100) / 100
    };
}

function main() {
    const guide = JSON.parse(fs.readFileSync(JSON_PATH, 'utf8'));
    const previous = yaml.load(fs.readFileSync(ITEMS_DB, 'utf8'));
    const market = yaml.load(fs.readFileSync(MARKET_STATE, 'utf8')) || {};
    const state = market.items || {};

    const imported = {};
    let priceChanged = 0;
    let added = 0;
    let skipped = 0;
    Object.keys(guide).forEach((rawId) => {
        if (rawId.startsWith('_')) return;
        const id = ID_ALIAS[rawId] || rawId;
        const row = guide[rawId];
        const keep = Object.prototype.hasOwnProperty.call(previous, id) || NEW_IN_26_2.has(id);
        if (!keep) {
            skipped += 1;
            return;
        }
        if (!imported[id]) {
            if (previous[id]) {
                if (Number(previous[id].unit_buy) !== Number(row.unit_buy)
                    || Number(previous[id].unit_sell) !== Number(row.unit_sell)) {
                    priceChanged += 1;
                }
            } else {
                added += 1;
            }
        }
        imported[id] = {
            name: row.name,
            category: row.category,
            stack: Number(row.stack),
            unit_buy: Number(row.unit_buy),
            unit_sell: Number(row.unit_sell),
            stack_buy: Number(row.stack_buy),
            stack_sell: Number(row.stack_sell)
        };
    });

    const items = { ...imported };
    if (previous.light && !items.light) {
        items.light = previous.light;
    }

    fs.writeFileSync(ARCHIVE, dumpItems(imported), 'utf8');
    fs.writeFileSync(ITEMS_DB, dumpItems(items), 'utf8');

    const vanilla = {};
    const web = {};
    Object.keys(items).forEach((id) => {
        const row = items[id];
        const cat = categoryMultiplier(row.category);
        let buy = Number(row.unit_buy) * cat;
        let sell = Number(row.unit_sell) * cat;
        if (sell > buy) sell = buy * 0.99;
        vanilla[id] = {
            buy: Math.round(buy * 100) / 100,
            sell: Math.round(sell * 100) / 100
        };
        web[id] = pricePair(row, id, state);
    });
    fs.writeFileSync(VANILLA_PRICES, dumpPrices(vanilla), 'utf8');
    fs.writeFileSync(WEB_PRICES, dumpPrices(web), 'utf8');

    console.log(JSON.stringify({
        guideItems: guide._export_metadata && guide._export_metadata.item_count,
        imported: Object.keys(imported).length,
        withCustom: Object.keys(items).length,
        priceChanged,
        added,
        skipped,
        sample: {
            magma_block: items.magma_block && items.magma_block.unit_buy,
            name_tag: items.name_tag && items.name_tag.unit_buy,
            honey_block: items.honey_block && items.honey_block.unit_buy,
            cinnabar: items.cinnabar && items.cinnabar.unit_buy,
            web_oak_planks: web.oak_planks,
            web_cinnabar: web.cinnabar
        }
    }, null, 2));
}

main();
