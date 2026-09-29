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
  if (n.includes("gravel")) return "v";
  if (n.includes("brick")) return "b";
  if (n.includes("concrete")) return "C";
  if (n.includes("quartz") || n.includes("iron") || n.includes("lantern") || n.includes("wall") || n.includes("bars")) return "W";
  if (n.includes("leaves")) return "L";
  if (n === "air") return ".";
  return n[0] || "?";
}

console.log("existing wall y64 x=-720 z=20..140 step 2");
for (let z = 20; z <= 140; z += 2) {
  const b = await get(-720, 64, z);
  if (b !== "air" && b !== "short_grass") console.log(z, b);
}

console.log("\nSOUTH band y63 x=-725..-640 step 2, z=108..140 step 2");
for (let z = 108; z <= 140; z += 2) {
  let row = "";
  for (let x = -725; x <= -640; x += 2) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}
