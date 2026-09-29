import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
async function get(x, y, z) {
  const r = await fetch("http://127.0.0.1:8765/get_block", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  return ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
}
function ch(n) {
  if (n === "grass_block") return "g";
  if (n.includes("andesite")) return "A";
  if (n === "stone") return "s";
  if (n.includes("cobble")) return "c";
  if (n.includes("dirt")) return "d";
  if (n.includes("slab") || n.includes("stair")) return "/";
  if (n.includes("froglight") || n === "shroomlight") return "*";
  if (n.includes("concrete")) return "C";
  if (n === "air") return ".";
  return n[0] || "?";
}

console.log("y63 x=-720..-390 step 10, z=400..480 step 5");
for (let z = 400; z <= 480; z += 5) {
  let row = "";
  for (let x = -720; x <= -390; x += 10) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}

console.log("\ny64 same");
for (let z = 400; z <= 480; z += 5) {
  let row = "";
  for (let x = -720; x <= -390; x += 10) row += ch(await get(x, 64, z));
  console.log(String(z).padStart(4), row);
}
console.log("done map");
