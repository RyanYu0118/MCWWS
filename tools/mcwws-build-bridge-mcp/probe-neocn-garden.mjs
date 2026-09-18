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
  if (id.includes("log") || id.includes("wood") || id.includes("plank")) return "W";
  if (id.includes("concrete")) return "K";
  if (id.includes("glass")) return "G";
  if (id.includes("water")) return "~";
  if (id.includes("slab") || id.includes("stair")) return "t";
  if (id.includes("brick")) return "B";
  if (id.includes("terracotta")) return "T";
  if (id.includes("wool")) return "o";
  if (id.includes("fence") || id.includes("wall")) return "#";
  if (id === "short_grass" || id === "tall_grass" || id === "fern") return ",";
  return id[0].toUpperCase();
}

const counts = {};
const map = [];
for (let z = 175; z >= 120; z -= 3) {
  let row = "";
  for (let x = -620; x <= -560; x += 3) {
    const b = await get(x, 63, z);
    counts[b] = (counts[b] || 0) + 1;
    row += ch(b);
  }
  map.push(String(z).padStart(3, " ") + " " + row);
}
console.log("x -620 step 3 to -560; z south->north");
console.log(map.join("\n"));
console.log("y63 counts", counts);

const structures = [];
for (let z = 170; z >= 125; z -= 4) {
  for (let x = -615; x <= -565; x += 4) {
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
console.log("--- y64 non-air", structures.length, "---");
for (const s of structures.slice(0, 120)) console.log(s.join(" "));
