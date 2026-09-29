/**
 * Cover shroomlight/froglight holes in the sidewalk wall.
 */
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
function isWall(b) {
  const n = nid(b);
  return (
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "iron_bars" ||
    n === "smooth_quartz_slab" ||
    n === "lantern"
  );
}
function isGlow(b) {
  const n = nid(b);
  return n.includes("froglight") || n === "shroomlight";
}
function isWalk(b) {
  const n = nid(b);
  return (
    n === "andesite" ||
    n === "stone" ||
    n === "cobblestone" ||
    n === "mossy_cobblestone" ||
    isGlow(b)
  );
}

const hits = [];
async function consider(x, z) {
  const a = await get(x, 64, z);
  if (!isGlow(a) && nid(a) !== "oak_leaves" && nid(a) !== "azalea_leaves") return;
  if (!isGlow(a)) return;
  const n = isWall(await get(x, 64, z - 1));
  const s = isWall(await get(x, 64, z + 1));
  const e = isWall(await get(x + 1, 64, z));
  const w = isWall(await get(x - 1, 64, z));
  const gy = nid(await get(x, 63, z));
  const g64 = nid(await get(x, 64, z));
  const onInner =
    gy === "grass_block" ||
    gy === "dirt" ||
    gy === "coarse_dirt" ||
    isWalk(await get(x - 1, 63, z)) ||
    isWalk(await get(x, 63, z - 1)) ||
    isWalk(await get(x, 63, z + 1));
  if (n || s || e || w || onInner) {
    hits.push({ x, z, n, s, e, w, gy, y64: g64 });
  }
}

console.log("scan near player + west line");
for (let z = 180; z <= 230; z++) {
  for (let x = -724; x <= -704; x++) await consider(x, z);
}
for (let z = 26; z <= 490; z++) {
  await consider(-720, z);
  await consider(-719, z);
}
for (let x = -718; x <= -706; x++) {
  await consider(x, 193);
  await consider(x, 211);
}

console.log("hits", hits.length);
for (const h of hits) console.log(JSON.stringify(h));

const blocks = [];
function addStack(x, z, face, n, s, e, w) {
  const pillar = face === "east" ? z % 6 === 0 : x % 6 === 0;
  if (pillar) {
    for (let y = 64; y <= 67; y++) blocks.push({ x, y, z, block: "quartz_pillar[axis=y]" });
    blocks.push({ x, y: 68, z, block: "lantern[hanging=false,waterlogged=false]" });
  } else {
    blocks.push({ x, y: 64, z, block: "polished_andesite" });
    blocks.push({
      x,
      y: 65,
      z,
      block: `smooth_quartz_stairs[facing=${face},half=top,shape=straight,waterlogged=false]`,
    });
    blocks.push({
      x,
      y: 66,
      z,
      block: `iron_bars[north=${n},south=${s},east=${e},west=${w},waterlogged=false]`,
    });
    blocks.push({ x, y: 67, z, block: "smooth_quartz_slab[type=bottom,waterlogged=false]" });
  }
}

for (const h of hits) {
  const face = h.w || h.e ? (h.n || h.s ? (h.s || h.n ? "east" : "south") : "east") : h.n ? "south" : "east";
  addStack(h.x, h.z, h.n && !h.s && !h.w && h.e ? "south" : face, h.n, h.s, h.e, h.w);
}

if (blocks.length) {
  const r = await fetch(`${BASE}/set_blocks`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", blocks }),
  });
  console.log(await r.json());
}
console.log("placed", blocks.length);
