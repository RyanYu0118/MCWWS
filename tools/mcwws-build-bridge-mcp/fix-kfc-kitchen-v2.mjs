/**
 * KFC kitchen + seating polish:
 * - no floating lights / lecterns (support or hang from ceiling)
 * - dual staff doors
 * - richer kitchen fit-out
 * - chair stairs facing corrected (back away from table)
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
const CEIL = 71; // smooth_quartz roof underside sits on y=71
const doorX = -542;
const wallZ = 218;
const kitZ0 = 219;
const kitZ1 = 225;

const map = new Map();
function set(x, y, z, block) {
  if (x < X0 + 1 || x > X1 - 1 || z < Z0 + 1 || z > Z1 - 1) return;
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

// ========== clear kitchen volume + dining furniture layer ==========
fill(X0 + 1, F + 1, kitZ0, X1 - 1, F + 5, kitZ1, "air");
// clear old floating sea lanterns in kitchen column
fill(X0 + 1, F + 5, kitZ0, X1 - 1, CEIL - 1, kitZ1, "air");
// clear dining chairs/tables/carpet (will rebuild); keep floor
fill(X0 + 1, F + 1, Z0 + 1, X1 - 1, F + 3, wallZ - 1, "air");
// clear partition + old cashier props on kitchen side of wall
fill(X0 + 1, F + 1, wallZ, X1 - 1, F + 4, wallZ, "air");
fill(X0 + 1, F + 1, wallZ + 1, X1 - 1, F + 3, wallZ + 1, "air");

// floors
fill(X0 + 1, F, Z0 + 1, X1 - 1, F, wallZ, "white_concrete");
fill(X0 + 1, F, kitZ0, X1 - 1, F, kitZ1, "smooth_stone");
// kitchen floor accent strip
fill(X0 + 1, F, wallZ + 1, X1 - 1, F, wallZ + 1, "polished_andesite");

// aisle carpet on solid floor
fill(doorX - 1, F + 1, Z0 + 1, doorX + 1, F + 1, wallZ - 1, "red_carpet");

// ========== partition + dual staff doors ==========
fill(X0 + 1, F + 1, wallZ, X1 - 1, F + 3, wallZ, "white_concrete");
fill(X0 + 1, F + 4, wallZ, X1 - 1, F + 4, wallZ, "red_concrete");

function staffDoor(x, hinge) {
  // door in partition, opens into kitchen (north); facing south = front toward dining
  fill(x, F + 1, wallZ, x, F + 2, wallZ, "air");
  set(
    x,
    F + 1,
    wallZ,
    `oak_door[facing=south,half=lower,hinge=${hinge},open=false,powered=false]`
  );
  set(
    x,
    F + 2,
    wallZ,
    `oak_door[facing=south,half=upper,hinge=${hinge},open=false,powered=false]`
  );
}
staffDoor(X0 + 3, "left"); // west staff door
staffDoor(X1 - 3, "right"); // east staff door
// small lights above doors (attached to wall, not floating)
set(X0 + 3, F + 3, wallZ, "lantern[hanging=false,waterlogged=false]");
set(X1 - 3, F + 3, wallZ, "lantern[hanging=false,waterlogged=false]");

// ========== three cashier windows with supported POS ==========
const registers = [doorX - 6, doorX, doorX + 6];
for (const rx of registers) {
  fill(rx - 1, F + 1, wallZ, rx + 1, F + 2, wallZ, "air");
  // front counter in wall plane
  fill(rx - 1, F + 1, wallZ, rx + 1, F + 1, wallZ, "smooth_quartz");
  fill(
    rx - 1,
    F + 2,
    wallZ,
    rx + 1,
    F + 2,
    wallZ,
    "smooth_quartz_slab[type=bottom,waterlogged=false]"
  );
  // kitchen-side desk under lectern (no float)
  fill(rx - 1, F + 1, wallZ + 1, rx + 1, F + 1, wallZ + 1, "smooth_quartz");
  set(rx, F + 2, wallZ + 1, "lectern[facing=south,has_book=false,powered=false]");
  set(rx - 1, F + 2, wallZ + 1, "stone_button[face=floor,facing=south,powered=false]");
  set(rx + 1, F + 2, wallZ + 1, "barrel[facing=up,open=false]");
}
// menu boards between windows
for (const bx of [doorX - 9, doorX - 3, doorX + 3, doorX + 9]) {
  set(bx, F + 2, wallZ, "brown_concrete");
  set(bx, F + 3, wallZ, "brown_concrete");
}

// ========== dining tables — chairs face table correctly ==========
// Stairs "facing" = full-block back face direction. Back should be away from table.
function placeTable(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  // south of table → back faces south
  set(
    tx,
    F + 1,
    tz + 1,
    "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]"
  );
  // north of table → back faces north
  set(
    tx,
    F + 1,
    tz - 1,
    "oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"
  );
}
for (const tx of [-552, -549]) {
  for (const tz of [212, 215]) placeTable(tx, tz);
}
for (const tx of [-535, -532]) {
  for (const tz of [212, 215]) placeTable(tx, tz);
}

// ========== kitchen fit-out ==========
// North cook line against exterior wall (z=225): solid counter + appliances
fill(X0 + 2, F + 1, kitZ1, X1 - 2, F + 1, kitZ1, "iron_block");
for (let x = X0 + 3; x <= X1 - 3; x++) {
  if ((x - (X0 + 3)) % 3 === 0) {
    set(x, F + 1, kitZ1, "smoker[facing=south,lit=false]");
  } else if ((x - (X0 + 3)) % 3 === 1) {
    set(x, F + 1, kitZ1, "furnace[facing=south,lit=false]");
  } else {
    set(x, F + 1, kitZ1, "blast_furnace[facing=south,lit=false]");
  }
}
// hood / shelf above cook line (supported by iron)
fill(X0 + 2, F + 2, kitZ1, X1 - 2, F + 2, kitZ1, "iron_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]");
// hanging lamps under ceiling above cook line (must attach to roof block above)
for (let x = X0 + 5; x <= X1 - 5; x += 4) {
  set(x, CEIL - 1, kitZ1 - 1, "lantern[hanging=true,waterlogged=false]");
}

// West prep counter (continuous, with legs/support = full blocks)
fill(X0 + 2, F + 1, kitZ0 + 1, X0 + 2, F + 1, kitZ1 - 1, "iron_block");
fill(
  X0 + 2,
  F + 2,
  kitZ0 + 1,
  X0 + 2,
  F + 2,
  kitZ1 - 1,
  "iron_trapdoor[facing=east,half=top,open=false,powered=false,waterlogged=false]"
);
// East prep + plating
fill(X1 - 2, F + 1, kitZ0 + 1, X1 - 2, F + 1, kitZ1 - 1, "iron_block");
fill(
  X1 - 2,
  F + 2,
  kitZ0 + 1,
  X1 - 2,
  F + 2,
  kitZ1 - 1,
  "iron_trapdoor[facing=west,half=top,open=false,powered=false,waterlogged=false]"
);

// Sink / wash station west-front kitchen
set(X0 + 3, F + 1, kitZ0 + 1, "cauldron");
set(X0 + 4, F + 1, kitZ0 + 1, "cauldron");
set(X0 + 5, F + 1, kitZ0 + 1, "barrel[facing=south,open=false]");
set(X0 + 3, F + 2, kitZ0 + 1, "tripwire_hook[facing=south,attached=false,powered=false]");

// Dry storage east-front
set(X1 - 3, F + 1, kitZ0 + 1, "barrel[facing=south,open=false]");
set(X1 - 4, F + 1, kitZ0 + 1, "barrel[facing=south,open=false]");
set(X1 - 5, F + 1, kitZ0 + 1, "barrel[facing=south,open=false]");
set(X1 - 3, F + 2, kitZ0 + 1, "barrel[facing=south,open=false]");
set(X1 - 4, F + 2, kitZ0 + 1, "composter[level=3]");

// Center island prep table (supported)
fill(doorX - 2, F + 1, 222, doorX + 2, F + 1, 222, "smooth_quartz");
fill(doorX - 2, F + 1, 223, doorX + 2, F + 1, 223, "smooth_quartz");
fill(
  doorX - 2,
  F + 2,
  222,
  doorX + 2,
  F + 2,
  222,
  "smooth_quartz_slab[type=bottom,waterlogged=false]"
);
set(doorX, F + 2, 223, "crafting_table");
set(doorX - 1, F + 2, 223, "smithing_table");
set(doorX + 1, F + 2, 223, "cartography_table");

// Ingredient crates near island
set(doorX - 3, F + 1, 222, "hay_block[axis=y]");
set(doorX + 3, F + 1, 222, "melon");
set(doorX - 3, F + 1, 223, "pumpkin");
set(doorX + 3, F + 1, 223, "barrel[facing=east,open=false]");

// Trash / utility
set(doorX, F + 1, kitZ0 + 2, "hopper[facing=down,enabled=true]");
set(doorX, F + 2, kitZ0 + 2, "cauldron");

// Kitchen ceiling lights: hanging lanterns under roof (never floating mid-air)
for (let x = X0 + 6; x <= X1 - 6; x += 5) {
  for (const z of [220, 223]) {
    set(x, CEIL - 1, z, "lantern[hanging=true,waterlogged=false]");
  }
}

// Floor drain accents
set(doorX - 4, F, 224, "iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
set(doorX + 4, F, 224, "iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");

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
console.log("kitchen polish done");
