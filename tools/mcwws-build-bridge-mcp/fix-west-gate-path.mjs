/**
 * West gate path: road -> moon gate -> pavilion.
 * Move creek out of z=148..151 west corridor.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, x, y, z }),
  });
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
async function post(blocks) {
  const res = await fetch(`${BASE}/set_blocks`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, blocks }),
  });
  const j = await res.json();
  if (!res.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
  return j;
}

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function nid(b) {
  return (b || "").split("[")[0];
}

const HARD = new Set([
  "calcite",
  "polished_deepslate",
  "stripped_dark_oak_wood",
  "dark_oak_log",
  "dark_oak_stairs",
  "dark_oak_slab",
  "cherry_log",
  "cherry_leaves",
  "stone_bricks",
  "oak_leaves",
  "oak_wood",
  "oak_log",
  "deepslate_tiles",
  "deepslate_tile_stairs",
  "cobblestone_wall",
  "chiseled_stone_bricks",
  "lantern",
  "bamboo",
]);

const mix = ["stone", "stone", "andesite", "cobblestone"];
function pave(x, z) {
  return mix[(((x + z * 3) % 4) + 4) % 4];
}

console.log("scan corridor");
const occ = new Map();
for (let z = 138; z <= 158; z++) {
  for (let x = -616; x <= -574; x++) {
    occ.set(`${x},${z}`, {
      a: nid(await get(x, 63, z)),
      b: nid(await get(x, 64, z)),
    });
  }
}
function cell(x, z) {
  return occ.get(`${x},${z}`) || { a: "air", b: "air" };
}
function blocked(x, z) {
  const c = cell(x, z);
  if (x === -604 && (z === 149 || z === 150)) return false;
  if (HARD.has(c.a)) return true;
  if (HARD.has(c.b) && c.b !== "oak_leaves") return true;
  return false;
}

for (let z = 148; z <= 151; z++) {
  for (let x = -612; x <= -598; x++) {
    const c = cell(x, z);
    if (c.a !== "water") continue;
    if (x === -604 && z !== 149 && z !== 150 && c.a === "calcite") continue;
    if (blocked(x, z) && !(x === -604 && (z === 149 || z === 150))) continue;
    set(x, 63, z, "grass_block");
    set(x, 62, z, "dirt");
    if (c.b === "lily_pad" || c.b === "spruce_slab") set(x, 64, z, "air");
  }
}

const creek = [];
for (let x = -602; x <= -592; x++) {
  creek.push([x, 154]);
  creek.push([x, 155]);
}
for (let x = -592; x <= -586; x++) {
  creek.push([x, 153]);
  creek.push([x, 154]);
}
for (const [x, z] of creek) {
  const c = cell(x, z);
  if (blocked(x, z)) continue;
  if (c.a === "stripped_dark_oak_wood") continue;
  set(x, 62, z, ((x + z) % 2 === 0 ? "gravel" : "coarse_dirt"));
  set(x, 63, z, "water");
  if (c.b === "air" || c.b === "short_grass" || c.b === "lily_pad" || c.b === "fern") {
    set(x, 64, z, "air");
  }
}

const path = [];
function addPath(x, z) {
  if (z < 138 || z > 156) return;
  if (x < -614 || x > -576) return;
  if (blocked(x, z) && !(x === -604 && (z === 149 || z === 150))) return;
  path.push([x, z]);
}
for (let x = -612; x <= -604; x++) {
  addPath(x, 149);
  addPath(x, 150);
}
let x = -603,
  z = 149;
while (x < -581 || z > 142) {
  addPath(x, z);
  addPath(x, z + 1);
  if (x < -581) x++;
  if (z > 142 && (x + z) % 2 === 0) z--;
}
for (let px = -583; px <= -577; px++) addPath(px, 144);

for (const [px, pz] of path) {
  const c = cell(px, pz);
  if (c.a === "stripped_dark_oak_wood") continue;
  set(px, 63, pz, pave(px, pz));
  if (c.b === "lily_pad" || c.b === "short_grass" || c.b === "fern" || c.b === "pink_petals") {
    set(px, 64, pz, "air");
  }
}

set(-604, 63, 149, "smooth_stone");
set(-604, 63, 150, "smooth_stone");
set(-604, 64, 149, "air");
set(-604, 64, 150, "air");
set(-604, 65, 149, "air");
set(-604, 65, 150, "air");

const blocks = [...map.values()];
console.log("blocks", blocks.length, "path", path.length);
console.log(await post(blocks));
console.log("gate", await get(-604, 63, 149), await get(-604, 64, 149));
console.log("road join", await get(-610, 63, 150));
console.log("mid", await get(-592, 63, 147), await get(-592, 63, 150));
console.log("to pavilion", await get(-582, 63, 144));
console.log("creek aside", await get(-600, 63, 154));
console.log("done");
