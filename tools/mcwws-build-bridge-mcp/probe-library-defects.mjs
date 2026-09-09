/**
 * Probe library defects: door hole, eaves gap, floating lanterns, broken columns.
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

const doorX = -4136,
  Z0 = -1328,
  F = 65,
  WALL = 75;

console.log("=== door opening north ===");
for (let y = F; y <= F + 5; y++) {
  let row = `y${y} `;
  for (let x = doorX - 3; x <= doorX + 3; x++) {
    row += (await get(x, y, Z0)).split("[")[0].padEnd(14);
  }
  console.log(row);
}

console.log("=== eaves vs wall north at WALL ===");
for (let x = doorX - 8; x <= doorX + 8; x += 2) {
  console.log(
    x,
    "wallZ0",
    (await get(x, WALL, Z0)).split("[")[0],
    "eaveZ0-1",
    (await get(x, WALL, Z0 - 1)).split("[")[0],
    "between?",
    (await get(x, WALL - 1, Z0 - 1)).split("[")[0]
  );
}

console.log("=== column sample ===");
for (const x of [-4160, -4152, -4136, -4120]) {
  for (const z of [-1320, -1312, -1304]) {
    console.log(x, z, "y66", (await get(x, 66, z)).split("[")[0], "y74", (await get(x, 74, z)).split("[")[0]);
  }
}

console.log("=== floating lantern hunt (interior air under/no solid above) ===");
let floaters = 0;
for (let x = -4166; x <= -4106; x += 2) {
  for (let z = -1326; z <= -1288; z += 2) {
    for (let y = 70; y <= 84; y++) {
      const b = await get(x, y, z);
      if (!b.startsWith("lantern")) continue;
      const above = await get(x, y + 1, z);
      if (above.startsWith("air")) {
        console.log("FLOAT", x, y, z, b.split("[")[0], "above", above.split("[")[0]);
        floaters++;
      }
    }
  }
}
console.log("floaters", floaters);

console.log("=== stairs outside ===");
for (let z = Z0; z >= Z0 - 4; z--) {
  console.log(z, "y64", (await get(doorX, 64, z)).split("[")[0], "y65", (await get(doorX, 65, z)).split("[")[0]);
}
