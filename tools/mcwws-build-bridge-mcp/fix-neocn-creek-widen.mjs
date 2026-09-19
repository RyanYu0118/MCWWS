/**
 * Widen L-garden creek to 2 blocks, reroute through bamboo groves,
 * plant castle-river bank flora (petals, wildflowers, firefly bush, grass).
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

const newLine = lerpPath([
  [-603, 150],
  [-598, 150],
  [-592, 150],
  [-586, 148],
  [-580, 147],
  [-578, 149],
  [-577, 152],
  [-576, 146],
  [-572, 147],
  [-568, 148],
  [-566, 152],
  [-565, 158],
  [-564, 164],
  [-563, 170],
  [-562, 176],
  [-562, 181],
  [-561, 186],
  [-562, 191],
  [-564, 195],
  [-566, 198],
]);

console.log("scan L water + bamboo");
const occ = new Map();
for (let z = 134; z <= 198; z++) {
  const x0 = z >= 165 ? -574 : -606;
  const x1 = -560;
  for (let x = x0; x <= x1; x++) {
    occ.set(`${x},${z}`, {
      a: nid(await get(x, 63, z)),
      b: nid(await get(x, 64, z)),
    });
  }
}
function cell(x, z) {
  return occ.get(`${x},${z}`) || { a: "air", b: "air" };
}
function hard(x, z) {
  const c = cell(x, z);
  return HARD.has(c.a) || HARD.has(c.b) || !inL(x, z) || x <= -607;
}

const bedMix = ["gravel", "gravel", "coarse_dirt", "clay"];
const bankMix = ["gravel", "coarse_dirt", "moss_block", "dirt", "mossy_cobblestone"];
function bedAt(x, z) {
  return bedMix[(((x * 3 + z) % 4) + 4) % 4];
}
function bankAt(x, z) {
  return bankMix[(((x * 5 + z * 2) % 5) + 5) % 5];
}

const channel = new Set();
function addCh(x, z) {
  if (hard(x, z)) return;
  channel.add(`${x},${z}`);
}
for (let i = 0; i < newLine.length; i++) {
  const [x, z] = newLine[i];
  addCh(x, z);
  addCh(x + 1, z);
  if (i % 4 !== 1) addCh(x, z + (i % 2 === 0 ? 1 : -1));
  addCh(x + 1, z + 1);
}

for (let z = 165; z <= 198; z++) {
  for (let x = -574; x <= -568; x++) {
    const c = cell(x, z);
    if (c.a !== "water") continue;
    if (channel.has(`${x},${z}`)) continue;
    if (hard(x, z)) continue;
    set(x, 63, z, "grass_block");
    set(x, 62, z, "dirt");
    if (c.b === "spruce_slab" || c.b === "lily_pad") set(x, 64, z, "air");
  }
}

for (const key of channel) {
  const [x, z] = key.split(",").map(Number);
  const c = cell(x, z);
  set(x, 62, z, bedAt(x, z));
  const i = Math.abs(x + z);
  set(x, 63, z, i % 5 === 0 ? "water[level=2]" : "water");
  if (c.b === "bamboo_block") {
    for (let y = 64; y <= 70; y++) set(x, y, z, "air");
  } else if (
    c.b === "air" ||
    c.b === "short_grass" ||
    c.b === "fern" ||
    c.b === "pink_petals" ||
    c.b === "moss_carpet" ||
    c.b === "lily_pad" ||
    c.b === "spruce_slab"
  ) {
    if ((x + z) % 13 === 0) set(x, 64, z, "lily_pad");
    else set(x, 64, z, "air");
  }
}

const flora = [
  "short_grass",
  "short_grass",
  "pink_petals",
  "wildflowers",
  "poppy",
  "dandelion",
  "fern",
  "bush",
  "firefly_bush",
  "leaf_litter",
  "oxeye_daisy",
  "azure_bluet",
];
function floraAt(x, z) {
  return flora[(((x * 11 + z * 3) % flora.length) + flora.length) % flora.length];
}

for (const key of channel) {
  const [x, z] = key.split(",").map(Number);
  for (const [dx, dz] of [
    [1, 0],
    [-1, 0],
    [0, 1],
    [0, -1],
    [2, 0],
    [0, 2],
    [-2, 0],
    [1, 1],
    [-1, 1],
  ]) {
    const nx = x + dx,
      nz = z + dz;
    if (channel.has(`${nx},${nz}`) || hard(nx, nz) || !inL(nx, nz)) continue;
    const c = cell(nx, nz);
    if (c.a === "water" && z < 165) continue;
    if (c.b === "bamboo_block") continue;
    if (c.a === "water") continue;
    if ((nx + nz) % 3 === 0) set(nx, 63, nz, bankAt(nx, nz));
    if (c.b === "air" || c.b === "short_grass" || c.b === "fern" || c.b === "moss_carpet") {
      const f = floraAt(nx, nz);
      if ((nx * 5 + nz) % 4 !== 0) set(nx, 64, nz, f);
    }
    if ((nx + nz) % 17 === 0) {
      set(nx, 64, nz, "tall_grass[half=lower]");
      set(nx, 65, nz, "tall_grass[half=upper]");
    }
  }
}

for (const [x, z] of [
  [-588, 150],
  [-577, 150],
  [-563, 170],
  [-562, 181],
  [-564, 195],
]) {
  if (!channel.has(`${x},${z}`)) continue;
  set(x, 64, z, "spruce_slab[type=bottom,waterlogged=true]");
}

const blocks = [...map.values()];
console.log("place", blocks.length, "channel", channel.size);
try {
  for (let i = 0; i < blocks.length; i += 8000) {
    console.log(await postBatch(blocks.slice(i, i + 8000)));
  }
} catch (e) {
  console.log("batch failed", e.message, "retry without extra flora ids");
  const safe = blocks.filter(
    (b) =>
      !["wildflowers", "firefly_bush", "bush", "leaf_litter"].includes(nid(b.block))
  );
  console.log(await postBatch(safe));
}
console.log("through bamboo", await get(-562, 63, 181), await get(-562, 64, 181));
console.log("old west", await get(-571, 63, 176));
console.log("width", await get(-563, 63, 170), await get(-562, 63, 170));
console.log("pavilion", await get(-566, 63, 180));
console.log("street", await get(-575, 64, 170));
console.log("done");
