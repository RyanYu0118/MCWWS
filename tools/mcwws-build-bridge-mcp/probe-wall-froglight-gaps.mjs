import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
async function get(x, y, z) {
  const r = await fetch("http://127.0.0.1:8765/get_block", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
function nid(b) {
  return (b || "").split("[")[0];
}
function isWall(b) {
  const n = nid(b);
  return (
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "iron_bars" ||
    n === "smooth_quartz_slab" ||
    n === "lantern"
  );
}
function isGlow(b) {
  const n = nid(b);
  return n.includes("froglight") || n === "shroomlight";
}

async function scan(x1, x2, z1, z2, label) {
  const hits = [];
  for (let z = z1; z <= z2; z++) {
    for (let x = x1; x <= x2; x++) {
      const a = await get(x, 64, z);
      const g = await get(x, 63, z);
      if (!isGlow(a) && !isGlow(g)) continue;
      const n = isWall(await get(x, 64, z - 1));
      const s = isWall(await get(x, 64, z + 1));
      const e = isWall(await get(x + 1, 64, z));
      const w = isWall(await get(x - 1, 64, z));
      hits.push({
        x,
        z,
        y64: nid(a),
        y63: nid(g),
        n,
        s,
        e,
        w,
        hasWall: isWall(a),
      });
    }
  }
  console.log("===", label, hits.length);
  for (const h of hits) console.log(JSON.stringify(h));
}

await scan(-722, -698, 26, 194, "west strip");
await scan(-720, -590, 24, 32, "north strip");
await scan(-722, -640, 185, 200, "south strip");
console.log("done");
