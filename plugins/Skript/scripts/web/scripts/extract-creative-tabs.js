/**
 * 从 Paper 26.2 映射 jar 反汇编的 CreativeModeTabs 中抽出创造栏物品顺序。
 * 用法: node plugins/Skript/scripts/web/scripts/extract-creative-tabs.js
 *
 * 需要先生成 javap 文本（脚本会在缺失时自动调用 javap）。
 */
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const ROOT = path.join(__dirname, '..', '..', '..', '..', '..');
const JAVAP_TXT = path.join(ROOT, 'tools', 'mcwws-ultimateshop-fix', 'creative-tabs-javap.txt');
const OUT_JSON = path.join(__dirname, '..', 'mcwws', 'creative_tabs_26.2.json');
const PAPER_JAR = path.join(ROOT, 'versions', '26.2', 'paper-26.2.jar');

const DYE_GRADIENT = [
    'white', 'light_gray', 'gray', 'black', 'brown', 'red', 'orange', 'yellow',
    'lime', 'green', 'cyan', 'light_blue', 'blue', 'purple', 'magenta', 'pink'
];

const DYE_WOOL_ORDER = [
    'white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
    'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black'
];

const COLOR_SUFFIX = {
    WOOL: 'wool',
    CARPET: 'carpet',
    DYED_TERRACOTTA: 'terracotta',
    CONCRETE: 'concrete',
    CONCRETE_POWDER: 'concrete_powder',
    GLAZED_TERRACOTTA: 'glazed_terracotta',
    STAINED_GLASS: 'stained_glass',
    STAINED_GLASS_PANE: 'stained_glass_pane',
    DYED_SHULKER_BOX: 'shulker_box',
    BED: 'bed',
    DYED_CANDLE: 'candle',
    BANNER: 'banner',
    HARNESS: 'harness',
    DYED_BUNDLE: 'bundle',
    DYE: 'dye'
};

const COPPER_FAMILIES = [
    'COPPER_BLOCK',
    'CHISELED_COPPER',
    'COPPER_GRATE',
    'CUT_COPPER',
    'CUT_COPPER_STAIRS',
    'CUT_COPPER_SLAB',
    'COPPER_BARS',
    'COPPER_DOOR',
    'COPPER_TRAPDOOR',
    'COPPER_BULB',
    'COPPER_CHAIN'
];

const TAB_LAMBDAS = {
    'lambda$bootstrap$1': 'building',
    'lambda$bootstrap$5': 'colored',
    'lambda$bootstrap$7': 'natural',
    'lambda$bootstrap$9': 'functional',
    'lambda$bootstrap$13': 'redstone',
    'lambda$bootstrap$18': 'tools',
    'lambda$bootstrap$21': 'combat',
    'lambda$bootstrap$24': 'food',
    'lambda$bootstrap$27': 'ingredients',
    'lambda$bootstrap$31': 'spawn_eggs',
    'lambda$bootstrap$33': 'op_blocks'
};

function ensureJavapDump() {
    if (fs.existsSync(JAVAP_TXT) && fs.statSync(JAVAP_TXT).size > 10000) {
        return;
    }
    if (!fs.existsSync(PAPER_JAR)) {
        throw new Error(`找不到 Paper 映射 jar: ${PAPER_JAR}`);
    }
    const javap = process.env.JAVA_HOME
        ? path.join(process.env.JAVA_HOME, 'bin', 'javap.exe')
        : 'javap';
    fs.mkdirSync(path.dirname(JAVAP_TXT), { recursive: true });
    const result = spawnSync(
        javap,
        ['-c', '-p', '-classpath', PAPER_JAR, 'net.minecraft.world.item.CreativeModeTabs'],
        { encoding: 'utf8', maxBuffer: 20 * 1024 * 1024 }
    );
    if (result.status !== 0) {
        throw new Error(`javap 失败: ${result.stderr || result.stdout}`);
    }
    fs.writeFileSync(JAVAP_TXT, result.stdout, 'utf8');
}

function copperIds(field) {
    const key = String(field || '').toLowerCase();
    const una = key === 'copper_block' ? 'copper_block' : key;
    const stem = key === 'copper_block' ? 'copper' : key;
    return {
        weathering: [una, `exposed_${stem}`, `weathered_${stem}`, `oxidized_${stem}`],
        waxed: [`waxed_${una}`, `waxed_exposed_${stem}`, `waxed_weathered_${stem}`, `waxed_oxidized_${stem}`]
    };
}

function expandCopper(field, mode) {
    const ids = copperIds(field);
    if (mode === 'weathering') return ids.weathering;
    if (mode === 'waxed') return ids.waxed;
    return ids.weathering.concat(ids.waxed);
}

function expandColor(field, colors) {
    const suffix = COLOR_SUFFIX[field];
    if (!suffix) {
        throw new Error(`未知 ColorCollection: ${field}`);
    }
    return colors.map((color) => `${color}_${suffix}`);
}

function parseMethods(text) {
    const methods = {};
    let current = null;
    let buf = [];
    const startRe = /^  (?:private|public|static|protected).+ (\S+)\(/;
    for (const line of text.split(/\r?\n/)) {
        const start = line.match(/^  (private|public) static .+ (lambda\$bootstrap\$\d+)\(/);
        if (start) {
            if (current) methods[current] = buf;
            current = start[2];
            buf = [];
            continue;
        }
        if (current && /^  (private|public|static) /.test(line) && !line.includes('Code:')) {
            methods[current] = buf;
            current = null;
            buf = [];
        }
        if (current) buf.push(line);
    }
    if (current) methods[current] = buf;
    return methods;
}

function extractField(line) {
    const m = line.match(/Field net\/minecraft\/world\/item\/Items\.([A-Z0-9_]+):L([^;]+);/);
    if (!m) return null;
    return { name: m[1], type: m[2] };
}

function emitFromMethod(lines) {
    const out = [];
    let pendingItem = null;
    let pendingColor = null;
    let pendingCopper = null;
    let copperMode = null;
    let copperFamilyCalls = 0;

    const flushItem = () => {
        if (pendingItem) {
            out.push(pendingItem.toLowerCase());
            pendingItem = null;
        }
    };

    for (const line of lines) {
        const field = extractField(line);
        if (field) {
            if (field.type === 'net/minecraft/world/item/Item') {
                pendingItem = field.name;
                pendingColor = null;
                pendingCopper = null;
                copperMode = null;
            } else if (field.type === 'net/minecraft/world/level/block/ColorCollection') {
                flushItem();
                pendingColor = field.name;
                pendingCopper = null;
                copperMode = null;
            } else if (field.type === 'net/minecraft/world/level/block/WeatheringCopperCollection') {
                flushItem();
                pendingCopper = field.name;
                pendingColor = null;
                copperMode = null;
            }
            continue;
        }

        if (line.includes('Method copperBlockFamilies:')) {
            copperFamilyCalls += 1;
            const mode = copperFamilyCalls === 1 ? 'weathering' : 'waxed';
            COPPER_FAMILIES.forEach((family) => {
                expandCopper(family, mode).forEach((id) => out.push(id));
            });
            pendingCopper = null;
            copperMode = null;
            continue;
        }

        if (line.includes('Method registerColoredItems:')) {
            if (pendingColor) {
                expandColor(pendingColor, DYE_GRADIENT).forEach((id) => out.push(id));
            }
            pendingColor = null;
            continue;
        }

        if (line.includes('Method net/minecraft/world/level/block/ColorCollection.forEach:')) {
            if (pendingColor) {
                expandColor(pendingColor, DYE_WOOL_ORDER).forEach((id) => out.push(id));
            }
            pendingColor = null;
            continue;
        }

        if (line.includes('Method net/minecraft/world/level/block/WeatheringCopperCollection.weathering:')) {
            copperMode = 'weathering';
            continue;
        }
        if (line.includes('Method net/minecraft/world/level/block/WeatheringCopperCollection.waxed:')) {
            copperMode = 'waxed';
            continue;
        }
        if (line.includes('Method net/minecraft/world/level/block/WeatheringCopperCollection.forEach:')) {
            if (pendingCopper) {
                expandCopper(pendingCopper, 'all').forEach((id) => out.push(id));
            }
            pendingCopper = null;
            copperMode = null;
            continue;
        }
        if (line.includes('Method net/minecraft/world/level/block/WeatheringCopperCollection$ByState.forEach:')) {
            if (pendingCopper) {
                expandCopper(pendingCopper, copperMode || 'all').forEach((id) => out.push(id));
            }
            pendingCopper = null;
            copperMode = null;
            continue;
        }
        if (line.includes('Method net/minecraft/world/level/block/WeatheringCopperCollection$ByState.unaffected:')) {
            if (pendingCopper) {
                const ids = expandCopper(pendingCopper, copperMode || 'weathering');
                out.push(ids[0]);
            }
            pendingCopper = null;
            copperMode = null;
            continue;
        }

        if (
            line.includes('InterfaceMethod net/minecraft/world/item/CreativeModeTab$Output.accept:(Lnet/minecraft/world/level/ItemLike;)V')
        ) {
            flushItem();
            continue;
        }
    }
    return out;
}

function uniqueKeepOrder(ids) {
    const seen = new Set();
    const out = [];
    ids.forEach((id) => {
        if (!id || seen.has(id)) return;
        seen.add(id);
        out.push(id);
    });
    return out;
}

function main() {
    ensureJavapDump();
    const text = fs.readFileSync(JAVAP_TXT, 'utf8');
    const methods = parseMethods(text);
    const tabs = {};
    Object.keys(TAB_LAMBDAS).forEach((lambda) => {
        const shopId = TAB_LAMBDAS[lambda];
        const lines = methods[lambda];
        if (!lines) {
            throw new Error(`找不到方法 ${lambda}`);
        }
        tabs[shopId] = uniqueKeepOrder(emitFromMethod(lines));
    });

    const doc = {
        version: '26.2',
        source: 'net.minecraft.world.item.CreativeModeTabs (Paper mapped jar)',
        dyeGradient: DYE_GRADIENT,
        tabs
    };
    fs.mkdirSync(path.dirname(OUT_JSON), { recursive: true });
    fs.writeFileSync(OUT_JSON, `${JSON.stringify(doc, null, 2)}\n`, 'utf8');
    Object.keys(tabs).forEach((id) => {
        const sample = tabs[id].slice(0, 8).join(', ');
        console.log(`${id}: ${tabs[id].length}  items  e.g. ${sample}`);
    });
    console.log(`\n写入 ${path.relative(ROOT, OUT_JSON)}`);
}

main();
