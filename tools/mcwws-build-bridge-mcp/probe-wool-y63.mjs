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
  return ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
}
const yellow = [];
const blue = [];
for (let z = 138; z <= 198; z += 1) {
  for (let x = -612; x <= -558; x += 1) {
    const id = await get(x, 63, z);
    if (id === "yellow_wool") yellow.push([x, 63, z]);
    if (id === "blue_wool") blue.push([x, 63, z]);
  }
}
console.log("yellow", yellow.length, JSON.stringify(yellow));
console.log("blue", blue.length, JSON.stringify(blue));
