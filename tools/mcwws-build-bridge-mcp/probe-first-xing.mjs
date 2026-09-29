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
  if (n.includes("froglight")) return "*";
  if (n.includes("concrete")) return "C";
  return n[0] || "?";
}

console.log("first crossroads z=190..215 x=-720..-500 step 8 y63");
for (let z = 190; z <= 215; z += 1) {
  let row = "";
  for (let x = -720; x <= -500; x += 8) row += ch(await get(x, 63, z));
  console.log(z, row);
}

console.log("\nNS sidewalk x=-722..-718 z=190..220 y64 wall?");
for (let z = 190; z <= 220; z++) {
  const w = await get(-720, 64, z);
  const g = await get(-720, 63, z);
  console.log(z, "y63", g, "y64", w);
}
console.log("done");
