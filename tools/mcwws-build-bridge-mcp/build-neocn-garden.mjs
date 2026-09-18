/**
 * Neo-Chinese garden NE of (-606,63,164), north of the ancient street.
 * AABB x -604..-576, z 136..162, y 62..71
 * Skip street bricks, west leaf line (x<=-607), mixed road.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -604,
  X1 = -576;
const Z0 = 136,
  Z1 = 162;
const F = 63;

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, x, y, z }),
  });
  const j = await r.json();
  return (j.block || "").replace(/^minecraft:/, "").split("[")[0];
}

async function postFill(x1, y1, z1, x2, y2, z2, block) {
  const res = await fetch(`${BASE}/fill`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, x1, y1, z1, x2, y2, z2, block }),
  });
  const json = await res.json();
  if (!res.ok || json.ok === false) throw new Error(json.error || JSON.stringify(json));
  return json;
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

const FORBIDDEN = new Set([
  "stone_bricks",
  "moss_block",
  "dark_oak_planks",
  "spruce_planks",
  "oak_leaves",
  "polished_andesite_slab",
  "polished_andesite",
  "oak_log",
  "dark_oak_log",
  "spruce_log",
]);

const occ = new Set();
console.log("scan occupancy...");
for (let z = Z0 - 1; z <= Z1 + 1; z++) {
  for (let x = X0 - 1; x <= X1 + 1; x++) {
    const b63 = await get(x, F, z);
    const b64 = await get(x, F + 1, z);
    if (FORBIDDEN.has(b63) || FORBIDDEN.has(b64)) occ.add(`${x},${z}`);
    if (x <= -607) occ.add(`${x},${z}`);
  }
}
console.log("protected cells", occ.size);

function free(x, z) {
  return x >= X0 && x <= X1 && z >= Z0 && z <= Z1 && !occ.has(`${x},${z}`);
}

const stair = (mat, facing, half = "bottom") =>
  `${mat}[facing=${facing},half=${half},shape=straight,waterlogged=false]`;

// --- pond (irregular) ---
const pond = [];
const pcx = -590,
  pcz = 150;
for (let z = pcz - 5; z <= pcz + 5; z++) {
  for (let x = pcx - 8; x <= pcx + 6; x++) {
    const dx = (x - pcx) / 7.2;
    const dz = (z - pcz) / 4.4;
    const n = Math.sin(x * 1.7 + z * 0.9) * 0.12;
    if (dx * dx + dz * dz + n < 1 && free(x, z)) pond.push([x, z]);
  }
}
const pondSet = new Set(pond.map(([x, z]) => `${x},${z}`));

// --- south moon-gate wall (facing street) ---
for (let x = -601; x <= -580; x++) {
  if (!free(x, Z1)) continue;
  const gate = x >= -594 && x <= -590;
  if (gate) continue;
  set(x, F, Z1, "calcite");
  set(x, F + 1, Z1, "calcite");
  set(x, F + 2, Z1, "calcite");
  set(x, F + 3, Z1, "dark_oak_slab[type=bottom,waterlogged=false]");
}
// moon gate circle at x=-592 center, z=Z1
for (const [x, y] of [
  [-595, 64],
  [-595, 65],
  [-594, 66],
  [-593, 67],
  [-592, 67],
  [-591, 67],
  [-590, 66],
  [-589, 65],
  [-589, 64],
]) {
  if (free(x, Z1)) set(x, y, Z1, "polished_deepslate");
}
set(-594, F, Z1, "calcite");
set(-590, F, Z1, "calcite");
set(-592, F, Z1, "smooth_stone");
set(-593, F, Z1, "smooth_stone");
set(-591, F, Z1, "smooth_stone");

// --- pond bottom / water / rim ---
for (const [x, z] of pond) {
  set(x, 62, z, "clay");
  set(x, F, z, "water");
}
for (const [x, z] of pond) {
  for (const [dx, dz] of [
    [1, 0],
    [-1, 0],
    [0, 1],
    [0, -1],
  ]) {
    const nx = x + dx,
      nz = z + dz;
    if (!pondSet.has(`${nx},${nz}`) && free(nx, nz)) {
      const rim = (nx + nz) % 3 === 0 ? "mossy_cobblestone" : (nx + nz) % 3 === 1 ? "cobblestone" : "andesite";
      set(nx, F, nz, rim);
    }
  }
}
let lily = 0;
for (const [x, z] of pond) {
  if ((x + z) % 5 === 0) {
    set(x, F + 1, z, "lily_pad");
    lily++;
  }
}

// --- zigzag deck ---
const path = [
  [-592, 161],
  [-592, 159],
  [-591, 157],
  [-589, 156],
  [-587, 155],
  [-586, 153],
  [-587, 151],
  [-585, 149],
  [-583, 147],
  [-582, 145],
  [-582, 143],
  [-581, 141],
];
for (const [x, z] of path) {
  if (pondSet.has(`${x},${z}`)) {
    set(x, F + 1, z, "spruce_slab[type=bottom,waterlogged=true]");
    set(x, F, z, "spruce_fence[waterlogged=true]");
  } else if (free(x, z)) {
    set(x, F, z, (x + z) % 2 === 0 ? "stone" : "andesite");
  }
}

// --- pavilion 5x5 at NE ---
const px0 = -583,
  px1 = -577,
  pz0 = 138,
  pz1 = 144;
for (let x = px0; x <= px1; x++) {
  for (let z = pz0; z <= pz1; z++) {
    if (!free(x, z)) continue;
    set(x, F, z, "stripped_dark_oak_wood");
  }
}
const posts = [
  [px0, pz0],
  [px1, pz0],
  [px0, pz1],
  [px1, pz1],
];
for (const [x, z] of posts) {
  for (let y = F + 1; y <= F + 4; y++) set(x, y, z, "dark_oak_log[axis=y]");
}
for (let x = px0; x <= px1; x++) {
  set(x, F + 4, pz0, "dark_oak_log[axis=x]");
  set(x, F + 4, pz1, "dark_oak_log[axis=x]");
}
for (let z = pz0; z <= pz1; z++) {
  set(px0, F + 4, z, "dark_oak_log[axis=z]");
  set(px1, F + 4, z, "dark_oak_log[axis=z]");
}
for (const [x, z] of posts) set(x, F + 4, z, "dark_oak_log[axis=y]");

const roofY = F + 5;
for (let x = px0 - 1; x <= px1 + 1; x++) {
  for (let z = pz0 - 1; z <= pz1 + 1; z++) {
    set(x, roofY, z, "deepslate_tiles");
  }
}
for (let x = px0; x <= px1; x++) {
  for (let z = pz0; z <= pz1; z++) set(x, roofY + 1, z, "deepslate_tiles");
}
set(-580, roofY + 2, 141, "deepslate_tiles");
set(-580, roofY + 3, 141, "lantern[hanging=false,waterlogged=false]");

// eaves: visual outward => write opposite facing
for (let x = px0 - 1; x <= px1 + 1; x++) {
  set(x, roofY, pz0 - 1, stair("deepslate_tile_stairs", "south"));
  set(x, roofY, pz1 + 1, stair("deepslate_tile_stairs", "north"));
}
for (let z = pz0 - 1; z <= pz1 + 1; z++) {
  set(px0 - 1, roofY, z, stair("deepslate_tile_stairs", "east"));
  set(px1 + 1, roofY, z, stair("deepslate_tile_stairs", "west"));
}
set(-580, roofY - 1, 141, "lantern[hanging=true,waterlogged=false]");
set(-580, F + 1, 141, "dark_oak_slab[type=top,waterlogged=false]");
// stools around table: sit facing table. table at 141, chairs:
// north of table z=140 sit south visual => facing north
set(-581, F + 1, 140, stair("dark_oak_stairs", "north"));
set(-579, F + 1, 140, stair("dark_oak_stairs", "north"));
set(-581, F + 1, 142, stair("dark_oak_stairs", "south"));
set(-579, F + 1, 142, stair("dark_oak_stairs", "south"));

// --- stone lanterns (wall attached to cobble) ---
function stoneLantern(x, z) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) return;
  set(x, F, z, "chiseled_stone_bricks");
  set(x, F + 1, z, "cobblestone_wall");
  set(x, F + 2, z, "lantern[hanging=false,waterlogged=false]");
}
stoneLantern(-598, 160);
stoneLantern(-585, 158);
stoneLantern(-596, 145);
stoneLantern(-578, 148);

// --- rockery NW ---
const rocks = [
  [-602, 140, "andesite"],
  [-601, 140, "mossy_cobblestone"],
  [-602, 141, "cobblestone"],
  [-600, 141, "andesite"],
  [-601, 142, "moss_block"],
  [-603, 141, "tuff"],
];
for (const [x, z, b] of rocks) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) continue;
  set(x, F, z, b);
  if ((x + z) % 2 === 0) set(x, F + 1, z, b);
}
set(-601, F + 2, 140, "andesite");
set(-601, F + 3, 140, "pointed_dripstone[thickness=tip,vertical_direction=up,waterlogged=false]");
set(-602, F + 1, 139, "azalea");
set(-600, F + 1, 142, "flowering_azalea");

// --- bamboo east belt ---
for (let z = 146; z <= 156; z += 1) {
  for (const x of [-577, -578]) {
    if (!free(x, z) || pondSet.has(`${x},${z}`)) continue;
    if ((x + z) % 3 === 0) continue;
    set(x, F, z, "grass_block");
    const h = 3 + ((x + z) % 4);
    for (let y = F + 1; y <= F + h; y++) {
      const leaves = y >= F + h - 1 ? "large" : "none";
      set(x, y, z, `bamboo[age=1,leaves=${leaves},stage=1]`);
    }
  }
}

// --- cherry accent ---
function cherry(cx, cz) {
  if (!free(cx, cz)) return;
  for (let y = F + 1; y <= F + 4; y++) set(cx, y, cz, "cherry_log[axis=y]");
  for (let dx = -2; dx <= 2; dx++) {
    for (let dz = -2; dz <= 2; dz++) {
      for (let dy = 3; dy <= 6; dy++) {
        if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 5) > 4) continue;
        if (dx === 0 && dz === 0 && dy <= 4) continue;
        set(cx + dx, F + dy, cz + dz, "cherry_leaves[distance=1,persistent=true,waterlogged=false]");
      }
    }
  }
}
cherry(-598, 138);

// --- shrubs / petals ---
const shrubs = [
  [-596, 160, "azalea"],
  [-588, 160, "flowering_azalea"],
  [-599, 154, "lilac[half=lower]"],
  [-586, 138, "peony[half=lower]"],
  [-595, 143, "pink_petals"],
  [-588, 146, "pink_petals"],
  [-584, 152, "moss_carpet"],
  [-597, 148, "moss_carpet"],
];
for (const [x, z, b] of shrubs) {
  if (!free(x, z) || pondSet.has(`${x},${z}`)) continue;
  if (b.includes("half=lower")) {
    set(x, F + 1, z, b);
    set(x, F + 2, z, b.replace("lower", "upper"));
  } else {
    set(x, F + 1, z, b);
  }
}

console.log("AABB", { X0, X1, Z0, Z1, pond: pond.length, details: map.size });
const blocks = [...map.values()];
for (let i = 0; i < blocks.length; i += 8000) {
  console.log("batch", i, await postBatch(blocks.slice(i, i + 8000)));
}

// hanging lanterns must sit under solid roof
console.log("qa roof lantern", await get(-580, 67, 141), await get(-580, 68, 141));
console.log("done");
