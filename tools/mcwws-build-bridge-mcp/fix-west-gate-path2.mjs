import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const mix = ["stone", "stone", "andesite", "cobblestone"];
const blocks = [];
function pave(x, z) {
  blocks.push({
    x,
    y: 63,
    z,
    block: mix[(((x + z * 3) % 4) + 4) % 4],
  });
}
for (let x = -612; x <= -598; x++) {
  if (x === -604) {
    blocks.push({ x, y: 63, z: 149, block: "smooth_stone" });
    blocks.push({ x, y: 63, z: 150, block: "smooth_stone" });
    continue;
  }
  pave(x, 149);
  pave(x, 150);
}
for (let i = 0; i <= 13; i++) {
  const x = -597 + i;
  const z = 148 - Math.floor((i * 4) / 13);
  if (x >= -583 && z <= 144) continue;
  pave(x, z);
  pave(x, z + 1);
}
const res = await fetch("http://127.0.0.1:8765/set_blocks", {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  },
  body: JSON.stringify({ world: "world", blocks }),
});
console.log(await res.json(), "n", blocks.length);
