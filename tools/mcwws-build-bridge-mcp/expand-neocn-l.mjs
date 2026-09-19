/**
 * Expand neo-Chinese garden into user FAWE L:
 *   A: x -606..-560, z 134..164
 *   B: x -574..-560, z 165..198
 * Skip street, west trees, existing pavilion/pond/wall, macdonalds.
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
  return b.split("[")[0];
}

function inL(x, z) {
  const a = x >= -606 && x <= -560 && z >= 134 && z <= 164;
  const b = x >= -574 && x <= -560 && z >= 165 && z <= 198;
  return a || b;
}

const BLOCK = new Set([
  "water",
  "stripped_dark_oak_wood",
  "calcite",
  "polished_deepslate",
  "bamboo_block",
  "cherry_log",
  "cherry_leaves",
  "dark_oak_log",
  "stone_bricks",
  "moss_block",
  "oak_leaves",
  "oak_wood",
  "oak_log",
  "spruce_planks",
  "dark_oak_planks",
  "chiseled_stone_bricks",
  "cobblestone_wall",
  "spruce_fence",
  "deepslate_tiles",
  "deepslate_tile_stairs",
  "lantern",
  "lily_pad",
]);

function hillH(x, z) {
  const hills = [
    { cx: -564, cz: 139, r: 4.8, h: 3 },
    { cx: -569, cz: 158, r: 3.5, h: 2 },
    { cx: -562, cz: 172, r: 4.2, h: 2 },
    { cx: -561, cz: 188, r: 3.8, h: 3 },
    { cx: -570, cz: 194, r: 3.2, h: 2 },
  ];
  let m = 0;
  for (const hill of hills) {
    const d = Math.hypot(x - hill.cx, z - hill.cz);
    if (d < hill.r) {
      const t = 1 - d / hill.r;
      m = Math.max(m, Math.round(hill.h * t * t * 1.4));
    }
  }
  return m;
}

function inPond2(x, z) {
  const dx = (x + 566) / 3.8;
  const dz = (z - 147) / 2.8;
  return dx * dx + dz * dz + Math.sin(x * 2.1 + z) * 0.08 < 1;
}
function inPond3(x, z) {
  const dx = (x + 568) / 3.2;
  const dz = (z - 190) / 2.6;
  return dx * dx + dz * dz + Math.cos(x * 1.3 - z * 0.7) * 0.1 < 1;
}

const stair = (mat, facing, half = "bottom") =>
  `${mat}[facing=${facing},half=${half},shape=straight,waterlogged=false]`;

console.log("scan L occupancy");
const occ = new Set();
const grass = [];
const ranges = [
  [-575, -560, 134, 164],
  [-574, -560, 165, 198],
];
for (const [x0, x1, z0, z1] of ranges) {
  for (let z = z0; z <= z1; z++) {
    for (let x = x0; x <= x1; x++) {
      const b63 = nid(await get(x, 63, z));
      const b64 = nid(await get(x, 64, z));
      if (BLOCK.has(b63) || BLOCK.has(b64) || x <= -607) {
        occ.add(`${x},${z}`);
        continue;
      }
      grass.push([x, z, b63]);
    }
  }
}
function free(x, z) {
  return inL(x, z) && !occ.has(`${x},${z}`);
}
console.log("free cells", grass.length, "blocked", occ.size);

for (let z = 144; z <= 151; z++) {
  for (let x = -570; x <= -562; x++) {
    if (!free(x, z) || !inPond2(x, z)) continue;
    set(x, 62, z, "clay");
    set(x, 63, z, "water");
    occ.add(`${x},${z}`);
  }
}
for (let z = 187; z <= 194; z++) {
  for (let x = -572; x <= -564; x++) {
    if (!free(x, z) || !inPond3(x, z)) continue;
    set(x, 62, z, "clay");
    set(x, 63, z, "water");
    occ.add(`${x},${z}`);
  }
}

const pondCells = [...map.values()].filter((b) => b.block === "water").map((b) => [b.x, b.z]);
const pondSet = new Set(pondCells.map(([x, z]) => `${x},${z}`));
for (const [x, z] of pondCells) {
  for (const [dx, dz] of [
    [1, 0],
    [-1, 0],
    [0, 1],
    [0, -1],
  ]) {
    const nx = x + dx,
      nz = z + dz;
    if (pondSet.has(`${nx},${nz}`) || !free(nx, nz)) continue;
    const k = (((nx * 3 + nz * 5) % 3) + 3) % 3;
    set(nx, 63, nz, k === 0 ? "mossy_cobblestone" : k === 1 ? "andesite" : "cobblestone");
  }
}
const lilies = [
  [-567, 147],
  [-566, 146],
  [-565, 148],
  [-569, 190],
  [-568, 189],
  [-567, 191],
];
for (const [x, z] of lilies) {
  if (pondSet.has(`${x},${z}`)) set(x, 64, z, "lily_pad");
}

const path = [
  [-576, 145],
  [-574, 146],
  [-572, 144],
  [-570, 143],
  [-569, 141],
  [-567, 152],
  [-566, 155],
  [-568, 160],
  [-569, 164],
  [-568, 167],
  [-566, 170],
  [-567, 174],
  [-569, 177],
  [-566, 184],
  [-565, 187],
  [-567, 193],
  [-563, 196],
  [-571, 169],
  [-564, 161],
  [-562, 150],
];
const mix = ["stone", "stone", "andesite", "cobblestone"];
for (const [x, z] of path) {
  if (pondSet.has(`${x},${z}`) || !free(x, z)) continue;
  const k = (((x + z * 3) % 4) + 4) % 4;
  set(x, 63, z, mix[k]);
}

for (const [x, z] of grass) {
  if (pondSet.has(`${x},${z}`)) continue;
  const h = hillH(x, z);
  if (h <= 0) continue;
  if (map.has(`${x},63,${z}`)) continue;
  for (let i = 0; i < h; i++) {
    set(x, 63 + i, z, i === h - 1 ? "grass_block" : "dirt");
  }
  if (h >= 2) {
    const r = (((x * 7 + z * 11) % 5) + 5) % 5;
    if (r === 0) set(x, 63 + h, z, "azalea");
    if (r === 1) set(x, 63 + h, z, "fern");
    if (r === 2 && h >= 3) set(x, 63 + h, z, "moss_carpet");
  }
}

function stoneLantern(x, z) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) return;
  set(x, 63, z, "chiseled_stone_bricks");
  set(x, 64, z, "cobblestone_wall");
  set(x, 65, z, "lantern[hanging=false,waterlogged=false]");
}
stoneLantern(-571, 141);
stoneLantern(-563, 157);
stoneLantern(-570, 176);
stoneLantern(-564, 196);

function cherry(cx, cz) {
  if (!free(cx, cz)) return;
  for (let y = 64; y <= 67; y++) set(cx, y, cz, "cherry_log[axis=y]");
  for (let dx = -2; dx <= 2; dx++) {
    for (let dz = -2; dz <= 2; dz++) {
      for (let dy = 3; dy <= 6; dy++) {
        if (Math.abs(dx) + Math.abs(dz) + (dy < 5 ? 1 : 0) > 4) continue;
        if (dx === 0 && dz === 0 && dy <= 4) continue;
        set(cx + dx, 63 + dy, cz + dz, "cherry_leaves[distance=1,persistent=true,waterlogged=false]");
      }
    }
  }
}
cherry(-562, 154);
cherry(-571, 196);

const bamboos = [
  [-561, 143, 5],
  [-561, 145, 4],
  [-562, 142, 6],
  [-561, 168, 5],
  [-561, 171, 3],
  [-562, 181, 6],
  [-561, 183, 4],
  [-573, 186, 4],
  [-572, 188, 5],
  [-561, 192, 5],
];
for (const [x, z, h] of bamboos) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) continue;
  for (let y = 64; y < 64 + h; y++) set(x, y, z, "bamboo_block[axis=y]");
}

set(-572, 63, 171, "andesite");
set(-572, 64, 171, "andesite");
set(-571, 63, 172, "mossy_cobblestone");
set(-573, 63, 172, "tuff");
set(-572, 65, 171, "andesite");
set(-571, 64, 173, "azalea");
set(-573, 64, 170, "flowering_azalea");

const hx0 = -567,
  hx1 = -564,
  hz0 = 179,
  hz1 = 182;
for (let x = hx0; x <= hx1; x++) {
  for (let z = hz0; z <= hz1; z++) {
    if (!free(x, z)) continue;
    set(x, 63, z, "stripped_dark_oak_wood");
  }
}
for (const [x, z] of [
  [hx0, hz0],
  [hx1, hz0],
  [hx1, hz1],
  [hx0, hz1],
]) {
  for (let y = 64; y <= 66; y++) set(x, y, z, "dark_oak_log[axis=y]");
}
for (let x = hx0; x <= hx1; x++) {
  set(x, 66, hz0, "dark_oak_log[axis=x]");
  set(x, 66, hz1, "dark_oak_log[axis=x]");
}
for (let z = hz0; z <= hz1; z++) set(hx1, 66, z, "dark_oak_log[axis=z]");
const ry = 67;
for (let x = hx0 - 1; x <= hx1 + 1; x++) {
  for (let z = hz0 - 1; z <= hz1 + 1; z++) set(x, ry, z, "deepslate_tiles");
}
for (let x = hx0 - 1; x <= hx1 + 1; x++) {
  set(x, ry, hz0 - 1, stair("deepslate_tile_stairs", "south"));
  set(x, ry, hz1 + 1, stair("deepslate_tile_stairs", "north"));
}
for (let z = hz0 - 1; z <= hz1 + 1; z++) {
  set(hx0 - 1, ry, z, stair("deepslate_tile_stairs", "east"));
  set(hx1 + 1, ry, z, stair("deepslate_tile_stairs", "west"));
}
set(-565, 66, 180, "lantern[hanging=true,waterlogged=false]");
set(-565, 67, 180, "deepslate_tiles");
set(-566, 64, 180, "dark_oak_slab[type=top,waterlogged=false]");
set(-566, 64, 179, stair("dark_oak_stairs", "north"));
set(-566, 64, 181, stair("dark_oak_stairs", "south"));

const shrubs = [
  [-563, 163, "flowering_azalea"],
  [-570, 165, "azalea"],
  [-562, 176, "lilac[half=lower]"],
  [-573, 180, "pink_petals"],
  [-566, 198, "peony[half=lower]"],
  [-561, 160, "pink_petals"],
  [-573, 166, "moss_carpet"],
];
for (const [x, z, b] of shrubs) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) continue;
  if (b.includes("half=lower")) {
    set(x, 64, z, b);
    set(x, 65, z, b.replace("lower", "upper"));
  } else set(x, 64, z, b);
}

const blocks = [...map.values()];
console.log("AABB L north -606..-560,134..164 / arm -574..-560,165..198; blocks", blocks.length);
for (let i = 0; i < blocks.length; i += 8000) {
  console.log(await postBatch(blocks.slice(i, i + 8000)));
}
console.log("qa half-pavilion lantern", await get(-565, 66, 180), await get(-565, 67, 180));
console.log("qa street", await get(-575, 64, 170));
console.log("done");
