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
  if (i.includes("bamboo")) return "b";
  if (i.includes("leaves") || i === "azalea" || i === "flowering_azalea") return "L";
  if (i.includes("dark_oak") || i.includes("stripped") || i.includes("spruce")) return "W";
  if (i.includes("calcite") || i.includes("deepslate")) return "D";
  if (i === "grass_block") return ".";
  if (i.includes("dirt") || i.includes("gravel") || i.includes("clay") || i.includes("moss") || i.includes("andesite") || i.includes("cobble")) return "s";
  if (i.includes("stone_brick")) return "B";
  if (i.includes("lantern")) return "n";
  if (i.includes("lily") || i.includes("petal") || i.includes("poppy") || i.includes("dandelion") || i.includes("lilac") || i.includes("peony") || i.includes("fern") || i.includes("allium") || i.includes("oxeye") || i.includes("cornflower") || i.includes("tulip") || i.includes("orchid")) return "f";
  return i[0].toUpperCase();
}

console.log("=== garden y63 x-604..-560 z136..198 water/bamboo ===");
for (let z = 136; z <= 198; z += 2) {
  const x0 = z >= 165 ? -574 : -604;
  let row = "";
  for (let x = x0; x <= -560; x += 2) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(3, " "), row);
}
console.log("=== y64 bamboo / plants ===");
for (let z = 136; z <= 198; z += 3) {
  const x0 = z >= 165 ? -574 : -604;
  let row = "";
  for (let x = x0; x <= -560; x += 2) row += ch(await get(x, 64, z));
  console.log(String(z).padStart(3, " "), row);
}

console.log("=== castle bank plants y93-94 ===");
const plants = {};
for (let z = -548; z <= -524; z += 2) {
  for (let x = -650; x <= -610; x += 2) {
    for (const y of [93, 94]) {
      const b = (await get(x, y, z)).split("[")[0];
      if (
        b !== "air" &&
        b !== "water" &&
        b !== "dirt" &&
        b !== "gravel" &&
        b !== "coarse_dirt" &&
        b !== "grass_block"
      ) {
        plants[b] = (plants[b] || 0) + 1;
      }
    }
  }
}
console.log(plants);
