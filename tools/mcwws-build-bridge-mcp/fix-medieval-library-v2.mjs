/**
 * Fix medieval library per user feedback:
 * 1) facade depth: eaves, wall lanterns, frames
 * 2) sparse hanging lights every 6 (even) blocks
 * 3) columns under beams
 * 4) zoned interior + designed desks
 * 5) exterior stairs at north door
 *
 * AABB x -4168..-4104, z -1328..-1286, floor ~65
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -4168,
  X1 = -4104;
const Z0 = -1328,
  Z1 = -1286;
const F = 65;
const WALL = 75;
const doorX = -4136;
const ridgeZ = Math.floor((Z0 + Z1) / 2); // -1307

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
  if (!res.ok || json.ok === false) throw new Error(json.error || res.statusText);
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
  if (!res.ok || json.ok === false) throw new Error(json.error || res.statusText);
  return json;
}

// ----- 2) strip excess lanterns inside (FAWE replace in interior) -----
async function fawe(path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  const json = await res.json();
  if (!res.ok || json.ok === false) throw new Error(json.error || res.statusText);
  return json;
}
console.log(
  "fawe pos1",
  await fawe("/fawe_pos", { which: "pos1", world, x: X0 + 1, y: F + 5, z: Z0 + 1 })
);
console.log(
  "fawe pos2",
  await fawe("/fawe_pos", { which: "pos2", world, x: X1 - 1, y: WALL + 10, z: Z1 - 1 })
);
try {
  console.log("fawe replace lantern", await fawe("/fawe_replace", { from: "lantern", to: "air" }));
} catch (e) {
  console.log("fawe replace skipped:", e.message);
}


// clear old simple furniture (interior only, keep outer walls)
console.log(
  "clear furniture...",
  await postFill(X0 + 1, F + 1, Z0 + 1, X1 - 1, F + 4, Z1 - 1, "air")
);
// keep gallery floor at F+5 — clear only desks on F+1..F+4 already
// clear old center clutter up to gallery
console.log(
  "clear mid...",
  await postFill(X0 + 5, F + 1, Z0 + 4, X1 - 5, WALL - 2, Z1 - 4, "air")
);

// ----- 5) exterior stairs + porch at north door -----
// porch landing outside door (z = Z0-1 = -1329)
fill(doorX - 4, 64, Z0 - 1, doorX + 4, 64, Z0 - 1, "stone_bricks");
fill(doorX - 4, 64, Z0 - 2, doorX + 4, 64, Z0 - 2, "stone_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
fill(doorX - 3, 63, Z0 - 3, doorX + 3, 63, Z0 - 3, "stone_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
// ensure door sits on sill at F (65) with solid under
fill(doorX - 2, F - 1, Z0, doorX + 2, F - 1, Z0, "stone_bricks");
fill(doorX - 2, F, Z0, doorX + 2, F + 3, Z0, "air");
set(doorX - 1, F, Z0, "spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]");
set(doorX - 1, F + 1, Z0, "spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]");
set(doorX, F, Z0, "spruce_door[facing=south,half=lower,hinge=right,open=false,powered=false]");
set(doorX, F + 1, Z0, "spruce_door[facing=south,half=upper,hinge=right,open=false,powered=false]");
set(doorX + 1, F, Z0, "air");
set(doorX + 1, F + 1, Z0, "air");
// carpet from door
fill(doorX - 1, F + 1, Z0 + 1, doorX + 1, F + 1, Z0 + 6, "red_carpet");

// ----- 1) facade depth: eaves + wall lanterns + window frames -----
// eaves ring (stairs facing outward)
for (let x = X0; x <= X1; x++) {
  set(x, WALL, Z0 - 1, "stone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
  set(x, WALL, Z1 + 1, "stone_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
  set(x, WALL + 1, Z0 - 1, "stone_brick_slab[type=bottom,waterlogged=false]");
  set(x, WALL + 1, Z1 + 1, "stone_brick_slab[type=bottom,waterlogged=false]");
}
for (let z = Z0; z <= Z1; z++) {
  set(X0 - 1, WALL, z, "stone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
  set(X1 + 1, WALL, z, "stone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
  set(X0 - 1, WALL + 1, z, "stone_brick_slab[type=bottom,waterlogged=false]");
  set(X1 + 1, WALL + 1, z, "stone_brick_slab[type=bottom,waterlogged=false]");
}
// wall lanterns every 6 on even-ish posts (facade)
for (let x = X0 + 6; x <= X1 - 6; x += 6) {
  set(x, F + 3, Z0 - 1, "lantern[hanging=false,waterlogged=false]");
  set(x, F + 2, Z0 - 1, "cobblestone_wall");
  set(x, F + 3, Z1 + 1, "lantern[hanging=false,waterlogged=false]");
  set(x, F + 2, Z1 + 1, "cobblestone_wall");
}
for (let z = Z0 + 6; z <= Z1 - 6; z += 6) {
  set(X0 - 1, F + 3, z, "lantern[hanging=false,waterlogged=false]");
  set(X0 - 1, F + 2, z, "cobblestone_wall");
  set(X1 + 1, F + 3, z, "lantern[hanging=false,waterlogged=false]");
  set(X1 + 1, F + 2, z, "cobblestone_wall");
}
// window hoods (protruding stairs above windows on north)
for (let x = X0 + 6; x <= X1 - 6; x += 6) {
  if (Math.abs(x - doorX) < 4) continue;
  set(x, F + 8, Z0 - 1, "stone_brick_stairs[facing=north,half=top,shape=straight,waterlogged=false]");
  set(x - 1, F + 7, Z0, "stone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
  set(x + 1, F + 7, Z0, "stone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
}

// ----- 3) columns under beam grid -----
const colXs = [];
const colZs = [];
for (let x = X0 + 8; x < X1; x += 8) colXs.push(x);
for (let z = Z0 + 8; z < Z1; z += 8) colZs.push(z);
for (const x of colXs) {
  for (const z of colZs) {
    // skip very center for open hall feel — leave 1 bay open
    if (Math.abs(x - doorX) <= 2 && Math.abs(z - ridgeZ) <= 2) continue;
    for (let y = F + 1; y <= WALL - 1; y++) set(x, y, z, "spruce_log[axis=y]");
    // capital
    set(x, WALL - 1, z, "spruce_log[axis=y]");
    set(x, F + 1, z, "stripped_spruce_log[axis=y]");
  }
}

// re-place cross beams on columns
for (const z of colZs) {
  fill(X0 + 1, WALL - 1, z, X1 - 1, WALL - 1, z, "spruce_log[axis=x]");
}
for (const x of colXs) {
  fill(x, WALL - 1, Z0 + 1, x, WALL - 1, Z1 - 1, "spruce_log[axis=z]");
}

// ----- 2) hanging lanterns every 6 on even grid under beams -----
for (const x of colXs) {
  for (const z of colZs) {
    if ((x + z) % 12 !== 0) continue; // sparse among column intersections
    set(x, WALL - 2, z, "lantern[hanging=true,waterlogged=false]");
  }
}
// a few along center aisle under ridge (every 6)
for (let x = doorX - 18; x <= doorX + 18; x += 6) {
  set(x, WALL + 8, ridgeZ, "lantern[hanging=true,waterlogged=false]");
}

// ----- 4) zoned interior -----
// A) vestibule (north) — low divider
fill(X0 + 6, F + 1, Z0 + 7, doorX - 4, F + 3, Z0 + 7, "spruce_fence");
fill(doorX + 4, F + 1, Z0 + 7, X1 - 6, F + 3, Z0 + 7, "spruce_fence");
set(doorX - 4, F + 1, Z0 + 7, "spruce_fence_gate[facing=east,in_wall=false,open=false,powered=false]");
set(doorX + 4, F + 1, Z0 + 7, "spruce_fence_gate[facing=west,in_wall=false,open=false,powered=false]");
fill(doorX - 3, F + 1, Z0 + 1, doorX + 3, F + 1, Z0 + 6, "red_carpet");

// B) west stacks — bookcases perpendicular to wall (medieval style)
for (let z = Z0 + 10; z <= ridgeZ - 4; z += 5) {
  fill(X0 + 2, F + 1, z, X0 + 8, F + 3, z, "bookshelf");
  fill(X0 + 2, F + 1, z + 1, X0 + 2, F + 3, z + 1, "bookshelf");
  // end panel + lectern desk facing aisle (east)
  set(X0 + 9, F + 1, z, "spruce_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
  set(X0 + 9, F + 2, z, "oak_slab[type=bottom,waterlogged=false]");
  set(X0 + 10, F + 1, z, "lectern[facing=east,has_book=false,powered=false]");
}

// C) east stacks
for (let z = Z0 + 10; z <= ridgeZ - 4; z += 5) {
  fill(X1 - 8, F + 1, z, X1 - 2, F + 3, z, "bookshelf");
  fill(X1 - 2, F + 1, z + 1, X1 - 2, F + 3, z + 1, "bookshelf");
  set(X1 - 9, F + 1, z, "spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
  set(X1 - 9, F + 2, z, "oak_slab[type=bottom,waterlogged=false]");
  set(X1 - 10, F + 1, z, "lectern[facing=west,has_book=false,powered=false]");
}

// D) reading hall — designed desks (fence + slab + carpet + trapdoor backs)
function readingDesk(tx, tz) {
  // table: two fences + slabs
  set(tx, F + 1, tz, "oak_fence");
  set(tx + 1, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "oak_slab[type=bottom,waterlogged=false]");
  set(tx + 1, F + 2, tz, "oak_slab[type=bottom,waterlogged=false]");
  set(tx, F + 3, tz, "brown_carpet");
  set(tx + 1, F + 3, tz, "flower_pot");
  // chairs north of table: visual toward south → facing=north (MC invert)
  set(tx, F + 1, tz - 1, "oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
  set(tx + 1, F + 1, tz - 1, "oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
  set(tx, F + 2, tz - 1, "spruce_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]");
  set(tx + 1, F + 2, tz - 1, "spruce_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]");
  // chairs south: visual toward north → facing=south
  set(tx, F + 1, tz + 1, "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
  set(tx + 1, F + 1, tz + 1, "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
  set(tx, F + 2, tz + 1, "spruce_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]");
  set(tx + 1, F + 2, tz + 1, "spruce_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]");
}
for (let x = doorX - 14; x <= doorX + 12; x += 7) {
  for (let z = ridgeZ - 2; z <= ridgeZ + 8; z += 6) {
    if (Math.abs(x - doorX) < 3) continue;
    readingDesk(x, z);
  }
}

// E) scriptorium / enchant zone at south end
fill(doorX - 6, F + 1, Z1 - 8, doorX + 6, F + 1, Z1 - 3, "dark_oak_planks");
fill(doorX - 5, F + 1, Z1 - 7, doorX + 5, F + 3, Z1 - 7, "bookshelf");
fill(doorX - 5, F + 1, Z1 - 4, doorX + 5, F + 3, Z1 - 4, "bookshelf");
set(doorX, F + 2, Z1 - 5, "enchanting_table");
set(doorX - 2, F + 2, Z1 - 5, "lectern[facing=north,has_book=false,powered=false]");
set(doorX + 2, F + 2, Z1 - 5, "lectern[facing=north,has_book=false,powered=false]");
set(doorX - 3, F + 2, Z1 - 6, "barrel[facing=up,open=false]");
set(doorX + 3, F + 2, Z1 - 6, "barrel[facing=up,open=false]");
fill(doorX - 1, F + 1, Z1 - 8, doorX + 1, F + 1, Z1 - 8, "purple_carpet");

// restore gallery edges if cleared
fill(X0 + 1, F + 5, Z0 + 1, X0 + 4, F + 5, Z1 - 1, "spruce_planks");
fill(X1 - 4, F + 5, Z0 + 1, X1 - 1, F + 5, Z1 - 1, "spruce_planks");
for (let z = Z0 + 1; z <= Z1 - 1; z++) {
  set(X0 + 4, F + 6, z, "spruce_fence");
  set(X1 - 4, F + 6, z, "spruce_fence");
}

const all = [...map.values()];
all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1) || a.y - b.y);
console.log("placements", all.length);
for (let i = 0; i < all.length; i += 8000) {
  console.log(await postBatch(all.slice(i, i + 8000)));
}
console.log("library polish done");
