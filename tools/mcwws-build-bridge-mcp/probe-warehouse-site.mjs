import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";

async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  const j = await r.json();
  return (j.block || "").replace(/^minecraft:/, "").split("[")[0];
}

function ch(id) {
  if (id === "air" || id === "cave_air") return " ";
  if (id === "grass_block") return ".";
  if (id === "dirt" || id === "rooted_dirt") return "d";
  if (id === "stone") return "S";
  if (id === "andesite") return "A";
  if (id === "cobblestone") return "C";
  if (id.includes("leaves")) return "L";
  if (id.includes("log") || id.includes("wood")) return "W";
  if (id.includes("concrete")) return "K";
  if (id.includes("glass")) return "G";
  if (id.includes("water")) return "~";
  if (id.includes("slab") || id.includes("stair")) return "t";
  if (id === "short_grass" || id === "tall_grass" || id === "fern") return ",";
  return id[0].toUpperCase();
}

const counts = {};
const map = [];
for (let z = 20; z <= 90; z += 4) {
  let row = "";
  for (let x = -730; x <= -650; x += 4) {
    const b = await get(x, 63, z);
    counts[b] = (counts[b] || 0) + 1;
    row += ch(b);
  }
  map.push(String(z).padStart(3, " ") + " " + row);
}
console.log("x from -730 step 4 to -650");
console.log(map.join("\n"));
console.log("counts", counts);

const structures = [];
for (let z = 24; z <= 80; z += 6) {
  for (let x = -720; x <= -660; x += 6) {
    const b = await get(x, 64, z);
    if (
      b !== "air" &&
      b !== "cave_air" &&
      b !== "short_grass" &&
      b !== "tall_grass" &&
      b !== "fern"
    ) {
      structures.push([x, 64, z, b]);
    }
  }
}
console.log("--- y64 non-air ---");
console.log(structures);
console.log("struct count", structures.length);
