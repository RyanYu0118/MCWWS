/**
 * Fix: (1) mirror-symmetric N/S fillets (2) no greenery on sidewalk x<=-721
 * (3) wall-priority: skip/yield when leaf would hit wall — never dig under walls
 * Also clear leaf wall along sidewalk–plaza junction (keep T open).
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
const SW_X1 = -721; // sidewalk inclusive; greenery forbidden for x <= SW_X1
const R = 8;
const CNW = { cx: PX0 + R, cz: PZ0 - R }; // (-712, 186)

function mirrorZ(z) {
  return PZ0 + PZ1 - z; // 194+210-z
}

/** NW corner box pave: outside disk (toward road), exact geometry then mirror */
function inNWFilletPave(x, z) {
  if (x < PX0 || x > PX0 + R) return false;
  if (z < PZ0 - R || z >= PZ0) return false;
  const dx = x + 0.5 - CNW.cx;
  const dz = z + 0.5 - CNW.cz;
  return dx * dx + dz * dz >= R * R;
}

function inSWFilletPave(x, z) {
  // strict mirror of NW
  return inNWFilletPave(x, mirrorZ(z));
}

function inPlaza(x, z) {
  return x >= PX0 && x <= PX1 && z >= PZ0 && z <= PZ1;
}
function inSidewalk(x, z) {
  return x >= SW_X0 && x <= SW_X1;
}
function inTaper(x, z) {
  if (x < -702 || x > -694) return false;
  const t = (x - -702) / (-694 - -702);
  const z0 = Math.round(PZ0 + t * (200 - PZ0));
  const z1 = Math.round(PZ1 - t * (PZ1 - 204));
  return z >= z0 && z <= z1;
}

function shouldPave(x, z) {
  if (inPlaza(x, z) || inTaper(x, z)) return true;
  // sidewalk band only as andesite road — do not treat as fillet host for leaves
  if (inSidewalk(x, z) && z >= PZ0 - R - 1 && z <= PZ1 + R + 1) return true;
  if (inNWFilletPave(x, z) || inSWFilletPave(x, z)) return true;
  return false;
}

const WALL_RE =
  /(concrete|quartz|terracotta|brick|iron_bar|glass|fence|wall|door|stairs|slab|log|planks|copper|deepslate|stone_brick|prismarine|purpur|nether|blackstone|basalt|smooth_stone|packed_mud|mud_brick)/i;

function isWallish(id) {
  if (!id || id === "air" || id === "cave_air") return false;
  if (id.includes("leaves") || id === "shroomlight" || id === "grass_block") return false;
  if (id === "stone" || id === "andesite" || id === "cobblestone" || id === "dirt" || id === "dirt_path")
    return false;
  return WALL_RE.test(id) || id.includes("white_") || id.includes("light_gray_");
}

async function hasWallNearby(x, z) {
  // wall priority: any solid structure at y64..70 blocks leaf here
  for (let y = DY; y <= DY + 6; y++) {
    const b = await get(x, y, z);
    if (isWallish(b)) return true;
  }
  // also if y63 is already a wall foundation (not grass/pavement mix)
  const g = await get(x, GY, z);
  if (isWallish(g)) return true;
  return false;
}

const X0 = SW_X0;
const X1 = PX1 + 2;
const Z0 = PZ0 - R - 2;
const Z1 = PZ1 + R + 2;

const blocks = [];
const paveKeys = new Set();

// 1) Clear ALL y64 decor in AABB (remove sidewalk hedges + asymmetric leaves)
for (let x = X0 - 1; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    blocks.push({ x, y: DY, z, block: "air" });
  }
}

// 2) Reset fillet boxes to grass, then repave symmetrically
for (let x = PX0; x <= PX0 + R; x++) {
  for (let z = PZ0 - R; z < PZ0; z++) {
    blocks.push({ x, y: GY, z, block: "grass_block" });
  }
  for (let z = PZ1 + 1; z <= PZ1 + R; z++) {
    blocks.push({ x, y: GY, z, block: "grass_block" });
  }
}

// 3) Pavement
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    if (!shouldPave(x, z)) continue;
    let block;
    if (inSidewalk(x, z)) block = "andesite";
    else block = pickMix();
    blocks.push({ x, y: GY, z, block });
    paveKeys.add(`${x},${z}`);
  }
}

function isPaved(x, z) {
  return paveKeys.has(`${x},${z}`);
}

function isSidewalkOnlyNeighbor(x, z) {
  // true if all paved neighbors are sidewalk cells — junction must stay open
  let any = false;
  for (const [nx, nz] of [
    [x - 1, z],
    [x + 1, z],
    [x, z - 1],
    [x, z + 1],
  ]) {
    if (!isPaved(nx, nz)) continue;
    any = true;
    if (!inSidewalk(nx, nz)) return false;
  }
  return any;
}

function touchesPlazaOrFillet(x, z) {
  for (const [nx, nz] of [
    [x - 1, z],
    [x + 1, z],
    [x, z - 1],
    [x, z + 1],
  ]) {
    if (!isPaved(nx, nz)) continue;
    if (inPlaza(nx, nz) || inNWFilletPave(nx, nz) || inSWFilletPave(nx, nz) || inTaper(nx, nz))
      return true;
  }
  return false;
}

// 4) Candidate leaf cells: grass next to plaza/fillet curb only
const edgeMap = new Map();
for (const key of paveKeys) {
  const [xs, zs] = key.split(",").map(Number);
  // do not grow leaves from sidewalk cells (prevents hedge on/along sidewalk)
  if (inSidewalk(xs, zs)) continue;
  for (const [nx, nz] of [
    [xs - 1, zs],
    [xs + 1, zs],
    [xs, zs - 1],
    [xs, zs + 1],
  ]) {
    if (isPaved(nx, nz)) continue;
    if (nx <= SW_X1) continue; // never on/west of sidewalk east edge
    if (nx < X0 || nx > X1 || nz < Z0 || nz > Z1) continue;
    if (isSidewalkOnlyNeighbor(nx, nz)) continue;
    if (!touchesPlazaOrFillet(nx, nz)) continue;
    edgeMap.set(`${nx},${nz}`, { x: nx, z: nz });
  }
}

// Mirror filter: keep NW candidates, add exact SW mirrors for symmetry of arc leaves
const nwEdges = [...edgeMap.values()].filter((e) => e.z <= PZ0);
const finalEdges = new Map();
for (const e of nwEdges) {
  finalEdges.set(`${e.x},${e.z}`, e);
  const mz = mirrorZ(e.z);
  // only mirror if still in south grass / south rim band
  if (mz >= PZ1) finalEdges.set(`${e.x},${mz}`, { x: e.x, z: mz });
}
// also keep east-plaza north/south rim that is between PZ0 and PZ1? no — those are on plaza
// keep any north-of-plaza edges already in nwEdges; mirrors handle south

const LEAF = "oak_leaves[distance=1,persistent=true]";
const decorList = [...finalEdges.values()].sort((a, b) => a.z - b.z || a.x - b.x);

let skippedWall = 0;
let placed = 0;
let i = 0;
// Apply wall writes after probing (must await)
const decorBlocks = [];
for (const e of decorList) {
  if (await hasWallNearby(e.x, e.z)) {
    skippedWall++;
    continue; // wall priority: yield, do not dig under / punch through
  }
  // ensure grass under leaf only when current ground is grass/dirt/air-ish — never replace wall
  const ground = await get(e.x, GY, e.z);
  if (isWallish(ground)) {
    skippedWall++;
    continue;
  }
  if (ground !== "grass_block" && !isPaved(e.x, e.z)) {
    decorBlocks.push({ x: e.x, y: GY, z: e.z, block: "grass_block" });
  }
  decorBlocks.push({
    x: e.x,
    y: DY,
    z: e.z,
    block: i % 6 === 0 ? "shroomlight" : LEAF,
  });
  placed++;
  i++;
}

blocks.push(...decorBlocks);

console.log({
  R,
  pave: paveKeys.size,
  edgeCandidates: decorList.length,
  placed,
  skippedWall,
  total: blocks.length,
});

await setBlocks(blocks);

function sym(b) {
  if (b === "stone") return "S";
  if (b === "andesite") return "A";
  if (b === "cobblestone") return "C";
  if (b === "grass_block") return ".";
  if (b.includes("leaves")) return "L";
  if (b === "shroomlight") return "*";
  if (b === "air" || b === "cave_air") return " ";
  if (isWallish(b)) return "W";
  return "?";
}

console.log("--- y63 NW/SW compare (should mirror) ---");
for (let z = 185; z <= 219; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -722; x <= -710; x++) row += sym(await get(x, GY, z));
  console.log(row);
}
console.log("--- y64 decor (no L for x<=-721; open T; yield walls) ---");
for (let z = 185; z <= 219; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -722; x <= -710; x++) row += sym(await get(x, DY, z));
  console.log(row);
}
