/**
 * Remove the straight z=410 field wall, then hug the curved south sidewalk.
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
    n === "stone" ||
    n === "cobblestone" ||
    n === "mossy_cobblestone" ||
    n.includes("froglight") ||
    n === "shroomlight"
  );
}
function isWall(b) {
  const n = nid(b);
  return (
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "iron_bars" ||
    n === "smooth_quartz_slab" ||
    n === "lantern"
  );
}
function isReplaceable(b) {
  const n = nid(b);
  return (
    n === "air" ||
    n === "short_grass" ||
    n === "tall_grass" ||
    n === "fern" ||
    n === "large_fern" ||
    n === "wheat" ||
    n === "vine" ||
    n === "moss_carpet" ||
    n.endsWith("_leaves") ||
    n.includes("froglight") ||
    n === "shroomlight" ||
    isWall(b)
  );
}

async function post(blocks) {
  for (let i = 0; i < blocks.length; i += 400) {
    const slice = blocks.slice(i, i + 400);
    const r = await fetch(`${BASE}/set_blocks`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ world: "world", blocks: slice }),
    });
    const j = await r.json();
    if (!r.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
    process.stdout.write(`\r${Math.min(i + 400, blocks.length)}/${blocks.length}`);
  }
  console.log();
}

console.log("clear straight z=410 field wall");
const clear = [];
for (let x = -719; x <= -420; x++) {
  for (let y = 64; y <= 69; y++) {
    const b = await get(x, y, 410);
    if (isWall(b)) clear.push({ x, y, z: 410, block: "air" });
  }
}
console.log("clear", clear.length);
if (clear.length) await post(clear);

async function findInner(x, zGuess) {
  const z0 = zGuess ?? 480;
  for (let dz = 0; dz <= 40; dz++) {
    for (const z of dz === 0 ? [z0] : [z0 + dz, z0 - dz]) {
      if (z < 400 || z > 520) continue;
      for (const y of [63, 64]) {
        const n = nid(await get(x, y, z));
        const s = nid(await get(x, y, z + 1));
        const s64 = nid(await get(x, 64, z + 1));
        if (n === "grass_block" && (isWalk(s) || isWalk(s64))) {
          return { x, z, gy: y };
        }
      }
    }
  }
  return null;
}

console.log("trace curve");
const cells = [];
const seen = new Set();
let zGuess = 484;
for (let x = -720; x <= -391; x++) {
  const hit = await findInner(x, zGuess);
  if (hit) {
    zGuess = hit.z;
    const k = `${hit.x},${hit.z}`;
    if (!seen.has(k)) {
      seen.add(k);
      cells.push({ ...hit, face: "north" });
    }
  }
}

console.log("west down to curve");
for (let z = 217; z < (cells[0] ? cells[0].z : 484); z++) {
  const x = -720;
  let gy = null;
  for (const y of [63, 64]) {
    if (nid(await get(x, y, z)) === "grass_block") {
      gy = y;
      break;
    }
  }
  if (gy == null) continue;
  const k = `${x},${z}`;
  if (!seen.has(k)) {
    seen.add(k);
    cells.push({ x, z, gy, face: "east" });
  }
}

console.log("path", cells.length, "z", Math.min(...cells.map((c) => c.z)), Math.max(...cells.map((c) => c.z)));

const wallSet = new Set(cells.map((c) => `${c.x},${c.z}`));
const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

let skip = 0;
for (const c of cells) {
  const base = c.gy + 1;
  let blocked = false;
  for (let y = base; y <= base + 4; y++) {
    if (!isReplaceable(await get(c.x, y, c.z))) {
      blocked = true;
      break;
    }
  }
  if (blocked) {
    skip++;
    continue;
  }
  const pillar = c.face === "east" ? c.z % 6 === 0 : c.x % 6 === 0;
  if (pillar) {
    for (let y = base; y <= base + 3; y++) set(c.x, y, c.z, "quartz_pillar[axis=y]");
    set(c.x, base + 4, c.z, "lantern[hanging=false,waterlogged=false]");
  } else {
    set(c.x, base, c.z, "polished_andesite");
    set(
      c.x,
      base + 1,
      c.z,
      `smooth_quartz_stairs[facing=${c.face},half=top,shape=straight,waterlogged=false]`
    );
    const n = wallSet.has(`${c.x},${c.z - 1}`);
    const s = wallSet.has(`${c.x},${c.z + 1}`);
    const e = wallSet.has(`${c.x + 1},${c.z}`);
    const w = wallSet.has(`${c.x - 1},${c.z}`);
    set(
      c.x,
      base + 2,
      c.z,
      `iron_bars[north=${n},south=${s},east=${e},west=${w},waterlogged=false]`
    );
    set(c.x, base + 3, c.z, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  }
}

const blocks = [...map.values()];
console.log("place", blocks.length, "skip", skip);
await post(blocks);
console.log("end", cells.filter((c) => c.x >= -395).slice(-5));
console.log("start", cells.filter((c) => c.x <= -715).slice(0, 8));
console.log("done");
