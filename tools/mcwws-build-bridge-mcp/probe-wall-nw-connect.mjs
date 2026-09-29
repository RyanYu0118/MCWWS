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
  return ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
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

console.log("NW corner y63 and y64 x=-712..-700 z=25..35");
for (let z = 25; z <= 35; z++) {
  let a = "", b = "";
  for (let x = -712; x <= -700; x++) {
    const g = await get(x, 63, z);
    const t = await get(x, 64, z);
    a += g === "grass_block" ? "g" : g.includes("andesite") ? "A" : g.includes("froglight") ? "*" : g.includes("concrete") ? "C" : g[0];
    b += isWall(t) ? "W" : t === "air" ? "." : t[0];
  }
  console.log("63", z, a);
  console.log("64", z, b);
}

console.log("\nwest wall x per z 26-100");
const missing = [];
for (let z = 26; z <= 100; z++) {
  let xs = [];
  for (let x = -722; x <= -700; x++) {
    if (isWall(await get(x, 64, z))) xs.push(x);
  }
  if (xs.length === 0) missing.push(z);
  if (z <= 45 || xs.length !== 1) console.log(z, xs.join(",") || "NONE");
}
console.log("missing", missing.join(","));
