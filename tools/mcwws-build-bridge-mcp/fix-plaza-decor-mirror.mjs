/**
 * Re-lay junction leaves with mirrored N/S and position-based shrooms.
 * No decor at x<=-721; wall priority; open T.
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

async function setBlocks(blocks) {
  const j = await api("/set_blocks", { world, blocks });
  if (!j.ok) throw new Error(JSON.stringify(j));
  console.log("wrote", blocks.length);
}

const PX0 = -720;
const PZ0 = 194;
const PZ1 = 210;
const SW_X1 = -721;
const R = 8;
const CNW = { cx: PX0 + R, cz: PZ0 - R };
const LEAF = "oak_leaves[distance=1,persistent=true]";

function mirrorZ(z) {
  return PZ0 + PZ1 - z;
}

function inNWFilletPave(x, z) {
  if (x < PX0 || x > PX0 + R) return false;
  if (z < PZ0 - R || z >= PZ0) return false;
  const dx = x + 0.5 - CNW.cx;
  const dz = z + 0.5 - CNW.cz;
  return dx * dx + dz * dz >= R * R;
}
function inSWFilletPave(x, z) {
  return inNWFilletPave(x, mirrorZ(z));
}
function inPlaza(x, z) {
  return x >= PX0 && x <= -703 && z >= PZ0 && z <= PZ1;
}
function inSidewalk(x, z) {
  return x >= -728 && x <= SW_X1;
}

const WALL_RE =
  /(concrete|quartz|terracotta|brick|iron_bar|glass|fence|wall|door|stairs|slab|log|planks)/i;
function isWallish(id) {
  if (!id || id === "air" || id === "cave_air") return false;
  if (id.includes("leaves") || id === "shroomlight" || id === "grass_block") return false;
  if (["stone", "andesite", "cobblestone", "dirt", "dirt_path"].includes(id)) return false;
  return WALL_RE.test(id) || id.includes("white_") || id.includes("light_gray_");
}

async function hasWall(x, z) {
  for (let y = DY; y <= DY + 6; y++) {
    if (isWallish(await get(x, y, z))) return true;
  }
  return isWallish(await get(x, GY, z));
}

const X0 = -728;
const X1 = -701;
const Z0 = 185;
const Z1 = 219;

// clear decor
const blocks = [];
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    blocks.push({ x, y: DY, z, block: "air" });
  }
}

// rebuild pave key from world read (fast-ish band)
const pave = new Set();
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    const b = await get(x, GY, z);
    if (["stone", "andesite", "cobblestone"].includes(b)) pave.add(`${x},${z}`);
  }
}
const isPaved = (x, z) => pave.has(`${x},${z}`);

function touchesPlazaOrFillet(x, z) {
  for (const [nx, nz] of [
    [x - 1, z],
    [x + 1, z],
    [x, z - 1],
    [x, z + 1],
  ]) {
    if (!isPaved(nx, nz)) continue;
    if (inPlaza(nx, nz) || inNWFilletPave(nx, nz) || inSWFilletPave(nx, nz)) return true;
    // east plaza path north/south rim
    if (nx >= PX0 && nz >= PZ0 && nz <= PZ1) return true;
  }
  return false;
}

const nw = [];
for (const key of pave) {
  const [xs, zs] = key.split(",").map(Number);
  if (inSidewalk(xs, zs)) continue;
  if (zs > PZ0) continue; // only seed from north half + plaza north edge
  for (const [nx, nz] of [
    [xs - 1, zs],
    [xs + 1, zs],
    [xs, zs - 1],
    [xs, zs + 1],
  ]) {
    if (isPaved(nx, nz)) continue;
    if (nx <= SW_X1) continue;
    if (nz > PZ0) continue; // north grass / north rim only
    if (!touchesPlazaOrFillet(nx, nz)) continue;
    nw.push({ x: nx, z: nz });
  }
}
const nwMap = new Map(nw.map((e) => [`${e.x},${e.z}`, e]));

function decorBlock(x, z, alongIndex) {
  // position-stable: same index on mirror gets same type
  return alongIndex % 6 === 0 ? "shroomlight" : LEAF;
}

const sortedNw = [...nwMap.values()].sort((a, b) => a.z - b.z || a.x - b.x);
let skipped = 0;
let idx = 0;
for (const e of sortedNw) {
  const mz = mirrorZ(e.z);
  const pair = [
    [e.x, e.z],
    [e.x, mz],
  ];
  const type = decorBlock(e.x, e.z, idx);
  for (const [x, z] of pair) {
    if (x <= SW_X1) continue;
    if (await hasWall(x, z)) {
      skipped++;
      continue;
    }
    const g = await get(x, GY, z);
    if (isWallish(g)) {
      skipped++;
      continue;
    }
    if (g !== "grass_block" && !isPaved(x, z)) {
      blocks.push({ x, y: GY, z, block: "grass_block" });
    }
    // never put leaf on pavement
    if (isPaved(x, z)) continue;
    blocks.push({ x, y: DY, z, block: type });
  }
  idx++;
}

await setBlocks(blocks);
console.log({ nw: sortedNw.length, skipped, total: blocks.length });

function sym(b) {
  if (b.includes("leaves")) return "L";
  if (b === "shroomlight") return "*";
  if (b === "air" || b === "cave_air") return " ";
  return b[0] || "?";
}
console.log("--- y64 mirrored check ---");
for (let z = 185; z <= 219; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -722; x <= -710; x++) row += sym(await get(x, DY, z));
  console.log(row);
}
