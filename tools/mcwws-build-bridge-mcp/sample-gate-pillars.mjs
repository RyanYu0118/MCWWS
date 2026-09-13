/**
 * Sample gate pillars + greenery near player (-704, 64, 185)
 */
import fs from "fs";

const token = fs
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
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}

function short(b) {
  const id = b.split("[")[0];
  if (id === "air" || id === "cave_air") return " ";
  if (id === "grass_block") return ".";
  if (id.includes("leaves")) return "L";
  if (id === "shroomlight") return "*";
  if (id === "stone") return "S";
  if (id === "andesite") return "A";
  if (id === "cobblestone") return "C";
  if (id.includes("white_concrete")) return "W";
  if (id.includes("light_gray_concrete")) return "g";
  if (id.includes("quartz")) return "Q";
  if (id.includes("iron_bar")) return "I";
  if (id.includes("lantern")) return "n";
  if (id.includes("candle") || id.includes("end_rod")) return "e";
  if (id.includes("stairs")) return "t";
  return id[0].toUpperCase();
}

console.log("=== columns at suspected pillars ===");
for (const [x, z] of [
  [-705, 194],
  [-704, 194],
  [-703, 194],
  [-705, 193],
  [-704, 193],
  [-703, 193],
  [-705, 192],
  [-705, 210],
  [-704, 210],
  [-703, 210],
  [-705, 211],
  [-704, 211],
]) {
  const col = [];
  for (let y = 63; y <= 72; y++) col.push(`${y}:${short(await get(x, y, z))}`);
  console.log(`(${x},${z})`, col.join(" "));
}

console.log("=== y63 x=-710..-700 z=188..216 ===");
for (let z = 188; z <= 216; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -710; x <= -700; x++) row += short(await get(x, 63, z));
  console.log(row);
}
console.log("=== y64 same ===");
for (let z = 188; z <= 216; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -710; x <= -700; x++) row += short(await get(x, 64, z));
  console.log(row);
}
console.log("=== y65 walls ===");
for (let z = 188; z <= 216; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -710; x <= -700; x++) row += short(await get(x, 65, z));
  console.log(row);
}
