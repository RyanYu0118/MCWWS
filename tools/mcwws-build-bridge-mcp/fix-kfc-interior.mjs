/**
 * Fix KFC interior: clear aisle, proper tables/chairs.
 * Footprint x -556..-528, z 210..226, floor y=64
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
const counterZ = 218; // Z0+8

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

// Clear dining interior furniture layer (keep walls/floor base)
fill(X0 + 2, F + 1, Z0 + 1, X1 - 2, F + 3, counterZ - 1, "air");

// Red carpet aisle only (3 wide), no clutter
fill(doorX - 1, F, Z0 + 1, doorX + 1, F, counterZ - 1, "red_carpet");
// Side dining floors white
fill(X0 + 2, F, Z0 + 2, doorX - 2, F, counterZ - 2, "white_concrete");
fill(doorX + 2, F, Z0 + 2, X1 - 2, F, counterZ - 2, "white_concrete");

// Tables: smooth_quartz slab on fence, chairs facing table with correct stairs facing
// West booths
const westTables = [-552, -549];
const eastTables = [-535, -532];
const tableZs = [212, 215];

function placeTable(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab");
  // chairs: north/south of table (along aisle direction), facing table
  // oak_stairs facing: north means stair faces north (player sits looking north)
  // For chair south of table, player faces north → facing=north
  set(tx, F + 1, tz + 1, "oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
  set(tx, F + 1, tz - 1, "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
}

for (const tx of westTables) {
  for (const tz of tableZs) placeTable(tx, tz);
}
for (const tx of eastTables) {
  for (const tz of tableZs) placeTable(tx, tz);
}

// Ensure aisle completely clear at y=65-66
fill(doorX - 1, F + 1, Z0 + 1, doorX + 1, F + 2, counterZ - 1, "air");

// Door opening
fill(doorX - 1, F + 1, Z0, doorX + 1, F + 2, Z0, "air");

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
console.log("interior fixed");
