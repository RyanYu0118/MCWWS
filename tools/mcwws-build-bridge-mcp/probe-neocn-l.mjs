import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  return ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
}
function ch(id) {
  if (id === "air" || id === "cave_air") return " ";
  if (id === "grass_block") return ".";
  if (id === "dirt") return "d";
  if (id === "water") return "~";
  if (id.includes("leaves") || id === "azalea" || id === "flowering_azalea") return "L";
  if (id.includes("bamboo")) return "b";
  if (id.includes("calcite") || id.includes("deepslate")) return "D";
  if (id.includes("dark_oak") || id.includes("spruce") || id.includes("cherry") || id.includes("stripped")) return "W";
  if (id.includes("stone_brick") || id.includes("moss_block")) return "B";
  if (id === "stone" || id === "andesite" || id.includes("cobble") || id === "tuff") return "s";
  if (id.includes("lantern")) return "n";
  if (id.includes("lily")) return "o";
  if (id.includes("plank") || id.includes("log") || id.includes("wood")) return "W";
  if (id.includes("concrete")) return "K";
  if (id.includes("glass")) return "G";
  if (id.includes("slab") || id.includes("stair")) return "t";
  if (id.includes("terracotta")) return "T";
  return id[0].toUpperCase();
}

console.log("=== north rect y63 x-606..-560 z164..134 ===");
const c1 = {};
for (let z = 164; z >= 134; z -= 3) {
  let row = "";
  for (let x = -606; x <= -560; x += 3) {
    const b = await get(x, 63, z);
    c1[b] = (c1[b] || 0) + 1;
    row += ch(b);
  }
  console.log(String(z).padStart(3, " "), row);
}
console.log("counts", c1);

console.log("=== south arm y63 x-574..-560 z165..198 ===");
const c2 = {};
for (let z = 165; z <= 198; z += 3) {
  let row = "";
  for (let x = -574; x <= -560; x += 2) {
    const b = await get(x, 63, z);
    c2[b] = (c2[b] || 0) + 1;
    row += ch(b);
  }
  console.log(String(z).padStart(3, " "), row);
}
console.log("counts", c2);

console.log("=== y64 solids in L ===");
const solids = [];
for (let z = 134; z <= 164; z += 4) {
  for (let x = -606; x <= -560; x += 4) {
    const b = await get(x, 64, z);
    if (b !== "air" && b !== "cave_air" && b !== "short_grass" && b !== "tall_grass" && b !== "fern" && b !== "lily_pad") {
      solids.push([x, 64, z, b]);
    }
  }
}
for (let z = 165; z <= 198; z += 4) {
  for (let x = -574; x <= -560; x += 3) {
    const b = await get(x, 64, z);
    if (b !== "air" && b !== "cave_air" && b !== "short_grass" && b !== "tall_grass" && b !== "fern") {
      solids.push([x, 64, z, b]);
    }
  }
}
for (const s of solids) console.log(s.join(" "));
console.log("solid count", solids.length);
