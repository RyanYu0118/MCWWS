import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
async function get(x, y, z) {
  const r = await fetch(`${BASE}/get_block`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  const j = await r.json();
  return (j.block || "").replace(/^minecraft:/, "");
}
const cx = -624, cy = 93, cz = 419;
console.log("feet", await get(cx, cy, cz), "above", await get(cx, cy + 1, cz), await get(cx, cy + 2, cz));
for (let z = cz - 6; z <= cz + 8; z++) {
  let row = "";
  for (let x = cx - 6; x <= cx + 6; x++) {
    const b = (await get(x, cy, z)).split("[")[0];
    const a = (await get(x, cy + 1, z)).split("[")[0];
    const ch =
      b.includes("grass") ? "g" :
      b.includes("dirt") ? "d" :
      b === "air" ? "." :
      a !== "air" && a !== "short_grass" && a !== "tall_grass" && !a.includes("flower") ? "B" :
      b[0] || "?";
    row += ch;
  }
  console.log(String(z).padStart(4), row);
}
console.log("sky column");
for (let y = 90; y <= 130; y++) {
  const b = await get(cx, y, cz);
  if (!b.startsWith("air")) console.log(y, b);
}
