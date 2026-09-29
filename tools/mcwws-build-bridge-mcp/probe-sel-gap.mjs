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
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
function nid(b) {
  return (b || "").split("[")[0];
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
function ch(n) {
  n = nid(n);
  if (n === "grass_block") return "g";
  if (n.includes("andesite")) return "A";
  if (n === "stone") return "s";
  if (n.includes("cobble")) return "c";
  if (n.includes("dirt")) return "d";
  if (n.includes("froglight") || n === "shroomlight") return "*";
  if (isWall(n)) return "W";
  if (n === "air") return ".";
  return n[0] || "?";
}

console.log("sel y63 x=-724..-700 z=205..225");
for (let z = 205; z <= 225; z++) {
  let row = "";
  for (let x = -724; x <= -700; x++) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}
console.log("y64");
for (let z = 205; z <= 225; z++) {
  let row = "";
  for (let x = -724; x <= -700; x++) {
    const n = nid(await get(x, 64, z));
    row += isWall(n) ? "W" : n === "air" ? "." : n[0];
  }
  console.log(String(z).padStart(4), row);
}
console.log("neighbors: z=193 and z=217 wall x");
for (const z of [193, 210, 211, 215, 217]) {
  for (let x = -722; x <= -704; x++) {
    const n = nid(await get(x, 64, z));
    if (isWall(n)) console.log("wall", x, z, n);
  }
}
