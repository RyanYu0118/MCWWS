/**
 * Correct west T-junction fillets (C1 with N-S sidewalk + plaza E-W edges):
 * quarter circles in the GRASS pockets NW / SW of corner (-720,194) / (-720,210),
 * centers at (-720-R, 194-R) and (-720-R, 210+R), R>=5.
 * Removes prior wrong quadrant (x>=-720, z<=194 bulge into plaza north).
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

const PX0 = -720;
const PX1 = -703;
const PZ0 = 194;
const PZ1 = 210;
const R = 8;

// C1 exterior fillet centers (in grass, SW of corner in map coords)
const CNW = { cx: PX0 - R, cz: PZ0 - R }; // (-728, 186)
const CSW = { cx: PX0 - R, cz: PZ1 + R }; // (-728, 218)

function inNWArc(x, z) {
  const dx = x - CNW.cx + 0.5;
  const dz = z - CNW.cz + 0.5;
  if (dx * dx + dz * dz > R * R) return false;
  // quarter disk: east of center (toward roads), south of center north pocket
  return x >= CNW.cx && z >= CNW.cz;
}

function inSWArc(x, z) {
  const dx = x - CSW.cx + 0.5;
  const dz = z - CSW.cz + 0.5;
  if (dx * dx + dz * dz > R * R) return false;
  return x >= CSW.cx && z <= CSW.cz;
}

// Wrong prior bulge: center at plaza corner, NE quadrant (x>=-720, z<=194)
function inWrongBulge(x, z) {
  const dx = x - PX0 + 0.5;
  const dz = z - PZ0 - 0.5;
  if (dx < 0 || dz > 0) return false;
  return dx * dx + dz * dz <= R * R && (x > PX0 || z < PZ0);
}

function inPlaza(x, z) {
  return x >= PX0 && x <= PX1 && z >= PZ0 && z <= PZ1;
}

function inTaper(x, z) {
  if (x < -702 || x > -694) return false;
  const t = (x - -702) / (-694 - -702);
  const z0 = Math.round(194 + t * (200 - 194));
  const z1 = Math.round(210 - t * (210 - 204));
  return z >= z0 && z <= z1;
}

function shouldPave(x, z) {
  if (inWrongBulge(x, z)) return false;
  if (inPlaza(x, z) || inTaper(x, z)) return true;
  if (inNWArc(x, z) || inSWArc(x, z)) return true;
  return false;
}

const pave = new Map();
const put = (x, z, block) => pave.set(`${x},${z}`, { x, y: GY, z, block });

// Plaza + taper
for (let x = PX0; x <= PX1; x++) {
  for (let z = PZ0; z <= PZ1; z++) {
    if (shouldPave(x, z)) put(x, z, pickMix());
  }
}
for (let x = -702; x <= -694; x++) {
  const t = (x - -702) / (-694 - -702);
  const z0 = Math.round(194 + t * (200 - 194));
  const z1 = Math.round(210 - t * (210 - 204));
  for (let z = z0; z <= z1; z++) put(x, z, pickMix());
}

// Correct C1 arcs in grass pockets (incl. sidewalk band x=-728..-721)
for (let x = CNW.cx; x <= PX0; x++) {
  for (let z = CNW.cz; z <= PZ0; z++) {
    if (inNWArc(x, z)) put(x, z, pickMix());
  }
}
for (let x = CSW.cx; x <= PX0; x++) {
  for (let z = PZ1; z <= CSW.cz; z++) {
    if (inSWArc(x, z)) put(x, z, pickMix());
  }
}

// Revert wrong bulge + any stray mix outside shouldPave in work AABB
const revert = [];
for (let x = PX0; x <= PX0 + R + 1; x++) {
  for (let z = PZ0 - R - 1; z <= PZ0; z++) {
    if (shouldPave(x, z)) continue;
    revert.push({ x, y: GY, z, block: "grass_block" });
  }
  for (let z = PZ1; z <= PZ1 + R + 1; z++) {
    if (shouldPave(x, z)) continue;
    revert.push({ x, y: GY, z, block: "grass_block" });
  }
}

// y64 cleanup + decor on outer rim
const cleanZ0 = CNW.cz - 1;
const cleanZ1 = CSW.cz + 1;
const cleanX0 = CNW.cx - 1;
const cleanX1 = PX1 + 2;
const clearY64 = [];
for (let x = cleanX0; x <= cleanX1; x++) {
  for (let z = cleanZ0; z <= cleanZ1; z++) {
    clearY64.push({ x, y: DY, z, block: "air" });
  }
}

function isPaved(x, z) {
  return pave.has(`${x},${z}`);
}

const edgeMap = new Map();
for (const { x, z } of pave.values()) {
  for (const [nx, nz] of [
    [x - 1, z],
    [x + 1, z],
    [x, z - 1],
    [x, z + 1],
  ]) {
    if (isPaved(nx, nz)) continue;
    if (nx < cleanX0 || nx > cleanX1 || nz < cleanZ0 || nz > cleanZ1) continue;
    edgeMap.set(`${nx},${nz}`, { x: nx, z: nz });
  }
}

const LEAF = "oak_leaves[distance=1,persistent=true]";
const edgeList = [...edgeMap.values()].sort((a, b) => a.z - b.z || a.x - b.x);
const decor = [];
let i = 0;
for (const e of edgeList) {
  decor.push({
    x: e.x,
    y: DY,
    z: e.z,
    block: i % 6 === 0 ? "shroomlight" : LEAF,
  });
  i++;
}

const blocks = [...revert, ...pave.values(), ...clearY64, ...decor];
console.log(
  "C1 fillet CNW",
  CNW,
  "CSW",
  CSW,
  "pave",
  pave.size,
  "revert",
  revert.length,
  "decor",
  decor.length
);

await setBlocks(blocks);

function sym(b) {
  if (b === "stone") return "S";
  if (b === "andesite") return "A";
  if (b === "cobblestone") return "C";
  if (b === "grass_block") return ".";
  return b[0] || "?";
}
console.log("--- NW pocket y63 x=-729..-719 z=185..195 ---");
for (let z = 185; z <= 195; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -729; x <= -719; x++) row += sym(await get(x, GY, z));
  console.log(row);
}
console.log("--- SW pocket y63 x=-729..-719 z=209..219 ---");
for (let z = 209; z <= 219; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -729; x <= -719; x++) row += sym(await get(x, GY, z));
  console.log(row);
}
