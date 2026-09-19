/**
 * Yellow-wool blob: neo-Chinese rockery with waterfall into creek.
 * Light-blue wool line: small dry dark-oak bridge.
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

const yellow = [
  [-575, 157], [-574, 157], [-579, 158], [-577, 158], [-575, 158], [-574, 158],
  [-580, 159], [-574, 159], [-582, 160], [-581, 160], [-580, 160], [-574, 160],
  [-579, 161], [-578, 161], [-577, 161], [-576, 161], [-575, 161],
];
const rock = new Set(yellow.map(([x, z]) => `${x},${z}`));
for (const [x, z] of yellow) {
  for (const [dx, dz] of [[0, 0], [1, 0], [-1, 0], [0, 1], [0, -1]]) {
    rock.add(`${x + dx},${z + dz}`);
  }
}

function height(x, z) {
  const d1 = Math.hypot(x + 578, z - 159);
  const d2 = Math.hypot(x + 580, z - 160);
  const d3 = Math.hypot(x + 575, z - 158);
  let h = 0;
  if (d1 < 3.6) h = Math.max(h, Math.round(5 * (1 - d1 / 3.6) ** 1.1));
  if (d2 < 2.8) h = Math.max(h, Math.round(4 * (1 - d2 / 2.8)));
  if (d3 < 2.4) h = Math.max(h, Math.round(3 * (1 - d3 / 2.4)));
  return Math.max(2, Math.min(6, h || 2));
}

const mats = ["andesite", "cobblestone", "mossy_cobblestone", "tuff", "stone", "moss_block"];
function mat(x, y, z) {
  if (y >= 66 && (x + z) % 3 === 0) return "moss_block";
  return mats[(((x * 5 + y * 2 + z) % 6) + 6) % 6];
}

const SKIP = new Set([
  "stripped_dark_oak_wood",
  "dark_oak_log",
  "dark_oak_planks",
  "calcite",
  "stone_bricks",
  "cherry_log",
  "deepslate_tiles",
]);

console.log("scan rock cells");
for (const key of rock) {
  const [x, z] = key.split(",").map(Number);
  if (x < -583 || x > -573 || z < 156 || z > 162) continue;
  const a = nid(await get(x, 63, z));
  const b = nid(await get(x, 64, z));
  if (SKIP.has(a) || SKIP.has(b)) continue;
  if (a === "water") continue;
  const h = height(x, z);
  for (let i = 0; i < h; i++) set(x, 63 + i, z, mat(x, 63 + i, z));
  if (h >= 3 && (x + z) % 4 === 0) set(x, 63 + h, z, (x + z) % 2 === 0 ? "azalea" : "fern");
  if (b === "yellow_wool") set(x, 64, z, mat(x, 64, z));
}

set(-578, 68, 159, "mossy_cobblestone");
set(-578, 69, 159, "water");
set(-578, 68, 158, "andesite");
set(-577, 67, 159, "cobblestone");
set(-577, 68, 159, "water");
set(-577, 67, 158, "water[level=1]");
set(-576, 66, 158, "andesite");
set(-576, 67, 158, "water[level=1]");
set(-576, 66, 157, "water");
set(-575, 65, 157, "mossy_cobblestone");
set(-575, 66, 157, "water[level=2]");
set(-575, 64, 156, "water");
set(-575, 63, 156, "gravel");
set(-574, 63, 155, "water");
set(-574, 62, 155, "gravel");
set(-573, 63, 154, "water");
set(-573, 62, 154, "gravel");
set(-573, 63, 153, "water");
set(-572, 63, 152, "water");
set(-572, 62, 152, "gravel");
set(-572, 63, 151, "water");
set(-572, 63, 150, "water");

set(-579, 67, 160, "andesite");
set(-580, 65, 160, "tuff");
set(-581, 64, 160, "mossy_cobblestone");
set(-582, 63, 160, "cobblestone");
set(-577, 64, 161, "moss_block");
set(-576, 65, 160, "andesite");
set(-574, 64, 159, "mossy_cobblestone");
set(-578, 66, 161, "fern");
set(-580, 66, 159, "azalea");
set(-576, 64, 157, "flowering_azalea");

const stair = (facing) =>
  `dark_oak_stairs[facing=${facing},half=bottom,shape=straight,waterlogged=false]`;

for (const x of [-572, -571]) {
  set(x, 63, 144, stair("north"));
  for (let z = 145; z <= 147; z++) set(x, 64, z, "dark_oak_planks");
  set(x, 63, 148, stair("south"));
}
for (let z = 144; z <= 148; z++) {
  set(-573, 64, z, "dark_oak_planks");
  set(-570, 64, z, "dark_oak_planks");
  set(-573, 65, z, "dark_oak_fence");
  set(-570, 65, z, "dark_oak_fence");
}
set(-575, 64, 197, "air");

const blocks = [...map.values()];
console.log("place", blocks.length);
console.log(await post(blocks));
console.log("peak", await get(-578, 69, 159));
console.log("fall", await get(-575, 66, 157), await get(-572, 63, 151));
console.log("bridge", await get(-572, 64, 146), await get(-572, 63, 144));
console.log("wool gone", await get(-580, 64, 160), await get(-572, 64, 147));
console.log("done");
