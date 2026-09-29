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
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "smooth_quartz_slab" ||
    n === "iron_bars" ||
    n === "lantern" ||
    n.endsWith("_leaves") ||
    n.includes("froglight") ||
    n === "moss_carpet"
  );
}

const west = [];
for (let z = 26; z <= 194; z++) {
  let xmin = null;
  let xmax = null;
  for (let x = -722; x <= -698; x++) {
    if (isWall(await get(x, 64, z))) {
      if (xmin == null) xmin = x;
      xmax = x;
    }
  }
  if (xmin != null) west.push({ z, xmin, xmax });
}

const extra = new Map();
function add(x, z, face) {
  extra.set(`${x},${z}`, face);
}

for (let i = 0; i < west.length - 1; i++) {
  let x = west[i].xmin;
  let z = west[i].z;
  const tx = west[i + 1].xmin;
  const tz = west[i + 1].z;
  const face = tz !== z && Math.abs(tx - x) <= Math.abs(tz - z) ? "east" : "north";
  while (x !== tx || z !== tz) {
    if (z !== tz) z += Math.sign(tz - z);
    else x += Math.sign(tx - x);
    if (x === tx && z === tz) break;
    if (isGrass(await get(x, 63, z))) add(x, z, z !== tz ? "east" : "south");
  }
}

// explicitly stitch NW: (-706,26) to (-709,27)
for (const x of [-708, -707, -706]) {
  if (isGrass(await get(x, 63, 27))) add(x, 27, "south");
}

console.log("stitch extra before y-swap check", extra.size);
console.log([...extra.keys()].slice(0, 20));

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

const wallSet = new Set();
for (const { z, xmin, xmax } of west) {
  for (let x = xmin; x <= xmax; x++) wallSet.add(`${x},${z}`);
}
for (const k of extra.keys()) wallSet.add(k);

let skip = 0;
for (const [k, face0] of extra) {
  const [x, z] = k.split(",").map(Number);
  const face = x >= -706 && z <= 28 ? "south" : "east";
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
  const pillar = face === "east" ? z % 6 === 0 : x % 6 === 0;
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

console.log("extra place", map.size, "skip", skip);
const blocks = [...map.values()];
for (let i = 0; i < blocks.length; i += 400) {
  const slice = blocks.slice(i, i + 400);
  const r = await fetch(`${BASE}/set_blocks`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", blocks: slice }),
  });
  const j = await r.json();
  if (!r.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
}
console.log("done extra", blocks.length);
