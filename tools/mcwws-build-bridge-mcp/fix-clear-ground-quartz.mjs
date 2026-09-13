/**
 * Remove erroneous y64 smooth_quartz / quartz_stairs copied from roof/cornice
 * during pillar fix. Keep pier bases (concrete). Gate opening = air.
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

const pier = new Set([
  ...[193, 194, 195, 209, 210, 211].flatMap((z) =>
    [-705, -704, -703].map((x) => `${x},${z}`)
  ),
]);

function pierFill(x, z) {
  // match y65
  return null; // filled after read
}

const blocks = [];
let cleared = 0;
let keptPier = 0;

for (let x = -720; x <= -698; x++) {
  for (let z = 188; z <= 216; z++) {
    const b = await get(x, 64, z);
    const i = id(b);
    if (!i.includes("quartz")) continue;

    if (pier.has(`${x},${z}`)) {
      // should be concrete, not quartz — restore from y65
      const above = id(await get(x, 65, z));
      let fill = "white_concrete";
      if (above.includes("light_gray_concrete")) fill = "light_gray_concrete";
      else if (above.includes("white_concrete")) fill = "white_concrete";
      else if (above.includes("iron_bar")) fill = "white_concrete";
      blocks.push({ x, y: 64, z, block: fill });
      keptPier++;
      continue;
    }

    // erroneous ground quartz / stairs → air
    blocks.push({ x, y: 64, z, block: "air" });
    cleared++;
  }
}

// Also clear any quartz at y63 in this band (shouldn't be, but just in case)
for (let x = -720; x <= -698; x++) {
  for (let z = 188; z <= 216; z++) {
    const b = await get(x, 63, z);
    const i = id(b);
    if (!i.includes("quartz")) continue;
    // replace with pave if in plaza, else grass
    if (z >= 194 && z <= 210 && x >= -720 && x <= -703) {
      const bag = ["stone", "stone", "andesite", "cobblestone"];
      blocks.push({ x, y: 63, z, block: bag[(Math.random() * 4) | 0] });
    } else {
      blocks.push({ x, y: 63, z, block: "grass_block" });
    }
    cleared++;
  }
}

const j = await api("/set_blocks", { world, blocks });
if (!j.ok) throw new Error(JSON.stringify(j));
console.log({ cleared, keptPier, wrote: blocks.length, ok: j.ok });

function sym(b) {
  const i = id(b);
  if (i === "air" || i === "cave_air") return " ";
  if (i.includes("quartz_stairs")) return "T";
  if (i.includes("smooth_quartz")) return "Q";
  if (i.includes("quartz")) return "q";
  if (i.includes("white_concrete")) return "W";
  if (i.includes("light_gray_concrete")) return "g";
  if (i.includes("leaves")) return "L";
  if (i === "shroomlight") return "*";
  return ".";
}

console.log("--- y64 after clean x=-712..-700 ---");
for (let z = 190; z <= 214; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -712; x <= -700; x++) row += sym(await get(x, 64, z));
  console.log(row);
}

// confirm roof quartz still at y70
console.log("y70 sample", await get(-704, 70, 202), await get(-706, 70, 202));
