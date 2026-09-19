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
const hits = [];
for (let z = 170; z <= 190; z++) {
  for (let x = -575; x <= -555; x++) {
        for (let y = 63; y <= 67; y++) {
      const b = await get(x, y, z);
      const id = b.split("[")[0];
      if (id.includes("wool") || id.includes("concrete") || id === "water") {
        if (id === "water" && y !== 63) continue;
        hits.push([x, y, z, id]);
      }
    }
  }
}
for (const h of hits) console.log(h.join("\t"));
