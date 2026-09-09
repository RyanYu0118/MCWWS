/**
 * Fix KFC sign (180°: mirror letters + reverse order) and path to shroomlight/leaf road at z=196.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const doorX = -542;
const Z_DOOR = 210;
const Z_ROAD = 196;
const GY = 63;
const F = 64;
const sy = 72; // WALL_TOP+2 from build (F+6+2=72)

const map = new Map();
function set(x, y, z, block) {
  // keep hard constraint for new structure bits except path may go to z=196
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

function mirrorRow(row) {
  return [...row].reverse().join("");
}
function mirrorLetter(patterns) {
  return patterns.map(mirrorRow);
}

// clear old sign band on south facade
for (let x = doorX - 9; x <= doorX + 9; x++) {
  for (let y = sy; y <= sy + 5; y++) {
    set(x, y, Z_DOOR, "white_concrete");
  }
}

// 180°: C F K from west to east, each glyph mirrored
const K = mirrorLetter(["#  #", "# # ", "##  ", "# # ", "#  #"]);
const LETTER_F = mirrorLetter(["####", "#   ", "### ", "#   ", "#   "]);
const LETTER_C = mirrorLetter([" ###", "#   ", "#   ", "#   ", " ###"]);

function plot(ox, patterns) {
  for (let r = 0; r < patterns.length; r++) {
    const row = patterns[r];
    for (let c = 0; c < row.length; c++) {
      if (row[c] === "#") set(ox + c, sy + (patterns.length - 1 - r), Z_DOOR, "red_concrete");
    }
  }
}
// After 180° rotation on wall: former left letter goes right → place C then F then K west→east
plot(doorX - 7, LETTER_C);
plot(doorX - 2, LETTER_F);
plot(doorX + 3, K);

// Path from door south to leaf/shroomlight road (z=196), stay on x=doorX±1
// Don't overwrite shroomlight/leaves at z=196; stop path at z=197
for (let z = Z_ROAD + 1; z <= Z_DOOR; z++) {
  for (let x = doorX - 1; x <= doorX + 1; x++) {
    set(x, GY, z, "polished_andesite");
    // walkable: clear leaves only on path corridor z>196
    if (z > Z_ROAD) {
      set(x, F, z, "air");
      set(x, F + 1, z, "air");
    }
  }
}
// landing pads next to road without destroying road markers at z=196
for (let x = doorX - 1; x <= doorX + 1; x++) {
  set(x, GY, Z_ROAD + 1, "polished_andesite");
}
// soft edge into road: replace only grass under foot at z=196 on path center if not shroomlight
// read via API first in runner — here set dirt_path only at doorX if we detect grass later

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, x, y, z }),
  });
  return (await r.json()).block;
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

// Avoid stomping shroomlight/leaves on z=196
for (let x = doorX - 1; x <= doorX + 1; x++) {
  const b = await get(x, F, Z_ROAD);
  console.log(`road marker ${x},${F},${Z_ROAD} = ${b}`);
}

const all = [...map.values()];
// remove any accidental writes to z=196 y=64 if we had them — none
all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1) || a.y - b.y);
console.log("placements", all.length);
for (let i = 0; i < all.length; i += 4000) {
  const r = await postBatch(all.slice(i, i + 4000));
  console.log("batch", r.changed, "undoable", r.undoable);
}
console.log("sign+path done");
