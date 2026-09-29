/**
 * Patch sidewalk wall: fill froglight gaps, 4-connect the curve,
 * extend east from (-705, 26) along the north inner edge.
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
    n.includes("froglight")
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
    n.endsWith("_leaves") ||
    n === "moss_carpet" ||
    n.includes("froglight")
  );
}

async function post(blocks) {
  const CHUNK = 400;
  for (let i = 0; i < blocks.length; i += CHUNK) {
    const slice = blocks.slice(i, i + CHUNK);
    const r = await fetch(`${BASE}/set_blocks`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ world: "world", blocks: slice }),
    });
    const j = await r.json();
    if (!r.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
    process.stdout.write(`\rplaced ${Math.min(i + CHUNK, blocks.length)}/${blocks.length}`);
  }
  console.log();
}

const cells = new Map(); // k -> face
function add(x, z, face) {
  const k = `${x},${z}`;
  if (!cells.has(k)) cells.set(k, face);
}

console.log("west+south inner");
for (let z = 26; z <= 194; z++) {
  let xFound = null;
  for (let x = -728; x <= -698; x++) {
    if (isGrass(await get(x, 63, z)) && isWalk(await get(x - 1, 63, z))) {
      xFound = x;
      break;
    }
  }
  if (xFound != null) add(xFound, z, "east");
}

console.log("SW fillet");
for (let z = 186; z <= 196; z++) {
  for (let x = -722; x <= -700; x++) {
    if (!isGrass(await get(x, 63, z))) continue;
    const w = isWalk(await get(x - 1, 63, z));
    const s = isWalk(await get(x, 63, z + 1));
    const e = isWalk(await get(x + 1, 63, z));
    const n = isWalk(await get(x, 63, z - 1));
    if (!(w || s || e || n)) continue;
    add(x, z, s && !w ? "north" : w ? "east" : s ? "north" : e ? "west" : "south");
  }
}

console.log("south inner");
let gap = 0;
for (let x = -720; x <= -640; x++) {
  let zFound = null;
  for (let z = 175; z <= 199; z++) {
    if (isGrass(await get(x, 63, z)) && isWalk(await get(x, 63, z + 1))) {
      zFound = z;
      break;
    }
  }
  if (zFound != null) {
    add(x, zFound, "north");
    gap = 0;
  } else if (x > -706) {
    gap++;
    if (gap >= 8) break;
  }
}

console.log("north inner east from -705,26");
let ngap = 0;
for (let x = -705; x <= -560; x++) {
  let zFound = null;
  for (let z = 24; z <= 32; z++) {
    if (isGrass(await get(x, 63, z)) && isWalk(await get(x, 63, z - 1))) {
      zFound = z;
      break;
    }
  }
  if (zFound != null) {
    add(x, zFound, "south");
    ngap = 0;
  } else {
    ngap++;
    if (x > -700 && ngap >= 6) {
      console.log("north stop x", x);
      break;
    }
  }
}

// 4-connect: close diagonal holes between cells that share a corner
const keys = [...cells.keys()].map((k) => k.split(",").map(Number));
const setK = new Set(cells.keys());
for (const [x, z] of keys) {
  for (const [dx, dz] of [
    [1, 1],
    [1, -1],
    [-1, 1],
    [-1, -1],
  ]) {
    const ox = x + dx;
    const oz = z + dz;
    if (!setK.has(`${ox},${oz}`)) continue;
    const a = `${x + dx},${z}`;
    const b = `${x},${z + dz}`;
    if (setK.has(a) || setK.has(b)) continue;
    const face = cells.get(`${x},${z}`);
    if (isGrass(await get(x + dx, 63, z))) add(x + dx, z, face);
    else if (isGrass(await get(x, 63, z + dz))) add(x, z + dz, face);
  }
}

console.log("cells", cells.size);

const wallSet = new Set(cells.keys());
const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

let skip = 0;
for (const [k, face] of cells) {
  const [x, z] = k.split(",").map(Number);
  if (!isGrass(await get(x, 63, z))) {
    skip++;
    continue;
  }
  let blocked = false;
  for (let y = 64; y <= 68; y++) {
    if (!isReplaceable(await get(x, y, z))) {
      blocked = true;
      break;
    }
  }
  if (blocked) {
    skip++;
    continue;
  }
  const pillar = face === "east" || face === "west" ? z % 6 === 0 : x % 6 === 0;
  if (pillar) {
    for (let y = 64; y <= 67; y++) set(x, y, z, "quartz_pillar[axis=y]");
    set(x, 68, z, "lantern[hanging=false,waterlogged=false]");
  } else {
    set(x, 64, z, "polished_andesite");
    set(
      x,
      65,
      z,
      `smooth_quartz_stairs[facing=${face},half=top,shape=straight,waterlogged=false]`
    );
    const n = wallSet.has(`${x},${z - 1}`);
    const s = wallSet.has(`${x},${z + 1}`);
    const e = wallSet.has(`${x + 1},${z}`);
    const w = wallSet.has(`${x - 1},${z}`);
    set(x, 66, z, `iron_bars[north=${n},south=${s},east=${e},west=${w},waterlogged=false]`);
    set(x, 67, z, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  }
}

const blocks = [...map.values()];
console.log("place", blocks.length, "skip", skip);
await post(blocks);

const xs = [...cells.keys()].map((k) => +k.split(",")[0]);
const zs = [...cells.keys()].map((k) => +k.split(",")[1]);
console.log("AABB x", Math.min(...xs), Math.max(...xs), "z", Math.min(...zs), Math.max(...zs));
console.log("done");
