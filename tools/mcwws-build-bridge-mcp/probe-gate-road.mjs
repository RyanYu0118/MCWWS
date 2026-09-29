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
  if (n.includes("concrete")) return "C";
  if (n.includes("froglight") || n === "shroomlight") return "*";
  if (n.includes("door") || n.includes("gate")) return "G";
  if (n.includes("slab") || n.includes("stair")) return "/";
  if (n.includes("wall") || n === "iron_bars" || n.includes("quartz") || n === "polished_andesite" || n === "lantern") return "W";
  return n[0] || "?";
}

console.log("road z=405..415 x=-530..-390 step 4 y63");
for (let z = 405; z <= 415; z++) {
  let row = "";
  for (let x = -530; x <= -390; x += 4) row += ch(await get(x, 63, z));
  console.log(z, row);
}

console.log("\nplayer -530..-500 x, 450..480 z y63");
for (let z = 450; z <= 480; z += 2) {
  let row = "";
  for (let x = -530; x <= -500; x += 2) row += ch(await get(x, 63, z));
  console.log(z, row);
}

console.log("\nfind gate-like y64 near existing wall x=-722..-700 z=160..210");
for (let z = 160; z <= 210; z++) {
  for (let x = -722; x <= -700; x++) {
    const n = await get(x, 64, z);
    if (n.includes("door") || n.includes("gate") || n.includes("wall")) {
      console.log("y64", x, z, n);
    }
  }
}
console.log("done2");
