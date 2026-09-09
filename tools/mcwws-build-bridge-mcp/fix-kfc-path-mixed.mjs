/**
 * Replace polished_andesite KFC approach path with mixed stone matching local road.
 * Do not touch z=196 shroomlight/leaf road markers.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";
const GY = 63;

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, x, y, z }),
  });
  const j = await r.json();
  return (j.block || "").replace(/^minecraft:/, "").split("[")[0];
}

async function postBatch(blocks) {
  const res = await fetch(`${BASE}/set_blocks`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, blocks }),
  });
  const json = await res.json();
  if (!res.ok || json.ok === false) throw new Error(json.error || res.statusText);
  return json;
}

const STONEISH = new Set([
  "stone",
  "andesite",
  "cobblestone",
  "blackstone",
  "gravel",
  "stone_bricks",
  "cracked_stone_bricks",
  "mossy_cobblestone",
  "diorite",
  "granite",
  "tuff",
  "deepslate",
  "polished_deepslate",
  "smooth_stone",
  "mossy_stone_bricks",
]);

// Sample palette from nearby mixed road (exclude polished path corridor)
const paletteCounts = new Map();
for (let x = -560; x <= -520; x++) {
  for (let z = 197; z <= 209; z++) {
    // skip the known polished corridor while sampling neighbors
    if (x >= -543 && x <= -541) continue;
    const b = await get(x, GY, z);
    if (STONEISH.has(b)) {
      paletteCounts.set(b, (paletteCounts.get(b) || 0) + 1);
    }
  }
}

let palette = [...paletteCounts.entries()].sort((a, b) => b[1] - a[1]);
if (palette.length === 0) {
  palette = [
    ["stone", 40],
    ["andesite", 25],
    ["cobblestone", 20],
    ["blackstone", 5],
    ["gravel", 5],
    ["stone_bricks", 5],
  ];
}
console.log("palette", Object.fromEntries(palette));

const weighted = [];
for (const [name, w] of palette) {
  for (let i = 0; i < Math.max(1, Math.round(w)); i++) weighted.push(name);
}

function pick(x, z) {
  // deterministic-ish mix so path looks natural but not pure random noise each run
  let h = (x * 374761393 + z * 668265263) | 0;
  h = (h ^ (h >>> 13)) * 1274126177;
  h = h ^ (h >>> 16);
  return weighted[Math.abs(h) % weighted.length];
}

// Find all polished_andesite in approach area and replace
const blocks = [];
let found = 0;
for (let x = -556; x <= -520; x++) {
  for (let z = 197; z <= 210; z++) {
    const b = await get(x, GY, z);
    if (b === "polished_andesite") {
      found++;
      blocks.push({ x, y: GY, z, block: pick(x, z) });
    }
  }
}
console.log("replacing polished_andesite", found);
for (let i = 0; i < blocks.length; i += 4000) {
  console.log(await postBatch(blocks.slice(i, i + 4000)));
}
console.log("done");
