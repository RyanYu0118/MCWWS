/**
 * Narrow castle-style creek through the L garden.
 * Materials from medieval castle river near (-629,92,-538):
 * gravel / coarse_dirt / dirt / moss banks, water over gravel bed.
 * Width 1–2 (castle is ~8–15). Sources + hill cascade for 活水.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, x, y, z }),
  });
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
async function postBatch(blocks) {
  const res = await fetch(`${BASE}/set_blocks`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, blocks }),
  });
  const json = await res.json();
  if (!res.ok || json.ok === false) throw new Error(json.error || JSON.stringify(json));
  return json;
}

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function nid(b) {
  return (b || "").split("[")[0];
}

function inL(x, z) {
  return (
    (x >= -606 && x <= -560 && z >= 134 && z <= 164) ||
    (x >= -574 && x <= -560 && z >= 165 && z <= 198)
  );
}

const HARD = new Set([
  "calcite",
  "polished_deepslate",
  "stripped_dark_oak_wood",
  "dark_oak_log",
  "dark_oak_stairs",
  "dark_oak_slab",
  "bamboo_block",
  "cherry_log",
  "cherry_leaves",
  "stone_bricks",
  "oak_leaves",
  "oak_wood",
  "oak_log",
  "spruce_planks",
  "dark_oak_planks",
  "deepslate_tiles",
  "deepslate_tile_stairs",
  "cobblestone_wall",
  "chiseled_stone_bricks",
  "lantern",
]);

function lerpPath(pts) {
  const out = [];
  const seen = new Set();
  for (let i = 0; i < pts.length - 1; i++) {
    const [x0, z0] = pts[i];
    const [x1, z1] = pts[i + 1];
    const n = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0), 1);
    for (let t = 0; t <= n; t++) {
      const x = Math.round(x0 + ((x1 - x0) * t) / n);
      const z = Math.round(z0 + ((z1 - z0) * t) / n);
      const k = `${x},${z}`;
      if (!seen.has(k)) {
        seen.add(k);
        out.push([x, z]);
      }
    }
  }
  return out;
}

const center = lerpPath([
  [-603, 150],
  [-600, 151],
  [-596, 150],
  [-592, 151],
  [-588, 149],
  [-584, 148],
  [-580, 147],
  [-576, 146],
  [-572, 147],
  [-568, 146],
  [-566, 148],
  [-567, 153],
  [-568, 158],
  [-569, 164],
  [-570, 170],
  [-571, 176],
  [-571, 182],
  [-570, 186],
  [-568, 190],
  [-567, 194],
  [-566, 198],
]);

const bedMix = ["gravel", "gravel", "coarse_dirt", "dirt", "clay"];
const bankMix = ["gravel", "coarse_dirt", "moss_block", "mossy_cobblestone", "dirt", "andesite"];

function bedAt(x, z) {
  return bedMix[(((x * 3 + z * 5) % 5) + 5) % 5];
}
function bankAt(x, z) {
  return bankMix[(((x * 7 + z * 2) % 6) + 6) % 6];
}

console.log("scan creek cells");
const blocked = new Set();
const samples = new Set();
for (const [x, z] of center) {
  samples.add(`${x},${z}`);
  for (const [dx, dz] of [
    [0, 0],
    [1, 0],
    [-1, 0],
    [0, 1],
    [0, -1],
  ])
    samples.add(`${x + dx},${z + dz}`);
}
for (const key of samples) {
  const [x, z] = key.split(",").map(Number);
  const a = nid(await get(x, 63, z));
  const b = nid(await get(x, 64, z));
  if (HARD.has(a) || HARD.has(b) || !inL(x, z) || x <= -607) blocked.add(key);
}
function ok(x, z) {
  return inL(x, z) && !blocked.has(`${x},${z}`);
}

const channel = [];
const seenCh = new Set();
function addCh(x, z) {
  const k = `${x},${z}`;
  if (seenCh.has(k) || !ok(x, z)) return;
  seenCh.add(k);
  channel.push([x, z]);
}

for (let i = 0; i < center.length; i++) {
  const [x, z] = center[i];
  addCh(x, z);
  const prev = center[Math.max(0, i - 1)];
  const alongX = Math.abs(x - prev[0]) >= Math.abs(z - prev[1]);
  const narrow = i % 5 === 2;
  if (!narrow) {
    if (alongX) addCh(x, z + (i % 3 === 0 ? 1 : -1));
    else addCh(x + (i % 3 === 0 ? 1 : -1), z);
  }
}

for (const [x, z] of channel) {
  set(x, 62, z, bedAt(x, z));
  const dist = channel.findIndex(([cx, cz]) => cx === x && cz === z);
  const flow = dist % 6;
  if (flow === 0) set(x, 63, z, "water");
  else set(x, 63, z, `water[level=${Math.min(flow, 4)}]`);
  const above = nid(await get(x, 64, z));
  if (above === "lily_pad" || above === "air" || above === "short_grass" || above === "fern" || above === "pink_petals" || above === "moss_carpet") {
    if ((x + z) % 11 === 0) set(x, 64, z, "lily_pad");
    else if ((x * 3 + z) % 9 === 0) set(x, 63, z, "water");
    else if (above !== "lily_pad") set(x, 64, z, "air");
  }
  if ((x + z * 2) % 13 === 0) set(x, 63, z, "water");
}

for (const [x, z] of channel) {
  for (const [dx, dz] of [
    [1, 0],
    [-1, 0],
    [0, 1],
    [0, -1],
    [1, 1],
    [-1, 1],
  ]) {
    const nx = x + dx,
      nz = z + dz;
    if (!ok(nx, nz) || seenCh.has(`${nx},${nz}`)) continue;
    const cur = nid(await get(nx, 63, nz));
    if (cur === "water") continue;
    if (HARD.has(cur)) continue;
    if ((nx + nz) % 4 === 0) continue;
    set(nx, 63, nz, bankAt(nx, nz));
    if ((nx + 2 * nz) % 7 === 0) set(nx, 64, nz, "moss_carpet");
  }
}

const fords = [
  [-588, 150],
  [-572, 147],
  [-569, 164],
  [-571, 176],
  [-567, 194],
];
for (const [x, z] of fords) {
  if (!seenCh.has(`${x},${z}`)) continue;
  set(x, 64, z, "spruce_slab[type=bottom,waterlogged=true]");
}

for (const [x, z] of [
  [-602, 141],
  [-602, 142],
  [-601, 141],
]) {
  if (!inL(x, z) || x <= -607) continue;
  const a = nid(await get(x, 63, z));
  if (HARD.has(a)) continue;
  set(x, 63, z, "mossy_cobblestone");
  set(x, 64, z, "water");
  set(x, 62, z, "gravel");
}
set(-602, 64, 143, "water[level=1]");
set(-602, 63, 143, "gravel");
set(-602, 64, 144, "water[level=2]");
set(-602, 63, 144, "gravel");
set(-602, 65, 141, "air");
set(-601, 65, 141, "air");

if (inL(-566, 198)) {
  set(-566, 63, 198, "water");
  set(-566, 62, 198, "gravel");
}
if (inL(-565, 198)) set(-565, 63, 198, "water[level=2]");

const blocks = [...map.values()];
console.log("creek blocks", blocks.length, "channel", channel.length);
for (let i = 0; i < blocks.length; i += 8000) {
  console.log(await postBatch(blocks.slice(i, i + 8000)));
}
console.log("mid", await get(-588, 63, 150), await get(-570, 63, 170));
console.log("street", await get(-575, 64, 170));
console.log("wall", await get(-604, 65, 146));
console.log("done");
