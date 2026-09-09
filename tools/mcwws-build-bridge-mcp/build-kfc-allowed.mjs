/**
 * KFC in allowed half-plane: Minecraft X < -527 AND Z > 209
 * (user "y" = horizontal Z in planning coords)
 * Footprint: x -556..-528 (28), z 210..226 (16) — west of library, north of McD street
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

// Strict bounds: X1 must be <= -528, Z0 must be >= 210
const X0 = -556,
  X1 = -528;
const Z0 = 210,
  Z1 = 226;
if (!(X1 < -527 && Z0 > 209)) {
  throw new Error("Footprint violates x<-527 && z>209");
}

const GY = 63;
const F = GY + 1;
const WALL_TOP = F + 6;

const map = new Map();
function set(x, y, z, block) {
  if (x >= -527 || z <= 209) {
    throw new Error(`Block outside allowed region: ${x},${z}`);
  }
  map.set(`${x},${y},${z}`, { x, y, z, block });
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

// clear only inside footprint
fill(X0, F, Z0, X1, F + 12, Z1, "air");
fill(X0, GY, Z0, X1, GY, Z1, "grass_block");
fill(X0, GY, Z0, X1, GY, Z1, "stone");
fill(X0, F, Z0, X1, F, Z1, "polished_diorite");
fill(X0 + 2, F, Z0 + 2, X1 - 2, F, Z0 + 8, "white_concrete");
for (let x = X0 + 3; x <= X1 - 3; x += 4)
  for (let z = Z0 + 3; z <= Z0 + 7; z += 3) set(x, F, z, "red_carpet");
fill(X0 + 2, F, Z0 + 10, X1 - 2, F, Z1 - 2, "smooth_stone");

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
for (let x = X0; x <= X1; x++) {
  set(x, F, Z0, "black_concrete");
  set(x, F, Z1, "black_concrete");
  set(x, F + 4, Z0, "red_concrete");
  set(x, F + 5, Z0, "red_concrete");
  set(x, F + 4, Z1, "red_concrete");
  set(x, F + 5, Z1, "red_concrete");
}
for (let z = Z0; z <= Z1; z++) {
  set(X0, F, z, "black_concrete");
  set(X1, F, z, "black_concrete");
  set(X0, F + 4, z, "red_concrete");
  set(X1, F + 4, z, "red_concrete");
  set(X0, F + 5, z, "red_concrete");
  set(X1, F + 5, z, "red_concrete");
}

// south facade faces McD (lower z) — glass + door
const doorX = Math.floor((X0 + X1) / 2);
for (let x = X0 + 2; x <= X1 - 2; x++) {
  for (let y = F + 1; y <= F + 3; y++) {
    if (x >= doorX - 1 && x <= doorX + 1 && y <= F + 2) set(x, y, Z0, "air");
    else set(x, y, Z0, "glass");
  }
}
for (let y = F; y <= F + 3; y++) {
  set(doorX - 2, y, Z0, "black_concrete");
  set(doorX + 2, y, Z0, "black_concrete");
}
fill(doorX - 2, F + 3, Z0, doorX + 2, F + 3, Z0, "black_concrete");
// steps only on Z0-1 if still z>209 — Z0-1=209 NOT allowed (need z>209)
// skip external steps south of 210 to respect hard constraint

for (let x = X0 + 3; x <= X1 - 3; x += 3) {
  for (let y = F + 2; y <= F + 3; y++) {
    set(x, y, Z1, "glass");
  }
}

for (const [cx, cz] of [
  [X0, Z0],
  [X1, Z0],
  [X0, Z1],
  [X1, Z1],
]) {
  fill(cx, F, cz, cx, WALL_TOP + 1, cz, "red_concrete");
  set(cx, WALL_TOP + 2, cz, "lantern");
}

fill(X0, WALL_TOP + 1, Z0, X1, WALL_TOP + 1, Z1, "smooth_quartz");
for (let x = X0; x <= X1; x++) {
  set(x, WALL_TOP + 1, Z0, "red_concrete");
  set(x, WALL_TOP + 1, Z1, "red_concrete");
}
for (let z = Z0; z <= Z1; z++) {
  set(X0, WALL_TOP + 1, z, "red_concrete");
  set(X1, WALL_TOP + 1, z, "red_concrete");
}

// roof sign on south face upper (inside footprint)
const sy = WALL_TOP + 2;
const sz = Z0;
fill(doorX - 8, sy, sz, doorX + 8, sy + 5, sz, "white_concrete");
function plot(ox, patterns) {
  for (let r = 0; r < patterns.length; r++) {
    const row = patterns[r];
    for (let c = 0; c < row.length; c++) {
      if (row[c] === "#") set(ox + c, sy + (patterns.length - 1 - r), sz, "red_concrete");
    }
  }
}
plot(doorX - 7, ["#  #", "# # ", "##  ", "# # ", "#  #"]);
plot(doorX - 2, ["####", "#   ", "### ", "#   ", "#   "]);
plot(doorX + 3, [" ###", "#   ", "#   ", "#   ", " ###"]);

const cz = Z0 + 8;
fill(X0 + 2, F + 1, cz, X1 - 2, F + 1, cz, "smooth_quartz");
fill(X0 + 2, F + 2, cz, X1 - 2, F + 2, cz, "smooth_quartz_slab");
fill(doorX - 2, F + 1, cz, doorX + 2, F + 3, cz, "air");
fill(doorX - 2, F + 1, cz, doorX + 2, F + 1, cz, "smooth_quartz");
for (let x = X0 + 3; x <= X1 - 3; x += 2) set(x, F + 1, Z1 - 2, "smoker");

function table(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab");
  set(tx - 1, F + 1, tz, "oak_stairs");
  set(tx + 1, F + 1, tz, "oak_stairs");
}
for (let x = X0 + 5; x <= X1 - 5; x += 5)
  for (let z = Z0 + 3; z <= Z0 + 6; z += 3) table(x, z);

for (let x = X0 + 4; x <= X1 - 4; x += 5)
  for (let z = Z0 + 3; z <= Z1 - 3; z += 4) set(x, WALL_TOP, z, "sea_lantern");

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
all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1) || a.y - b.y);
console.log(`placements=${all.length} x ${X0}..${X1} z ${Z0}..${Z1}`);
for (let i = 0; i < all.length; i += 4000) {
  const r = await postBatch(all.slice(i, i + 4000));
  console.log(`batch changed=${r.changed} undoable=${r.undoable}`);
}
console.log("KFC in allowed region done");
