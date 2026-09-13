/**
 * Find quartz stairs / smooth quartz on ground near plaza gate.
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
for (let x = -730; x <= -690; x++) {
  for (let z = 180; z <= 220; z++) {
    for (const y of [62, 63, 64, 65]) {
      const b = await get(x, y, z);
      const id = b.split("[")[0];
      if (id.includes("quartz")) {
        hits.push({ x, y, z, b: id + (b.includes("[") ? b.slice(b.indexOf("[")) : "") });
      }
    }
  }
}
console.log("quartz hits", hits.length);
const byY = {};
for (const h of hits) {
  byY[h.y] = (byY[h.y] || 0) + 1;
}
console.log("byY", byY);
const stairs = hits.filter((h) => h.b.includes("stairs"));
const smooth = hits.filter((h) => h.b.includes("smooth_quartz") && !h.b.includes("stairs"));
const other = hits.filter((h) => !h.b.includes("stairs") && !h.b.includes("smooth_quartz"));
console.log("stairs", stairs.length, "smooth", smooth.length, "other", other.length);
console.log("stairs sample", stairs.slice(0, 40));
console.log("smooth sample", smooth.slice(0, 40));
console.log("other sample", other.slice(0, 20));
