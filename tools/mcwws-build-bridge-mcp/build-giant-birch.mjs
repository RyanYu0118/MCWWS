import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const WORLD = "world";

// Trunk base on grass under player feet projection
const CX = -624;
const CZ = 418;
const GROUND_Y = 63;
const TRUNK_BASE = GROUND_Y + 1; // 64

async function api(path, body) {
  const r = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  const j = await r.json();
  if (!j.ok) throw new Error(`${path}: ${j.error || JSON.stringify(j)}`);
  return j;
}

async function setBlocks(blocks) {
  const CHUNK = 400;
  let placed = 0;
  for (let i = 0; i < blocks.length; i += CHUNK) {
    const slice = blocks.slice(i, i + CHUNK);
    await api("/set_blocks", { world: WORLD, blocks: slice });
    placed += slice.length;
    process.stdout.write(`\rplaced ${placed}/${blocks.length}`);
  }
  console.log();
}

function hash(x, y, z) {
  let n = (x * 374761393 + y * 668265263 + z * 1274126177) | 0;
  n = (n ^ (n >>> 13)) * 1274126177;
  return ((n ^ (n >>> 16)) >>> 0) / 4294967296;
}

function add(map, x, y, z, block) {
  const k = `${x},${y},${z}`;
  const prev = map.get(k);
  // logs win over leaves
  if (prev && prev.startsWith("birch_log") && block.startsWith("birch_leaves")) return;
  map.set(k, block);
}

const blocks = new Map();

// --- 2x2 giant trunk (classic mega silhouette) ---
const trunkH = 22; // top of trunk at TRUNK_BASE + trunkH - 1
for (let dy = 0; dy < trunkH; dy++) {
  const y = TRUNK_BASE + dy;
  for (const [dx, dz] of [
    [0, 0],
    [1, 0],
    [0, 1],
    [1, 1],
  ]) {
    add(blocks, CX + dx, y, CZ + dz, "birch_log[axis=y]");
  }
}

// --- side branches (log arms) ---
const branchSpecs = [
  { y: 10, dir: [1, 0], len: 4 },
  { y: 12, dir: [-1, 0], len: 3 },
  { y: 14, dir: [0, 1], len: 4 },
  { y: 15, dir: [0, -1], len: 3 },
  { y: 17, dir: [1, 1], len: 3 },
  { y: 18, dir: [-1, -1], len: 3 },
];
for (const b of branchSpecs) {
  const axis = Math.abs(b.dir[0]) >= Math.abs(b.dir[1]) ? "x" : "z";
  for (let i = 1; i <= b.len; i++) {
    const x = CX + (b.dir[0] >= 0 ? 1 : 0) + b.dir[0] * i;
    const z = CZ + (b.dir[1] >= 0 ? 1 : 0) + b.dir[1] * i;
    const y = TRUNK_BASE + b.y + Math.floor((i - 1) / 2);
    add(blocks, x, y, z, `birch_log[axis=${axis}]`);
  }
}

// --- canopy: layered ellipsoids of birch leaves ---
const canopyCenterY = TRUNK_BASE + 18;
const layers = [
  { cy: canopyCenterY - 4, rx: 4, ry: 2, rz: 4, dens: 0.55 },
  { cy: canopyCenterY - 1, rx: 6, ry: 3, rz: 6, dens: 0.75 },
  { cy: canopyCenterY + 2, rx: 7, ry: 3, rz: 7, dens: 0.85 },
  { cy: canopyCenterY + 5, rx: 6, ry: 3, rz: 6, dens: 0.8 },
  { cy: canopyCenterY + 8, rx: 4, ry: 2, rz: 4, dens: 0.7 },
  { cy: canopyCenterY + 10, rx: 2, ry: 2, rz: 2, dens: 0.9 },
];

const leaf = "birch_leaves[distance=1,persistent=true,waterlogged=false]";
const trunkSet = new Set(
  [...blocks.entries()].filter(([, b]) => b.startsWith("birch_log")).map(([k]) => k)
);

for (const L of layers) {
  for (let dy = -L.ry; dy <= L.ry; dy++) {
    for (let dz = -L.rz; dz <= L.rz; dz++) {
      for (let dx = -L.rx; dx <= L.rx; dx++) {
        const nx = dx / L.rx;
        const ny = dy / L.ry;
        const nz = dz / L.rz;
        const d2 = nx * nx + ny * ny + nz * nz;
        if (d2 > 1.05) continue;
        const x = CX + dx;
        const y = L.cy + dy;
        const z = CZ + dz;
        const k = `${x},${y},${z}`;
        if (trunkSet.has(k)) continue;
        // denser toward center, with noise holes for natural look
        const edge = Math.sqrt(d2);
        const keep = hash(x, y, z) < L.dens * (1.15 - edge * 0.35);
        if (!keep && edge > 0.55) continue;
        if (!keep && hash(x + 1, y, z) > 0.35) continue;
        add(blocks, x, y, z, leaf);
      }
    }
  }
}

// leaf clusters at branch tips
for (const b of branchSpecs) {
  const tipX = CX + (b.dir[0] >= 0 ? 1 : 0) + b.dir[0] * b.len;
  const tipZ = CZ + (b.dir[1] >= 0 ? 1 : 0) + b.dir[1] * b.len;
  const tipY = TRUNK_BASE + b.y + Math.floor((b.len - 1) / 2);
  for (let dy = -2; dy <= 2; dy++) {
    for (let dz = -2; dz <= 2; dz++) {
      for (let dx = -2; dx <= 2; dx++) {
        if (dx * dx + dy * dy + dz * dz > 5) continue;
        if (hash(tipX + dx, tipY + dy, tipZ + dz) < 0.25) continue;
        add(blocks, tipX + dx, tipY + dy, tipZ + dz, leaf);
      }
    }
  }
}

// clear tall grass only where trunk sits (replace with air first via logs)
const list = [...blocks.entries()].map(([k, block]) => {
  const [x, y, z] = k.split(",").map(Number);
  return { x, y, z, block };
});

console.log(
  `Giant birch: ${list.length} blocks, AABB x[${Math.min(...list.map((b) => b.x))}..${Math.max(
    ...list.map((b) => b.x)
  )}] y[${Math.min(...list.map((b) => b.y))}..${Math.max(...list.map((b) => b.y))}] z[${Math.min(
    ...list.map((b) => b.z)
  )}..${Math.max(...list.map((b) => b.z))}]`
);

await setBlocks(list);
console.log("done");
