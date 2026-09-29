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
function ch(b) {
  const n = nid(b);
  if (n === "air") return ".";
  if (n === "grass_block") return "g";
  if (n === "short_grass" || n === "tall_grass" || n === "fern") return ",";
  if (n.includes("andesite")) return "A";
  if (n.includes("stone")) return "s";
  if (n.includes("cobble")) return "c";
  if (n.includes("brick")) return "b";
  if (n.includes("wall")) return "W";
  if (n.includes("fence")) return "#";
  if (n.includes("iron")) return "I";
  if (n.includes("glass")) return "G";
  if (n.includes("concrete")) return "C";
  if (n.includes("leaves")) return "L";
  if (n.includes("log") || n.includes("wood") || n.includes("plank")) return "P";
  if (n.includes("lantern") || n.includes("torch") || n.includes("lamp")) return "*";
  if (n.includes("dirt")) return "d";
  if (n.includes("gravel")) return "v";
  if (n.includes("slab") || n.includes("stair")) return "/";
  return n[0] || "?";
}

console.log("=== SAMPLE WALL x=-720 y64-68 z91-120 ===");
for (let y = 68; y >= 63; y--) {
  console.log("--- y", y);
  const counts = {};
  for (let z = 91; z <= 120; z++) {
    const b = await get(-720, y, z);
    const n = nid(b);
    counts[n] = (counts[n] || 0) + 1;
    if (z <= 100 || z % 5 === 0) console.log(z, b);
  }
  console.log("counts", counts);
}

console.log("=== SAMPLE neighbors x=-722..-718 at y64 z91,100,110,120 ===");
for (const z of [91, 100, 110, 120]) {
  let row = "";
  for (let x = -722; x <= -718; x++) {
    row += `${x}:${nid(await get(x, 64, z))} `;
  }
  console.log("z", z, row);
}

console.log("=== ground y63 x=-735..-660 z=30..130 step 2 (compact) ===");
// too big; sample west sidewalk strip and south strip
console.log("west strip y63 x=-728..-710 z=30..125");
for (let z = 30; z <= 125; z += 3) {
  let row = "";
  for (let x = -728; x <= -710; x++) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}
