import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const cache = new Map();
async function get(x, y, z) {
  const k = `${x},${y},${z}`;
  if (cache.has(k)) return cache.get(k);
  const r = await fetch("http://127.0.0.1:8765/get_block", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  const n = ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
  cache.set(k, n);
  return n;
}
function isWalk(n) {
  return (
    n === "andesite" ||
    n === "polished_andesite" ||
    n === "stone" ||
    n === "cobblestone" ||
    n === "mossy_cobblestone" ||
    n.includes("froglight") ||
    n === "shroomlight"
  );
}
function isGrass(n) {
  return n === "grass_block";
}
function isWall(n) {
  return (
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "iron_bars" ||
    n === "smooth_quartz_slab" ||
    n === "lantern"
  );
}

console.log("north inner of curved road, sample x");
for (const x of [-720, -680, -640, -600, -560, -520, -480, -440, -400, -391]) {
  let hit = null;
  for (let z = 400; z <= 510; z++) {
    for (const y of [63, 64]) {
      const n = await get(x, y, z);
      const s = await get(x, y, z + 1);
      const s2 = await get(x, 64, z + 1);
      if (isGrass(n) && (isWalk(s) || isWalk(s2))) {
        hit = { z, y, n, s: isWalk(s) ? s : s2 };
        break;
      }
    }
    if (hit) break;
  }
  console.log("x", x, hit);
}
console.log("done samples");
