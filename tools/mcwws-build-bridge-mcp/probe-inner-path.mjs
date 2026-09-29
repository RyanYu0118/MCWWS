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
function isWalk(n) {
  return (
    n === "andesite" ||
    n === "polished_andesite" ||
    n === "stone" ||
    n === "cobblestone" ||
    n.includes("froglight") ||
    n === "shroomlight"
  );
}
function isGrass(n) {
  return n === "grass_block";
}

async function col(x, z) {
  const y63 = await get(x, 63, z);
  const y64 = await get(x, 64, z);
  const y65 = await get(x, 65, z);
  return { y63, y64, y65 };
}

console.log("inner grass east of west sidewalk z=210..420 step 10");
for (let z = 210; z <= 420; z += 10) {
  let xg = null;
  for (let x = -724; x <= -708; x++) {
    const g = await get(x, 63, z);
    const w = await get(x - 1, 63, z);
    if (isGrass(g) && isWalk(w)) {
      xg = x;
      break;
    }
    const g4 = await get(x, 64, z);
    const w4 = await get(x - 1, 64, z);
    if (isGrass(g4) && isWalk(w4)) {
      xg = x + "@64";
      break;
    }
  }
  console.log("z", z, "inner", xg);
}

console.log("\nEW inner (grass north of walk) x=-720..-391 step 20");
for (let x = -720; x <= -391; x += 20) {
  let found = null;
  for (let z = 395; z <= 425; z++) {
    for (const y of [63, 64]) {
      const n = await get(x, y, z);
      const s = await get(x, y, z + 1);
      if (isGrass(n) && isWalk(s)) {
        found = { z, y, n, s };
        break;
      }
    }
    if (found) break;
  }
  console.log("x", x, found);
}
console.log("done inner");
