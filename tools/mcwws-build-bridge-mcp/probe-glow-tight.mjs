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

const found = [];
async function check(x, z) {
  const a = await get(x, 64, z);
  const b = await get(x, 63, z);
  if (!isGlow(a) && !isGlow(b)) return;
  found.push({
    x,
    z,
    y64: nid(a),
    y63: nid(b),
    n: isWall(await get(x, 64, z - 1)),
    s: isWall(await get(x, 64, z + 1)),
    e: isWall(await get(x + 1, 64, z)),
    w: isWall(await get(x - 1, 64, z)),
    wall: isWall(a),
  });
}

console.log("south near player");
for (let z = 186; z <= 200; z++) {
  for (let x = -724; x <= -698; x++) await check(x, z);
}
console.log("west wall line");
for (let z = 26; z <= 194; z += 1) {
  for (let x = -721; x <= -718; x++) await check(x, z);
}
console.log("north wall line");
for (let x = -720; x <= -590; x++) {
  for (let z = 25; z <= 27; z++) await check(x, z);
}
console.log("HITS", found.length);
for (const h of found) console.log(JSON.stringify(h));
