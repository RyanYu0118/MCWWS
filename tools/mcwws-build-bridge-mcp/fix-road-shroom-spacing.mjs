/**
 * Fix shroomlight spacing on west road: between leaf rows (z=202), period 6
 * (place one, skip 5 blocks, place on next = every 6).
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X_WEST = -721;
const X_EAST = -552;
const Z_MID = 202;
const Z_LEAF_N = 199;
const Z_LEAF_S = 205;
const DY = 64;
const LEAF = "oak_leaves[distance=1,persistent=true,waterlogged=false]";

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

let shrooms = 0;
for (let x = X_WEST; x <= X_EAST; x++) {
  // restore center to air first (clear old every-5 shrooms), leaves stay on sides
  set(x, DY, Z_MID, "air");
  // period 6: (x - X_WEST) % 6 === 0  →  place, then 5 empty, then next
  if ((x - X_WEST) % 6 === 0) {
    set(x, DY, Z_MID, "shroomlight");
    shrooms++;
  }
  // keep leaf rows intact (in case center ops never touched them)
  set(x, DY, Z_LEAF_N, LEAF);
  set(x, DY, Z_LEAF_S, LEAF);
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
all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1));
console.log({ placements: all.length, shrooms, first: X_WEST, period: 6 });
console.log(await postBatch(all));
console.log("shroom spacing fixed");
