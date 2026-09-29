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
    n === "mossy_cobblestone"
  );
}
function isWall(n) {
  return (
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "smooth_quartz_slab" ||
    n === "iron_bars" ||
    n === "lantern"
  );
}
function ch(n) {
  if (n === "grass_block") return "g";
  if (isWalk(n)) return n.includes("andesite") ? "A" : n === "stone" ? "s" : "c";
  if (n.includes("concrete")) return "C";
  if (n.includes("dirt")) return "d";
  if (n.includes("leaves")) return "L";
  return n[0] || "?";
}

console.log("=== north band y63 x=-720..-640 z=18..40 ===");
for (let z = 18; z <= 40; z++) {
  let row = "";
  for (let x = -720; x <= -640; x += 2) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}

console.log("\n=== wall y64 along traced west: gaps z=26..190 ===");
const gaps = [];
let prevX = null;
for (let z = 26; z <= 190; z++) {
  let found = null;
  for (let x = -722; x <= -700; x++) {
    const b = await get(x, 64, z);
    if (isWall(b)) {
      found = x + ":" + b;
      break;
    }
  }
  if (!found) gaps.push(z);
  else if (z <= 40 || z % 10 === 0) console.log(z, found);
}
console.log("west gaps z", gaps.join(","));

console.log("\n=== around -705,26 y63-64 x=-710..-640 z=22..32 ===");
for (let z = 22; z <= 32; z++) {
  let row63 = "";
  let row64 = "";
  for (let x = -710; x <= -640; x++) {
    row63 += ch(await get(x, 63, z));
    const a = await get(x, 64, z);
    row64 += isWall(a) ? "W" : a === "air" ? "." : a[0];
  }
  console.log("63", z, row63);
  console.log("64", z, row64);
}
