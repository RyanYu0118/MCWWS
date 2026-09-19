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
  if (i.includes("dirt")) return "d";
  if (i.includes("sand")) return "s";
  if (i.includes("gravel")) return "g";
  if (i.includes("clay") || i.includes("mud")) return "c";
  if (i.includes("moss")) return "m";
  if (i.includes("stone") || i.includes("andesite") || i.includes("cobble") || i.includes("granite") || i.includes("diorite") || i.includes("tuff")) return "S";
  if (i.includes("brick")) return "B";
  if (i.includes("leaves")) return "L";
  if (i.includes("log") || i.includes("wood") || i.includes("plank")) return "W";
  if (i.includes("seagrass") || i.includes("kelp") || i.includes("lily")) return "o";
  if (i.includes("slab") || i.includes("stair")) return "t";
  return i[0].toUpperCase();
}

const cx = -629, cz = -538;
for (const y of [91, 92, 93]) {
  console.log("=== y", y, "===");
  for (let z = cz - 14; z <= cz + 18; z++) {
    let row = "";
    for (let x = cx - 18; x <= cx + 18; x++) row += ch(await get(x, y, z));
    console.log(String(z).padStart(5, " "), row);
  }
}
console.log("=== bed y90 ===");
for (let z = cz - 8; z <= cz + 8; z += 2) {
  let row = "";
  for (let x = cx - 14; x <= cx + 14; x += 2) row += ch(await get(x, 90, z));
  console.log(String(z).padStart(5, " "), row);
}
console.log("samples");
for (const [x, y, z] of [
  [-629, 92, -538],
  [-629, 91, -538],
  [-629, 90, -538],
  [-629, 89, -538],
  [-620, 92, -538],
  [-640, 92, -538],
  [-629, 93, -530],
  [-629, 92, -520],
]) {
  console.log(x, y, z, await get(x, y, z));
}
