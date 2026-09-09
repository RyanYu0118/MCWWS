/**
 * Probe KFC interior floors / counter / kitchen.
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

const X0 = -556,
  X1 = -528,
  Z0 = 210,
  Z1 = 226;
const doorX = -542;

console.log("=== aisle cross-section x=-542 y=63..66 z=211..220 ===");
for (let z = 211; z <= 220; z++) {
  const row = [];
  for (let y = 63; y <= 66; y++) row.push(`${y}:${await get(doorX, y, z)}`);
  console.log(`z=${z}`, row.join(" | "));
}
console.log("=== dining vs carpet at y=64 z=213 x=-552..-532 ===");
const line = [];
for (let x = -552; x <= -532; x++) line.push(`${x}:${await get(x, 64, 213)}`);
console.log(line.join(" "));
console.log("=== y=65 same ===");
const line2 = [];
for (let x = -552; x <= -532; x++) line2.push(`${x}:${await get(x, 65, 213)}`);
console.log(line2.join(" "));
console.log("=== back half z=218..224 center ===");
for (let z = 217; z <= 224; z++) {
  console.log(
    `z=${z}`,
    await get(doorX, 64, z),
    await get(doorX, 65, z),
    await get(doorX, 66, z)
  );
}
