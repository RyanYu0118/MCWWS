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
for (let z = 134; z <= 198; z++) {
  const x0 = z >= 165 ? -580 : -610;
  for (let x = x0; x <= -556; x++) {
    for (const y of [64]) {
      const id = await get(x, y, z);
      if (id === "yellow_wool") yellow.push([x, y, z]);
      if (id === "light_blue_wool" || id === "blue_wool" || id === "cyan_wool") blue.push([x, y, z, id]);
    }
  }
}
console.log("yellow", yellow.length);
for (const p of yellow) console.log("Y", p.join(" "));
console.log("blue", blue.length);
for (const p of blue) console.log("B", p.join(" "));
