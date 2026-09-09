/**
 * Litematica schematic library: index + keyword search for build references.
 */
import fs from "fs";
import path from "path";
import zlib from "zlib";
import { fileURLToPath } from "url";
import nbt from "prismarine-nbt";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PKG_ROOT = path.resolve(__dirname, "..");

export const DEFAULT_SCHEM_ROOT =
  process.env.MCWWS_SCHEM_ROOT ||
  "D:/Minecraft/游戏主体/.minecraft/schematics";

export const DEFAULT_INDEX_PATH =
  process.env.MCWWS_SCHEM_INDEX ||
  path.join(PKG_ROOT, "data", "schem-index.json");

/** Query synonyms → expand recall (zh/en) */
const SYNONYMS = {
  kfc: ["kfc", "肯德基", "餐厅", "快餐", "restaurant", "qsr", "food"],
  肯德基: ["kfc", "肯德基", "餐厅", "快餐", "restaurant"],
  mcdonald: ["mcdonald", "mcdonalds", "麦当劳", "快餐", "restaurant"],
  麦当劳: ["mcdonald", "麦当劳", "快餐", "餐厅"],
  modern: ["modern", "现代", "当代", "玻璃", "curtain", "幕墙"],
  现代: ["modern", "现代", "玻璃", "幕墙", "当代"],
  shop: ["shop", "store", "商店", "商铺", "retail"],
  商店: ["shop", "store", "商店", "商铺"],
  house: ["house", "住宅", "房子", "别墅", "home"],
  住宅: ["house", "住宅", "房子", "别墅"],
  castle: ["castle", "城堡", "中世纪", "medieval"],
  城堡: ["castle", "城堡", "中世纪"],
  japanese: ["japanese", "日式", "和风", "japan"],
  日式: ["japanese", "日式", "和风"],
  chinese: ["chinese", "中式", "古风", "中华"],
  中式: ["chinese", "中式", "古风"],
  church: ["church", "教堂", "cathedral"],
  教堂: ["church", "教堂"],
  tree: ["tree", "树", "pine", "oak", "植物"],
  interior: ["interior", "室内", "家具", "furniture"],
  室内: ["interior", "室内", "家具"],
};

function tokenize(text) {
  return String(text || "")
    .toLowerCase()
    .split(/[^a-z0-9\u4e00-\u9fff]+/i)
    .filter((t) => t.length >= 1);
}

function expandQueryTokens(query) {
  const base = tokenize(query);
  const out = new Set(base);
  for (const t of base) {
    const syn = SYNONYMS[t];
    if (syn) syn.forEach((s) => out.add(s.toLowerCase()));
  }
  return [...out];
}

function pathTags(relPath) {
  return tokenize(
    relPath
      .replace(/\\/g, "/")
      .split("/")
      .slice(0, -1)
      .join(" ")
  );
}

export function ensureDataDir(indexPath = DEFAULT_INDEX_PATH) {
  fs.mkdirSync(path.dirname(indexPath), { recursive: true });
}

export function loadIndex(indexPath = DEFAULT_INDEX_PATH) {
  if (!fs.existsSync(indexPath)) {
    return {
      version: 1,
      root: DEFAULT_SCHEM_ROOT,
      builtAt: null,
      count: 0,
      entries: [],
    };
  }
  return JSON.parse(fs.readFileSync(indexPath, "utf8"));
}

export function saveIndex(index, indexPath = DEFAULT_INDEX_PATH) {
  ensureDataDir(indexPath);
  fs.writeFileSync(indexPath, JSON.stringify(index));
}

function walkLitematics(root) {
  const out = [];
  function walk(dir) {
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch {
      return;
    }
    for (const ent of entries) {
      const full = path.join(dir, ent.name);
      if (ent.isDirectory()) walk(full);
      else if (ent.isFile() && ent.name.toLowerCase().endsWith(".litematic")) {
        out.push(full);
      }
    }
  }
  walk(root);
  return out;
}

export async function readLitematicMeta(filePath) {
  const buf = fs.readFileSync(filePath);
  let raw;
  try {
    raw = zlib.gunzipSync(buf);
  } catch {
    // some exports may be uncompressed
    raw = buf;
  }
  const { parsed } = await nbt.parse(raw);
  const simple = nbt.simplify(parsed);
  const meta = simple.Metadata || {};
  const size = meta.EnclosingSize || {};
  return {
    name: meta.Name || "",
    author: meta.Author || "",
    description: meta.Description || "",
    regionCount: meta.RegionCount ?? null,
    totalBlocks: meta.TotalBlocks ?? null,
    totalVolume: meta.TotalVolume ?? null,
    sizeX: size.x ?? null,
    sizeY: size.y ?? null,
    sizeZ: size.z ?? null,
  };
}

function entryKey(rel, size, mtimeMs) {
  return `${rel}|${size}|${mtimeMs}`;
}

/**
 * Build or refresh index.
 * @param {{ root?: string, indexPath?: string, parseMeta?: boolean, concurrency?: number, maxFiles?: number, onProgress?: Function }} opts
 */
export async function buildIndex(opts = {}) {
  const root = opts.root || DEFAULT_SCHEM_ROOT;
  const indexPath = opts.indexPath || DEFAULT_INDEX_PATH;
  const parseMeta = opts.parseMeta !== false;
  const concurrency = Math.max(1, opts.concurrency || 8);
  const maxFiles = opts.maxFiles || Infinity;
  const onProgress = opts.onProgress || (() => {});

  if (!fs.existsSync(root)) {
    throw new Error(`Schematic root not found: ${root}`);
  }

  const prev = loadIndex(indexPath);
  const prevByRel = new Map((prev.entries || []).map((e) => [e.rel, e]));

  const files = walkLitematics(root).slice(0, maxFiles);
  const entries = [];
  let i = 0;
  let parsed = 0;
  let reused = 0;
  let failed = 0;

  async function handle(file) {
    const st = fs.statSync(file);
    const rel = path.relative(root, file).replace(/\\/g, "/");
    const old = prevByRel.get(rel);
    if (
      old &&
      old.sizeBytes === st.size &&
      old.mtimeMs === st.mtimeMs &&
      (!parseMeta || old.metaParsed)
    ) {
      reused++;
      entries.push(old);
      return;
    }

    const base = {
      rel,
      fileName: path.basename(file),
      sizeBytes: st.size,
      mtimeMs: st.mtimeMs,
      tags: pathTags(rel),
      metaParsed: false,
      name: "",
      author: "",
      description: "",
      regionCount: null,
      totalBlocks: null,
      totalVolume: null,
      sizeX: null,
      sizeY: null,
      sizeZ: null,
    };

    if (parseMeta) {
      try {
        const meta = await readLitematicMeta(file);
        Object.assign(base, meta, { metaParsed: true });
        parsed++;
      } catch {
        failed++;
      }
    }

    // searchable blob
    base.searchText = [
      base.fileName,
      base.rel,
      base.name,
      base.author,
      base.description,
      ...(base.tags || []),
    ]
      .join(" ")
      .toLowerCase();

    entries.push(base);
  }

  // simple pool
  const queue = [...files];
  async function worker() {
    while (queue.length) {
      const file = queue.shift();
      await handle(file);
      i++;
      if (i % 200 === 0 || i === files.length) {
        onProgress({ done: i, total: files.length, parsed, reused, failed });
      }
    }
  }
  await Promise.all(Array.from({ length: concurrency }, () => worker()));

  entries.sort((a, b) => a.rel.localeCompare(b.rel, "zh"));

  const index = {
    version: 1,
    root,
    builtAt: new Date().toISOString(),
    count: entries.length,
    parsed,
    reused,
    failed,
    entries,
  };
  saveIndex(index, indexPath);
  return index;
}

/**
 * Search indexed schematics by free-text description.
 */
export function searchSchems(query, opts = {}) {
  const index = opts.index || loadIndex(opts.indexPath);
  const limit = Math.min(50, Math.max(1, opts.limit || 12));
  const tokens = expandQueryTokens(query);
  if (!tokens.length) {
    return { ok: true, query, tokens: [], totalIndexed: index.count || 0, hits: [] };
  }

  const scored = [];
  for (const e of index.entries || []) {
    const hay = e.searchText || "";
    let score = 0;
    const matched = [];
    for (const t of tokens) {
      if (!t) continue;
      if (hay.includes(t)) {
        matched.push(t);
        // filename / meta name weigh more
        if ((e.fileName || "").toLowerCase().includes(t)) score += 8;
        else if ((e.name || "").toLowerCase().includes(t)) score += 6;
        else if ((e.tags || []).includes(t)) score += 5;
        else score += 2;
      }
    }
    if (matched.length === 0) continue;
    // prefer more token coverage
    score += matched.length * 3;
    // mild preference for mid-size buildings (not tiny props, not huge megabuilds)
    const vol = e.totalBlocks || 0;
    if (vol >= 500 && vol <= 80000) score += 2;
    scored.push({ score, matched, entry: e });
  }

  scored.sort((a, b) => b.score - a.score || a.entry.rel.localeCompare(b.entry.rel, "zh"));

  return {
    ok: true,
    query,
    tokens,
    totalIndexed: index.count || 0,
    builtAt: index.builtAt,
    root: index.root,
    hits: scored.slice(0, limit).map(({ score, matched, entry }) => ({
      score,
      matched,
      rel: entry.rel,
      fileName: entry.fileName,
      absolutePath: path.join(index.root, entry.rel),
      name: entry.name,
      author: entry.author,
      description: entry.description,
      tags: entry.tags,
      size: { x: entry.sizeX, y: entry.sizeY, z: entry.sizeZ },
      totalBlocks: entry.totalBlocks,
      totalVolume: entry.totalVolume,
    })),
  };
}

export function schemInfo(relOrName, opts = {}) {
  const index = opts.index || loadIndex(opts.indexPath);
  const q = String(relOrName || "").replace(/\\/g, "/").toLowerCase();
  const entry =
    (index.entries || []).find((e) => e.rel.replace(/\\/g, "/").toLowerCase() === q) ||
    (index.entries || []).find((e) => e.fileName.toLowerCase() === q) ||
    (index.entries || []).find((e) => e.rel.toLowerCase().includes(q));
  if (!entry) {
    return { ok: false, error: `Not found in index: ${relOrName}` };
  }
  return {
    ok: true,
    absolutePath: path.join(index.root, entry.rel),
    entry,
  };
}

export function indexStatus(indexPath = DEFAULT_INDEX_PATH) {
  const index = loadIndex(indexPath);
  return {
    ok: true,
    indexPath,
    root: index.root || DEFAULT_SCHEM_ROOT,
    builtAt: index.builtAt,
    count: index.count || 0,
    parsed: index.parsed ?? null,
    failed: index.failed ?? null,
    exists: fs.existsSync(indexPath),
  };
}
