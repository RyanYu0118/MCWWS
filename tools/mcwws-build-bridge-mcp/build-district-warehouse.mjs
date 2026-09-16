/**
 * Modern freight warehouse SE of linear-planning origin (-712,63,30).
 * AABB building x -704..-664, z 38..70, y 63..77
 * Apron z 71..76; keep west of andesite road (x >= -710).
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -704,
  X1 = -664;
const Z0 = 38,
  Z1 = 70;
const F = 63;
const ROOF = 75;
const PARA = 76;

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
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

const stair = (facing, half = "bottom") =>
  `polished_andesite_stairs[facing=${facing},half=${half},shape=straight,waterlogged=false]`;
const door = (facing, half, hinge) =>
  `iron_door[facing=${facing},half=${half},hinge=${hinge},open=false,powered=false]`;

console.log("AABB", { X0, X1, Z0, Z1, F, ROOF, PARA });

const docks = [
  [-700, -698],
  [-690, -688],
  [-680, -678],
  [-670, -668],
];

const detailsOnly = !!process.env.WAREHOUSE_DETAILS_ONLY;

if (!detailsOnly) {
// 1) floor + truck apron (do not air-clear grass outside)
console.log("floor", await postFill(X0, F, Z0, X1, F, Z1, "light_gray_concrete"));
const apron = [];
const mix = ["stone", "stone", "andesite", "cobblestone"];
for (let z = Z1 + 1; z <= Z1 + 6; z++) {
  for (let x = X0; x <= X1; x++) {
    const k = (((x + z * 5) % 4) + 4) % 4;
    apron.push({ x, y: F, z, block: mix[k] });
  }
}
for (let i = 0; i < apron.length; i += 8000) {
  console.log("apron", await postBatch(apron.slice(i, i + 8000)));
}
// west pedestrian strip toward road, stop before andesite (~-718)
console.log("west path", await postFill(X0 - 5, F, 50, X0 - 1, F, 54, "smooth_stone"));

// 2) interior volume air (furniture layer only; walls not yet)
console.log("air", await postFill(X0 + 1, F + 1, Z0 + 1, X1 - 1, ROOF - 1, Z1 - 1, "air"));

// 3) shell walls + roof
console.log("wall N", await postFill(X0, F + 1, Z0, X1, ROOF, Z0, "light_gray_concrete"));
console.log("wall S", await postFill(X0, F + 1, Z1, X1, ROOF, Z1, "light_gray_concrete"));
console.log("wall W", await postFill(X0, F + 1, Z0, X0, ROOF, Z1, "light_gray_concrete"));
console.log("wall E", await postFill(X1, F + 1, Z0, X1, ROOF, Z1, "light_gray_concrete"));
console.log("roof", await postFill(X0, ROOF, Z0, X1, ROOF, Z1, "gray_concrete"));
console.log("parapet N", await postFill(X0, PARA, Z0, X1, PARA, Z0, "light_gray_concrete"));
console.log("parapet S", await postFill(X0, PARA, Z1, X1, PARA, Z1, "light_gray_concrete"));
console.log("parapet W", await postFill(X0, PARA, Z0, X0, PARA, Z1, "light_gray_concrete"));
console.log("parapet E", await postFill(X1, PARA, Z0, X1, PARA, Z1, "light_gray_concrete"));

// waist / base stripes
console.log("base N", await postFill(X0, F + 1, Z0, X1, F + 1, Z0, "gray_concrete"));
console.log("base S", await postFill(X0, F + 1, Z1, X1, F + 1, Z1, "gray_concrete"));
console.log("base W", await postFill(X0, F + 1, Z0, X0, F + 1, Z1, "gray_concrete"));
console.log("base E", await postFill(X1, F + 1, Z0, X1, F + 1, Z1, "gray_concrete"));
console.log("belt N", await postFill(X0, 70, Z0, X1, 70, Z0, "orange_concrete"));
console.log("belt S", await postFill(X0, 70, Z1, X1, 70, Z1, "orange_concrete"));
console.log("belt W", await postFill(X0, 70, Z0, X0, 70, Z1, "orange_concrete"));
console.log("belt E", await postFill(X1, 70, Z0, X1, 70, Z1, "orange_concrete"));

// 4) openings: south docks, north entry, west staff
for (const [a, b] of docks) {
  console.log("dock", await postFill(a, F + 1, Z1, b, F + 5, Z1, "air"));
}
console.log("north door void", await postFill(-687, F + 1, Z0, -684, F + 3, Z0, "air"));
console.log("west door void", await postFill(X0, F + 1, 52, X0, F + 3, 53, "air"));
console.log("clerestory N", await postFill(X0 + 2, 71, Z0, X1 - 2, 73, Z0, "white_stained_glass"));
console.log("clerestory S", await postFill(X0 + 2, 71, Z1, X1 - 2, 73, Z1, "white_stained_glass"));
console.log("clerestory W", await postFill(X0, 71, Z0 + 2, X0, 73, Z1 - 2, "white_stained_glass"));
console.log("clerestory E", await postFill(X1, 71, Z0 + 2, X1, 73, Z1 - 2, "white_stained_glass"));
// restore belt through glass band on long walls except docks later via details
console.log("re-belt N", await postFill(X0, 70, Z0, X1, 70, Z0, "orange_concrete"));
console.log("re-belt S", await postFill(X0, 70, Z1, X1, 70, Z1, "orange_concrete"));

// north office curtain (glass above sill)
console.log("curtain", await postFill(-703, 65, Z0, -695, 68, Z0, "white_stained_glass"));
}

// ----- details map -----
// corner piers (protrude 1)
for (const [x, z] of [
  [X0, Z0],
  [X1, Z0],
  [X0, Z1],
  [X1, Z1],
]) {
  fill(x, F, z, x, PARA + 1, z, "gray_concrete");
}
// extra outer piers / buttresses
for (let x = X0 + 8; x < X1; x += 8) {
  fill(x, F, Z0 - 1, x, 69, Z0 - 1, "gray_concrete");
  fill(x, F, Z1 + 1, x, 69, Z1 + 1, "gray_concrete");
  set(x, 70, Z0 - 1, "lantern[hanging=false,waterlogged=false]");
  set(x, 70, Z1 + 1, "lantern[hanging=false,waterlogged=false]");
}
for (let z = Z0 + 8; z < Z1; z += 8) {
  fill(X0 - 1, F, z, X0 - 1, 69, z, "gray_concrete");
  fill(X1 + 1, F, z, X1 + 1, 69, z, "gray_concrete");
  set(X0 - 1, 70, z, "lantern[hanging=false,waterlogged=false]");
  set(X1 + 1, 70, z, "lantern[hanging=false,waterlogged=false]");
}

// eaves: tag facing opposite of visual outward
for (let x = X0; x <= X1; x++) {
  set(x, 74, Z0 - 1, stair("south")); // visual north
  set(x, 74, Z1 + 1, stair("north")); // visual south
}
for (let z = Z0; z <= Z1; z++) {
  set(X0 - 1, 74, z, stair("east")); // visual west
  set(X1 + 1, 74, z, stair("west")); // visual east
}

// south dock canopy extra
for (let x = X0 + 1; x <= X1 - 1; x++) {
  set(x, 69, Z1 + 1, "gray_concrete");
  set(x, 69, Z1 + 2, stair("north"));
}
// north entry canopy
for (let x = -689; x <= -682; x++) {
  set(x, 67, Z0 - 1, "gray_concrete");
  set(x, 67, Z0 - 2, stair("south"));
}
fill(-689, F, Z0 - 1, -682, F, Z0 - 2, "smooth_stone");
fill(-688, F, Z0 - 3, -683, F, Z0 - 3, stair("south"));

// doors
set(-687, F + 1, Z0, door("south", "lower", "left"));
set(-687, F + 2, Z0, door("south", "upper", "left"));
set(-686, F + 1, Z0, door("south", "lower", "right"));
set(-686, F + 2, Z0, door("south", "upper", "right"));
set(-685, F + 1, Z0, door("south", "lower", "left"));
set(-685, F + 2, Z0, door("south", "upper", "left"));
set(-684, F + 1, Z0, door("south", "lower", "right"));
set(-684, F + 2, Z0, door("south", "upper", "right"));
set(X0, F + 1, 52, door("east", "lower", "left"));
set(X0, F + 2, 52, door("east", "upper", "left"));
set(X0, F + 1, 53, door("east", "lower", "right"));
set(X0, F + 2, 53, door("east", "upper", "right"));

// interior columns + beams (must land on floor)
const colsX = [-696, -688, -680, -672];
const colsZ = [46, 54, 62];
for (const x of colsX) {
  for (const z of colsZ) {
    fill(x, F + 1, z, x, ROOF - 1, z, "iron_block");
  }
}
// beams under roof
for (const z of colsZ) {
  fill(X0 + 1, ROOF - 1, z, X1 - 1, ROOF - 1, z, "polished_andesite");
  for (const x of colsX) set(x, ROOF - 1, z, "iron_block");
}
for (const x of colsX) {
  fill(x, ROOF - 1, Z0 + 1, x, ROOF - 1, Z1 - 1, "polished_andesite");
  for (const z of colsZ) set(x, ROOF - 1, z, "iron_block");
}

// hanging lights every 6
for (let x = X0 + 4; x <= X1 - 4; x += 6) {
  for (let z = Z0 + 4; z <= Z1 - 4; z += 6) {
    set(x, ROOF - 1, z, "light_gray_concrete");
    set(x, ROOF - 2, z, "lantern[hanging=true,waterlogged=false]");
  }
}

// office mezzanine NW
fill(X0 + 1, 69, Z0 + 1, X0 + 10, 69, Z0 + 9, "smooth_stone");
fill(X0 + 1, 70, Z0 + 9, X0 + 10, 73, Z0 + 9, "white_stained_glass");
fill(X0 + 10, 70, Z0 + 1, X0 + 10, 73, Z0 + 8, "white_stained_glass");
// stair up to mezzanine: walk west onto landing; visual west => facing east
for (let i = 0; i < 6; i++) {
  const x = X0 + 16 - i;
  const y = F + 1 + i;
  set(x, y, Z0 + 3, stair("east"));
  set(x, y, Z0 + 4, stair("east"));
}
fill(X0 + 10, 69, Z0 + 3, X0 + 10, 69, Z0 + 4, "smooth_stone");
set(X0 + 4, 70, Z0 + 4, "lectern[facing=south,has_book=false,powered=false]");
fill(X0 + 3, 70, Z0 + 5, X0 + 6, 70, Z0 + 5, "smooth_stone_slab[type=top,waterlogged=false]");
set(X0 + 3, 70, Z0 + 6, "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
set(X0 + 6, 70, Z0 + 6, "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
set(X0 + 2, 70, Z0 + 2, "chest[facing=south,type=single]");
set(X0 + 3, 70, Z0 + 2, "cartography_table");
set(X0 + 8, 70, Z0 + 2, "lantern[hanging=false,waterlogged=false]");

// storage racks: aisles, barrels on floor with iron-bar cages against columns
for (const x of [-700, -692, -684, -676, -668]) {
  for (let z = 48; z <= 64; z += 2) {
    if (colsZ.includes(z) && colsX.includes(x)) continue;
    set(x, F + 1, z, "barrel[facing=up,open=false]");
    if (z % 4 === 0) set(x, F + 2, z, "barrel[facing=up,open=false]");
  }
}
// staging near docks
for (const [a] of docks) {
  set(a, F + 1, Z1 - 2, "chest[facing=north,type=single]");
  set(a + 2, F + 1, Z1 - 2, "barrel[facing=south,open=false]");
  set(a + 1, F + 1, Z1 - 3, "crafting_table");
}

// dock bumpers + safety
for (const [a, b] of docks) {
  fill(a, F, Z1 + 1, b, F, Z1 + 1, "orange_concrete");
  set(a - 1, F + 1, Z1 + 1, "orange_concrete");
  set(b + 1, F + 1, Z1 + 1, "orange_concrete");
}

// east service pipes attached to wall
for (let y = 65; y <= 73; y++) {
  set(X1 + 1, y, 44, "iron_bars");
  set(X1 + 1, y, 58, "iron_bars");
}
set(X1 + 1, 65, 44, "cauldron");
set(X1 + 1, 65, 58, "cauldron");
for (let y = 66; y <= 72; y++) set(X1, y, 50, "ladder[facing=east,waterlogged=false]");

// roof HVAC
function hvac(cx, cz) {
  fill(cx - 1, PARA, cz - 1, cx + 1, PARA, cz + 1, "iron_block");
  set(cx, PARA + 1, cz, "smooth_stone");
  set(cx, PARA + 2, cz, "lightning_rod");
  set(cx + 1, PARA + 1, cz, "iron_trapdoor");
  set(cx - 1, PARA + 1, cz, "iron_trapdoor");
}
hvac(-696, 46);
hvac(-680, 54);
hvac(-672, 62);

// roof corner antennas
set(X0, PARA + 1, Z0, "end_rod[facing=up]");
set(X1, PARA + 1, Z0, "end_rod[facing=up]");
set(X0, PARA + 1, Z1, "end_rod[facing=up]");
set(X1, PARA + 1, Z1, "end_rod[facing=up]");

// west staff canopy
fill(X0 - 2, F, 51, X0 - 1, F, 54, "smooth_stone");
for (let z = 51; z <= 54; z++) {
  set(X0 - 1, 67, z, "gray_concrete");
  set(X0 - 2, 67, z, stair("east"));
}

// dumpster / dirty east-south corner
fill(X1 + 2, F, Z1 - 2, X1 + 4, F, Z1, "gray_concrete");
set(X1 + 3, F + 1, Z1 - 1, "cauldron");
set(X1 + 2, F + 1, Z1, "composter");
set(X1 + 4, F + 1, Z1, "barrel[facing=west,open=false]");

const blocks = [...map.values()];
console.log("detail blocks", blocks.length);
for (let i = 0; i < blocks.length; i += 8000) {
  console.log("batch", i, await postBatch(blocks.slice(i, i + 8000)));
}
console.log("done");
