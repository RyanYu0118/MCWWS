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
  if (i === "grass_block") return ".";
  if (i.includes("dirt")) return "d";
  if (i.includes("sand")) return "s";
  if (i.includes("clay") || i.includes("mud")) return "c";
  if (i.includes("moss")) return "m";
  if (i.includes("stone") || i.includes("andesite") || i.includes("cobble") || i.includes("granite") || i.includes("diorite")) return "S";
  if (i.includes("brick")) return "B";
  if (i.includes("leaves")) return "L";
  if (i.includes("log") || i.includes("wood") || i.includes("plank")) return "W";
  if (i.includes("seagrass") || i.includes("kelp") || i.includes("lily")) return "o";
  return i[0].toUpperCase();
}

const px = -633, pz = -538;
console.log("player y scan");
for (let y = 88; y <= 98; y++) {
  console.log("y", y, await get(px, y, pz));
}

console.log("=== water search around player ===");
const waters = [];
for (let z = pz - 40; z <= pz + 40; z += 3) {
  for (let x = px - 40; x <= px + 40; x += 3) {
    for (const y of [90, 91, 92, 93, 94, 95, 96]) {
      const b = await get(x, y, z);
      if (b.split("[")[0] === "water") {
        waters.push([x, y, z, b]);
        break;
      }
    }
  }
}
console.log("water hits", waters.length);
for (const w of waters.slice(0, 40)) console.log(w.join(" "));

if (waters.length) {
  const [sx, sy, sz] = waters[0];
  console.log("=== slice y", sy, "around", sx, sz, "===");
  for (let z = sz - 12; z <= sz + 12; z++) {
    let row = "";
    for (let x = sx - 16; x <= sx + 16; x++) row += ch(await get(x, sy, z));
    console.log(String(z).padStart(5, " "), row);
  }
  console.log("=== y-1 bed ===");
  for (let z = sz - 8; z <= sz + 8; z += 2) {
    let row = "";
    for (let x = sx - 12; x <= sx + 12; x += 2) row += ch(await get(x, sy - 1, z));
    console.log(String(z).padStart(5, " "), row);
  }
  console.log("sample states");
  for (const [x, y, z] of waters.slice(0, 8)) {
    console.log(x, y, z, await get(x, y, z), "bed", await get(x, y - 1, z), "rim+", await get(x + 1, y, z));
  }
}
