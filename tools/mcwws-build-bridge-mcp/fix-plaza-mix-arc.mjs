/**
 * Plaza mix pave (2:1:1 stone/andesite/cobble, per-block random) +
 * west outer quarter-circle arcs to N/S sidewalks.
 * Prefer FAWE when actor-player works; this script is fallback that matches
 * //set stone,stone,andesite,cobblestone weights.
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
  // Same as //set stone,stone,andesite,cobblestone
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

// Plaza rectangle (include west edge that was wrongly cut to grass)
const PX0 = -720,
  PX1 = -703,
  PZ0 = 194,
  PZ1 = 210;

// East taper: from gate width toward narrow road ~z 200-204
// Keep as filled trapezoid then trim; here approximate by per-x z band
function taperZRange(x) {
  // x -702 .. -694: z band shrinks toward 200..204
  if (x < -702 || x > -694) return null;
  const t = (x - -702) / (-694 - -702); // 0 at west, 1 at east
  const z0 = Math.round(194 + t * (200 - 194));
  const z1 = Math.round(210 - t * (210 - 204));
  return [z0, z1];
}

// Outer quarter-circles at NW / SW of plaza, center on plaza corners,
// expanding WEST into sidewalk (x < -720) and N/S beyond z 194/210.
const ARC_R = 5;
const NW = { cx: -720, cz: 194 }; // quarter: x<=cx, z<=cz (west+north)
const SW = { cx: -720, cz: 210 }; // quarter: x<=cx, z>=cz (west+south)

function inOuterArc(x, z, corner, northish) {
  const dx = x - corner.cx;
  const dz = z - corner.cz;
  if (dx > 0) return false; // only west of plaza west edge
  if (northish) {
    if (dz > 0) return false;
  } else {
    if (dz < 0) return false;
  }
  return dx * dx + dz * dz <= ARC_R * ARC_R;
}

const pave = new Map(); // "x,z" -> block
function put(x, z, block) {
  pave.set(`${x},${z}`, { x, y: GY, z, block });
}

// 1) Full plaza rect
for (let x = PX0; x <= PX1; x++) {
  for (let z = PZ0; z <= PZ1; z++) {
    put(x, z, pickMix());
  }
}

// 2) East taper
for (let x = -702; x <= -694; x++) {
  const r = taperZRange(x);
  if (!r) continue;
  for (let z = r[0]; z <= r[1]; z++) put(x, z, pickMix());
}

// 3) Outer arcs (west sidewalk connection) — fill circle cells with mix
for (let x = NW.cx - ARC_R; x <= NW.cx; x++) {
  for (let z = NW.cz - ARC_R; z <= NW.cz; z++) {
    if (inOuterArc(x, z, NW, true)) put(x, z, pickMix());
  }
}
for (let x = SW.cx - ARC_R; x <= SW.cx; x++) {
  for (let z = SW.cz; z <= SW.cz + ARC_R; z++) {
    if (inOuterArc(x, z, SW, false)) put(x, z, pickMix());
  }
}

// Leaf rows + shroomlights along new west-facing curb of arcs & plaza
// North curb of plaza: z=193 leaves along x where paved at z=194
// South curb: z=211
// West curb of plaza: x=-721 leaves where paved at x=-720 (not in arc)
// Along arc outer rim: place leaves on cells just outside the circle

const decor = [];
function leafAt(x, z) {
  decor.push({ x, y: DY, z, block: "oak_leaves[distance=1,persistent=true]" });
}
function shroomAt(x, z) {
  // replace leaf slot with shroomlight
  decor.push({ x, y: DY, z, block: "shroomlight" });
}

function isPaved(x, z) {
  return pave.has(`${x},${z}`);
}

// Decor: for each paved cell, if neighbor is not paved (outer edge), put leaf on that neighbor at DY
const edgeSet = new Map(); // key -> {x,z}
for (const { x, z } of pave.values()) {
  const neigh = [
    [x - 1, z],
    [x + 1, z],
    [x, z - 1],
    [x, z + 1],
  ];
  for (const [nx, nz] of neigh) {
    if (isPaved(nx, nz)) continue;
    // Only decorate west/north/south edges near plaza, not east into gate/taper interior grass wrongly
    // Skip east of taper end and deep inside courtyard grass east of gate
    if (nx > -694) continue;
    if (nz < 188 || nz > 216) continue;
    if (nx < -726) continue;
    edgeSet.set(`${nx},${nz}`, { x: nx, z: nz });
  }
}

const edgeList = [...edgeSet.values()].sort((a, b) => a.x - b.x || a.z - b.z);
let shroomEvery = 0;
for (const e of edgeList) {
  // spacing period 6 along perimeter order
  if (shroomEvery % 6 === 0) shroomAt(e.x, e.z);
  else leafAt(e.x, e.z);
  shroomEvery++;
}

const blocks = [...pave.values(), ...decor];
console.log(`pave cells=${pave.size} decor=${decor.length} total=${blocks.length}`);

// Count mix
const c = { stone: 0, andesite: 0, cobblestone: 0 };
for (const b of pave.values()) c[b.block] = (c[b.block] || 0) + 1;
console.log("mix", c);

await setBlocks(blocks);

// verify samples
const samples = [
  [-710, 202],
  [-712, 200],
  [-708, 205],
  [-715, 198],
  [-720, 194],
  [-723, 194],
  [-723, 191],
  [-725, 194],
  [-720, 210],
  [-723, 213],
];
for (const [x, z] of samples) {
  console.log(`y63 ${x},${z} ->`, await get(x, GY, z));
  console.log(`y64 ${x},${z} ->`, await get(x, DY, z));
}
