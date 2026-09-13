/**
 * Tight scan: quartz at y62-65 near plaza/gate only.
 */
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
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}

const hits = [];
for (let x = -720; x <= -698; x++) {
  for (let z = 188; z <= 216; z++) {
    for (const y of [63, 64, 65]) {
      const b = await get(x, y, z);
      const id = b.split("[")[0];
      if (id.includes("quartz")) hits.push(`${x},${y},${z} ${b.split("[")[0]}`);
    }
  }
}
console.log("count", hits.length);
hits.forEach((h) => console.log(h));

console.log("\n=== y64 map quartz=Q other ---");
for (let z = 188; z <= 216; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -712; x <= -700; x++) {
    const b = (await get(x, 64, z)).split("[")[0];
    if (b.includes("quartz_stairs")) row += "T";
    else if (b.includes("smooth_quartz")) row += "Q";
    else if (b.includes("quartz")) row += "q";
    else if (b === "air" || b === "cave_air") row += " ";
    else if (b.includes("leaves")) row += "L";
    else if (b === "shroomlight") row += "*";
    else if (b.includes("concrete")) row += "W";
    else row += ".";
  }
  console.log(row);
}
