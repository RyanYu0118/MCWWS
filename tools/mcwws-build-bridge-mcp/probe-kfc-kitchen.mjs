/**
 * Probe KFC kitchen + table facing for polish pass.
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
  return (await r.json()).block || "";
}

const doorX = -542;
console.log("=== table at -552,212 area ===");
for (let y = 64; y <= 66; y++) {
  for (let z = 211; z <= 213; z++) {
    console.log(`(-552,${y},${z})`, await get(-552, y, z));
  }
}
console.log("=== wallZ=218 strip ===");
for (let x = -554; x <= -530; x++) {
  const a = await get(x, 65, 218);
  const b = await get(x, 66, 218);
  if (!a.includes("air") || !b.includes("air")) console.log(x, a, "|", b);
}
console.log("=== kitchen z=219..225 x=-552..-532 y=65-70 ===");
for (let z = 219; z <= 225; z++) {
  for (let x of [-554, -548, -542, -536, -530]) {
    const row = [];
    for (let y = 64; y <= 70; y++) {
      const b = (await get(x, y, z)).replace("minecraft:", "");
      if (!b.startsWith("air")) row.push(`${y}:${b.split("[")[0]}`);
    }
    if (row.length) console.log(`x=${x} z=${z}`, row.join(" "));
  }
}
console.log("=== ceiling lights ===");
for (let y = 68; y <= 71; y++) {
  console.log(`(-542,${y},222)`, await get(-542, y, 222));
  console.log(`(-547,${y},222)`, await get(-547, y, 222));
}
console.log("=== lectern support ===");
console.log(await get(-542, 65, 219), await get(-542, 66, 219), await get(-542, 64, 219));
console.log("=== doors ===");
for (let x = -554; x <= -530; x++) {
  for (let z of [218, 219]) {
    const b = await get(x, 65, z);
    if (b.includes("door")) console.log(x, z, b, await get(x, 66, z));
  }
}
