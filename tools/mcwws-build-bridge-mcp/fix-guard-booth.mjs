/**
 * Fix guard booth: door on ground, pane connections, solid west wall, desk to north.
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
    set(x, 64, z, "air");
  }
}

for (let z = ZN + 1; z <= ZS - 1; z++) {
  set(XW, 64, z, "calcite");
  set(XW, 65, z, "calcite");
  set(XW, 66, z, "calcite");
}

for (let x = XW + 1; x <= XE - 1; x++) {
  const west = true;
  const east = true;
  set(x, 64, ZN, "calcite");
  set(x, 65, ZN, pane(false, false, east, west));
  set(x, 66, ZN, pane(false, false, east, west));
  set(x, 64, ZS, "calcite");
  set(x, 65, ZS, pane(false, false, east, west));
  set(x, 66, ZS, pane(false, false, east, west));
}

for (let z of [97, 98, 100, 101]) {
  const north = z !== 100;
  const south = z !== 98;
  set(XE, 64, z, "calcite");
  set(XE, 65, z, pane(north, south, false, false));
  set(XE, 66, z, pane(north, south, false, false));
}

set(
  XE,
  64,
  doorZ,
  "dark_oak_door[facing=east,half=lower,hinge=left,open=false,powered=false]"
);
set(
  XE,
  65,
  doorZ,
  "dark_oak_door[facing=east,half=upper,hinge=left,open=false,powered=false]"
);
set(XE, 66, doorZ, "calcite");

for (const x of [-695, -694, -693, -692]) {
  set(x, 64, 97, "dark_oak_slab[type=bottom,waterlogged=false]");
}
set(-693, 64, 97, "lectern[facing=south,has_book=false,powered=false]");
set(-693, 64, 98, stair("dark_oak_stairs", "south"));
set(-695, 64, 98, "potted_azalea_bush");

const blocks = [...map.values()];
console.log(`fix booth ${blocks.length}`);
await post(blocks);
console.log("done");
