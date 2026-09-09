/**
 * Smaller KFC west of McDonald's (avoid north road + east library_garden).
 * McD: x -548..-514, z 177..198
 * Library: x -517..-409, z 203..345
 * KFC: x -578..-549 (30), z 179..196 (18) — west of McD, same z band
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -578,
  X1 = -549;
const Z0 = 179,
  Z1 = 196;
const GY = 63;
const F = GY + 1;
const WALL_TOP = F + 6;

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

// only clear inside footprint (+1 margin), not the street/library
fill(X0 - 1, F, Z0 - 1, X1 + 1, F + 12, Z1 + 1, "air");
fill(X0 - 1, GY, Z0 - 1, X1 + 1, GY, Z1 + 1, "grass_block");

fill(X0, GY, Z0, X1, GY, Z1, "stone");
fill(X0, F, Z0, X1, F, Z1, "polished_diorite");
fill(X0 + 2, F, Z0 + 2, X1 - 2, F, Z0 + 8, "white_concrete");
for (let x = X0 + 3; x <= X1 - 3; x += 4)
  for (let z = Z0 + 3; z <= Z0 + 7; z += 3) fill(x, F, z, x, F, z, "red_carpet");
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

// east facade faces McD (glass + door)
const doorZ = Math.floor((Z0 + Z1) / 2);
for (let z = Z0 + 2; z <= Z1 - 2; z++) {
  for (let y = F + 1; y <= F + 3; y++) {
    if (z >= doorZ - 1 && z <= doorZ + 1 && y <= F + 2) set(X1, y, z, "air");
    else set(X1, y, z, "glass");
  }
}
for (let y = F; y <= F + 3; y++) {
  set(X1, y, doorZ - 2, "black_concrete");
  set(X1, y, doorZ + 2, "black_concrete");
}
fill(X1, F + 3, doorZ - 2, X1, F + 3, doorZ + 2, "black_concrete");
fill(X1 + 1, GY, doorZ - 1, X1 + 2, GY, doorZ + 1, "polished_andesite");
fill(X1 + 1, F, doorZ - 1, X1 + 1, F, doorZ + 1, "smooth_quartz_stairs");

// south/north small windows
for (let x = X0 + 3; x <= X1 - 3; x += 3) {
  for (let y = F + 2; y <= F + 3; y++) {
    set(x, y, Z0, "glass");
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

fill(X0 - 1, WALL_TOP + 1, Z0 - 1, X1 + 1, WALL_TOP + 1, Z1 + 1, "smooth_quartz");
for (let x = X0 - 1; x <= X1 + 1; x++) {
  set(x, WALL_TOP + 1, Z0 - 1, "red_concrete");
  set(x, WALL_TOP + 1, Z1 + 1, "red_concrete");
}
for (let z = Z0 - 1; z <= Z1 + 1; z++) {
  set(X0 - 1, WALL_TOP + 1, z, "red_concrete");
  set(X1 + 1, WALL_TOP + 1, z, "red_concrete");
}
// awning on east (toward McD)
fill(X1 + 1, F + 4, Z0 + 1, X1 + 2, F + 4, Z1 - 1, "red_concrete");

// KFC sign on east upper
const sx = X1 + 1;
const sy = WALL_TOP + 2;
const sz0 = doorZ - 6;
fill(sx, sy, sz0, sx, sy + 5, sz0 + 14, "white_concrete");
function plot(oz, patterns) {
  for (let r = 0; r < patterns.length; r++) {
    const row = patterns[r];
    for (let c = 0; c < row.length; c++) {
      if (row[c] === "#") set(sx, sy + (patterns.length - 1 - r), oz + c, "red_concrete");
    }
  }
}
plot(sz0 + 1, ["#  #", "# # ", "##  ", "# # ", "#  #"]);
plot(sz0 + 6, ["####", "#   ", "### ", "#   ", "#   "]);
plot(sz0 + 11, [" ###", "#   ", "#   ", "#   ", " ###"]);

// interior
const cz = Z0 + 9;
fill(X0 + 2, F + 1, cz, X1 - 2, F + 1, cz, "smooth_quartz");
fill(X0 + 2, F + 2, cz, X1 - 2, F + 2, cz, "smooth_quartz_slab");
fill(doorZ - 1 > Z0 ? X1 - 5 : X0 + 3, F + 1, cz, X1 - 3, F + 3, cz, "air");
fill(X1 - 5, F + 1, cz, X1 - 3, F + 1, cz, "smooth_quartz");
for (let x = X0 + 3; x <= X1 - 8; x += 2) {
  set(x, F + 1, Z1 - 3, "smoker");
}
function table(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab");
  set(tx - 1, F + 1, tz, "oak_stairs");
  set(tx + 1, F + 1, tz, "oak_stairs");
}
for (let x = X0 + 5; x <= X1 - 6; x += 5)
  for (let z = Z0 + 3; z <= Z0 + 7; z += 4) table(x, z);

for (let x = X0 + 4; x <= X1 - 4; x += 5)
  for (let z = Z0 + 3; z <= Z1 - 3; z += 5) {
    set(x, WALL_TOP, z, "sea_lantern");
  }

fill(X1 - 1, F, doorZ - 1, X1 - 1, F, doorZ + 1, "red_carpet");
fill(X0 + 3, F, doorZ, X1 - 2, F, doorZ, "red_carpet");

// short path east to McD only (2 blocks, don't pave whole street)
fill(X1 + 1, GY, doorZ - 1, -549, GY, doorZ + 1, "polished_andesite");

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
console.log(`placements=${all.length} footprint x ${X0}..${X1} z ${Z0}..${Z1}`);
for (let i = 0; i < all.length; i += 4000) {
  const r = await postBatch(all.slice(i, i + 4000));
  console.log(`batch changed=${r.changed}`);
}
console.log("small KFC done");
