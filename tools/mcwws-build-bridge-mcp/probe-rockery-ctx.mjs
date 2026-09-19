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
  if (i.includes("wool")) return i[0] === "y" ? "Y" : "B";
  if (i === "grass_block") return ".";
  if (i.includes("dirt") || i.includes("gravel") || i.includes("stone") || i.includes("andesite") || i.includes("cobble") || i.includes("moss") || i.includes("clay")) return "s";
  if (i.includes("dark_oak") || i.includes("stripped") || i.includes("plank") || i.includes("spruce")) return "W";
  if (i.includes("bamboo") || i.includes("leaves")) return "L";
  if (i.includes("calcite") || i.includes("deepslate")) return "D";
  return i[0].toUpperCase();
}
console.log("=== rockery y63 x-586..-570 z154..164 ===");
for (let z = 154; z <= 164; z++) {
  let row = "";
  for (let x = -586; x <= -570; x++) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(3, " "), row);
}
console.log("=== rockery y64 ===");
for (let z = 154; z <= 164; z++) {
  let row = "";
  for (let x = -586; x <= -570; x++) row += ch(await get(x, 64, z));
  console.log(String(z).padStart(3, " "), row);
}
console.log("=== bridge y63 x-578..-566 z142..152 ===");
for (let z = 142; z <= 152; z++) {
  let row = "";
  for (let x = -578; x <= -566; x++) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(3, " "), row);
}
console.log("=== bridge y64 ===");
for (let z = 142; z <= 152; z++) {
  let row = "";
  for (let x = -578; x <= -566; x++) row += ch(await get(x, 64, z));
  console.log(String(z).padStart(3, " "), row);
}
for (const [x,y,z] of [[-575,63,158],[-572,63,146],[-572,64,146],[-580,63,160],[-574,63,157]]) {
  console.log(x,y,z, await get(x,y,z));
}
