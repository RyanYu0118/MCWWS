/**
 * 1) Invert chair facing (MC stairs visual ≠ facing property)
 * 2) Restore wing curtain glass (z=212)
 * 3) Colonel Sanders head medallion above KFC letters
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
const Z_WING = 212;
const wallZ = 218;
const X0 = -556,
  X1 = -528;
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

/**
 * Visual sit toward table: stairs.facing is OPPOSITE to appearance.
 * facing property = opposite of chair→table direction.
 */
function chairFacingVisualTowardTable(dxToTable, dzToTable) {
  // opposite vector
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

function restoreWingGlass(xa, xb) {
  for (let x = xa; x <= xb; x++) {
    const isPost = x === xa || x === xb || (x - xa) % 3 === 0;
    for (let y = F; y <= F + 3; y++) {
      if (y === F) set(x, y, Z_WING, "iron_block");
      else if (isPost) set(x, y, Z_WING, "gray_concrete");
      else set(x, y, Z_WING, "gray_stained_glass");
    }
    for (let y = F + 4; y <= F + 5; y++) {
      const n = Math.abs((x * 3 + y * 5 + Z_WING * 7) % 5);
      set(x, y, Z_WING, n <= 1 ? "red_concrete" : n <= 3 ? "bricks" : "red_terracotta");
    }
  }
}
restoreWingGlass(X0, WING_L);
restoreWingGlass(WING_R, X1);

fill(-554, F + 1, 211, -530, F + 3, wallZ - 1, "air");
fill(doorX - 1, F + 1, 211, doorX + 1, F + 1, wallZ - 1, "red_carpet");

for (const x of [WING_L, WING_R]) {
  fill(x, F + 1, 213, x, F + 3, 215, "air");
  const facing = x < doorX ? "east" : "west";
  set(x, F + 1, 214, `oak_door[facing=${facing},half=lower,hinge=left,open=false,powered=false]`);
  set(x, F + 2, 214, `oak_door[facing=${facing},half=upper,hinge=left,open=false,powered=false]`);
}

for (const tx of [-553, -550]) for (const tz of [214, 216]) placeTable(tx, tz);
for (const tx of [-534, -531]) for (const tz of [214, 216]) placeTable(tx, tz);
for (const tx of [-545, -539]) for (const tz of [214, 216]) placeTable(tx, tz);

// Colonel head — 13 wide, bottom-up rows (last row drawn at oy)
const HEAD = [
  "   RRRRRRR   ",
  "  RWWWWWWWR  ",
  " RWSSSSSSSWR ",
  " RWSSSSSSSWR ",
  " RWSGSSSGWSR ",
  " RWSSSSSSSWR ",
  " RWSSBBBSSWR ",
  "  RWSSSSSWR  ",
  "   RWWWWWR   ",
  "  RR  R  RR  ",
  "   RRRRRRR   ",
];
const mat = {
  R: "red_concrete",
  W: "smooth_quartz",
  S: "white_terracotta",
  G: "gray_concrete",
  B: "black_concrete",
};

function plotHead(zFace) {
  const h = HEAD.length;
  const w = HEAD[0].length;
  const ox = doorX - Math.floor(w / 2);
  const oy = 76;
  fill(ox - 1, oy - 1, zFace, ox + w, oy + h, zFace, "smooth_quartz");
  for (let r = 0; r < h; r++) {
    const row = HEAD[r];
    for (let c = 0; c < row.length; c++) {
      const ch = row[c];
      if (ch === " " || !mat[ch]) continue;
      set(ox + c, oy + (h - 1 - r), zFace, mat[ch]);
    }
  }
}
plotHead(Z_ENT);
plotHead(Z_ENT - 1);

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
const sample = all.find((b) => b.x === -545 && b.z === 215 && b.block.includes("stairs"));
console.log("chair south of table214 expect facing=south", sample);
console.log("placements", all.length);
for (let i = 0; i < all.length; i += 4000) {
  console.log(await postBatch(all.slice(i, i + 4000)));
}
console.log("done");
