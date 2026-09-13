/**
 * Correct grass-side fillet (C1):
 * In corner box, pave where dist(center) >= R (toward road),
 * leave quarter-disk dist < R as grass (outer).
 * Previous dist<=R put pavement blobs/islands in grass and leaf rings on road.
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
  const chunk = 3500;
  for (let i = 0; i < blocks.length; i += chunk) {
    const slice = blocks.slice(i, i + chunk);
    const j = await api("/set_blocks", { world, blocks: slice });
    if (!j.ok) throw new Error(JSON.stringify(j));
    console.log(`wrote ${i + slice.length}/${blocks.length}`);
  }
}

const PX0 = -720;
const PX1 = -703;
const PZ0 = 194;
const PZ1 = 210;
const SW_X0 = -728;
const SW_X1 = -721;
const R = 8;

// Centers deep in grass; quarter-disk around center stays GRASS
const CNW = { cx: PX0 + R, cz: PZ0 - R }; // (-712, 186)
const CSW = { cx: PX0 + R, cz: PZ1 + R }; // (-712, 218)

/** NW corner box: x in [PX0, PX0+R], z in [PZ0-R, PZ0]; pave outside disk */
function inNWFilletPave(x, z) {
  if (x < PX0 || x > PX0 + R) return false;
  if (z < PZ0 - R || z > PZ0) return false;
  if (z === PZ0 && x >= PX0) return false; // plaza row handled separately
  const dx = x + 0.5 - CNW.cx;
  const dz = z + 0.5 - CNW.cz;
  return dx * dx + dz * dz >= R * R;
}

function inSWFilletPave(x, z) {
  if (x < PX0 || x > PX0 + R) return false;
  if (z < PZ1 || z > PZ1 + R) return false;
  if (z === PZ1 && x >= PX0) return false;
  const dx = x + 0.5 - CSW.cx;
  const dz = z + 0.5 - CSW.cz;
  return dx * dx + dz * dz >= R * R;
}

/** Entire NW/SW corner boxes (for cleanup) */
function inNWBox(x, z) {
  return x >= PX0 && x <= PX0 + R && z >= PZ0 - R && z <= PZ0;
}
function inSWBox(x, z) {
  return x >= PX0 && x <= PX0 + R && z >= PZ1 && z <= PZ1 + R;
}

function inPlaza(x, z) {
  return x >= PX0 && x <= PX1 && z >= PZ0 && z <= PZ1;
}
function inSidewalk(x, z) {
  return x >= SW_X0 && x <= SW_X1 && z >= PZ0 - R - 1 && z <= PZ1 + R + 1;
}
function inTaper(x, z) {
  if (x < -702 || x > -694) return false;
  const t = (x - -702) / (-694 - -702);
  const z0 = Math.round(PZ0 + t * (200 - PZ0));
  const z1 = Math.round(PZ1 - t * (PZ1 - 204));
  return z >= z0 && z <= z1;
}

function shouldPave(x, z) {
  if (inPlaza(x, z) || inSidewalk(x, z) || inTaper(x, z)) return true;
  if (inNWFilletPave(x, z) || inSWFilletPave(x, z)) return true;
  return false;
}

const X0 = SW_X0 - 1;
const X1 = Math.max(PX1, PX0 + R) + 2;
const Z0 = PZ0 - R - 2;
const Z1 = PZ1 + R + 2;

const blocks = [];
const paveKeys = new Set();

// Clear all y64 decor in work area (kill leaf islands / hedge wall)
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    blocks.push({ x, y: DY, z, block: "air" });
  }
}

// Reset corner boxes + stray prior arcs to grass first
for (let x = PX0; x <= PX0 + R; x++) {
  for (let z = PZ0 - R; z < PZ0; z++) {
    blocks.push({ x, y: GY, z, block: "grass_block" });
  }
  for (let z = PZ1 + 1; z <= PZ1 + R; z++) {
    blocks.push({ x, y: GY, z, block: "grass_block" });
  }
}
// Also clear wrong west-of-plaza mix stubs north/south of sidewalk band
for (let x = SW_X0; x < PX0; x++) {
  for (let z = PZ0 - R; z < PZ0; z++) {
    if (inSidewalk(x, z)) continue;
    blocks.push({ x, y: GY, z, block: "grass_block" });
  }
  for (let z = PZ1 + 1; z <= PZ1 + R; z++) {
    if (inSidewalk(x, z)) continue;
    blocks.push({ x, y: GY, z, block: "grass_block" });
  }
}

// Lay pavement
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    if (!shouldPave(x, z)) continue;
    let block;
    if (inSidewalk(x, z) && !inNWBox(x, z) && !inSWBox(x, z)) {
      block = "andesite";
    } else if (inSidewalk(x, z) && (z < PZ0 || z > PZ1)) {
      // sidewalk continues N/S past plaza — keep andesite
      block = "andesite";
    } else {
      block = pickMix();
    }
    // sidewalk cells that are pure sidewalk
    if (inSidewalk(x, z) && !inNWFilletPave(x, z) && !inSWFilletPave(x, z) && !inPlaza(x, z)) {
      block = "andesite";
    }
    blocks.push({ x, y: GY, z, block });
    paveKeys.add(`${x},${z}`);
  }
}

function isPaved(x, z) {
  return paveKeys.has(`${x},${z}`);
}

// Leaves ONLY on grass adjacent to pavement
const LEAF = "oak_leaves[distance=1,persistent=true]";
const edgeMap = new Map();
for (const key of paveKeys) {
  const [xs, zs] = key.split(",").map(Number);
  for (const [nx, nz] of [
    [xs - 1, zs],
    [xs + 1, zs],
    [xs, zs - 1],
    [xs, zs + 1],
  ]) {
    if (isPaved(nx, nz)) continue;
    if (nx < X0 || nx > X1 || nz < Z0 || nz > Z1) continue;
    edgeMap.set(`${nx},${nz}`, { x: nx, z: nz });
  }
}
const edgeList = [...edgeMap.values()].sort((a, b) => a.z - b.z || a.x - b.x);
let i = 0;
for (const e of edgeList) {
  blocks.push({ x: e.x, y: GY, z: e.z, block: "grass_block" });
  blocks.push({
    x: e.x,
    y: DY,
    z: e.z,
    block: i % 6 === 0 ? "shroomlight" : LEAF,
  });
  i++;
}

console.log({ CNW, CSW, R, pave: paveKeys.size, decor: edgeList.length, total: blocks.length });
await setBlocks(blocks);

function sym(b) {
  if (b === "stone") return "S";
  if (b === "andesite") return "A";
  if (b === "cobblestone") return "C";
  if (b === "grass_block") return ".";
  if (b.includes("leaves")) return "L";
  if (b === "shroomlight") return "*";
  if (b === "air") return " ";
  return "?";
}

console.log("--- NW fillet y63: pavement near corner, grass disk outer ---");
for (let z = 185; z <= 195; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -721; x <= -710; x++) row += sym(await get(x, GY, z));
  console.log(row);
}
console.log("--- NW y64: leaves on grass only, no hedge on z=194 ---");
for (let z = 185; z <= 195; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -721; x <= -710; x++) {
    const b = await get(x, DY, z);
    row += b === "air" || b === "cave_air" ? " " : sym(b);
  }
  console.log(row);
}
{
  let row = "ent|";
  for (let x = -720; x <= -703; x++) {
    const b = await get(x, DY, 194);
    row += b === "air" || b === "cave_air" ? " " : sym(b);
  }
  console.log(row);
}
