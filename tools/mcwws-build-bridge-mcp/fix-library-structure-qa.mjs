/**
 * Structural QA fix — optimized (FAWE lantern purge + targeted rebuild).
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -4168,
  X1 = -4104;
const Z0 = -1328,
  Z1 = -1286;
const F = 65;
const WALL = 75;
const doorX = -4136;

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

async function api(path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body ?? {}),
  });
  const json = await res.json();
  if (!res.ok || json.ok === false) throw new Error(json.error || JSON.stringify(json));
  return json;
}

async function get(x, y, z) {
  const j = await api("/get_block", { world, x, y, z });
  return (j.block || "").replace(/^minecraft:/, "").split("[")[0];
}

const colXs = [];
const colZs = [];
for (let x = X0 + 8; x < X1; x += 8) colXs.push(x);
for (let z = Z0 + 8; z < Z1; z += 8) colZs.push(z);

// 1) purge ALL lanterns in and around building, then re-place correctly
await api("/fawe_pos", { which: "pos1", world, x: X0 - 2, y: F, z: Z0 - 3 });
await api("/fawe_pos", { which: "pos2", world, x: X1 + 2, y: WALL + 12, z: Z1 + 3 });
try {
  console.log("replace lantern", await api("/fawe_replace", { from: "lantern", to: "air" }));
} catch (e) {
  console.log("lantern replace:", e.message);
}
try {
  console.log("replace wall posts", await api("/fawe_replace", { from: "cobblestone_wall", to: "air" }));
} catch (e) {
  console.log("wall replace:", e.message);
}

// 2) door → exact 2x2
for (let x = doorX - 3; x <= doorX + 3; x++) {
  for (let y = F; y <= F + 3; y++) {
    const isLeaf = (x === doorX - 1 || x === doorX) && (y === F || y === F + 1);
    if (!isLeaf) set(x, y, Z0, "stone_bricks");
  }
}
set(doorX - 1, F, Z0, "spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]");
set(doorX - 1, F + 1, Z0, "spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]");
set(doorX, F, Z0, "spruce_door[facing=south,half=lower,hinge=right,open=false,powered=false]");
set(doorX, F + 1, Z0, "spruce_door[facing=south,half=upper,hinge=right,open=false,powered=false]");
// restore wall plate log above door frame
fill(doorX - 3, F + 4, Z0, doorX + 3, F + 4, Z0, "spruce_log[axis=x]");

// 3) stairs: landing block-y == interior floor F
fill(doorX - 4, 63, Z0 - 4, doorX + 4, F + 1, Z0 - 1, "air");
fill(doorX - 3, 64, Z0 - 1, doorX + 3, 64, Z0 - 1, "stone_bricks");
fill(doorX - 3, F, Z0 - 1, doorX + 3, F, Z0 - 1, "stone_bricks"); // = interior floor height
fill(
  doorX - 3,
  64,
  Z0 - 2,
  doorX + 3,
  64,
  Z0 - 2,
  "stone_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]"
);
fill(doorX - 2, 64, Z0 - 3, doorX + 2, 64, Z0 - 3, "stone_bricks");

// 4) eaves sealed with corbels (no wind gap)
for (let x = X0; x <= X1; x++) {
  set(x, WALL, Z0, "spruce_log[axis=x]");
  set(x, WALL - 1, Z0 - 1, "stone_brick_stairs[facing=south,half=top,shape=straight,waterlogged=false]");
  set(x, WALL, Z0 - 1, "stone_bricks"); // solid eave plate connected via corbel
  set(x, WALL + 1, Z0 - 1, "stone_brick_slab[type=bottom,waterlogged=false]");

  set(x, WALL, Z1, "spruce_log[axis=x]");
  set(x, WALL - 1, Z1 + 1, "stone_brick_stairs[facing=north,half=top,shape=straight,waterlogged=false]");
  set(x, WALL, Z1 + 1, "stone_bricks");
  set(x, WALL + 1, Z1 + 1, "stone_brick_slab[type=bottom,waterlogged=false]");
}
for (let z = Z0; z <= Z1; z++) {
  set(X0, WALL, z, "spruce_log[axis=z]");
  set(X0 - 1, WALL - 1, z, "stone_brick_stairs[facing=east,half=top,shape=straight,waterlogged=false]");
  set(X0 - 1, WALL, z, "stone_bricks");
  set(X0 - 1, WALL + 1, z, "stone_brick_slab[type=bottom,waterlogged=false]");

  set(X1, WALL, z, "spruce_log[axis=z]");
  set(X1 + 1, WALL - 1, z, "stone_brick_stairs[facing=west,half=top,shape=straight,waterlogged=false]");
  set(X1 + 1, WALL, z, "stone_bricks");
  set(X1 + 1, WALL + 1, z, "stone_brick_slab[type=bottom,waterlogged=false]");
}

// 5) columns FIRST, then beams (never clear column cells)
for (const x of colXs) {
  for (const z of colZs) {
    for (let y = F + 1; y <= WALL - 1; y++) {
      set(x, y, z, y === F + 1 ? "stripped_spruce_log[axis=y]" : "spruce_log[axis=y]");
    }
  }
}
for (const z of colZs) {
  for (let x = X0 + 1; x <= X1 - 1; x++) set(x, WALL - 1, z, "spruce_log[axis=x]");
}
for (const x of colXs) {
  for (const z of colZs) set(x, WALL - 1, z, "spruce_log[axis=y]"); // intersections stay vertical
  for (let z = Z0 + 1; z <= Z1 - 1; z++) {
    if (!colZs.includes(z)) set(x, WALL - 1, z, "spruce_log[axis=z]");
  }
}

// 6) lights under solid only, spacing 6
for (let i = 0; i < colXs.length; i += 1) {
  for (let j = 0; j < colZs.length; j += 1) {
    if ((i + j) % 2 !== 0) continue;
    const x = colXs[i],
      z = colZs[j];
    set(x, WALL - 2, z, "lantern[hanging=true,waterlogged=false]");
  }
}
for (let x = X0 + 6; x <= X1 - 6; x += 6) {
  set(x, WALL - 1, Z0 - 1, "lantern[hanging=true,waterlogged=false]"); // under solid eave plate at WALL
  set(x, WALL - 1, Z1 + 1, "lantern[hanging=true,waterlogged=false]");
}

const all = [...map.values()];
all.sort((a, b) => a.y - b.y);
console.log("placements", all.length);
for (let i = 0; i < all.length; i += 8000) {
  console.log(await api("/set_blocks", { world, blocks: all.slice(i, i + 8000) }));
}

// final spot checks
console.log("QA door L/R/mid", await get(doorX - 1, F, Z0), await get(doorX, F, Z0), await get(doorX + 1, F, Z0));
console.log("QA above door", await get(doorX, F + 2, Z0), await get(doorX - 2, F, Z0));
console.log("QA landing=F", await get(doorX, F, Z0 - 1), "step", await get(doorX, 64, Z0 - 2));
console.log("QA eave", await get(doorX, WALL - 1, Z0 - 1), await get(doorX, WALL, Z0 - 1));
console.log("QA column", await get(colXs[0], F + 2, colZs[0]), await get(colXs[0], WALL - 1, colZs[0]));

// sample float check at light positions
for (const x of [colXs[0], colXs[1], doorX]) {
  for (const z of [colZs[0], Z0 - 1]) {
    for (let y = WALL - 2; y <= WALL; y++) {
      const b = await get(x, y, z);
      if (b === "lantern") {
        const above = await get(x, y + 1, z);
        console.log("light", x, y, z, "above", above, above === "air" ? "FLOAT!" : "ok");
      }
    }
  }
}
console.log("done");
