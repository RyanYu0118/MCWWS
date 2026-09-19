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
const hits = [];
for (let z = 140; z <= 200; z++) {
  for (let x = -590; x <= -555; x++) {
    for (const y of [63, 64, 65, 66]) {
      const id = await get(x, y, z);
      if (id.includes("blue") && (id.includes("wool") || id.includes("concrete"))) {
        hits.push([x, y, z, id]);
      }
    }
  }
}
console.log("blue-like", hits.length);
for (const h of hits) console.log(h.join(" "));
