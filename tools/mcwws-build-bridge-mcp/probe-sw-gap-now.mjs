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
function isWall(n) {
  return (
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "iron_bars" ||
    n === "smooth_quartz_slab" ||
    n === "lantern"
  );
}
function ch63(n) {
  if (n === "grass_block") return "g";
  if (n.includes("andesite")) return "A";
  if (n === "stone") return "s";
  if (n.includes("cobble")) return "c";
  if (n.includes("dirt")) return "d";
  if (n.includes("froglight")) return "*";
  if (n.includes("leaves")) return "L";
  return n[0] || "?";
}

console.log("y63 x=-724..-700 z=180..198");
for (let z = 180; z <= 198; z++) {
  let row = "";
  for (let x = -724; x <= -700; x++) row += ch63(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}
console.log("y64 wall");
for (let z = 180; z <= 198; z++) {
  let row = "";
  for (let x = -724; x <= -700; x++) {
    const n = await get(x, 64, z);
    row += isWall(n) ? "W" : n === "air" ? "." : n[0];
  }
  console.log(String(z).padStart(4), row);
}
