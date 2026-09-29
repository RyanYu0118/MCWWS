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

console.log("south z191-195 x=-722..-698 y64");
for (let z = 189; z <= 196; z++) {
  for (let x = -722; x <= -698; x++) {
    const b = await get(x, 64, z);
    const n = nid(b);
    if (n.includes("froglight") || n === "shroomlight" || n === "air") {
      const neighbors =
        (isWall(await get(x - 1, 64, z)) ? "W" : "") +
        (isWall(await get(x + 1, 64, z)) ? "E" : "") +
        (isWall(await get(x, 64, z - 1)) ? "N" : "") +
        (isWall(await get(x, 64, z + 1)) ? "S" : "");
      if (neighbors || n.includes("froglight") || n === "shroomlight") {
        console.log(x, z, n, "y63=" + nid(await get(x, 63, z)), "adj=" + neighbors);
      }
    }
  }
}
console.log("done south holes");
