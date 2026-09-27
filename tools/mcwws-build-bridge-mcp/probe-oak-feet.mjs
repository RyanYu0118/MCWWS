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
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
const cx = -5028, cz = 1507;
for (let dy = 4; dy >= -6; dy--) {
  const y = 85 + dy;
  const b = await get(cx, y, cz);
  if (!b.startsWith("air")) console.log("col", y, b);
}
console.log("--- ground y scan ---");
for (let y = 70; y <= 90; y++) {
  let grass = 0, solid = 0;
  for (let z = cz - 2; z <= cz + 2; z++) {
    for (let x = cx - 2; x <= cx + 2; x++) {
      const n = (await get(x, y, z)).split("[")[0];
      if (n === "grass_block") grass++;
      else if (n !== "air" && n !== "short_grass" && n !== "tall_grass") solid++;
    }
  }
  if (grass || solid) console.log("y", y, "grass", grass, "other", solid);
}
