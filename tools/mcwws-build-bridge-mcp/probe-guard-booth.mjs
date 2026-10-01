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
function short(b) {
  const n = (b || "").split("[")[0].replace(/^minecraft:/, "");
  if (n === "grass_block") return "g";
  if (n === "air") return ".";
  if (n === "calcite") return "C";
  if (n.includes("andesite")) return "A";
  if (n === "stone") return "s";
  if (n.includes("cobble")) return "o";
  if (n.includes("water")) return "~";
  if (n.includes("leaves")) return "L";
  if (n.includes("log") || n.includes("wood") || n.includes("plank")) return "P";
  if (n.includes("bamboo")) return "b";
  if (n.includes("quartz") || n === "iron_bars") return "W";
  if (n.includes("concrete")) return "K";
  if (n.includes("dirt")) return "d";
  if (n === "shroomlight" || n.includes("froglight")) return "*";
  if (n.includes("slab")) return "_";
  if (n.includes("stairs")) return "/";
  if (n.includes("fence") || n.includes("wall")) return "|";
  if (n.includes("glass")) return "#";
  return n.slice(0, 1);
}

const X0 = -698,
  X1 = -688,
  Z0 = 94,
  Z1 = 104;
for (const y of [63, 64, 65, 66, 67, 68]) {
  console.log("\ny=" + y + "  x " + X0 + ".." + X1);
  process.stdout.write("    ");
  for (let x = X0; x <= X1; x++) process.stdout.write(String(Math.abs(x) % 10));
  console.log();
  for (let z = Z0; z <= Z1; z++) {
    let row = String(z).padStart(4);
    for (let x = X0; x <= X1; x++) row += short(await get(x, y, z));
    console.log(row);
  }
}

console.log("\n--- details inside FAWE ---");
for (let z = 96; z <= 102; z++) {
  for (let x = -696; x <= -690; x++) {
    const a = await get(x, 63, z);
    const b = await get(x, 64, z);
    const c = await get(x, 68, z);
    if (!a.startsWith("grass") || b !== "minecraft:air") {
      console.log(x, z, "y63=" + a, "y64=" + b, "y68=" + c);
    }
  }
}
