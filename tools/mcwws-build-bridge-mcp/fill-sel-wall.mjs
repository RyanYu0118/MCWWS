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
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
function nid(b) {
  return (b || "").split("[")[0];
}
function isWalk(b) {
  const n = nid(b);
  return (
    n === "andesite" ||
    n === "stone" ||
    n === "cobblestone" ||
    n === "mossy_cobblestone" ||
    n.includes("froglight") ||
    n === "shroomlight"
  );
}
function isReplaceable(b) {
  const n = nid(b);
  return (
    n === "air" ||
    n === "short_grass" ||
    n === "tall_grass" ||
    n.endsWith("_leaves") ||
    n.includes("froglight") ||
    n === "shroomlight" ||
    n === "vine" ||
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "iron_bars" ||
    n === "smooth_quartz_slab" ||
    n === "lantern"
  );
}

const cells = new Map();
function add(x, z, face) {
  cells.set(`${x},${z}`, { x, z, face });
}

for (let z = 211; z <= 216; z++) {
  for (let x = -719; x <= -706; x++) {
    if (nid(await get(x, 63, z)) !== "grass_block") continue;
    const w = isWalk(await get(x - 1, 63, z));
    const n = isWalk(await get(x, 63, z - 1));
    if (w) add(x, z, "east");
    else if (n) add(x, z, "south");
  }
}

// keep only the inner lip: westernmost per z, plus northernmost per x in the box
const byZ = new Map();
const byX = new Map();
for (const c of cells.values()) {
  if (!byZ.has(c.z) || c.x < byZ.get(c.z).x) byZ.set(c.z, c);
  if (!byX.has(c.x) || c.z < byX.get(c.x).z) byX.set(c.x, c);
}
const lip = new Map();
for (const c of byZ.values()) lip.set(`${c.x},${c.z}`, c);
for (const c of byX.values()) {
  if (c.z === 211 || c.z === 212) lip.set(`${c.x},${c.z}`, { ...c, face: "south" });
}

console.log("lip", [...lip.values()]);

const wallSet = new Set(lip.keys());
wallSet.add("-720,216");
wallSet.add("-720,217");
const blocks = [];
for (const c of lip.values()) {
  let ok = true;
  for (let y = 64; y <= 68; y++) {
    if (!isReplaceable(await get(c.x, y, c.z))) {
      ok = false;
      break;
    }
  }
  if (!ok) {
    console.log("skip", c);
    continue;
  }
  const pillar = c.face === "east" ? c.z % 6 === 0 : c.x % 6 === 0;
  if (pillar) {
    for (let y = 64; y <= 67; y++) blocks.push({ x: c.x, y, z: c.z, block: "quartz_pillar[axis=y]" });
    blocks.push({ x: c.x, y: 68, z: c.z, block: "lantern[hanging=false,waterlogged=false]" });
  } else {
    const n = wallSet.has(`${c.x},${c.z - 1}`);
    const s = wallSet.has(`${c.x},${c.z + 1}`);
    const e = wallSet.has(`${c.x + 1},${c.z}`);
    const w = wallSet.has(`${c.x - 1},${c.z}`);
    blocks.push({ x: c.x, y: 64, z: c.z, block: "polished_andesite" });
    blocks.push({
      x: c.x,
      y: 65,
      z: c.z,
      block: `smooth_quartz_stairs[facing=${c.face},half=top,shape=straight,waterlogged=false]`,
    });
    blocks.push({
      x: c.x,
      y: 66,
      z: c.z,
      block: `iron_bars[north=${n},south=${s},east=${e},west=${w},waterlogged=false]`,
    });
    blocks.push({ x: c.x, y: 67, z: c.z, block: "smooth_quartz_slab[type=bottom,waterlogged=false]" });
  }
}

const r = await fetch(`${BASE}/set_blocks`, {
  method: "POST",
  headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
  body: JSON.stringify({ world: "world", blocks }),
});
console.log(await r.json());
console.log("placed", blocks.length);
