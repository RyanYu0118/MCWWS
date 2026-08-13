/**
 * 根据 mcwws/economy/database/items.yml 批量生成 UltimateShop 商店商品。
 * 分类与页内顺序对齐 Java 26.2 原版创造物品栏（见 creative_tabs_26.2.json）。
 * 用法: node plugins/Skript/scripts/web/scripts/sync-ultimateshop-from-items.js
 */
const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

const ROOT = path.join(__dirname, '..', '..', '..', '..', '..');
const ITEMS_DB = path.join(ROOT, 'plugins', 'Skript', 'scripts', 'mcwws', 'economy', 'database', 'items.yml');
const SHOPS_DIR = path.join(ROOT, 'plugins', 'UltimateShop', 'shops');
const TABS_JSON = path.join(__dirname, '..', 'mcwws', 'creative_tabs_26.2.json');

const VANILLA_TAB_ORDER = [
    'building',
    'colored',
    'natural',
    'functional',
    'redstone',
    'tools',
    'combat',
    'food',
    'ingredients'
];

const SHOP_META = {
    building: { shopName: '{lang:building-shop-name}', menu: 'example-shop-menu' },
    colored: { shopName: '{lang:colored-shop-name}', menu: 'example-shop-menu' },
    natural: { shopName: '{lang:natural-shop-name}', menu: 'example-shop-menu' },
    functional: { shopName: '{lang:functional-shop-name}', menu: 'example-shop-menu' },
    redstone: { shopName: '{lang:redstone-shop-name}', menu: 'example-shop-menu' },
    tools: { shopName: '{lang:tools-shop-name}', menu: 'example-shop-menu' },
    combat: { shopName: '{lang:combat-shop-name}', menu: 'example-shop-menu' },
    food: { shopName: '{lang:food-shop-name}', menu: 'example-shop-menu' },
    ingredients: { shopName: '{lang:ingredients-shop-name}', menu: 'example-shop-menu' },
    brewing: { shopName: '{lang:brewing-shop-name}', menu: 'example-shop-menu' },
    enchantments: { shopName: '{lang:enchantments-shop-name}', menu: 'example-shop-menu' },
    mcwws: { shopName: '{lang:mcwws-shop-name}', menu: 'example-shop-menu' },
    daily: { shopName: '{lang:daily-shop-name}', menu: 'daily-shop-menu' },
    example: { shopName: 'Example Shop', menu: 'example-shop-menu' }
};

/** 26.2 改名后，物价表仍用旧 ID 时对回创造栏位置 */
const CREATIVE_ID_ALIASES = {
    dry_short_grass: 'short_dry_grass',
    dry_tall_grass: 'tall_dry_grass'
};

const LEFTOVER_TAB = {
    goat_horn: 'ingredients',
    ominous_bottle: 'ingredients',
    suspicious_stew: 'food',
    firework_rocket: 'tools',
    cut_sandstone_slab: 'building',
    creaking_heart: 'functional',
    short_dry_grass: 'natural',
    tall_dry_grass: 'natural'
};

const PRESERVE_SHOPS = new Set(['daily', 'example']);
const GENERATED_SHOPS = new Set([
    ...VANILLA_TAB_ORDER,
    'brewing',
    'enchantments',
    'mcwws'
]);

function normalizeId(id) {
    return String(id || '').trim().toLowerCase().replace(/-/g, '_');
}

function pricedShopId(creativeId, pricedSet) {
    const id = normalizeId(creativeId);
    if (pricedSet.has(id)) return id;
    const aliased = CREATIVE_ID_ALIASES[id];
    if (aliased && pricedSet.has(aliased)) return aliased;
    return null;
}

function leftoverShopForItem(itemId, category) {
    const id = normalizeId(itemId);
    const cat = String(category || '').toLowerCase();
    if (cat === 'enchantments') return 'enchantments';
    if (cat === 'brewing') return 'brewing';
    if (cat === 'mcwws') return 'mcwws';
    return LEFTOVER_TAB[id] || 'ingredients';
}

function buildProductEntry(itemId) {
    const mat = normalizeId(itemId).toUpperCase();
    return {
        'price-mode': 'CLASSIC_ALL',
        'product-mode': 'CLASSIC_ALL',
        products: {
            1: {
                material: mat,
                amount: 1
            }
        },
        'buy-prices': {
            1: {
                'economy-plugin': 'Vault',
                amount: `%mcwws.price_buy_${normalizeId(itemId)}%`,
                placeholder: '{amount}$',
                'start-apply': 0
            }
        },
        'sell-prices': {
            1: {
                'economy-plugin': 'Vault',
                amount: `%mcwws.price_sell_${normalizeId(itemId)}%`,
                placeholder: '{amount}$',
                'start-apply': 0
            }
        }
    };
}

/** example-shop-menu 每页商品槽位（与 layout 中 A-U 一一对应） */
const LAYOUT_PRODUCT_SLOTS = 'ABCDEFGHIJKLMNOPQRSTU'.split('');
const SLOTS_PER_PAGE = LAYOUT_PRODUCT_SLOTS.length;

function slotId(index) {
    const i = Number(index);
    if (i >= 1 && i <= SLOTS_PER_PAGE) {
        return LAYOUT_PRODUCT_SLOTS[i - 1];
    }
    return `p${i}`;
}

function chunkArray(list, size) {
    const chunks = [];
    for (let i = 0; i < list.length; i += size) {
        chunks.push(list.slice(i, size + i));
    }
    return chunks;
}

/** 54 格 GUI 底行：上一页=47(返回左2格)，返回=49，下一页=51(返回右2格) */
/** 使用 4/5 作为翻页 ID，避免与商品槽位 A-U 及菜单按钮冲突 */
const PREV_PAGE_BTN = '4';
const NEXT_PAGE_BTN = '5';

function buildPaginationLayoutRow({ hasPrev, hasNext }) {
    const row = ['a', '0', '0', '0', '3', '0', '0', '0', 'b'];
    if (hasPrev) row[2] = PREV_PAGE_BTN;
    if (hasNext) row[6] = NEXT_PAGE_BTN;
    return row.join('');
}

function buildShopDoc(shopId, itemIds, options = {}) {
    const meta = SHOP_META[shopId] || {
        shopName: shopId,
        menu: 'example-shop-menu'
    };
    const pageIndex = Number(options.pageIndex) || 0;
    const totalPages = Number(options.totalPages) || 1;
    const prevShopId = options.prevShopId || null;
    const nextShopId = options.nextShopId || null;
    const pageLabel = totalPages > 1 ? ` (${pageIndex + 1}/${totalPages})` : '';

    const items = {};
    itemIds.forEach((itemId, idx) => {
        items[slotId(idx + 1)] = buildProductEntry(itemId);
    });

    const doc = {
        settings: {
            menu: meta.menu,
            'buy-more': true,
            'shop-name': `${meta.shopName}${pageLabel}`,
            'hide-message': false,
            'secret-shop-items': shopId === 'mcwws',
            'allow-favourite': true
        },
        items
    };

    if (totalPages > 1) {
        const hasPrev = pageIndex > 0 && !!prevShopId;
        const hasNext = !!nextShopId;
        const menuButtons = {};
        if (hasPrev) {
            menuButtons[PREV_PAGE_BTN] = {
                'display-item': {
                    material: 'ARROW',
                    name: '{lang:previous-page-button}'
                },
                actions: {
                    1: {
                        type: 'shop_menu',
                        shop: prevShopId
                    }
                }
            };
        }
        if (hasNext) {
            menuButtons[NEXT_PAGE_BTN] = {
                'display-item': {
                    material: 'ARROW',
                    name: '{lang:next-page-button}'
                },
                actions: {
                    1: {
                        type: 'shop_menu',
                        shop: nextShopId
                    }
                }
            };
        }
        // layout 必须写在 menu-settings 内才会覆盖 menus/example-shop-menu.yml，
        // 否则底行仍用 a0003000b，47/51 格会被黑色玻璃板 (0) 占满。
        doc.settings['menu-settings'] = {
            layout: [
                '0f00s00x0',
                '000000000',
                '1ABCDEFG2',
                '1HIJKLMN2',
                '1OPQRSTU2',
                buildPaginationLayoutRow({ hasPrev, hasNext })
            ],
            buttons: menuButtons
        };
    }

    return doc;
}

function shopPageFileName(shopId, pageIndex) {
    return pageIndex === 0 ? `${shopId}.yml` : `${shopId}__p${pageIndex + 1}.yml`;
}

function shopPageId(shopId, pageIndex) {
    return pageIndex === 0 ? shopId : `${shopId}__p${pageIndex + 1}`;
}

function writeShopPages(shopId, itemIds) {
    const unique = [];
    const seen = new Set();
    itemIds.forEach((itemId) => {
        const id = normalizeId(itemId);
        if (!id || seen.has(id)) return;
        seen.add(id);
        unique.push(id);
    });
    if (unique.length === 0) return 0;

    const pages = chunkArray(unique, SLOTS_PER_PAGE);
    const stalePattern = new RegExp(`^${shopId}(__p\\d+)?\\.yml$`);
    fs.readdirSync(SHOPS_DIR)
        .filter((name) => stalePattern.test(name))
        .forEach((name) => fs.unlinkSync(path.join(SHOPS_DIR, name)));

    pages.forEach((pageItems, pageIndex) => {
        const prevShopId = pageIndex > 0 ? shopPageId(shopId, pageIndex - 1) : null;
        const nextShopId = pageIndex < pages.length - 1 ? shopPageId(shopId, pageIndex + 1) : null;
        const doc = buildShopDoc(shopId, pageItems, {
            pageIndex,
            totalPages: pages.length,
            prevShopId,
            nextShopId
        });
        const outPath = path.join(SHOPS_DIR, shopPageFileName(shopId, pageIndex));
        fs.writeFileSync(outPath, yaml.dump(doc, { lineWidth: 120, noRefs: true }), 'utf8');
    });

    const pageInfo = pages.length > 1 ? ` (${pages.length} 页)` : '';
    console.log(`${shopId}.yml → ${unique.length} 个商品${pageInfo}`);
    return unique.length;
}

function deleteRetiredShops() {
    const keepPrefixes = new Set([...PRESERVE_SHOPS, ...GENERATED_SHOPS]);
    fs.readdirSync(SHOPS_DIR)
        .filter((name) => name.endsWith('.yml'))
        .forEach((name) => {
            const base = name.replace(/__p\d+\.yml$/, '.yml').replace(/\.yml$/, '');
            if (keepPrefixes.has(base)) return;
            fs.unlinkSync(path.join(SHOPS_DIR, name));
            console.log(`删除旧商店 ${name}`);
        });
}

function main() {
    const itemsDb = yaml.load(fs.readFileSync(ITEMS_DB, 'utf8'));
    const tabsDoc = JSON.parse(fs.readFileSync(TABS_JSON, 'utf8'));
    const pricedSet = new Set();
    const categoryOf = {};
    Object.keys(itemsDb).forEach((itemId) => {
        const row = itemsDb[itemId];
        if (!row || row.unit_buy == null) return;
        const id = normalizeId(itemId);
        pricedSet.add(id);
        categoryOf[id] = row.category;
    });

    const assigned = new Set();
    const shopBuckets = {};
    const ensureBucket = (shopId) => {
        if (!shopBuckets[shopId]) shopBuckets[shopId] = [];
        return shopBuckets[shopId];
    };

    VANILLA_TAB_ORDER.forEach((tabId) => {
        const list = tabsDoc.tabs[tabId] || [];
        list.forEach((creativeId) => {
            const shopItemId = pricedShopId(creativeId, pricedSet);
            if (!shopItemId || assigned.has(shopItemId)) return;
            assigned.add(shopItemId);
            ensureBucket(tabId).push(shopItemId);
        });
    });

    [...pricedSet].sort((a, b) => a.localeCompare(b)).forEach((itemId) => {
        if (assigned.has(itemId)) return;
        const shopId = leftoverShopForItem(itemId, categoryOf[itemId]);
        assigned.add(itemId);
        ensureBucket(shopId).push(itemId);
    });

    deleteRetiredShops();

    let total = 0;
    Object.keys(shopBuckets).sort().forEach((shopId) => {
        if (PRESERVE_SHOPS.has(shopId)) return;
        total += writeShopPages(shopId, shopBuckets[shopId]);
    });

    console.log(`\n完成：共 ${total} 个物品已写入创造栏分类商店（已跳过 daily / example）。`);
}

main();
