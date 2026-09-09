/**
 * Build a refined KFC storefront north of McDonald's, slightly larger, no NPCs.
 * McD: x -548..-514, z 177..198, y~63-75
 * KFC: x -552..-510 (43), z 214..241 (28), ground y=63
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -552,
  X1 = -510; // 43 wide (McD ~34)
const Z0 = 214,
  Z1 = 241; // 28 deep (McD ~21)
const GY = 63; // grass
const F = GY + 1; // floor 64

const map = new Map();
const key = (x, y, z) => `${x},${y},${z}`;
function set(x, y, z, block) {
  map.set(key(x, y, z), { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  const ax = Math.min(x1, x2),
    bx = Math.max(x1, x2);
  const ay = Math.min(y1, y2),
    by = Math.max(y1, y2);
  const az = Math.min(z1, z2),
    bz = Math.max(z1, z2);
  for (let x = ax; x <= bx; x++)
    for (let y = ay; y <= by; y++)
      for (let z = az; z <= bz; z++) set(x, y, z, block);
}
function hollowBox(x1, y1, z1, x2, y2, z2, block) {
  fill(x1, y1, z1, x2, y2, z2, block);
  fill(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1, "air");
}

// --- clear volume ---
fill(X0 - 2, F, Z0 - 6, X1 + 2, F + 16, Z1 + 2, "air");
fill(X0 - 2, GY, Z0 - 6, X1 + 2, GY, Z1 + 2, "grass_block");

// plaza between McD and KFC (street)
fill(-548, GY, 199, -514, GY, 213, "grass_block");
fill(-545, GY, 200, -517, GY, 212, "polished_andesite");
// center crosswalk stripes
for (let z = 200; z <= 212; z++) {
  if (z % 2 === 0) fill(-534, GY, z, -528, GY, z, "white_concrete");
}

// --- foundation & floor ---
fill(X0, GY, Z0, X1, GY, Z1, "stone");
fill(X0, F, Z0, X1, F, Z1, "polished_diorite");
// dining carpet zones (south half)
fill(X0 + 2, F, Z0 + 2, X1 - 2, F, Z0 + 12, "white_concrete");
for (let x = X0 + 3; x <= X1 - 3; x += 4) {
  for (let z = Z0 + 3; z <= Z0 + 11; z += 4) {
    fill(x, F, z, x + 1, F, z + 1, "red_carpet");
  }
}
// kitchen floor (north)
fill(X0 + 2, F, Z0 + 16, X1 - 2, F, Z1 - 2, "smooth_stone");

// --- outer walls shell ---
const WALL_TOP = F + 7; // 71
// white body
for (let y = F; y <= WALL_TOP; y++) {
  for (let x = X0; x <= X1; x++) {
    set(x, y, Z0, "white_concrete");
    set(x, y, Z1, "white_concrete");
  }
  for (let z = Z0; z <= Z1; z++) {
    set(X0, y, z, "white_concrete");
    set(X1, y, z, "white_concrete");
  }
}
// black baseboard
for (let x = X0; x <= X1; x++) {
  set(x, F, Z0, "black_concrete");
  set(x, F, Z1, "black_concrete");
}
for (let z = Z0; z <= Z1; z++) {
  set(X0, F, z, "black_concrete");
  set(X1, F, z, "black_concrete");
}
// red brand stripe
for (let x = X0; x <= X1; x++) {
  set(x, F + 5, Z0, "red_concrete");
  set(x, F + 5, Z1, "red_concrete");
  set(x, F + 6, Z0, "red_concrete");
  set(x, F + 6, Z1, "red_concrete");
}
for (let z = Z0; z <= Z1; z++) {
  set(X0, F + 5, z, "red_concrete");
  set(X1, F + 5, z, "red_concrete");
  set(X0, F + 6, z, "red_concrete");
  set(X1, F + 6, z, "red_concrete");
}

// --- south glass facade (facing McD) ---
const doorX = Math.floor((X0 + X1) / 2); // -531
for (let x = X0 + 3; x <= X1 - 3; x++) {
  for (let y = F + 1; y <= F + 4; y++) {
    if (x >= doorX - 1 && x <= doorX + 1 && y <= F + 3) {
      set(x, y, Z0, "air"); // entrance opening
    } else {
      set(x, y, Z0, "glass");
    }
  }
}
// entrance frame
for (let y = F; y <= F + 4; y++) {
  set(doorX - 2, y, Z0, "black_concrete");
  set(doorX + 2, y, Z0, "black_concrete");
}
fill(doorX - 2, F + 4, Z0, doorX + 2, F + 4, Z0, "black_concrete");
set(doorX - 1, F, Z0 - 1, "smooth_quartz_stairs");
set(doorX, F, Z0 - 1, "smooth_quartz_stairs");
set(doorX + 1, F, Z0 - 1, "smooth_quartz_stairs");
fill(doorX - 2, GY, Z0 - 1, doorX + 2, GY, Z0 - 1, "polished_andesite");

// side windows
for (let z = Z0 + 4; z <= Z0 + 12; z += 3) {
  for (let y = F + 2; y <= F + 4; y++) {
    set(X0, y, z, "glass");
    set(X1, y, z, "glass");
    set(X0, y, z + 1, "glass");
    set(X1, y, z + 1, "glass");
  }
}

// --- corner red pillars (brand) ---
for (const [cx, cz] of [
  [X0, Z0],
  [X1, Z0],
  [X0, Z1],
  [X1, Z1],
]) {
  fill(cx, F, cz, cx, WALL_TOP + 2, cz, "red_concrete");
  set(cx, WALL_TOP + 3, cz, "lantern");
}

// --- roof ---
fill(X0 - 1, WALL_TOP + 1, Z0 - 1, X1 + 1, WALL_TOP + 1, Z1 + 1, "smooth_quartz");
fill(X0, WALL_TOP + 2, Z0, X1, WALL_TOP + 2, Z1, "smooth_quartz_slab");
// red parapet rim
for (let x = X0 - 1; x <= X1 + 1; x++) {
  set(x, WALL_TOP + 1, Z0 - 1, "red_concrete");
  set(x, WALL_TOP + 1, Z1 + 1, "red_concrete");
}
for (let z = Z0 - 1; z <= Z1 + 1; z++) {
  set(X0 - 1, WALL_TOP + 1, z, "red_concrete");
  set(X1 + 1, WALL_TOP + 1, z, "red_concrete");
}
// overhang awning south
fill(X0 + 1, F + 5, Z0 - 2, X1 - 1, F + 5, Z0 - 1, "red_concrete");
fill(X0 + 1, F + 4, Z0 - 2, X1 - 1, F + 4, Z0 - 2, "red_concrete");
for (let x = X0 + 2; x <= X1 - 2; x += 4) {
  set(x, F + 3, Z0 - 2, "black_concrete");
}

// --- roof sign "KFC" facing south (pixel letters on billboard) ---
const sx = doorX - 8;
const sy = WALL_TOP + 2;
const sz = Z0 - 2;
fill(sx - 1, sy, sz, sx + 17, sy + 6, sz, "white_concrete");
fill(sx - 1, sy, sz - 1, sx + 17, sy + 6, sz - 1, "black_concrete");

function plotLetter(originX, patterns, block = "red_concrete") {
  for (let r = 0; r < patterns.length; r++) {
    const row = patterns[r];
    for (let c = 0; c < row.length; c++) {
      if (row[c] === "#") set(originX + c, sy + (patterns.length - 1 - r), sz, block);
    }
  }
}
const K = ["#  #", "# # ", "##  ", "# # ", "#  #"];
const LETTER_F = ["####", "#   ", "### ", "#   ", "#   "];
const LETTER_C = [" ###", "#   ", "#   ", "#   ", " ###"];
plotLetter(sx + 1, K);
plotLetter(sx + 7, LETTER_F);
plotLetter(sx + 12, LETTER_C);

// bucket-like logo tower east of sign
fill(X1 - 4, WALL_TOP + 2, Z0 + 2, X1 - 2, WALL_TOP + 6, Z0 + 4, "red_concrete");
fill(X1 - 3, WALL_TOP + 3, Z0 + 3, X1 - 3, WALL_TOP + 5, Z0 + 3, "white_concrete");
set(X1 - 3, WALL_TOP + 7, Z0 + 3, "white_concrete");

// --- interior counter ---
const cz = Z0 + 14;
fill(X0 + 3, F + 1, cz, X1 - 3, F + 1, cz, "smooth_quartz");
fill(X0 + 3, F + 2, cz, X1 - 3, F + 2, cz, "smooth_quartz_slab");
// pass-through glass above counter
for (let x = X0 + 5; x <= X1 - 5; x++) {
  set(x, F + 3, cz, "glass_pane");
  set(x, F + 4, cz, "glass_pane");
}
// order gap
fill(doorX - 2, F + 1, cz, doorX + 2, F + 4, cz, "air");
fill(doorX - 2, F + 1, cz, doorX + 2, F + 1, cz, "smooth_quartz");

// --- kitchen (north of counter) ---
fill(X0 + 3, F + 1, Z1 - 5, X1 - 3, F + 1, Z1 - 3, "iron_block");
for (let x = X0 + 4; x <= X1 - 4; x += 2) {
  set(x, F + 1, Z1 - 4, "smoker");
  set(x, F + 2, Z1 - 4, "iron_bars");
}
fill(X0 + 3, F + 1, cz + 2, X0 + 6, F + 3, cz + 5, "white_concrete"); // fridge wall
fill(X1 - 6, F + 1, cz + 2, X1 - 3, F + 3, cz + 5, "white_concrete");

// --- dining furniture ---
function table(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab");
  set(tx - 1, F + 1, tz, "oak_stairs");
  set(tx + 1, F + 1, tz, "oak_stairs");
  set(tx, F + 1, tz - 1, "oak_stairs");
  set(tx, F + 1, tz + 1, "oak_stairs");
  set(tx, F + 3, tz, "lantern");
}
for (let x = X0 + 6; x <= X1 - 6; x += 6) {
  for (let z = Z0 + 4; z <= Z0 + 10; z += 5) {
    table(x, z);
  }
}

// --- interior ceiling lights ---
for (let x = X0 + 4; x <= X1 - 4; x += 5) {
  for (let z = Z0 + 3; z <= Z1 - 3; z += 5) {
    set(x, WALL_TOP, z, "sea_lantern");
    set(x, WALL_TOP - 1, z, "white_stained_glass");
  }
}

// --- outdoor patio south ---
fill(X0 + 4, GY, Z0 - 5, X1 - 4, GY, Z0 - 2, "polished_andesite");
for (let x = X0 + 6; x <= X1 - 6; x += 5) {
  set(x, F, Z0 - 4, "oak_fence");
  set(x, F + 1, Z0 - 4, "oak_pressure_plate");
  set(x - 1, F, Z0 - 4, "oak_stairs");
  set(x + 1, F, Z0 - 4, "oak_stairs");
  set(x, F + 2, Z0 - 4, "lantern");
}
// planters
for (const x of [X0 + 2, X1 - 2]) {
  set(x, F, Z0 - 3, "white_concrete");
  set(x, F + 1, Z0 - 3, "flowering_azalea_leaves");
  set(x, F, Z0 + 2, "white_concrete");
  set(x, F + 1, Z0 + 2, "azalea");
}

// path from patio to street
fill(doorX - 2, GY, Z0 - 6, doorX + 2, GY, 213, "polished_andesite");

// --- decorative cornice ---
for (let x = X0; x <= X1; x++) {
  set(x, WALL_TOP, Z0, "smooth_quartz_slab");
  set(x, WALL_TOP, Z1, "smooth_quartz_slab");
}

// clear interior air carefully (don't wipe furniture): already placed solids after clear
// Re-clear only shell interior empty zones that might have leftover? Initial clear was enough.

// strip accidental blocks in entrance
fill(doorX - 1, F + 1, Z0, doorX + 1, F + 3, Z0, "air");
set(doorX - 1, F, Z0, "polished_diorite");
set(doorX, F, Z0, "polished_diorite");
set(doorX + 1, F, Z0, "polished_diorite");

// red carpet runner from door to counter
fill(doorX - 1, F, Z0 + 1, doorX + 1, F, cz - 1, "red_carpet");

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
all.sort((a, b) => {
  const aa = a.block === "air" ? 0 : 1;
  const bb = b.block === "air" ? 0 : 1;
  if (aa !== bb) return aa - bb;
  return a.y - b.y;
});

console.log(`KFC placements: ${all.length}`);
const BATCH = 4000;
for (let i = 0; i < all.length; i += BATCH) {
  const chunk = all.slice(i, i + BATCH);
  const r = await postBatch(chunk);
  console.log(`Batch ${i / BATCH + 1}: changed=${r.changed}`);
}
console.log("KFC done.");
console.log(`Footprint x ${X0}..${X1}, z ${Z0}..${Z1}, floor y=${F}`);
