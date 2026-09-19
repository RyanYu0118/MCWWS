/**
 * Follow yellow-wool path markers; wooden bridge over creek
 * in the yellow gap east of the half-pavilion (no waterlogged blocks).
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
  [-602, 149], [-600, 147], [-597, 144], [-595, 143], [-594, 143], [-592, 142], [-589, 142],
  [-586, 141], [-585, 141], [-584, 141],
  [-576, 141], [-575, 141], [-573, 142], [-572, 144], [-571, 142], [-569, 142], [-566, 142], [-564, 143],
  [-561, 144], [-560, 144], [-559, 144],
  [-572, 149], [-571, 150], [-571, 153], [-571, 155], [-571, 158],
  [-572, 160], [-572, 162], [-572, 164], [-571, 167], [-570, 169],
  [-569, 171], [-568, 173], [-567, 175], [-566, 176], [-565, 177], [-565, 178],
  [-565, 183], [-565, 184], [-566, 185], [-567, 186], [-568, 187],
  [-570, 189], [-571, 191], [-572, 193], [-573, 195], [-574, 196], [-575, 197], [-576, 198],
];

function inPavilion(x, z) {
  const north = x >= -583 && x <= -577 && z >= 138 && z <= 144;
  const half = x >= -567 && x <= -564 && z >= 179 && z <= 182;
  return north || half;
}

const mix = ["stone", "stone", "andesite", "cobblestone"];
function paveId(x, z) {
  return mix[(((x + z * 3) % 4) + 4) % 4];
}

const SKIP = new Set([
  "calcite",
  "polished_deepslate",
  "stripped_dark_oak_wood",
  "dark_oak_log",
  "stone_bricks",
  "oak_leaves",
  "cherry_log",
  "deepslate_tiles",
  "chiseled_stone_bricks",
  "cobblestone_wall",
  "lantern",
]);

function lerp(a, b) {
  const [x0, z0] = a;
  const [x1, z1] = b;
  const n = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0), 1);
  const out = [];
  for (let t = 0; t <= n; t++) {
    out.push([Math.round(x0 + ((x1 - x0) * t) / n), Math.round(z0 + ((z1 - z0) * t) / n)]);
  }
  return out;
}

const cells = new Set();
for (let i = 0; i < yellow.length; i++) {
  const cur = yellow[i];
  const next = yellow[i + 1];
  let pts = [cur];
  if (next) {
    const d = Math.max(Math.abs(next[0] - cur[0]), Math.abs(next[1] - cur[1]));
    if (d > 0 && d <= 4) pts = lerp(cur, next);
  }
  for (const [x, z] of pts) {
    cells.add(`${x},${z}`);
    cells.add(`${x + 1},${z}`);
    cells.add(`${x},${z + 1}`);
  }
}

console.log("read grounds");
for (const key of cells) {
  const [x, z] = key.split(",").map(Number);
  if (inPavilion(x, z)) continue;
  const a = nid(await get(x, 63, z));
  const b = nid(await get(x, 64, z));
  if (SKIP.has(a) || a === "water") continue;
  if (SKIP.has(b) && b !== "yellow_wool") continue;
  set(x, 63, z, paveId(x, z));
  if (b === "yellow_wool" || b === "air" || b === "short_grass" || b === "fern" || b === "pink_petals" || b === "azure_bluet" || b === "dandelion" || b === "poppy") {
    set(x, 64, z, "air");
  }
}

const stair = (facing) =>
  `dark_oak_stairs[facing=${facing},half=bottom,shape=straight,waterlogged=false]`;

for (const x of [-562, -561]) {
  set(x, 63, 178, stair("north"));
  for (let z = 179; z <= 181; z++) {
    set(x, 64, z, "dark_oak_planks");
    const below = nid(await get(x, 63, z));
    if (below !== "water") set(x, 63, z, "dark_oak_planks");
  }
  set(x, 63, 182, stair("south"));
}
for (let z = 178; z <= 182; z++) {
  set(-563, 64, z, "dark_oak_planks");
  set(-560, 64, z, "dark_oak_planks");
  set(-563, 65, z, "dark_oak_fence");
  set(-560, 65, z, "dark_oak_fence");
}
for (const x of [-565, -564, -563]) {
  if (!inPavilion(x, 178)) set(x, 63, 178, paveId(x, 178));
  if (!inPavilion(x, 183)) set(x, 63, 183, paveId(x, 183));
}

const blocks = [...map.values()];
console.log("place", blocks.length);
console.log(await post(blocks));
console.log("path", await get(-571, 63, 160), await get(-571, 64, 160));
console.log("bridge deck", await get(-562, 64, 180));
console.log("bridge stair", await get(-562, 63, 178));
console.log("pavilion", await get(-565, 63, 180));
console.log("street", await get(-575, 63, 197));
console.log("done");
