/**
 * Fence the sidewalk inner edge (grass against andesite/stone/cobble).
 * Recipe copied from FAWE sample at x=-720, y=64-68, z=91-120.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const cache = new Map();

async function get(x, y, z) {
  const k = `${x},${y},${z}`;
  if (cache.has(k)) return cache.get(k);
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  const n = ((await r.json()).block || "").replace(/^minecraft:/, "");
  cache.set(k, n);
  return n;
}
function nid(b) {
  return (b || "").split("[")[0];
}
function isWalk(b) {
  const n = nid(b);
  return (
    n === "andesite" ||
    n === "polished_andesite" ||
    n === "stone" ||
    n === "cobblestone" ||
    n === "mossy_cobblestone"
  );
}
function isGrass(b) {
  return nid(b) === "grass_block";
}
function isReplaceable(b) {
  const n = nid(b);
  return (
    n === "air" ||
    n === "short_grass" ||
    n === "tall_grass" ||
    n === "fern" ||
    n === "large_fern" ||
    n.includes("poppy") ||
    n.includes("dandelion") ||
    n.includes("daisy") ||
    n.includes("cornflower") ||
    n.includes("allium") ||
    n.includes("bluet") ||
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "smooth_quartz_slab" ||
    n === "iron_bars" ||
    n === "lantern" ||
    n === "oak_leaves" ||
    n === "birch_leaves" ||
    n === "acacia_leaves" ||
    n === "azalea_leaves" ||
    n === "flowering_azalea_leaves" ||
    n === "moss_carpet" ||
    n.includes("froglight")
  );
}

async function post(blocks) {
  const CHUNK = 400;
  let changed = 0;
  for (let i = 0; i < blocks.length; i += CHUNK) {
    const slice = blocks.slice(i, i + CHUNK);
    const r = await fetch(`${BASE}/set_blocks`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ world: "world", blocks: slice }),
    });
    const j = await r.json();
    if (!r.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
    changed += j.changed || slice.length;
    process.stdout.write(`\rplaced ${Math.min(i + CHUNK, blocks.length)}/${blocks.length}`);
  }
  console.log();
  return changed;
}

const path = []; // {x,z, face} face = inward stairs facing
const seen = new Set();
function add(x, z, face) {
  const k = `${x},${z}`;
  if (seen.has(k)) return;
  seen.add(k);
  path.push({ x, z, face });
}

console.log("trace west (north to south)");
for (let z = 26; z <= 192; z++) {
  let xFound = null;
  for (let x = -728; x <= -700; x++) {
    const n = await get(x, 63, z);
    const w = await get(x - 1, 63, z);
    if (isGrass(n) && isWalk(w)) {
      xFound = x;
      break;
    }
  }
  if (xFound != null) add(xFound, z, "east");
}

console.log("trace SW fillet");
for (let z = 186; z <= 196; z++) {
  for (let x = -722; x <= -708; x++) {
    const n = await get(x, 63, z);
    if (!isGrass(n)) continue;
    const e = await get(x + 1, 63, z);
    const w = await get(x - 1, 63, z);
    const s = await get(x, 63, z + 1);
    const nn = await get(x, 63, z - 1);
    const walkN = isWalk(nn);
    const walkS = isWalk(s);
    const walkW = isWalk(w);
    const walkE = isWalk(e);
    if (!(walkN || walkS || walkW || walkE)) continue;
    let face = "east";
    if (walkS && !walkW) face = "north";
    else if (walkW) face = "east";
    else if (walkS) face = "north";
    else if (walkE) face = "west";
    else if (walkN) face = "south";
    add(x, z, face);
  }
}

console.log("trace south (west to east)");
let gap = 0;
for (let x = -719; x <= -640; x++) {
  let zFound = null;
  for (let z = 175; z <= 199; z++) {
    const n = await get(x, 63, z);
    const s = await get(x, 63, z + 1);
    if (isGrass(n) && isWalk(s)) {
      zFound = z;
      break;
    }
  }
  if (zFound != null) {
    add(x, zFound, "north");
    gap = 0;
  } else {
    gap++;
    if (x > -706 && gap >= 6) {
      console.log("south stop at x", x);
      break;
    }
  }
}

console.log("path cells", path.length, "z", Math.min(...path.map((p) => p.z)), "..", Math.max(...path.map((p) => p.z)), "x", Math.min(...path.map((p) => p.x)), "..", Math.max(...path.map((p) => p.x)));

const wallSet = new Set(path.map((p) => `${p.x},${p.z}`));
const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

let skipped = 0;
for (const p of path) {
  const g = nid(await get(p.x, 63, p.z));
  if (g !== "grass_block") {
    skipped++;
    continue;
  }
  let blocked = false;
  for (let y = 64; y <= 68; y++) {
    const cur = await get(p.x, y, p.z);
    if (!isReplaceable(cur)) {
      blocked = true;
      break;
    }
  }
  if (blocked) {
    skipped++;
    continue;
  }

  const pillar = p.face === "east" || p.face === "west" ? p.z % 6 === 0 : p.x % 6 === 0;
  if (pillar) {
    for (let y = 64; y <= 67; y++) set(p.x, y, p.z, "quartz_pillar[axis=y]");
    set(p.x, 68, p.z, "lantern[hanging=false,waterlogged=false]");
  } else {
    set(p.x, 64, p.z, "polished_andesite");
    set(
      p.x,
      65,
      p.z,
      `smooth_quartz_stairs[facing=${p.face},half=top,shape=straight,waterlogged=false]`
    );
    const n = wallSet.has(`${p.x},${p.z - 1}`);
    const s = wallSet.has(`${p.x},${p.z + 1}`);
    const e = wallSet.has(`${p.x + 1},${p.z}`);
    const w = wallSet.has(`${p.x - 1},${p.z}`);
    set(
      p.x,
      66,
      p.z,
      `iron_bars[north=${n},south=${s},east=${e},west=${w},waterlogged=false]`
    );
    set(p.x, 67, p.z, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  }
}

const blocks = [...map.values()];
console.log("place", blocks.length, "skip", skipped);
await post(blocks);

const sample = path.filter((p) => p.z <= 40).slice(0, 8);
console.log("north samples", sample);
console.log("south samples", path.filter((p) => p.face === "north").slice(0, 12));
console.log("done");
