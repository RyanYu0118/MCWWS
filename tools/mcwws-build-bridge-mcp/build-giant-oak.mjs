/**
 * Giant oak on the grass directly under Ryan_yu__ (-5028, 63, 1507).
 * Trunk stops below the player's feet (y=86) so they are not stuck in logs.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const WORLD = "world";

const TX = -5028;
const TZ = 1507;
const BASE_Y = 64;
const TRUNK_TOP = 84;

async function setBlocks(blocks) {
  const CHUNK = 400;
  let placed = 0;
  for (let i = 0; i < blocks.length; i += CHUNK) {
    const slice = blocks.slice(i, i + CHUNK);
    const r = await fetch(`${BASE}/set_blocks`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ world: WORLD, blocks: slice }),
    });
    const j = await r.json();
    if (!r.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
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

const map = new Map();
function add(x, y, z, block) {
  if (y < 64 || y > 102) return;
  const k = `${x},${y},${z}`;
  const prev = map.get(k);
  if (prev && prev.startsWith("oak_log") && block.startsWith("oak_leaves")) return;
  map.set(k, block);
}

for (let y = BASE_Y; y <= TRUNK_TOP; y++) {
  for (const [dx, dz] of [
    [0, 0],
    [1, 0],
    [0, 1],
    [1, 1],
  ]) {
    add(TX + dx, y, TZ + dz, "oak_log[axis=y]");
  }
}

const branches = [
  { y: 14, dir: [1, 0], len: 5, axis: "x" },
  { y: 16, dir: [-1, 0], len: 5, axis: "x" },
  { y: 15, dir: [0, 1], len: 5, axis: "z" },
  { y: 17, dir: [0, -1], len: 4, axis: "z" },
  { y: 19, dir: [1, 1], len: 4, axis: "x" },
  { y: 20, dir: [-1, -1], len: 4, axis: "z" },
];
for (const b of branches) {
  for (let i = 1; i <= b.len; i++) {
    const x = TX + (b.dir[0] > 0 ? 1 : 0) + b.dir[0] * i;
    const z = TZ + (b.dir[1] > 0 ? 1 : 0) + b.dir[1] * i;
    const y = BASE_Y + b.y + Math.floor((i - 1) / 2);
    add(x, y, z, `oak_log[axis=${b.axis}]`);
  }
}

const leaf = "oak_leaves[distance=1,persistent=true,waterlogged=false]";
const layers = [
  { cy: 78, rx: 5, ry: 3, rz: 5, dens: 0.7 },
  { cy: 82, rx: 7, ry: 3, rz: 7, dens: 0.82 },
  { cy: 86, rx: 9, ry: 4, rz: 9, dens: 0.88 },
  { cy: 91, rx: 8, ry: 3, rz: 8, dens: 0.84 },
  { cy: 95, rx: 5, ry: 3, rz: 5, dens: 0.78 },
  { cy: 98, rx: 3, ry: 2, rz: 3, dens: 0.9 },
];
const cx = TX + 0.5;
const cz = TZ + 0.5;
for (const L of layers) {
  for (let dy = -L.ry; dy <= L.ry; dy++) {
    for (let dz = -L.rz; dz <= L.rz; dz++) {
      for (let dx = -L.rx; dx <= L.rx; dx++) {
        const nx = dx / L.rx;
        const ny = dy / L.ry;
        const nz = dz / L.rz;
        const d2 = nx * nx + ny * ny + nz * nz;
        if (d2 > 1) continue;
        const x = Math.round(cx + dx);
        const y = L.cy + dy;
        const z = Math.round(cz + dz);
        if (y < 72) continue;
        const edge = Math.sqrt(d2);
        if (edge > 0.42 && hash(x, y, z) > L.dens * (1.2 - edge * 0.45)) continue;
        add(x, y, z, leaf);
      }
    }
  }
}

for (const b of branches) {
  const tipX = TX + (b.dir[0] > 0 ? 1 : 0) + b.dir[0] * b.len;
  const tipZ = TZ + (b.dir[1] > 0 ? 1 : 0) + b.dir[1] * b.len;
  const tipY = BASE_Y + b.y + Math.floor((b.len - 1) / 2);
  for (let dy = -2; dy <= 3; dy++) {
    for (let dz = -3; dz <= 3; dz++) {
      for (let dx = -3; dx <= 3; dx++) {
        if (dx * dx + dy * dy + dz * dz > 8) continue;
        if (hash(tipX + dx, tipY + dy, tipZ + dz) < 0.2) continue;
        add(tipX + dx, tipY + dy, tipZ + dz, leaf);
      }
    }
  }
}

// Keep the air column the player is hovering in.
for (let y = 85; y <= 88; y++) {
  for (const [dx, dz] of [
    [0, 0],
    [1, 0],
    [0, 1],
    [1, 1],
  ]) {
    map.delete(`${TX + dx},${y},${TZ + dz}`);
  }
}

const list = [...map.entries()].map(([k, block]) => {
  const [x, y, z] = k.split(",").map(Number);
  return { x, y, z, block };
});
const xs = list.map((b) => b.x);
const ys = list.map((b) => b.y);
const zs = list.map((b) => b.z);
console.log(
  `oak ${list.length} blocks  x ${Math.min(...xs)}..${Math.max(...xs)}  y ${Math.min(...ys)}..${Math.max(...ys)}  z ${Math.min(...zs)}..${Math.max(...zs)}`
);
await setBlocks(list);
console.log("done");
