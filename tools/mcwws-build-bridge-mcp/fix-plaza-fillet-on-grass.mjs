/**
 * Fix wrong "leaf islands on road":
 * 1) Clear ALL y64 leaves/shrooms on the junction (they sat on pavement)
 * 2) Restore plaza + west sidewalk pavement continuous (no hedge barrier)
 * 3) R=8 quarter-circles INTO grass (N of z=194 / S of z=210, east of sidewalk)
 * 4) Leaves ONLY on grass cells next to pavement curb
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
const SW_X0 = -728; // N-S sidewalk west band
const SW_X1 = -721;
const R = 8;

// Fillet centers IN GRASS (north/south of plaza, east of sidewalk)
// Tangent to plaza west edge x=PX0 and north/south edges z=PZ0/PZ1
const CNW = { cx: PX0 + R, cz: PZ0 - R }; // (-712, 186)
const CSW = { cx: PX0 + R, cz: PZ1 + R }; // (-712, 218)

function inNWGrassArc(x, z) {
  // quarter disk in grass: north of plaza, east of sidewalk, toward corner
  if (x < PX0 || x > CNW.cx) return false;
  if (z < CNW.cz || z >= PZ0) return false;
  const dx = x + 0.5 - CNW.cx;
  const dz = z + 0.5 - CNW.cz;
  return dx * dx + dz * dz <= R * R;
}

function inSWGrassArc(x, z) {
  if (x < PX0 || x > CSW.cx) return false;
  if (z <= PZ1 || z > CSW.cz) return false;
  const dx = x + 0.5 - CSW.cx;
  const dz = z + 0.5 - CSW.cz;
  return dx * dx + dz * dz <= R * R;
}

function inPlaza(x, z) {
  return x >= PX0 && x <= PX1 && z >= PZ0 && z <= PZ1;
}

function inSidewalk(x, z) {
  return x >= SW_X0 && x <= SW_X1 && z >= PZ0 - R - 2 && z <= PZ1 + R + 2;
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
  if (inNWGrassArc(x, z) || inSWGrassArc(x, z)) return true;
  return false;
}

// Work AABB
const X0 = SW_X0 - 1;
const X1 = PX1 + 2;
const Z0 = PZ0 - R - 2;
const Z1 = PZ1 + R + 2;

const blocks = [];
const paveKeys = new Set();

// 1) Clear ALL y64 decor in work AABB (remove leaf islands on road)
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    blocks.push({ x, y: DY, z, block: "air" });
  }
}

// 2) Revert cells that should NOT be paved back to grass
//    (wrong prior arcs: west of plaza into sidewalk north/south mix leftovers,
//     and wrong bulge east into plaza-north that was already partly fixed)
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    if (shouldPave(x, z)) continue;
    // only revert stone mix / leftover paving, leave other structures alone
    // we'll set grass for non-pave in north/south grass bands
    if (z < PZ0 || z > PZ1 || x < PX0) {
      blocks.push({ x, y: GY, z, block: "grass_block" });
    }
  }
}

// 3) Lay pavement
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    if (!shouldPave(x, z)) continue;
    let block;
    if (inSidewalk(x, z) && !inNWGrassArc(x, z) && !inSWGrassArc(x, z)) {
      // keep N-S sidewalk as andesite (original look); arcs use mix
      block = "andesite";
    } else {
      block = pickMix();
    }
    blocks.push({ x, y: GY, z, block });
    paveKeys.add(`${x},${z}`);
  }
}

// Also ensure full plaza + taper even if outside Z0..Z1 edge cases
for (let x = PX0; x <= PX1; x++) {
  for (let z = PZ0; z <= PZ1; z++) {
    if (paveKeys.has(`${x},${z}`)) continue;
    blocks.push({ x, y: GY, z, block: pickMix() });
    paveKeys.add(`${x},${z}`);
  }
}

function isPaved(x, z) {
  return paveKeys.has(`${x},${z}`);
}

// 4) Leaves ONLY on grass (not paved), adjacent to paved curb
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
    // must be outside pavement = grass curb
    edgeMap.set(`${nx},${nz}`, { x: nx, z: nz });
  }
}

const edgeList = [...edgeMap.values()].sort((a, b) => a.z - b.z || a.x - b.x);
let i = 0;
for (const e of edgeList) {
  // ensure grass under leaf
  blocks.push({ x: e.x, y: GY, z: e.z, block: "grass_block" });
  blocks.push({
    x: e.x,
    y: DY,
    z: e.z,
    block: i % 6 === 0 ? "shroomlight" : LEAF,
  });
  i++;
}

console.log({
  CNW,
  CSW,
  R,
  pave: paveKeys.size,
  decor: edgeList.length,
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
  if (b === "air") return " ";
  return "?";
}

console.log("--- NW grass fillet y63 (should arc into grass N of 194) ---");
for (let z = 185; z <= 195; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -721; x <= -710; x++) row += sym(await get(x, GY, z));
  console.log(row);
}
console.log("--- NW y64 (leaves only on grass '.') ---");
for (let z = 185; z <= 195; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -721; x <= -710; x++) row += sym(await get(x, DY, z));
  console.log(row);
}
console.log("--- entrance z=194 y64 x=-720..-703 (must be clear, no hedge wall) ---");
{
  let row = "194|";
  for (let x = -720; x <= -703; x++) row += sym(await get(x, DY, z === undefined ? 194 : 194));
  // fix
  row = "194|";
  for (let x = -720; x <= -703; x++) row += sym(await get(x, DY, 194));
  console.log(row);
}
