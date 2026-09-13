/**
 * Fix over-fill from quartz roof: only pier body y65-69 counts as wall.
 * Remove bogus y64 concrete under roof-only cells; restore grass; finish leaf to wall.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";
const headers = {
  Authorization: `Bearer ${token}`,
  "Content-Type": "application/json",
};

async function api(path, body) {
  const r = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
  return r.json();
}

async function get(x, y, z) {
  const j = await api("/get_block", { world, x, y, z });
  return (j.block || "").replace(/^minecraft:/, "");
}
const id = (b) => (b || "").split("[")[0];

async function setBlocks(blocks) {
  const j = await api("/set_blocks", { world, blocks });
  if (!j.ok) throw new Error(JSON.stringify(j));
  console.log("wrote", blocks.length);
}

const LEAF = "oak_leaves[distance=1,persistent=true]";
const X0 = -712,
  X1 = -698,
  Z0 = 188,
  Z1 = 216;

function isConcrete(b) {
  const i = id(b);
  return i.includes("white_concrete") || i.includes("light_gray_concrete");
}
function isQuartz(b) {
  return id(b).includes("quartz");
}
function isIron(b) {
  return id(b).includes("iron_bar");
}
function isDecor(b) {
  const i = id(b);
  return i.includes("leaves") || i === "shroomlight" || i === "vine";
}
function isPave(b) {
  return ["stone", "andesite", "cobblestone"].includes(id(b));
}

const blocks = [];
const pier = new Set(); // real pier footprint from y65-69 concrete/iron only

for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    let fill = null;
    let isPier = false;
    for (let y = 65; y <= 69; y++) {
      const b = await get(x, y, z);
      if (isConcrete(b)) {
        isPier = true;
        if (y === 65) fill = id(b);
        else if (!fill) fill = id(b);
      } else if (isIron(b)) {
        isPier = true;
        if (!fill) fill = "white_concrete";
      }
    }
    if (isPier) {
      pier.add(`${x},${z}`);
      const at64 = id(await get(x, 64, z));
      const want = fill || "white_concrete";
      if (at64 !== want) blocks.push({ x, y: 64, z, block: want });
      // no decor on pier
      if (isDecor(at64)) blocks.push({ x, y: 64, z, block: want });
      const at63 = id(await get(x, 63, z));
      if (at63 === "dirt" || at63 === "air" || at63 === "cave_air") {
        if (z >= 194 && z <= 210) {
          blocks.push({
            x,
            y: 63,
            z,
            block: ["stone", "stone", "andesite", "cobblestone"][(Math.random() * 4) | 0],
          });
        } else {
          blocks.push({ x, y: 63, z, block: "grass_block" });
        }
      }
    }
  }
}

// Remove y64 concrete that is NOT under a real pier (false roof fills)
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    if (pier.has(`${x},${z}`)) continue;
    const at64 = id(await get(x, 64, z));
    if (!isConcrete(at64)) continue;
    // was false fill under quartz roof — clear to air; ground stays
    blocks.push({ x, y: 64, z, block: "air" });
  }
}

// Clear remaining decor inside pier or at x>=-705 on wall band
for (let x = -705; x <= -700; x++) {
  for (let z = Z0; z <= Z1; z++) {
    const at64 = id(await get(x, 64, z));
    if (!isDecor(at64)) continue;
    if (pier.has(`${x},${z}`)) {
      const want = "white_concrete";
      blocks.push({ x, y: 64, z, block: want });
    } else {
      blocks.push({ x, y: 64, z, block: "air" });
    }
  }
}

// Connect leaves on z=193 and z=211 from west up to x=-706 (stop before pier x=-705)
for (const z of [193, 211]) {
  for (let x = -710; x <= -706; x++) {
    if (pier.has(`${x},${z}`)) continue;
    const g = id(await get(x, 63, z));
    if (isPave(g)) continue;
    if (g !== "grass_block") blocks.push({ x, y: 63, z, block: "grass_block" });
    const shroom = (x + 710) % 6 === 0;
    blocks.push({ x, y: 64, z, block: shroom ? "shroomlight" : LEAF });
  }
}

const map = new Map();
for (const b of blocks) map.set(`${b.x},${b.y},${b.z}`, b);
const final = [...map.values()];
console.log({ pier: pier.size, ops: final.length, pierList: [...pier].sort() });
await setBlocks(final);

function sym(b) {
  const i = id(b);
  if (i === "air" || i === "cave_air") return " ";
  if (i.includes("leaves")) return "L";
  if (i === "shroomlight") return "*";
  if (i.includes("white_concrete")) return "W";
  if (i.includes("light_gray_concrete")) return "g";
  if (i.includes("iron_bar")) return "I";
  if (i.includes("quartz")) return "Q";
  if (i === "grass_block") return ".";
  if (i === "stone") return "S";
  if (i === "andesite") return "A";
  if (i === "cobblestone") return "C";
  return "?";
}

console.log("--- piers y64/y65 ---");
for (const key of [...pier].sort()) {
  const [x, z] = key.split(",").map(Number);
  console.log(key, "y63", sym(await get(x, 63, z)), "y64", sym(await get(x, 64, z)), "y65", sym(await get(x, 65, z)));
}
console.log("--- y64 north connect ---");
for (let z = 192; z <= 195; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -712; x <= -700; x++) row += sym(await get(x, 64, z));
  console.log(row);
}
console.log("--- y64 south connect ---");
for (let z = 209; z <= 212; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -712; x <= -700; x++) row += sym(await get(x, 64, z));
  console.log(row);
}
