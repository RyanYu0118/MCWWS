/**
 * Security booth framed by player's four quartz pillars:
 *   (-696,96) (-690,96) (-696,102) (-690,102)
 * Door on east (x=-690). Do not replace pillars or their y68 lanterns.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const XW = -696,
  XE = -690,
  ZN = 96,
  ZS = 102;
const F = 63;
const doorZ = 99;

function isPillar(x, z) {
  return (x === XW || x === XE) && (z === ZN || z === ZS);
}

async function post(blocks) {
  for (let i = 0; i < blocks.length; i += 400) {
    const slice = blocks.slice(i, i + 400);
    const r = await fetch(`${BASE}/set_blocks`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ world, blocks: slice }),
    });
    const j = await r.json();
    if (!r.ok || j.ok === false) throw new Error(j.error || JSON.stringify(j));
    process.stdout.write(`\r${Math.min(i + 400, blocks.length)}/${blocks.length}`);
  }
  console.log();
}

const map = new Map();
function set(x, y, z, block) {
  if (isPillar(x, z)) return;
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
const stair = (mat, facing, half = "bottom") =>
  `${mat}[facing=${facing},half=${half},shape=straight,waterlogged=false]`;
const pane = (n, s, e, w) =>
  `glass_pane[north=${n},south=${s},east=${e},west=${w},waterlogged=false]`;

for (let x = XW + 1; x <= XE - 1; x++) {
  for (let z = ZN + 1; z <= ZS - 1; z++) {
    set(x, F, z, "polished_andesite");
  }
}

for (let z = ZN + 1; z <= ZS - 1; z++) {
  for (const x of [XW, XE]) {
    if (x === XE && z === doorZ) {
      set(x, 64, z, "smooth_stone");
      continue;
    }
    set(x, 64, z, "calcite");
    if (x === XW) {
      set(x, 65, z, pane(false, false, true, false));
      set(x, 66, z, pane(false, false, true, false));
    } else {
      set(x, 65, z, pane(false, false, false, true));
      set(x, 66, z, pane(false, false, false, true));
    }
  }
}
for (let x = XW + 1; x <= XE - 1; x++) {
  for (const z of [ZN, ZS]) {
    set(x, 64, z, "calcite");
    if (z === ZN) {
      set(x, 65, z, pane(false, true, false, false));
      set(x, 66, z, pane(false, true, false, false));
    } else {
      set(x, 65, z, pane(true, false, false, false));
      set(x, 66, z, pane(true, false, false, false));
    }
  }
}

set(
  XE,
  65,
  doorZ,
  "dark_oak_door[facing=east,half=lower,hinge=left,open=false,powered=false]"
);
set(
  XE,
  66,
  doorZ,
  "dark_oak_door[facing=east,half=upper,hinge=left,open=false,powered=false]"
);

for (let x = XW + 1; x <= XE - 1; x++) {
  for (let z = ZN + 1; z <= ZS - 1; z++) {
    set(x, 67, z, "deepslate_tiles");
  }
}
for (let x = XW + 1; x <= XE - 1; x++) {
  set(x, 67, ZN, stair("deepslate_tile_stairs", "south"));
  set(x, 67, ZS, stair("deepslate_tile_stairs", "north"));
}
for (let z = ZN + 1; z <= ZS - 1; z++) {
  set(XW, 67, z, stair("deepslate_tile_stairs", "east"));
  set(XE, 67, z, stair("deepslate_tile_stairs", "west"));
}

for (let x = XW + 2; x <= XE - 2; x++) {
  set(x, 68, 99, "deepslate_tile_slab[type=bottom,waterlogged=false]");
}

set(-693, 66, 99, "lantern[hanging=true,waterlogged=false]");
set(-691, 66, 98, "lantern[hanging=true,waterlogged=false]");
set(-691, 66, 100, "lantern[hanging=true,waterlogged=false]");

set(-694, 64, 99, stair("dark_oak_stairs", "west"));
set(-693, 64, 99, "lectern[facing=east,has_book=false,powered=false]");
set(-693, 64, 98, "dark_oak_slab[type=bottom,waterlogged=false]");
set(-693, 64, 100, "dark_oak_slab[type=bottom,waterlogged=false]");
set(-694, 64, 98, "potted_azalea_bush");

const blocks = [...map.values()];
console.log(
  `booth on quartz pillars x[${XW}..${XE}] z[${ZN}..${ZS}]  blocks ${blocks.length}`
);
await post(blocks);
console.log("done");
