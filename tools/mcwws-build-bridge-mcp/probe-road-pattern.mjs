/**
 * Sample finished road pattern (leaves + shroomlight spacing) near McD/east.
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

console.log("=== east reference x=-540..-520 z=196..206 ===");
for (let z = 196; z <= 206; z++) {
  const row = [];
  for (let x = -540; x >= -560; x--) {
    const b64 = await get(x, 64, z);
    const b63 = await get(x, 63, z);
    let c = ".";
    if (b64 === "shroomlight") c = "S";
    else if (b64.includes("leaves")) c = "L";
    else if (b64 === "vine") c = "v";
    else if (["stone", "andesite", "cobblestone", "blackstone"].includes(b63)) c = "#";
    else if (b63 === "grass_block") c = ",";
    row.push(c);
  }
  console.log(`z=${z}`, row.join(""));
}

console.log("=== shroomlights on z=202 y64 x=-560..-520 ===");
for (let x = -560; x <= -520; x++) {
  const b = await get(x, 64, 202);
  if (b === "shroomlight") console.log("shroom", x);
}
console.log("=== shroom on z=196 ===");
for (let x = -560; x <= -520; x++) {
  const b = await get(x, 64, 196);
  if (b === "shroomlight") console.log("shroom196", x);
}
console.log("=== floor palette weights under good road x=-545..-535 z=200-204 ===");
const p = {};
for (let x = -545; x <= -535; x++) {
  for (let z = 200; z <= 204; z++) {
    const b = await get(x, 63, z);
    p[b] = (p[b] || 0) + 1;
  }
}
console.log(p);

console.log("=== where road already stone west of player ===");
for (let x = -551; x >= -580; x--) {
  const kinds = new Set();
  for (let z = 200; z <= 204; z++) kinds.add(await get(x, 63, z));
  const allGrass = [...kinds].every((k) => k === "grass_block");
  const anyStone = [...kinds].some((k) =>
    ["stone", "andesite", "cobblestone", "blackstone"].includes(k)
  );
  if (x === -551 || allGrass || !anyStone) {
    console.log(x, [...kinds].join(","));
    if (allGrass) break;
  }
}
