import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  return (await r.json()).block || "";
}
async function post(blocks) {
  const res = await fetch(`${BASE}/set_blocks`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world: "world", blocks }),
  });
  const j = await res.json();
  if (!res.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
  return j;
}

const bambooCols = new Map();
const wetSlabs = [];
for (let z = 134; z <= 198; z++) {
  const x0 = z >= 165 ? -574 : -606;
  for (let x = x0; x <= -560; x++) {
    const b = await get(x, 64, z);
    const id = b.replace("minecraft:", "").split("[")[0];
    if (id === "bamboo_block") bambooCols.set(`${x},${z}`, [64]);
    if ((id === "spruce_slab" || id.includes("slab") || id.includes("stairs")) && b.includes("waterlogged=true")) {
      wetSlabs.push({ x, y: 64, z, b });
    }
  }
}
for (const [key] of bambooCols) {
  const [x, z] = key.split(",").map(Number);
  const ys = [64];
  for (let y = 65; y <= 71; y++) {
    const id = (await get(x, y, z)).replace("minecraft:", "").split("[")[0];
    if (id === "bamboo_block") ys.push(y);
    else break;
  }
  bambooCols.set(key, ys);
}
console.log("bamboo", [...bambooCols.entries()]);
console.log("wet", wetSlabs);

const clear = [];
for (const [key, ys] of bambooCols) {
  const [x, z] = key.split(",").map(Number);
  for (const y of ys) clear.push({ x, y, z, block: "air" });
}
for (const s of wetSlabs) clear.push({ x: s.x, y: s.y, z: s.z, block: "air" });
if (clear.length) console.log("clear", await post(clear));

const y64 = [];
const planted = [];
for (const [key, ys] of bambooCols) {
  const [x, z] = key.split(",").map(Number);
  let px = x,
    pz = z;
  const g = (await get(x, 63, z)).replace("minecraft:", "").split("[")[0];
  if (g === "water" || g === "air") {
    let moved = false;
    for (const [dx, dz] of [
      [1, 0],
      [-1, 0],
      [0, 1],
      [0, -1],
      [1, 1],
      [-1, 1],
      [2, 0],
      [0, 2],
    ]) {
      const nx = x + dx,
        nz = z + dz;
      const ng = (await get(nx, 63, nz)).replace("minecraft:", "").split("[")[0];
      const up = (await get(nx, 64, nz)).replace("minecraft:", "").split("[")[0];
      if (ng !== "water" && ng !== "air" && (up === "air" || up === "short_grass" || up === "fern" || up === "pink_petals")) {
        px = nx;
        pz = nz;
        moved = true;
        break;
      }
    }
    if (!moved) continue;
  }
  const h = Math.max(4, Math.min(6, ys.length + 1));
  y64.push({ x: px, y: 64, z: pz, block: "bamboo[age=1,leaves=none,stage=1]" });
  planted.push({ x: px, z: pz, h });
}
if (y64.length) console.log("base", await post(y64));
const upper = [];
for (const { x, z, h } of planted) {
  for (let y = 65; y < 64 + h; y++) {
    const leaves = y >= 64 + h - 1 ? "large" : y >= 64 + h - 2 ? "small" : "none";
    upper.push({ x, y, z, block: `bamboo[age=1,leaves=${leaves},stage=1]` });
  }
}
if (upper.length) console.log("upper", await post(upper));

const fords = [];
for (const s of wetSlabs) {
  let placed = false;
  for (const [dx, dz] of [
    [1, 0],
    [-1, 0],
    [0, 1],
    [0, -1],
    [2, 0],
    [0, 2],
    [1, 1],
  ]) {
    const nx = s.x + dx,
      nz = s.z + dz;
    const g = (await get(nx, 63, nz)).replace("minecraft:", "").split("[")[0];
    const up = (await get(nx, 64, nz)).replace("minecraft:", "").split("[")[0];
    if (g !== "water" && g !== "air" && (up === "air" || up === "short_grass" || up === "fern" || up === "pink_petals" || up === "moss_carpet")) {
      fords.push({
        x: nx,
        y: 64,
        z: nz,
        block: "spruce_slab[type=bottom,waterlogged=false]",
      });
      placed = true;
      break;
    }
  }
  if (!placed) {
    fords.push({
      x: s.x,
      y: 64,
      z: s.z,
      block: "andesite_slab[type=bottom,waterlogged=false]",
    });
  }
}
if (fords.length) console.log("dry", await post(fords));
console.log("sample bamboo", planted[0] && (await get(planted[0].x, 64, planted[0].z)));
console.log("done", planted.length, fords.length);
