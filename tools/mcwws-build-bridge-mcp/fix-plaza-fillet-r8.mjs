/**
 * Fix west T-junction fillets to match user red arcs:
 * quarter-disks into the GRASS pockets (east of N-S sidewalk, north/south of plaza),
 * NOT into the sidewalk. Radius 8 (>=5).
 * Mix: stone,stone,andesite,cobblestone (2:1:1) per-block random.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";
const GY = 63;
const DY = 64;

const headers = {
  Authorization: `Bearer ${token}`,
  "Content-Type": "application/json",
};

async function api(path, body) {
  const r = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
  return r.json();
}

async function get(x, y, z) {
  const j = await api("/get_block", { world, x, y, z });
  return (j.block || "").replace(/^minecraft:/, "").split("[")[0];
}

function pickMix() {
  const bag = ["stone", "stone", "andesite", "cobblestone"];
  return bag[(Math.random() * bag.length) | 0];
}

async function setBlocks(blocks) {
  const chunk = 4000;
  for (let i = 0; i < blocks.length; i += chunk) {
    const slice = blocks.slice(i, i + chunk);
    const j = await api("/set_blocks", { world, blocks: slice });
    if (!j.ok) throw new Error(JSON.stringify(j));
    console.log(`wrote ${i + slice.length}/${blocks.length}`);
  }
}

// Plaza / path band
const PX0 = -720;
const PX1 = -703;
const PZ0 = 194;
const PZ1 = 210;
const R = 8; // >=5, smoother than prior R=5

// Concave corners at west edge of plaza (T into N-S sidewalk)
// Grass pockets are EAST of sidewalk / relative to corner: x >= PX0
const NW = { cx: PX0, cz: PZ0 }; // quarter: dx>=0, dz<=0
const SW = { cx: PX0, cz: PZ1 }; // quarter: dx>=0, dz>=0

function inNW(x, z) {
  const dx = x - NW.cx;
  const dz = z - NW.cz;
  if (dx < 0 || dz > 0) return false;
  // block-center distance for rounder look
  const fx = dx + 0.5;
  const fz = dz - 0.5;
  return fx * fx + fz * fz <= R * R;
}

function inSW(x, z) {
  const dx = x - SW.cx;
  const dz = z - SW.cz;
  if (dx < 0 || dz < 0) return false;
  const fx = dx + 0.5;
  const fz = dz + 0.5;
  return fx * fx + fz * fz <= R * R;
}

const pave = new Map();
const clearY64 = new Map(); // air out old leaves on work AABB
const put = (x, z, block) => pave.set(`${x},${z}`, { x, y: GY, z, block });

// 1) Ensure plaza rect solid
for (let x = PX0; x <= PX1; x++) {
  for (let z = PZ0; z <= PZ1; z++) put(x, z, pickMix());
}

// 2) East taper (keep existing shape)
for (let x = -702; x <= -694; x++) {
  const t = (x - -702) / (-694 - -702);
  const z0 = Math.round(194 + t * (200 - 194));
  const z1 = Math.round(210 - t * (210 - 204));
  for (let z = z0; z <= z1; z++) put(x, z, pickMix());
}

// 3) Correct quarter-disks into grass (north & south of plaza, east of west edge)
for (let x = PX0; x <= PX0 + R; x++) {
  for (let z = PZ0 - R; z <= PZ0; z++) {
    if (inNW(x, z)) put(x, z, pickMix());
  }
  for (let z = PZ1; z <= PZ1 + R; z++) {
    if (inSW(x, z)) put(x, z, pickMix());
  }
}

// 4) Revert WRONG prior west arcs (west of plaza into/near sidewalk north/south)
//    Only touch mix leftovers on grass — do NOT overwrite solid N-S sidewalk andesite band.
const SIDEWALK_X1 = -721; // inclusive east edge of N-S sidewalk
const revertGrass = [];
for (let x = -728; x <= -721; x++) {
  for (let z = PZ0 - R - 1; z < PZ0; z++) {
    // north-west of plaza: if somehow mix, leave sidewalk alone — sidewalk stays
  }
}
// Clear mix stubs east of sidewalk but OUTSIDE the new disks (old jagged bits north/south)
for (let x = PX0; x <= PX0 + R + 2; x++) {
  for (let z = PZ0 - R - 2; z < PZ0; z++) {
    if (pave.has(`${x},${z}`)) continue;
    if (x < PX0) continue;
    revertGrass.push({ x, z });
  }
  for (let z = PZ1 + 1; z <= PZ1 + R + 2; z++) {
    if (pave.has(`${x},${z}`)) continue;
    if (x < PX0) continue;
    revertGrass.push({ x, z });
  }
}

// Work AABB for y64 cleanup (old leaves/shrooms along prior edges)
const cleanX0 = PX0 - 1;
const cleanX1 = PX1 + 2;
const cleanZ0 = PZ0 - R - 2;
const cleanZ1 = PZ1 + R + 2;
for (let x = cleanX0; x <= cleanX1; x++) {
  for (let z = cleanZ0; z <= cleanZ1; z++) {
    clearY64.set(`${x},${z}`, { x, y: DY, z, block: "air" });
  }
}

function isPaved(x, z) {
  return pave.has(`${x},${z}`);
}

// 5) Leaf + shroomlight on outer rim of paved set (within clean band, not on sidewalk west)
const edge = [];
for (const { x, z } of pave.values()) {
  for (const [nx, nz] of [
    [x - 1, z],
    [x + 1, z],
    [x, z - 1],
    [x, z + 1],
  ]) {
    if (isPaved(nx, nz)) continue;
    if (nx <= SIDEWALK_X1) continue; // don't plant on N-S sidewalk
    if (nx > -694 || nz < cleanZ0 || nz > cleanZ1) continue;
    edge.push({ x: nx, z: nz });
  }
}
// unique
const edgeMap = new Map(edge.map((e) => [`${e.x},${e.z}`, e]));
const edgeList = [...edgeMap.values()].sort((a, b) => a.z - b.z || a.x - b.x);

const decor = [];
const LEAF = "oak_leaves[distance=1,persistent=true]";
let i = 0;
for (const e of edgeList) {
  // period 6: shroom every 6 along sorted rim
  const block = i % 6 === 0 ? "shroomlight" : LEAF;
  decor.push({ x: e.x, y: DY, z: e.z, block });
  // remove from clear map so we don't air after placing
  clearY64.delete(`${e.x},${e.z}`);
  i++;
}

// Also: for revertGrass cells that currently may be stone mix, set back to grass_block
const revertBlocks = revertGrass.map(({ x, z }) => ({
  x,
  y: GY,
  z,
  block: "grass_block",
}));

const blocks = [
  ...revertBlocks,
  ...pave.values(),
  ...clearY64.values(),
  ...decor,
];

const mix = { stone: 0, andesite: 0, cobblestone: 0 };
for (const b of pave.values()) mix[b.block]++;
console.log("AABB arcs: NW/SW R=", R, "centers", NW, SW);
console.log("pave", pave.size, "decor", decor.length, "revert", revertBlocks.length, "mix", mix);

await setBlocks(blocks);

// Print arc masks for QA
function sym(b) {
  if (b === "stone") return "S";
  if (b === "andesite") return "A";
  if (b === "cobblestone") return "C";
  if (b === "grass_block") return ".";
  if (b.includes("leaves")) return "L";
  if (b === "shroomlight") return "*";
  return b[0] || "?";
}
console.log("--- NW arc y63 x=-720..-711 z=186..194 ---");
for (let z = 186; z <= 194; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -720; x <= -711; x++) row += sym(await get(x, GY, z));
  console.log(row);
}
console.log("--- SW arc y63 x=-720..-711 z=210..218 ---");
for (let z = 210; z <= 218; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -720; x <= -711; x++) row += sym(await get(x, GY, z));
  console.log(row);
}
console.log("--- NW rim y64 ---");
for (let z = 186; z <= 194; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -720; x <= -711; x++) {
    const b = await get(x, DY, z);
    row += b === "air" || b === "cave_air" ? " " : sym(b);
  }
  console.log(row);
}
