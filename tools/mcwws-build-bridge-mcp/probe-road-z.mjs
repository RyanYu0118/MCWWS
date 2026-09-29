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
    n.includes("froglight")
  );
}

console.log("walk at x=-391 y63/64 z=190..480 step 5");
for (let z = 190; z <= 480; z += 5) {
  const a = await get(-391, 63, z);
  const b = await get(-391, 64, z);
  if (isWalk(a) || isWalk(b) || a === "grass_block" || b === "grass_block") {
    console.log(z, "y63", a, "y64", b);
  }
}

console.log("\nwalk at x=-600 y63 z=190..480 step 10");
for (let z = 190; z <= 480; z += 10) {
  const a = await get(-600, 63, z);
  if (isWalk(a) || a.includes("slab") || a.includes("stair") || a.includes("dirt")) {
    console.log(z, a);
  }
}

console.log("\nwalk at x=-500 y63/64 z=190..480 step 10");
for (let z = 190; z <= 480; z += 10) {
  const a = await get(-500, 63, z);
  const b = await get(-500, 64, z);
  if (isWalk(a) || isWalk(b) || a.includes("slab")) console.log(z, "63", a, "64", b);
}
console.log("done roads");
