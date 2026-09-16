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
  const j = await r.json();
  return (j.block || "").replace(/^minecraft:/, "");
}

const pts = [
  ["floor", -684, 63, 54],
  ["N wall", -684, 64, 38],
  ["N door", -686, 64, 38],
  ["N glass", -700, 66, 38],
  ["dock", -699, 64, 70],
  ["dock roof", -699, 70, 70],
  ["col", -688, 64, 54],
  ["beam", -688, 74, 54],
  ["hang", -700, 73, 42],
  ["hang under", -700, 73, 42],
  ["lantern", -700, 73, 42],
  ["roof", -684, 75, 54],
  ["hvac", -696, 76, 46],
  ["mezz", -700, 69, 42],
  ["eave N", -684, 74, 37],
  ["eave S", -684, 74, 71],
  ["W door", -704, 64, 52],
  ["apron", -684, 63, 73],
];

for (const [n, x, y, z] of pts) {
  console.log(n.padEnd(12), x, y, z, await get(x, y, z));
}

console.log("--- south wall z=70 y=64 ---");
let row = "";
for (let x = -704; x <= -664; x++) {
  const b = await get(x, 64, 70);
  const id = b.split("[")[0];
  row += id === "air" ? "_" : id === "gray_concrete" ? "G" : id === "light_gray_concrete" ? "L" : id[0];
}
console.log(row);

console.log("--- lantern attachments ---");
for (let x = -700; x <= -668; x += 6) {
  for (let z = 42; z <= 66; z += 6) {
    const lamp = await get(x, 73, z);
    const above = await get(x, 74, z);
    if (lamp.includes("lantern") && above.split("[")[0] === "air") {
      console.log("FLOAT", x, 73, z, lamp, "above", above);
    }
  }
}
console.log("lantern scan done");
