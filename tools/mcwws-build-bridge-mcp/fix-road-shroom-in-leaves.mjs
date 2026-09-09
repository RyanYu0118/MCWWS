/**
 * Move shroomlights into BOTH leaf rows (z=199 & z=205); clear road center z=202.
 * Spacing period 6: place one, skip 5 blocks, place next.
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
  // road center must stay walkable
  set(x, DY, Z_MID, "air");

  const onBeat = (x - X_WEST) % 6 === 0;
  if (onBeat) {
    set(x, DY, Z_LEAF_N, "shroomlight");
    set(x, DY, Z_LEAF_S, "shroomlight");
    shrooms += 2;
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
all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1));
console.log({ placements: all.length, shrooms, period: 6, sides: [Z_LEAF_N, Z_LEAF_S] });
console.log(await postBatch(all));
console.log("shrooms in leaf rows done");
