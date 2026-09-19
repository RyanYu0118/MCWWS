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
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
function ch(id) {
  const i = id.split("[")[0];
  if (i === "air") return " ";
  if (i === "water") return "~";
  if (i === "grass_block") return ".";
  if (i.includes("dirt") || i.includes("gravel") || i.includes("clay") || i.includes("moss") || i.includes("andesite") || i.includes("cobble") || i === "stone") return "s";
  if (i.includes("calcite") || i.includes("deepslate")) return "D";
  if (i.includes("dark_oak") || i.includes("stripped") || i.includes("spruce")) return "W";
  if (i.includes("leaves") || i.includes("bamboo")) return "L";
  if (i.includes("slab") || i.includes("stair")) return "t";
  if (i.includes("stone_brick")) return "B";
  return i[0].toUpperCase();
}
console.log("=== y63 x-616..-574 z138..156 ===");
for (let z = 138; z <= 156; z++) {
  let row = "";
  for (let x = -616; x <= -574; x++) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(3, " "), row);
}
console.log("=== y64 same ===");
for (let z = 138; z <= 156; z++) {
  let row = "";
  for (let x = -616; x <= -574; x++) row += ch(await get(x, 64, z));
  console.log(String(z).padStart(3, " "), row);
}
console.log("gate", await get(-604, 63, 149), await get(-604, 64, 149), await get(-604, 65, 150));
console.log("road", await get(-611, 63, 150), await get(-608, 63, 150));
console.log("pavilion", await get(-580, 63, 141));
