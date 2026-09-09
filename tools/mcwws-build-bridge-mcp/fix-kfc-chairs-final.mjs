/**
 * Non-overlapping tables; chair facing = OPPOSITE of chair→table (visual toward table).
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";
const F = 64;
const doorX = -542;
const Z_WING = 212;
const wallZ = 218;
const WING_L = -548,
  WING_R = -536;

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

function chairFacingVisualTowardTable(dxToTable, dzToTable) {
  const ox = dxToTable === 0 ? 0 : -dxToTable;
  const oz = dzToTable === 0 ? 0 : -dzToTable;
  if (Math.abs(dxToTable) >= Math.abs(dzToTable)) return ox > 0 ? "east" : "west";
  return oz > 0 ? "south" : "north";
}

function placeTable(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  for (const [dx, dz] of [
    [0, 1],
    [0, -1],
  ]) {
    const cx = tx + dx;
    const cz = tz + dz;
    if (cx >= doorX - 1 && cx <= doorX + 1) continue;
    if (cz <= Z_WING || cz >= wallZ) continue;
    const facing = chairFacingVisualTowardTable(-dx, -dz);
    set(
      cx,
      F + 1,
      cz,
      `oak_stairs[facing=${facing},half=bottom,shape=straight,waterlogged=false]`
    );
  }
}

fill(-554, F + 1, 211, -530, F + 3, wallZ - 1, "air");
fill(doorX - 1, F + 1, 211, doorX + 1, F + 1, wallZ - 1, "red_carpet");
for (const x of [WING_L, WING_R]) {
  fill(x, F + 1, 213, x, F + 3, 215, "air");
  const facing = x < doorX ? "east" : "west";
  set(x, F + 1, 214, `oak_door[facing=${facing},half=lower,hinge=left,open=false,powered=false]`);
  set(x, F + 2, 214, `oak_door[facing=${facing},half=upper,hinge=left,open=false,powered=false]`);
}

// centers 3 apart: 214 and 217 only
for (const tx of [-553, -550]) for (const tz of [214, 217]) placeTable(tx, tz);
for (const tx of [-534, -531]) for (const tz of [214, 217]) placeTable(tx, tz);
for (const tx of [-545, -539]) for (const tz of [214, 217]) placeTable(tx, tz);

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
const c215 = all.find((b) => b.x === -545 && b.z === 215 && b.block.includes("stairs"));
const c213 = all.find((b) => b.x === -545 && b.z === 213 && b.block.includes("stairs"));
console.log("south chair of 214 expect facing=south", c215);
console.log("north chair of 214 expect facing=north", c213);
console.log(await postBatch(all));
