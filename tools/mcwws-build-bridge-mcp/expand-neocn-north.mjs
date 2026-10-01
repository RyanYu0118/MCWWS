/**
 * Expand neo-Chinese garden north into FAWE
 *   (-560,63,137) – (-606,63,127)
 * Same language as build-neocn-garden / west calcite screen.
 * Skip sidewalk wall z=126 except a moon-gate opening; skip pavilion eaves z=137.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -606,
  X1 = -560;
const Z0 = 127,
  Z1 = 137;
const WX = -604;
const F = 63;

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world, x, y, z }),
  });
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
async function post(blocks) {
  for (let i = 0; i < blocks.length; i += 400) {
    const slice = blocks.slice(i, i + 400);
    const r = await fetch(`${BASE}/set_blocks`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ world, blocks: slice }),
    });
    const j = await r.json();
    if (!r.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
    process.stdout.write(`\r${Math.min(i + 400, blocks.length)}/${blocks.length}`);
  }
  console.log();
}

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function nid(b) {
  return (b || "").split("[")[0];
}
const stair = (mat, facing, half = "bottom") =>
  `${mat}[facing=${facing},half=${half},shape=straight,waterlogged=false]`;

function inBox(x, z) {
  return x >= X0 && x <= X1 && z >= Z0 && z <= Z1;
}

const KEEP = new Set([
  "stripped_dark_oak_wood",
  "dark_oak_log",
  "deepslate_tiles",
  "deepslate_tile_stairs",
  "stone_bricks",
  "oak_leaves",
]);
const SIDEWALK_WALL = new Set([
  "polished_andesite",
  "quartz_pillar",
  "smooth_quartz_stairs",
  "smooth_quartz_slab",
  "iron_bars",
  "lantern",
]);

const occ = new Set();
console.log("scan");
for (let z = Z0; z <= Z1; z++) {
  for (let x = X0; x <= X1; x++) {
    if (x <= -607) {
      occ.add(`${x},${z}`);
      continue;
    }
    const a = nid(await get(x, 63, z));
    const b = nid(await get(x, 64, z));
    const c = nid(await get(x, 68, z));
    if (KEEP.has(a) || KEEP.has(b) || KEEP.has(c)) occ.add(`${x},${z}`);
    if (z === 137 && x >= -584 && x <= -576) occ.add(`${x},${z}`);
  }
}
function free(x, z) {
  return inBox(x, z) && !occ.has(`${x},${z}`);
}

const westH = {
  127: 3,
  128: 4,
  129: 2,
  130: 5,
  131: 3,
  132: 6,
  133: 4,
  134: 2,
  135: 5,
  136: 3,
};
for (let z = 127; z <= 136; z++) {
  const h = westH[z];
  if (!h || !free(WX, z)) continue;
  set(WX, F, z, "calcite");
  for (let y = 64; y < F + h; y++) set(WX, y, z, "calcite");
  const capY = F + h;
  if (h >= 5 && z % 2 === 1) set(WX, capY, z, "polished_deepslate");
  else set(WX, capY, z, "dark_oak_slab[type=bottom,waterlogged=false]");
}

const gateXs = new Set([-588, -587, -586]);
for (let x = -603; x <= -561; x++) {
  if (x === WX) continue;
  if (gateXs.has(x)) continue;
  if (!free(x, Z0)) continue;
  const h = 3 + ((x * 5 + 11) % 3 === 0 ? 1 : 0);
  set(x, F, Z0, "calcite");
  for (let y = 64; y < F + h; y++) set(x, y, Z0, "calcite");
  set(x, F + h, Z0, "dark_oak_slab[type=bottom,waterlogged=false]");
}
for (const [x, y, b] of [
  [-589, 64, "polished_deepslate"],
  [-589, 65, "polished_deepslate"],
  [-589, 66, "calcite"],
  [-585, 64, "polished_deepslate"],
  [-585, 65, "polished_deepslate"],
  [-585, 66, "calcite"],
  [-588, 66, "polished_deepslate"],
  [-587, 67, "polished_deepslate"],
  [-586, 66, "polished_deepslate"],
]) {
  if (free(x, Z0) || gateXs.has(x)) set(x, y, Z0, b);
}
for (const x of gateXs) {
  set(x, F, Z0, "smooth_stone");
  set(x, 64, Z0, "air");
  set(x, 65, Z0, "air");
}
set(-589, 66, Z0, "lantern[hanging=false,waterlogged=false]");
set(-585, 66, Z0, "lantern[hanging=false,waterlogged=false]");

for (const x of gateXs) {
  for (let y = 64; y <= 68; y++) {
    const cur = nid(await get(x, y, 126));
    if (SIDEWALK_WALL.has(cur) || cur.includes("froglight") || cur === "shroomlight") {
      set(x, y, 126, "air");
    }
  }
}

function inPond(x, z) {
  const dx = (x + 596) / 4.2;
  const dz = (z - 132) / 2.6;
  return dx * dx + dz * dz + Math.sin(x * 1.6 + z) * 0.08 < 1;
}
const pond = [];
for (let z = 129; z <= 135; z++) {
  for (let x = -601; x <= -591; x++) {
    if (!free(x, z) || x === WX) continue;
    if (!inPond(x, z)) continue;
    set(x, 62, z, "clay");
    set(x, F, z, "water");
    pond.push([x, z]);
    occ.add(`${x},${z}`);
  }
}
const pondSet = new Set(pond.map(([x, z]) => `${x},${z}`));
for (const [x, z] of pond) {
  for (const [dx, dz] of [
    [1, 0],
    [-1, 0],
    [0, 1],
    [0, -1],
  ]) {
    const nx = x + dx,
      nz = z + dz;
    if (pondSet.has(`${nx},${nz}`) || !free(nx, nz) || nx === WX || nz === Z0) continue;
    const k = (((nx + nz) % 3) + 3) % 3;
    set(nx, F, nz, k === 0 ? "mossy_cobblestone" : k === 1 ? "andesite" : "cobblestone");
  }
}
for (const [x, z] of [
  [-597, 132],
  [-596, 131],
  [-595, 133],
]) {
  if (pondSet.has(`${x},${z}`)) set(x, 64, z, "lily_pad");
}

const mix = ["stone", "stone", "andesite", "cobblestone"];
const path = [
  [-587, 127],
  [-587, 129],
  [-585, 130],
  [-583, 132],
  [-581, 134],
  [-580, 136],
  [-575, 133],
  [-571, 131],
  [-568, 129],
  [-564, 132],
  [-562, 135],
];
for (const [x, z] of path) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) continue;
  const k = (((x + z * 3) % 4) + 4) % 4;
  set(x, F, z, mix[k]);
}

function hillH(x, z) {
  const hills = [
    { cx: -570, cz: 132, r: 4.2, h: 3 },
    { cx: -592, cz: 134, r: 3.2, h: 2 },
  ];
  let m = 0;
  for (const hill of hills) {
    const d = Math.hypot(x - hill.cx, z - hill.cz);
    if (d < hill.r) {
      const t = 1 - d / hill.r;
      m = Math.max(m, Math.round(hill.h * t * t * 1.35));
    }
  }
  return m;
}
for (let z = Z0 + 1; z <= Z1; z++) {
  for (let x = X0 + 1; x <= X1; x++) {
    if (!free(x, z) || pondSet.has(`${x},${z}`) || x === WX) continue;
    const h = hillH(x, z);
    if (h <= 0) continue;
    if (map.has(`${x},${F},${z}`)) continue;
    for (let i = 0; i < h; i++) set(x, F + i, z, i === h - 1 ? "grass_block" : "dirt");
    const r = (((x * 7 + z * 11) % 5) + 5) % 5;
    if (h >= 2 && r === 0) set(x, F + h, z, "azalea");
    if (h >= 2 && r === 1) set(x, F + h, z, "fern");
  }
}

function stoneLantern(x, z) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) return;
  set(x, F, z, "chiseled_stone_bricks");
  set(x, 64, z, "cobblestone_wall");
  set(x, 65, z, "lantern[hanging=false,waterlogged=false]");
}
stoneLantern(-576, 132);
stoneLantern(-599, 129);

const cx = -572,
  cz = 129;
if (free(cx, cz)) {
  for (let y = 64; y <= 67; y++) set(cx, y, cz, "cherry_log[axis=y]");
  for (let dx = -2; dx <= 2; dx++) {
    for (let dz = -2; dz <= 2; dz++) {
      for (let dy = 3; dy <= 6; dy++) {
        if (Math.abs(dx) + Math.abs(dz) + (dy < 5 ? 1 : 0) > 4) continue;
        if (dx === 0 && dz === 0 && dy <= 4) continue;
        set(cx + dx, F + dy, cz + dz, "cherry_leaves[distance=1,persistent=true,waterlogged=false]");
      }
    }
  }
}

const bamboos = [
  [-561, 128],
  [-561, 130],
  [-562, 129],
  [-561, 133],
  [-562, 135],
];
for (const [x, z] of bamboos) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) continue;
  set(x, 64, z, "bamboo[age=1,leaves=none,stage=0]");
  set(x, 65, z, "bamboo[age=1,leaves=large,stage=0]");
}

for (const [x, z, b] of [
  [-563, 128, "flowering_azalea"],
  [-578, 130, "azalea"],
  [-565, 136, "pink_petals"],
  [-591, 136, "peony[half=lower]"],
]) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) continue;
  if (b.includes("half=lower")) {
    set(x, 64, z, b);
    set(x, 65, z, b.replace("lower", "upper"));
  } else set(x, 64, z, b);
}

const hx0 = -569,
  hx1 = -566,
  hz0 = 134,
  hz1 = 136;
for (let x = hx0; x <= hx1; x++) {
  for (let z = hz0; z <= hz1; z++) {
    if (!free(x, z)) continue;
    set(x, F, z, "stripped_dark_oak_wood");
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
for (let z = hz0; z <= hz1; z++) {
  set(hx0, 66, z, "dark_oak_log[axis=z]");
  set(hx1, 66, z, "dark_oak_log[axis=z]");
}
const ry = 67;
for (let x = hx0 - 1; x <= hx1 + 1; x++) {
  for (let z = hz0 - 1; z <= hz1 + 1; z++) {
    if (z === 137 && x >= -584 && x <= -576) continue;
    set(x, ry, z, "deepslate_tiles");
  }
}
for (let x = hx0 - 1; x <= hx1 + 1; x++) {
  set(x, ry, hz0 - 1, stair("deepslate_tile_stairs", "south"));
}
for (let z = hz0 - 1; z <= hz1 + 1; z++) {
  set(hx0 - 1, ry, z, stair("deepslate_tile_stairs", "east"));
  set(hx1 + 1, ry, z, stair("deepslate_tile_stairs", "west"));
}
if (hz1 + 1 < 137) {
  for (let x = hx0 - 1; x <= hx1 + 1; x++) {
    set(x, ry, hz1 + 1, stair("deepslate_tile_stairs", "north"));
  }
}
set(-567, 66, 135, "lantern[hanging=true,waterlogged=false]");

const blocks = [...map.values()];
console.log(
  `AABB x[${X0}..${X1}] z[${Z0}..${Z1}] y62-70  blocks ${blocks.length}  skip pavilion eaves / sidewalk except gate`
);
await post(blocks);
console.log("done");
