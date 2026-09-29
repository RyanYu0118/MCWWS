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
    n.includes("andesite") ||
    n === "stone" ||
    n.includes("cobble") ||
    n === "gravel" ||
    n.includes("brick")
  );
}
function isGrass(n) {
  return n === "grass_block";
}
function isPlaza(n) {
  return n.includes("concrete") || n.includes("terracotta");
}

console.log("column x=-680 y63 z=70..190 step 2");
for (let z = 70; z <= 190; z += 2) {
  const n = await get(-680, 63, z);
  if (z % 4 === 0 || isWalk(n) || isPlaza(n) || n === "grass_block") {
    // print changes
  }
}
let prev = "";
for (let z = 70; z <= 190; z += 2) {
  const n = await get(-680, 63, z);
  const t = isGrass(n) ? "g" : isWalk(n) ? "A" : isPlaza(n) ? "C" : n[0];
  if (t !== prev) console.log("x-680 z", z, n);
  prev = t;
}

console.log("\ncolumn x=-720 y63 z=20..90 every");
prev = "";
for (let z = 20; z <= 90; z++) {
  const n = await get(-720, 63, z);
  const t = isGrass(n) ? "g" : isWalk(n) ? "A" : n[0];
  if (t !== prev) console.log("x-720 z", z, n);
  prev = t;
}

console.log("\nwest first-grass x for z=25..125");
for (let z = 25; z <= 125; z += 2) {
  let found = null;
  for (let x = -728; x <= -700; x++) {
    const n = await get(x, 63, z);
    const w = await get(x - 1, 63, z);
    if (isGrass(n) && isWalk(w)) {
      found = x;
      break;
    }
  }
  console.log(z, "grassInner", found, "at", found != null ? await get(found, 63, z) : "", "west", found != null ? await get(found - 1, 63, z) : "");
}

console.log("\nsouth first-grass z for x=-720..-640 step 2 (walk south of grass)");
for (let x = -720; x <= -640; x += 2) {
  let found = null;
  for (let z = 115; z <= 200; z++) {
    const n = await get(x, 63, z);
    const s = await get(x, 63, z + 1);
    if (isGrass(n) && isWalk(s)) {
      found = z;
      break;
    }
  }
  if (x % 4 === 0 || found) console.log("x", x, "southGrass", found, found != null ? `s+1=${await get(x, 63, found + 1)}` : "none");
}
