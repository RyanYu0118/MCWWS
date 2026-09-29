/**
 * Extend sidewalk wall south from after the gate/crossroads,
 * then east to (-391, 410). Follow 1-block road slope.
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
    n === "mossy_cobblestone" ||
    n.includes("froglight") ||
    n === "shroomlight"
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
    n === "wheat" ||
    n === "vine" ||
    n === "moss_carpet" ||
    n.endsWith("_leaves") ||
    n.includes("froglight") ||
    n === "shroomlight" ||
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "smooth_quartz_slab" ||
    n === "iron_bars" ||
    n === "lantern" ||
    n.includes("poppy") ||
    n.includes("dandelion") ||
    n.includes("daisy") ||
    n.includes("allium") ||
    n.includes("bluet") ||
    n.includes("cornflower")
  );
}

async function grassY(x, z) {
  const a = nid(await get(x, 64, z));
  const b = nid(await get(x, 63, z));
  if (a === "grass_block") return 64;
  if (b === "grass_block") return 63;
  return null;
}

const cells = []; // {x,z,gy,face}
const seen = new Set();
function add(x, z, gy, face) {
  const k = `${x},${z}`;
  if (seen.has(k)) return;
  seen.add(k);
  cells.push({ x, z, gy, face });
}

console.log("west southbound after intersection");
for (let z = 217; z <= 410; z++) {
  const x = -720;
  let found = null;
  for (const y of [63, 64]) {
    const n = nid(await get(x, y, z));
    const w = nid(await get(x - 1, y, z));
    if (n === "grass_block" && (isWalk(w) || w.includes("froglight") || w === "shroomlight")) {
      found = { x, gy: y };
      break;
    }
    if (n === "grass_block") {
      found = { x, gy: y };
      break;
    }
  }
  if (found) add(found.x, z, found.gy, "east");
}

console.log("east along z=410 to -391");
for (let x = -719; x <= -391; x++) {
  let found = null;
  for (const y of [64, 63]) {
    const n = nid(await get(x, y, 410));
    const s = nid(await get(x, y, 411));
    const s64 = nid(await get(x, 64, 411));
    if (n === "grass_block" && (isWalk(s) || isWalk(s64) || n === "grass_block")) {
      found = { z: 410, gy: y };
      break;
    }
  }
  if (found) add(x, found.z, found.gy, "north");
}

console.log("cells", cells.length);

const wallSet = new Set(cells.map((c) => `${c.x},${c.z}`));
const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

let skip = 0;
for (const c of cells) {
  let blocked = false;
  const base = c.gy + 1;
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
console.log("place", blocks.length, "skip", skip, "AABB", {
  x: [Math.min(...cells.map((c) => c.x)), Math.max(...cells.map((c) => c.x))],
  z: [Math.min(...cells.map((c) => c.z)), Math.max(...cells.map((c) => c.z))],
  gy: [Math.min(...cells.map((c) => c.gy)), Math.max(...cells.map((c) => c.gy))],
});

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
console.log("\ndone");
