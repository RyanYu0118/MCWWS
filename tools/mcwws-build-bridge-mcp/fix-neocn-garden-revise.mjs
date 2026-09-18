/**
 * Revise neo-Chinese garden:
 * 1) remove south wall (opens view to ancient street)
 * 2) rebuild screen on west, uneven heights + off-center moon gate
 * 3) add mounds / 驳岸 for 错落
 * 4) break regular lily-pad grid into clusters
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, x, y, z }),
  });
  const j = await r.json();
  return (j.block || "").replace(/^minecraft:/, "");
}
async function postFill(x1, y1, z1, x2, y2, z2, block) {
  const res = await fetch(`${BASE}/fill`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, x1, y1, z1, x2, y2, z2, block }),
  });
  const json = await res.json();
  if (!res.ok || json.ok === false) throw new Error(json.error || JSON.stringify(json));
  return json;
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
  if (!res.ok || json.ok === false) throw new Error(json.error || JSON.stringify(json));
  return json;
}

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function id(b) {
  return b.split("[")[0];
}

const KEEP = new Set([
  "stone_bricks",
  "dark_oak_planks",
  "spruce_planks",
  "oak_leaves",
  "polished_andesite_slab",
  "oak_log",
  "spruce_log",
]);

function hillH(x, z) {
  const hills = [
    { cx: -601, cz: 140, r: 5.2, h: 3 },
    { cx: -579, cz: 156, r: 4.4, h: 2 },
    { cx: -596, cz: 143, r: 3.6, h: 2 },
  ];
  let m = 0;
  for (const hill of hills) {
    const d = Math.hypot(x - hill.cx, z - hill.cz);
    if (d < hill.r) {
      const t = 1 - d / hill.r;
      m = Math.max(m, Math.round(hill.h * t * t * 1.35));
    }
  }
  return m;
}

console.log("clear south wall");
console.log(await postFill(-601, 64, 162, -580, 67, 162, "air"));
for (let x = -601; x <= -580; x++) set(x, 63, 162, "grass_block");

const WX = -604;
const gateZ = new Set([148, 149, 150, 151]);
const westH = {
  137: 0,
  138: 2,
  139: 3,
  140: 5,
  141: 4,
  142: 2,
  143: 0,
  144: 3,
  145: 4,
  146: 6,
  147: 4,
  148: 0,
  149: 0,
  150: 0,
  151: 0,
  152: 5,
  153: 6,
  154: 3,
  155: 2,
  156: 4,
  157: 5,
  158: 3,
  159: 0,
  160: 2,
  161: 3,
};

for (let z = 137; z <= 161; z++) {
  const h = westH[z] || 0;
  if (h <= 0 || gateZ.has(z)) continue;
  const g = await get(WX, 63, z);
  if (KEEP.has(id(g))) continue;
  set(WX, 63, z, "calcite");
  for (let y = 64; y < 63 + h; y++) set(WX, y, z, "calcite");
  const capY = 63 + h;
  if (h >= 4 && z % 2 === 1) {
    set(WX, capY, z, "polished_deepslate");
  } else {
    set(WX, capY, z, "dark_oak_slab[type=bottom,waterlogged=false]");
  }
}

for (const [x, y, b] of [
  [WX, 64, "polished_deepslate"],
  [WX, 65, "polished_deepslate"],
  [WX, 66, "polished_deepslate"],
  [WX, 67, "calcite"],
]) {
  set(x, y, 147, b);
  set(x, y, 152, b);
}
set(WX, 67, 148, "polished_deepslate");
set(WX, 68, 149, "polished_deepslate");
set(WX, 68, 150, "polished_deepslate");
set(WX, 67, 151, "polished_deepslate");
set(WX, 66, 148, "air");
set(WX, 66, 151, "air");
set(WX, 63, 149, "smooth_stone");
set(WX, 63, 150, "smooth_stone");
set(WX, 64, 149, "air");
set(WX, 64, 150, "air");
set(WX, 65, 149, "air");
set(WX, 65, 150, "air");

set(WX, 66, 147, "lantern[hanging=false,waterlogged=false]");
set(WX, 67, 152, "lantern[hanging=false,waterlogged=false]");

console.log("scan pond lilies / floor");
const pond = [];
for (let z = 144; z <= 156; z++) {
  for (let x = -598; x <= -582; x++) {
    const b = id(await get(x, 63, z));
    if (b === "water") pond.push([x, z]);
    const up = id(await get(x, 64, z));
    if (up === "lily_pad") set(x, 64, z, "air");
  }
}
const lilySpots = [
  [-593, 151],
  [-592, 151],
  [-593, 150],
  [-587, 148],
  [-586, 149],
  [-589, 153],
  [-594, 147],
];
for (const [x, z] of lilySpots) {
  if (pond.some(([px, pz]) => px === x && pz === z)) set(x, 64, z, "lily_pad");
}

for (let z = 136; z <= 160; z++) {
  for (let x = -603; x <= -576; x++) {
    if (x === WX) continue;
    const h = hillH(x, z);
    if (h <= 0) continue;
    const floor = id(await get(x, 63, z));
    if (
      floor === "water" ||
      floor === "stripped_dark_oak_wood" ||
      floor.includes("plank") ||
      KEEP.has(floor)
    )
      continue;
    if (floor === "bamboo_block") continue;
    const top = id(await get(x, 64, z));
    if (
      top === "bamboo_block" ||
      top === "cherry_log" ||
      top === "lantern" ||
      top === "cobblestone_wall" ||
      top.includes("stairs") ||
      top === "azalea" ||
      top === "flowering_azalea" ||
      top === "lilac" ||
      top === "peony"
    )
      continue;
    for (let i = 0; i < h; i++) {
      const y = 63 + i;
      const last = i === h - 1;
      set(x, y, z, last ? "grass_block" : "dirt");
    }
    if (h >= 2 && (x * 5 + z * 3) % 7 === 1) {
      set(x, 63 + h, z, (x + z) % 2 === 0 ? "azalea" : "fern");
    }
    if (h >= 3 && (x + z) % 5 === 2) {
      set(x, 63 + h, z, "moss_carpet");
    }
  }
}

const bank = [
  [-597, 154, 1, "mossy_cobblestone"],
  [-596, 153, 1, "andesite"],
  [-595, 155, 2, "mossy_cobblestone"],
  [-584, 147, 1, "cobblestone"],
  [-583, 148, 2, "andesite"],
  [-598, 148, 1, "tuff"],
  [-585, 154, 1, "mossy_cobblestone"],
];
for (const [x, z, h, b] of bank) {
  const floor = id(await get(x, 63, z));
  if (floor === "water") continue;
  for (let i = 0; i < h; i++) set(x, 63 + i, z, b);
}

set(-602, 64, 140, "andesite");
set(-602, 65, 140, "andesite");
set(-601, 64, 140, "mossy_cobblestone");
set(-601, 65, 140, "andesite");
set(-601, 66, 140, "andesite");
set(-600, 64, 141, "tuff");
set(-603, 64, 141, "cobblestone");
set(-601, 67, 140, "pointed_dripstone[thickness=tip,vertical_direction=up,waterlogged=false]");

const blocks = [...map.values()];
console.log("place", blocks.length);
for (let i = 0; i < blocks.length; i += 8000) {
  console.log(await postBatch(blocks.slice(i, i + 8000)));
}
console.log("south open", await get(-592, 65, 162));
console.log("west gate", await get(-604, 65, 150), await get(-604, 68, 150));
console.log("done");
