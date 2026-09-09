/**
 * Fix KFC: chair facing toward tables, lobby doors + more seats, restore logo.
 *
 * Chair algorithm: oak_stairs.facing = direction FROM chair TOWARD nearest table
 * (fence with slab above = table).
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const F = 64;
const doorX = -542;
const Z_ENT = 210;
const wallZ = 218;
const WING_L = -548;
const WING_R = -536;

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

/** facing points toward the table */
function chairFacingToward(dx, dz) {
  if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? "east" : "west";
  return dz > 0 ? "south" : "north";
}

function placeTable(tx, tz) {
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  // four sides if space — N/S primary for dining; clear aisle
  const seats = [
    [0, 1],
    [0, -1],
  ];
  for (const [dx, dz] of seats) {
    const cx = tx + dx;
    const cz = tz + dz;
    // keep red carpet aisle clear
    if (cx >= doorX - 1 && cx <= doorX + 1) continue;
    const facing = chairFacingToward(-dx, -dz); // from chair to table = -seat offset
    // seat at (tx+dx,tz+dz), table at (tx,tz); vector chair→table = (-dx,-dz)
    set(
      cx,
      F + 1,
      cz,
      `oak_stairs[facing=${facing},half=bottom,shape=straight,waterlogged=false]`
    );
  }
}

// clear old dining furniture in lobby/wings (keep kitchen past wallZ)
fill(-554, F + 1, 211, -530, F + 3, wallZ - 1, "air");
// re-lay aisle carpet
fill(doorX - 1, F + 1, Z_ENT + 1, doorX + 1, F + 1, wallZ - 1, "red_carpet");

// ========== side doors: lobby ↔ west/east dining wings ==========
function wingDoor(x, hinge) {
  fill(x, F + 1, 214, x, F + 2, 215, "air");
  // double opening in wing wall for walkthrough + one door leaf
  set(x, F + 1, 214, `oak_door[facing=${x < doorX ? "east" : "west"},half=lower,hinge=${hinge},open=false,powered=false]`);
  set(x, F + 2, 214, `oak_door[facing=${x < doorX ? "east" : "west"},half=upper,hinge=${hinge},open=false,powered=false]`);
  fill(x, F + 1, 215, x, F + 2, 215, "air"); // adjacent air so not a dead wall
}
wingDoor(WING_L, "left");
wingDoor(WING_R, "right");
// extra open arch next to doors for wider lobby feel
fill(WING_L, F + 1, 213, WING_L, F + 3, 213, "air");
fill(WING_R, F + 1, 213, WING_R, F + 3, 213, "air");

// ========== denser seating ==========
// west wing
for (const tx of [-553, -550]) {
  for (const tz of [213, 215, 217]) placeTable(tx, tz);
}
// east wing
for (const tx of [-534, -531]) {
  for (const tz of [213, 215, 217]) placeTable(tx, tz);
}
// center lobby flanks (off carpet)
for (const tx of [-545, -539]) {
  for (const tz of [212, 214, 216]) placeTable(tx, tz);
}

// ========== restore KFC logo — bold on white panel, street-facing ==========
// Clean panel on south facade
const sy = F + 6; // 70
fill(doorX - 8, sy - 1, Z_ENT, doorX + 8, sy + 5, Z_ENT, "smooth_quartz");
fill(doorX - 8, sy - 1, Z_ENT, doorX + 8, sy - 1, Z_ENT, "gray_concrete");
fill(doorX - 8, sy + 5, Z_ENT, doorX + 8, sy + 5, Z_ENT, "gray_concrete");

function mirrorRow(row) {
  return [...row].reverse().join("");
}
function mirrorLetter(patterns) {
  return patterns.map(mirrorRow);
}
// After 180° for south-facing read from street: C F K west→east, glyphs mirrored
const LETTER_K = mirrorLetter(["#  #", "# # ", "##  ", "# # ", "#  #"]);
const LETTER_F = mirrorLetter(["####", "#   ", "### ", "#   ", "#   "]);
const LETTER_C = mirrorLetter([" ###", "#   ", "#   ", "#   ", " ###"]);

function plot(ox, patterns, zFace) {
  for (let r = 0; r < patterns.length; r++) {
    const row = patterns[r];
    for (let c = 0; c < row.length; c++) {
      const y = sy + (patterns.length - 1 - r);
      const x = ox + c;
      set(x, y, zFace, row[c] === "#" ? "red_concrete" : "smooth_quartz");
    }
  }
}
plot(doorX - 7, LETTER_C, Z_ENT);
plot(doorX - 2, LETTER_F, Z_ENT);
plot(doorX + 3, LETTER_K, Z_ENT);

// duplicate logo on cantilever nose z=209 so visible from road under vault
fill(doorX - 8, sy - 1, Z_ENT - 1, doorX + 8, sy + 5, Z_ENT - 1, "smooth_quartz");
plot(doorX - 7, LETTER_C, Z_ENT - 1);
plot(doorX - 2, LETTER_F, Z_ENT - 1);
plot(doorX + 3, LETTER_K, Z_ENT - 1);
// keep door clear below
fill(doorX - 1, F, Z_ENT, doorX + 1, F + 2, Z_ENT, "air");
fill(doorX - 1, F, Z_ENT - 1, doorX + 1, F + 2, Z_ENT - 1, "air");

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
console.log("placements", all.length);

 // sanity: print one chair facing
for (const b of all) {
  if (b.block.includes("oak_stairs") && b.x === -545 && b.z === 213) {
    console.log("sample chair", b);
  }
}

for (let i = 0; i < all.length; i += 4000) {
  console.log(await postBatch(all.slice(i, i + 4000)));
}
console.log("lobby/logo/chairs fixed");
