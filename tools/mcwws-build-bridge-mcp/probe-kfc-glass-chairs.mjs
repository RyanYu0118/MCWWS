/**
 * Probe broken glass on wing facades + current chairs + logo top.
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
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}

console.log("=== west wing south z=212 y64-68 ===");
for (let y = 64; y <= 69; y++) {
  let row = `y${y} `;
  for (let x = -556; x <= -548; x++) row += (await get(x, y, 212)).split("[")[0].slice(0, 5).padEnd(6);
  console.log(row);
}
console.log("=== east wing south z=212 ===");
for (let y = 64; y <= 69; y++) {
  let row = `y${y} `;
  for (let x = -536; x <= -528; x++) row += (await get(x, y, 212)).split("[")[0].slice(0, 5).padEnd(6);
  console.log(row);
}
console.log("=== sample table -545,212 chairs ===");
for (const z of [211, 212, 213]) {
  console.log(z, await get(-545, 65, z));
}
console.log("=== above logo y76-82 z=210 ===");
for (let y = 75; y <= 82; y++) {
  console.log(y, await get(-542, y, 210), await get(-542, y, 209));
}
