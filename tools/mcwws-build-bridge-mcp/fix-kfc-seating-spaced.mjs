/**
 * Re-seat lobby/wings with non-overlapping tables; chairs face toward table.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";
const F = 64;
const doorX = -542;
const wallZ = 218;

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

function chairFacingToward(dx, dz) {
  if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? "east" : "west";
  return dz > 0 ? "south" : "north";
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
    if (cz >= wallZ) continue;
    // vector chair → table
    const facing = chairFacingToward(-dx, -dz);
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

// keep wing door openings
for (const x of [-548, -536]) {
  fill(x, F + 1, 213, x, F + 3, 215, "air");
  const facing = x < doorX ? "east" : "west";
  set(x, F + 1, 214, `oak_door[facing=${facing},half=lower,hinge=left,open=false,powered=false]`);
  set(x, F + 2, 214, `oak_door[facing=${facing},half=upper,hinge=left,open=false,powered=false]`);
}

// spacing: table centers 3 apart → seats don't collide
for (const tx of [-553, -550]) for (const tz of [213, 216]) placeTable(tx, tz);
for (const tx of [-534, -531]) for (const tz of [213, 216]) placeTable(tx, tz);
for (const tx of [-545, -539]) for (const tz of [212, 215]) placeTable(tx, tz);

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
// verify: table -545,212 → chair -545,213 must face north
const c = all.find((b) => b.x === -545 && b.z === 213 && b.block.includes("stairs"));
console.log("expect facing=north", c);
console.log(await postBatch(all));
