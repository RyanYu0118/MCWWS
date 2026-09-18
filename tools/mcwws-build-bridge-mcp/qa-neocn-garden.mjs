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
const pts = [
  ["street keep", -591, 64, 166],
  ["leaf keep", -607, 64, 150],
  ["pond", -590, 63, 150],
  ["lily", -590, 64, 150],
  ["gate floor", -592, 63, 162],
  ["gate air", -592, 65, 162],
  ["gate lintel", -592, 67, 162],
  ["eave S", -580, 68, 145],
  ["eave N", -580, 68, 137],
  ["hang", -580, 67, 141],
  ["hang above", -580, 68, 141],
  ["bamboo", -577, 64, 150],
  ["cherry", -598, 64, 138],
  ["lantern post", -598, 65, 160],
];
for (const [n, x, y, z] of pts) console.log(n.padEnd(12), await get(x, y, z));

function ch(id) {
  const i = id.split("[")[0];
  if (i === "air" || i === "cave_air") return " ";
  if (i === "grass_block") return ".";
  if (i === "water") return "~";
  if (i.includes("leaves")) return "L";
  if (i.includes("bamboo")) return "b";
  if (i.includes("calcite") || i.includes("deepslate")) return "D";
  if (i.includes("dark_oak") || i.includes("spruce")) return "W";
  if (i.includes("lantern")) return "n";
  if (i.includes("lily")) return "o";
  if (i.includes("stone") || i.includes("andesite") || i.includes("cobble")) return "s";
  return i[0].toUpperCase();
}
console.log("y63 z162..136 x-604..-576");
for (let z = 162; z >= 136; z -= 2) {
  let row = "";
  for (let x = -604; x <= -576; x += 2) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(3, " "), row);
}
