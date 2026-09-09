/**
 * Probe lobby furniture, openings to wings, and logo on south facade.
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

console.log("=== logo band z=210 y70-76 x=-550..-534 ===");
for (let y = 69; y <= 76; y++) {
  let row = `y=${y} `;
  for (let x = -550; x <= -534; x++) {
    const b = (await get(x, y, 210)).replace("minecraft:", "").split("[")[0];
    row += b.startsWith("red") ? "R" : b.includes("quartz") || b.includes("white") || b.includes("powder") ? "W" : b.includes("glass") ? "G" : b.includes("gray") || b.includes("iron") ? "D" : b === "air" ? "." : "?";
  }
  console.log(row);
}

console.log("=== chairs/tables in lobby z=211..217 ===");
for (let z = 211; z <= 217; z++) {
  for (let x = -554; x <= -530; x++) {
    const b = await get(x, 65, z);
    if (b.includes("stairs") || b.includes("fence") || b.includes("slab")) {
      console.log(x, 65, z, b.split("[")[0], b.includes("facing=") ? b.match(/facing=(\w+)/)[1] : "");
    }
  }
}

console.log("=== wing walls at x=-548 and x=-536 z=211..218 y65 ===");
for (let z = 211; z <= 218; z++) {
  console.log(
    `z=${z}`,
    "L",
    (await get(-548, 65, z)).split("[")[0],
    "R",
    (await get(-536, 65, z)).split("[")[0]
  );
}
