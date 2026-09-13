/**
 * Sample junction for asymmetry / sidewalk leaves / walls.
 * Token read from env BUILD_BRIDGE_TOKEN or config.yml
 */
import fs from "fs";

const token =
  process.env.BUILD_BRIDGE_TOKEN ||
  fs
    .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
    .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  return ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
}

function sym(b) {
  if (b === "stone") return "S";
  if (b === "andesite") return "A";
  if (b === "cobblestone") return "C";
  if (b === "grass_block") return ".";
  if (b.includes("leaves")) return "L";
  if (b === "shroomlight") return "*";
  if (b === "air" || b === "cave_air") return " ";
  if (b.includes("concrete") || b.includes("quartz") || b.includes("iron")) return "W";
  return b[0].toUpperCase();
}

console.log("=== y63 pave x=-728..-705 z=185..219 ===");
for (let z = 185; z <= 219; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -728; x <= -705; x++) row += sym(await get(x, 63, z));
  console.log(row);
}
console.log("=== y64 decor same ===");
for (let z = 185; z <= 219; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -728; x <= -705; x++) row += sym(await get(x, 64, z));
  console.log(row);
}
console.log("=== walls y64-70 near gate/plaza ===");
const walls = [];
for (let x = -728; x <= -700; x++) {
  for (let z = 185; z <= 220; z++) {
    for (let y = 64; y <= 72; y++) {
      const b = await get(x, y, z);
      if (
        b.includes("concrete") ||
        b.includes("quartz") ||
        b.includes("iron_bar") ||
        b.includes("wall") ||
        b.includes("fence")
      ) {
        walls.push(`${x},${y},${z}=${b}`);
      }
    }
  }
}
console.log("wall hits", walls.length);
console.log(walls.slice(0, 40).join("\n"));
