/**
 * Probe current KFC shell extents for remodel.
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
  return ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
}

// find non-air / building blocks
const hits = [];
for (let x = -560; x <= -520; x++) {
  for (let z = 206; z <= 228; z++) {
    for (let y = 63; y <= 78; y++) {
      const b = await get(x, y, z);
      if (
        b.includes("concrete") ||
        b.includes("quartz") ||
        b.includes("glass") ||
        b === "smooth_stone" ||
        b.includes("lantern") ||
        b === "sea_lantern"
      ) {
        hits.push([x, y, z, b]);
        break;
      }
    }
  }
}
const xs = hits.map((h) => h[0]);
const zs = hits.map((h) => h[2]);
const ys = hits.map((h) => h[1]);
console.log("bbox", {
  x: [Math.min(...xs), Math.max(...xs)],
  z: [Math.min(...zs), Math.max(...zs)],
  samples: hits.length,
});
console.log("south facade y72 sign band");
for (let x = -550; x <= -534; x++) {
  const row = [];
  for (let y = 70; y <= 76; y++) row.push((await get(x, y, 210)).slice(0, 4));
  console.log(x, row.join(" "));
}
console.log("path front z207-209 x=-545..-539");
for (let z = 207; z <= 210; z++) {
  console.log(z, await get(-542, 63, z), await get(-542, 64, z));
}
