/**
 * Extend road west: z=200..204 floor, leaf rows z=199 & z=205 with embedded shroomlights.
 * X from -721 to -552 (connects to existing pavement near player).
 * Palette matches feet road: stone / andesite / cobblestone.
 * Shroomlights: BOTH leaf rows, period 6 (place, skip 5, place); NEVER on road center.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X_WEST = -721;
const X_EAST = -552; // join existing stone road
const Z_ROAD0 = 200;
const Z_ROAD1 = 204;
const Z_LEAF_N = 199;
const Z_LEAF_S = 205;
const GY = 63;
const DY = 64;

const weighted = [];
for (let i = 0; i < 55; i++) weighted.push("stone");
for (let i = 0; i < 25; i++) weighted.push("andesite");
for (let i = 0; i < 20; i++) weighted.push("cobblestone");

function pick(x, z) {
  let h = (x * 374761393 + z * 668265263) | 0;
  h = (h ^ (h >>> 13)) * 1274126177;
  h = h ^ (h >>> 16);
  return weighted[Math.abs(h) % weighted.length];
}

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

const LEAF = "oak_leaves[distance=1,persistent=true,waterlogged=false]";

for (let x = X_WEST; x <= X_EAST; x++) {
  // clear walkway clutter at y=64 on road
  for (let z = Z_ROAD0; z <= Z_ROAD1; z++) {
    set(x, DY, z, "air");
    set(x, GY, z, pick(x, z));
  }
  // leaf rows + shroomlights embedded in leaves (both sides); never on road center
  const onBeat = (x - X_WEST) % 6 === 0;
  if (onBeat) {
    set(x, DY, Z_LEAF_N, "shroomlight");
    set(x, DY, Z_LEAF_S, "shroomlight");
  } else {
    set(x, DY, Z_LEAF_N, LEAF);
    set(x, DY, Z_LEAF_S, LEAF);
  }
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

const all = [...map.values()];
all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1) || a.y - b.y);
console.log({
  placements: all.length,
  aabb: { x: [X_WEST, X_EAST], y: [GY, DY], z: [Z_LEAF_N, Z_LEAF_S] },
  shrooms: all.filter((b) => b.block === "shroomlight").length,
});
for (let i = 0; i < all.length; i += 4000) {
  console.log(await postBatch(all.slice(i, i + 4000)));
}
console.log("road extend done");
