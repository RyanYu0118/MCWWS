/**
 * Sample road under player and existing leaf/shroomlight pattern toward -X.
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

const px = -551;
const counts = {};
console.log("=== feet band z=200..204 y=63 around player ===");
for (let z = 200; z <= 204; z++) {
  for (let x = px - 5; x <= px + 5; x++) {
    const b = (await get(x, 63, z)).split("[")[0];
    counts[b] = (counts[b] || 0) + 1;
  }
}
console.log("y63 counts", counts);

const counts64 = {};
for (let z = 198; z <= 206; z++) {
  for (let x = px - 8; x <= px + 2; x++) {
    const b = (await get(x, 64, z)).split("[")[0];
    if (b !== "air") counts64[b] = (counts64[b] || 0) + 1;
  }
}
console.log("y64 non-air nearby", counts64);

console.log("=== cross section at x=-551 y63-65 z198-206 ===");
for (let z = 198; z <= 206; z++) {
  console.log(
    `z=${z}`,
    await get(px, 63, z),
    "|",
    await get(px, 64, z),
    "|",
    await get(px, 65, z)
  );
}

console.log("=== look west along center z=202 for leaves/shroom ===");
for (let x = px; x >= px - 40; x -= 1) {
  const g = (await get(x, 63, 202)).split("[")[0];
  const a = (await get(x, 64, 202)).split("[")[0];
  const n = (await get(x, 64, 199)).split("[")[0];
  const s = (await get(x, 64, 205)).split("[")[0];
  if (x % 5 === 0 || a !== "air" || n.includes("leaves") || s.includes("leaves") || a.includes("shroom")) {
    console.log(`x=${x}`, "floor", g, "mid64", a, "z199", n, "z205", s);
  }
}

console.log("=== sample far west x=-600,-650,-700 ===");
for (const x of [-600, -650, -700, -721]) {
  for (const z of [200, 202, 204]) {
    console.log(x, z, "y63", await get(x, 63, z), "y64", await get(x, 64, z));
  }
}
