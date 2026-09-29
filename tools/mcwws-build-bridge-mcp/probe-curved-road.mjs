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
function isWalk(n) {
  return (
    n === "andesite" ||
    n === "polished_andesite" ||
    n === "stone" ||
    n === "cobblestone" ||
    n === "mossy_cobblestone" ||
    n.includes("froglight") ||
    n === "shroomlight"
  );
}
function ch(n) {
  if (n === "grass_block") return "g";
  if (isWalk(n)) return n.includes("andesite") ? "A" : n === "stone" ? "s" : n.includes("froglight") ? "*" : "c";
  if (n.includes("dirt")) return "d";
  if (n.includes("slab") || n.includes("stair")) return "/";
  if (n === "air") return ".";
  return n[0] || "?";
}

console.log("road y63 x=-720..-390 step 8 z=420..500 step 4");
for (let z = 420; z <= 500; z += 4) {
  let row = "";
  for (let x = -720; x <= -390; x += 8) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}
console.log("y64");
for (let z = 420; z <= 500; z += 4) {
  let row = "";
  for (let x = -720; x <= -390; x += 8) row += ch(await get(x, 64, z));
  console.log(String(z).padStart(4), row);
}
console.log("done road");
