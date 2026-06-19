/**
 * 根据 mcwws/economy/database/items.yml 批量生成 UltimateShop 商店商品。
 * 用法: node plugins/Skript/scripts/web/scripts/sync-ultimateshop-from-items.js
 */
const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

const ROOT = path.join(__dirname, '..', '..', '..', '..', '..');
const ITEMS_DB = path.join(ROOT, 'plugins', 'Skript', 'scripts', 'mcwws', 'economy', 'database', 'items.yml');
const SHOPS_DIR = path.join(ROOT, 'plugins', 'UltimateShop', 'shops');

const SHOP_META = {
    logs: { shopName: '{lang:logs-shop-name}', menu: 'example-shop-menu' },
    farming: { shopName: '{lang:farming-shop-name}', menu: 'example-shop-menu' },
    flowers: { shopName: '{lang:flowers-shop-name}', menu: 'example-shop-menu' },
    drops: { shopName: '{lang:drops-shop-name}', menu: 'example-shop-menu' },
    minerals: { shopName: '{lang:minerals-shop-name}', menu: 'example-shop-menu' },
    blocks: { shopName: '{lang:blocks-shop-name}', menu: 'example-shop-menu' },
    blocks2: { shopName: '{lang:blocks2-shop-name}', menu: 'example-shop-menu' },
    redstone: { shopName: '{lang:redstone-shop-name}', menu: 'example-shop-menu' },
    transport: { shopName: '{lang:transport-shop-name}', menu: 'example-shop-menu' },
    fish: { shopName: '{lang:fish-shop-name}', menu: 'example-shop-menu' },
    wools: { shopName: '{lang:wools-shop-name}', menu: 'example-shop-menu' },
    concretes: { shopName: '{lang:concretes-shop-name}', menu: 'example-shop-menu' },
    terracottas: { shopName: '{lang:terracottas-shop-name}', menu: 'example-shop-menu' },
    glass: { shopName: '{lang:glass-shop-name}', menu: 'example-shop-menu' },
    special: { shopName: '{lang:special-items-shop-name}', menu: 'example-shop-menu' },
    archaeology: { shopName: '{lang:archaeology-shop-name}', menu: 'example-shop-menu' },
    armor: { shopName: '{lang:armor-shop-name}', menu: 'example-shop-menu' },
    brewing: { shopName: '{lang:brewing-shop-name}', menu: 'example-shop-menu' },
    tools: { shopName: '{lang:tools-shop-name}', menu: 'example-shop-menu' },
    weapons: { shopName: '{lang:weapons-shop-name}', menu: 'example-shop-menu' },
    utility: { shopName: '{lang:utility-shop-name}', menu: 'example-shop-menu' },
    enchantments: { shopName: '{lang:enchantments-shop-name}', menu: 'example-shop-menu' },
    end: { shopName: '{lang:end-shop-name}', menu: 'example-shop-menu' },
    nether: { shopName: '{lang:nether-shop-name}', menu: 'example-shop-menu' },
    discs: { shopName: '{lang:discs-shop-name}', menu: 'example-shop-menu' },
    daily: { shopName: '{lang:daily-shop-name}', menu: 'daily-shop-menu' },
    example: { shopName: 'Example Shop', menu: 'example-shop-menu' }
};

function normalizeId(id) {
    return String(id || '').trim().toLowerCase().replace(/-/g, '_');
}

function resolveShopForItem(itemId, category) {
    const id = normalizeId(itemId);
    const cat = String(category || 'utility').toLowerCase();

    if (cat === 'dyed') {
        if (id.includes('wool') || id === 'carpet' || id.endsWith('_carpet')) return 'wools';
        if (id.includes('concrete') && !id.includes('terracotta')) return 'concretes';
        if (id.includes('terracotta')) return 'terracottas';
        if (id.includes('glass') || id.includes('stained_glass')) return 'glass';
        return 'wools';
    }

    const map = {
        wood: 'logs',
        plants: 'flowers',
        food: 'farming',
        drops: 'drops',
        ores: 'minerals',
        copper: 'minerals',
        redstone: 'redstone',
        light: 'redstone',
        transport: 'transport',
        ocean: 'fish',
        archaeology: 'archaeology',
        armor: 'armor',
        brewing: 'brewing',
        tools: 'tools',
        weapons: 'weapons',
        utility: 'utility',
        enchantments: 'enchantments',
        end: 'end',
        nether: 'nether',
        discs: 'discs',
        brick: 'blocks',
        earth: 'blocks',
        sand: 'blocks',
        stone: 'blocks2',
        ice: 'blocks2',
        'deep dark': 'blocks2'
    };

    return map[cat] || 'utility';
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
        chunks.push(list.slice(i, i + size));
    }
    return chunks;
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
            'secret-shop-items': false,
            'allow-favourite': true
        },
        items
    };

    if (totalPages > 1) {
        doc.settings.layout = [
            '0f00s00x0',
            '000000000',
            '1ABCDEFG2',
            '1HIJKLMN2',
            '1OPQRSTU2',
            pageIndex > 0 && nextShopId ? 'aN003Pb' : (nextShopId ? 'a0003Pb' : (pageIndex > 0 ? 'aN0030b' : 'a0003000b'))
        ];
        doc.buttons = {};
        if (pageIndex > 0 && prevShopId) {
            doc.buttons.N = {
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
        if (nextShopId) {
            doc.buttons.P = {
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
    }

    return doc;
}

function shopPageFileName(shopId, pageIndex) {
    return pageIndex === 0 ? `${shopId}.yml` : `${shopId}__p${pageIndex + 1}.yml`;
}

function shopPageId(shopId, pageIndex) {
    return pageIndex === 0 ? shopId : `${shopId}__p${pageIndex + 1}`;
}

function main() {
    const itemsDb = yaml.load(fs.readFileSync(ITEMS_DB, 'utf8'));
    const shopBuckets = {};

    Object.keys(itemsDb).forEach((itemId) => {
        const row = itemsDb[itemId];
        if (!row || row.unit_buy == null) return;
        const shopId = resolveShopForItem(itemId, row.category);
        if (!shopBuckets[shopId]) shopBuckets[shopId] = [];
        shopBuckets[shopId].push(normalizeId(itemId));
    });

    const preserve = new Set(['daily', 'example']);
    let total = 0;
    Object.keys(shopBuckets).sort().forEach((shopId) => {
        if (preserve.has(shopId)) return;
        const unique = [...new Set(shopBuckets[shopId])].sort((a, b) => a.localeCompare(b));
        const pages = chunkArray(unique, SLOTS_PER_PAGE);
        const stalePattern = new RegExp(`^${shopId}__p\\d+\\.yml$`);
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
        total += unique.length;
    });

    console.log(`\n完成：共 ${total} 个物品已写入 ${Object.keys(shopBuckets).filter((k) => !preserve.has(k)).length} 个商店（已跳过 daily / example）。`);
}

main();
