/**
 * Medieval library on grass near player.
 * AABB: x -4168..-4104 (65), z -1328..-1286 (43), y 64..88
 * Entrance north (z=-1328) facing player at ~-4136,-1330
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -4168,
  X1 = -4104; // 65 wide
const Z0 = -1328,
  Z1 = -1286; // 43 deep
const GY = 64;
const F = 65; // floor walk
const WALL = F + 10; // 75
const ROOF = F + 16; // 81
const doorX = Math.floor((X0 + X1) / 2); // -4136

const map = new Map();
function set(x, y, z, block) {
  if (x < X0 || x > X1 || z < Z0 || z > Z1) return;
  if (y < GY - 1 || y > ROOF + 4) return;
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

function wallMix(x, y, z) {
  const n = Math.abs((x * 5 + y * 3 + z * 2) % 8);
  if (n <= 3) return "stone_bricks";
  if (n <= 5) return "mossy_stone_bricks";
  if (n === 6) return "cracked_stone_bricks";
  return "cobblestone";
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

console.log("AABB", { X0, X1, Z0, Z1, GY, ROOF });

// 1) clear interior volume (targeted)
console.log("clear...", await postFill(X0, F, Z0, X1, ROOF + 2, Z1, "air"));

// 2) foundation + floor via fill then detail mix with batches
console.log("foundation...", await postFill(X0, GY, Z0, X1, GY, Z1, "stone_bricks"));
console.log("floor...", await postFill(X0 + 1, F, Z0 + 1, X1 - 1, F, Z1 - 1, "spruce_planks"));
// center aisle darker
console.log(
  "aisle...",
  await postFill(doorX - 2, F, Z0 + 1, doorX + 2, F, Z1 - 1, "dark_oak_planks")
);

// 3) outer walls
for (let x = X0; x <= X1; x++) {
  for (let y = F; y <= WALL; y++) {
    set(x, y, Z0, wallMix(x, y, Z0));
    set(x, y, Z1, wallMix(x, y, Z1));
  }
}
for (let z = Z0; z <= Z1; z++) {
  for (let y = F; y <= WALL; y++) {
    set(X0, y, z, wallMix(X0, y, z));
    set(X1, y, z, wallMix(X1, y, z));
  }
}
// buttresses
for (let x = X0 + 4; x < X1; x += 8) {
  for (const z of [Z0 - 0, Z1]) {
    for (let y = F; y <= WALL + 1; y++) {
      set(x, y, z, "stone_bricks");
      if (z === Z0) set(x, y, Z0, "stone_bricks");
    }
    set(x, WALL + 2, z, "cobblestone_wall");
  }
}
for (let z = Z0 + 4; z < Z1; z += 8) {
  for (const x of [X0, X1]) {
    for (let y = F; y <= WALL + 1; y++) set(x, y, z, "stone_bricks");
    set(x, WALL + 2, z, "cobblestone_wall");
  }
}

// corner towers (slightly taller)
for (const [tx, tz] of [
  [X0, Z0],
  [X1, Z0],
  [X0, Z1],
  [X1, Z1],
]) {
  fill(tx, F, tz, tx + (tx === X0 ? 2 : -2), WALL + 4, tz + (tz === Z0 ? 2 : -2), "stone_bricks");
  fill(tx, WALL + 5, tz, tx + (tx === X0 ? 2 : -2), WALL + 5, tz + (tz === Z0 ? 2 : -2), "dark_oak_slab[type=bottom,waterlogged=false]");
}

// north entrance (3 wide)
fill(doorX - 2, F, Z0, doorX + 2, F + 4, Z0, "air");
fill(doorX - 3, F, Z0, doorX - 3, F + 5, Z0, "stone_bricks");
fill(doorX + 3, F, Z0, doorX + 3, F + 5, Z0, "stone_bricks");
fill(doorX - 2, F + 5, Z0, doorX + 2, F + 5, Z0, "stone_brick_stairs[facing=south,half=top,shape=straight,waterlogged=false]");
// doors
set(doorX - 1, F, Z0, "spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]");
set(doorX - 1, F + 1, Z0, "spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]");
set(doorX, F, Z0, "spruce_door[facing=south,half=lower,hinge=right,open=false,powered=false]");
set(doorX, F + 1, Z0, "spruce_door[facing=south,half=upper,hinge=right,open=false,powered=false]");
set(doorX + 1, F, Z0, "air");
set(doorX + 1, F + 1, Z0, "air");
// entrance steps outside north (still in AABB? Z0 is north wall - steps at Z0-1 outside AABB)
// put steps just inside apron: z=Z0+1 already floor

// windows — tall on long walls
for (let x = X0 + 6; x <= X1 - 6; x += 6) {
  if (Math.abs(x - doorX) < 4) continue;
  for (let y = F + 2; y <= F + 7; y++) {
    set(x, y, Z0, "glass");
    set(x, y, Z1, "glass");
  }
  set(x, F + 8, Z0, "stone_brick_stairs[facing=south,half=top,shape=straight,waterlogged=false]");
  set(x, F + 8, Z1, "stone_brick_stairs[facing=north,half=top,shape=straight,waterlogged=false]");
}
for (let z = Z0 + 6; z <= Z1 - 6; z += 6) {
  for (let y = F + 2; y <= F + 7; y++) {
    set(X0, y, z, "glass");
    set(X1, y, z, "glass");
  }
}

// spruce timber ring / beams
for (let x = X0; x <= X1; x++) {
  set(x, F + 4, Z0, "spruce_log[axis=x]");
  set(x, F + 4, Z1, "spruce_log[axis=x]");
  set(x, WALL, Z0, "spruce_log[axis=x]");
  set(x, WALL, Z1, "spruce_log[axis=x]");
}
for (let z = Z0; z <= Z1; z++) {
  set(X0, F + 4, z, "spruce_log[axis=z]");
  set(X1, F + 4, z, "spruce_log[axis=z]");
  set(X0, WALL, z, "spruce_log[axis=z]");
  set(X1, WALL, z, "spruce_log[axis=z]");
}
// cross beams
for (let z = Z0 + 8; z < Z1; z += 8) {
  fill(X0 + 1, WALL - 1, z, X1 - 1, WALL - 1, z, "spruce_log[axis=x]");
  for (let x = X0 + 8; x < X1; x += 8) {
    set(x, WALL - 1, z, "spruce_log[axis=y]");
    set(x, WALL - 2, z, "lantern[hanging=true,waterlogged=false]");
  }
}

// pitched roof (simple gable N-S ridge along x)
const ridgeZ = Math.floor((Z0 + Z1) / 2);
for (let z = Z0; z <= Z1; z++) {
  const dist = Math.abs(z - ridgeZ);
  const maxDist = Math.max(ridgeZ - Z0, Z1 - ridgeZ);
  const rise = Math.max(0, Math.round(((maxDist - dist) / maxDist) * 8));
  for (let x = X0; x <= X1; x++) {
    const y = WALL + 1 + rise;
    set(x, y, z, "dark_oak_planks");
    if (rise > 0 && dist > 0) {
      const face = z < ridgeZ ? "south" : "north";
      set(
        x,
        y,
        z,
        `dark_oak_stairs[facing=${face},half=bottom,shape=straight,waterlogged=false]`
      );
    }
    // under-roof glow every other
    if (x % 4 === 0 && rise >= 1) set(x, y - 1, z, "lantern[hanging=true,waterlogged=false]");
  }
}
// ridge cap
fill(X0, WALL + 9, ridgeZ, X1, WALL + 9, ridgeZ, "dark_oak_slab[type=bottom,waterlogged=false]");

// ========== INTERIOR ==========
// bookshelf walls along east/west interiors
for (let z = Z0 + 3; z <= Z1 - 3; z++) {
  if (z % 3 === 0) continue; // gaps for aisles
  for (let y = F + 1; y <= F + 3; y++) {
    set(X0 + 2, y, z, "bookshelf");
    set(X0 + 3, y, z, "bookshelf");
    set(X1 - 2, y, z, "bookshelf");
    set(X1 - 3, y, z, "bookshelf");
  }
  // upper gallery shelves
  for (let y = F + 6; y <= F + 8; y++) {
    set(X0 + 2, y, z, "bookshelf");
    set(X1 - 2, y, z, "bookshelf");
  }
}
// reading tables — chairs face table with inverted MC facing
function placeTable(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "oak_slab[type=bottom,waterlogged=false]");
  // visual toward table → facing = opposite of chair→table
  // south seat: toward N → facing=south
  set(tx, F + 1, tz + 1, "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
  set(tx, F + 1, tz - 1, "oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
}
for (let x = doorX - 18; x <= doorX + 18; x += 6) {
  for (let z = Z0 + 10; z <= Z1 - 8; z += 6) {
    if (Math.abs(x - doorX) <= 3) continue; // keep center aisle
    placeTable(x, z);
  }
}

// central lectern / catalog desk
fill(doorX - 2, F + 1, ridgeZ - 1, doorX + 2, F + 1, ridgeZ + 1, "dark_oak_planks");
set(doorX, F + 2, ridgeZ, "lectern[facing=north,has_book=false,powered=false]");
set(doorX - 1, F + 2, ridgeZ, "enchanting_table");
set(doorX + 1, F + 2, ridgeZ, "bookshelf");

// second floor gallery walkways (edges)
fill(X0 + 1, F + 5, Z0 + 1, X0 + 4, F + 5, Z1 - 1, "spruce_planks");
fill(X1 - 4, F + 5, Z0 + 1, X1 - 1, F + 5, Z1 - 1, "spruce_planks");
fill(X0 + 5, F + 5, Z0 + 1, X1 - 5, F + 5, Z0 + 3, "spruce_planks");
fill(X0 + 5, F + 5, Z1 - 3, X1 - 5, F + 5, Z1 - 1, "spruce_planks");
// railings
for (let z = Z0 + 1; z <= Z1 - 1; z++) {
  set(X0 + 4, F + 6, z, "spruce_fence");
  set(X1 - 4, F + 6, z, "spruce_fence");
}
// stairs to gallery (west)
fill(X0 + 5, F + 1, Z0 + 4, X0 + 5, F + 1, Z0 + 4, "spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
for (let i = 0; i < 5; i++) {
  set(
    X0 + 5 + i,
    F + 1 + i,
    Z0 + 4,
    "spruce_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]"
  );
}

// carpet path
fill(doorX - 1, F + 1, Z0 + 1, doorX + 1, F + 1, ridgeZ - 2, "red_carpet");

// chandelier-ish chains under ridge
for (let x = doorX - 12; x <= doorX + 12; x += 8) {
  set(x, WALL + 4, ridgeZ, "lantern[hanging=true,waterlogged=false]");
}

// exterior path apron north of door (inside AABB: z=Z0 is wall — small plaza inside)
fill(doorX - 4, F, Z0 + 1, doorX + 4, F, Z0 + 3, "stone_bricks");

// sign plaque above door
fill(doorX - 4, F + 6, Z0, doorX + 4, F + 8, Z0, "polished_andesite");
// plaque letters hint with red terracotta accents
for (const ox of [-3, -1, 1, 3]) set(doorX + ox, F + 7, Z0, "red_terracotta");

async function flush() {
  const all = [...map.values()];
  map.clear();
  all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1) || a.y - b.y);
  console.log("flush", all.length);
  for (let i = 0; i < all.length; i += 8000) {
    console.log(await postBatch(all.slice(i, i + 8000)));
  }
}

await flush();
console.log("medieval library done");
