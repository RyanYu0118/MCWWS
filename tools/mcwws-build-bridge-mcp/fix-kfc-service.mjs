/**
 * KFC interior service fix:
 * 1) Carpet on top of solid floor (same walk height as concrete)
 * 2) Clear cashier counters facing dining hall
 * 3) Partition wall between dining and kitchen + kitchen fit-out
 *
 * Footprint x -556..-528, z 210..226; floor F=64
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -556,
  X1 = -528;
const Z0 = 210,
  Z1 = 226;
const F = 64;
const doorX = -542;
const wallZ = 218; // partition dining | kitchen
const kitZ0 = 219;
const kitZ1 = Z1 - 1; // 225

const map = new Map();
function set(x, y, z, block) {
  // interior only; do not rewrite exterior shell at X0/X1/Z1
  if (x < X0 + 1 || x > X1 - 1 || z < Z0 || z > Z1 - 1) return;
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

// ---- clear furniture / old counter layers inside (keep outer walls at X0/X1/Z0/Z1) ----
fill(X0 + 1, F + 1, Z0 + 1, X1 - 1, F + 3, Z1 - 1, "air");

// ---- floors (solid same height) ----
fill(X0 + 1, F, Z0 + 1, X1 - 1, F, wallZ - 1, "white_concrete"); // dining
fill(X0 + 1, F, wallZ, X1 - 1, F, wallZ, "white_concrete"); // under partition
fill(X0 + 1, F, kitZ0, X1 - 1, F, kitZ1, "smooth_stone"); // kitchen

// aisle carpet ON TOP of solid floor (F+1) — no trip vs concrete
fill(doorX - 1, F + 1, Z0 + 1, doorX + 1, F + 1, wallZ - 1, "red_carpet");

// ---- partition wall dining ↔ kitchen ----
fill(X0 + 1, F + 1, wallZ, X1 - 1, F + 3, wallZ, "white_concrete");
fill(X0 + 1, F + 4, wallZ, X1 - 1, F + 4, wallZ, "red_concrete"); // brand stripe

// cashier openings: three service windows with counter tops
const registers = [doorX - 6, doorX, doorX + 6]; // -548, -542, -536
for (const rx of registers) {
  // window opening 3 wide × 2 tall
  fill(rx - 1, F + 1, wallZ, rx + 1, F + 2, wallZ, "air");
  // counter base + top (cashier facing south into dining)
  fill(rx - 1, F + 1, wallZ, rx + 1, F + 1, wallZ, "smooth_quartz");
  fill(rx - 1, F + 2, wallZ, rx + 1, F + 2, wallZ, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  // POS / cash register props on kitchen side of counter
  set(rx, F + 2, wallZ + 1, "lectern[facing=south,has_book=false,powered=false]");
  // order tray / barrel under counter kitchen side
  set(rx - 1, F + 1, wallZ + 1, "barrel[facing=up,open=false]");
  set(rx + 1, F + 1, wallZ + 1, "barrel[facing=up,open=false]");
}

// staff door (east end of partition)
fill(X1 - 3, F + 1, wallZ, X1 - 2, F + 2, wallZ, "air");
set(X1 - 3, F + 1, wallZ, "oak_door[facing=west,half=lower,hinge=left,open=false,powered=false]");
set(X1 - 3, F + 2, wallZ, "oak_door[facing=west,half=upper,hinge=left,open=false,powered=false]");
// light above door
set(X1 - 2, F + 3, wallZ, "lantern[hanging=false,waterlogged=false]");

// ---- dining tables (same layout as before, chairs face table) ----
function placeTable(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  set(tx, F + 1, tz + 1, "oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
  set(tx, F + 1, tz - 1, "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
}
for (const tx of [-552, -549]) {
  for (const tz of [212, 215]) placeTable(tx, tz);
}
for (const tx of [-535, -532]) {
  for (const tz of [212, 215]) placeTable(tx, tz);
}

// menu boards on partition between register windows (dining-facing face is the wall itself)
for (const bx of [doorX - 9, doorX - 3, doorX + 3, doorX + 9]) {
  set(bx, F + 2, wallZ, "brown_concrete");
  set(bx, F + 3, wallZ, "brown_concrete");
}

// ---- kitchen fit-out ----
// back wall cook line
for (let x = X0 + 3; x <= X1 - 3; x += 2) {
  set(x, F + 1, kitZ1, "smoker[facing=south,lit=false]");
}
// prep counters along west/east kitchen walls
fill(X0 + 2, F + 1, kitZ0 + 1, X0 + 2, F + 1, kitZ1 - 1, "iron_block");
fill(X1 - 2, F + 1, kitZ0 + 1, X1 - 2, F + 1, kitZ1 - 1, "iron_block");
fill(X0 + 2, F + 2, kitZ0 + 1, X0 + 2, F + 2, kitZ1 - 1, "iron_trapdoor[facing=east,half=top,open=false,powered=false,waterlogged=false]");
fill(X1 - 2, F + 2, kitZ0 + 1, X1 - 2, F + 2, kitZ1 - 1, "iron_trapdoor[facing=west,half=top,open=false,powered=false,waterlogged=false]");
// fridge / storage
set(X0 + 3, F + 1, kitZ0 + 1, "iron_door[facing=east,half=lower,hinge=left,open=false,powered=false]");
set(X0 + 3, F + 2, kitZ0 + 1, "iron_door[facing=east,half=upper,hinge=left,open=false,powered=false]");
set(X0 + 4, F + 1, kitZ0 + 1, "barrel[facing=south,open=false]");
set(X0 + 5, F + 1, kitZ0 + 1, "barrel[facing=south,open=false]");
// pass shelf center kitchen
fill(doorX - 2, F + 1, kitZ0 + 2, doorX + 2, F + 1, kitZ0 + 2, "smooth_quartz");
fill(doorX - 2, F + 2, kitZ0 + 2, doorX + 2, F + 2, kitZ0 + 2, "smooth_quartz_slab[type=bottom,waterlogged=false]");
// kitchen lighting
for (let x = X0 + 5; x <= X1 - 5; x += 5) {
  set(x, F + 5, 222, "sea_lantern");
}

// keep door opening clear (south)
fill(doorX - 1, F + 1, Z0, doorX + 1, F + 2, Z0, "air");
// carpet should not sit in door threshold outside — already starts Z0+1

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
  if (!res.ok || json.ok === false) throw new Error(json.error || res.statusText);
  return json;
}

const all = [...map.values()];
all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1) || a.y - b.y);
console.log("placements", all.length);
for (let i = 0; i < all.length; i += 4000) {
  console.log(await postBatch(all.slice(i, i + 4000)));
}
console.log("service interior done");
