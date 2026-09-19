import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const blocks = [];
function w(x, z) {
  blocks.push({ x, y: 62, z, block: "gravel" });
  blocks.push({ x, y: 63, z, block: "water" });
}
for (const [x, z] of [
  [-602, 141],
  [-601, 141],
  [-596, 150],
  [-592, 150],
  [-590, 150],
  [-588, 149],
  [-566, 147],
  [-568, 146],
  [-569, 164],
  [-571, 176],
  [-568, 190],
  [-567, 194],
]) {
  w(x, z);
}
const res = await fetch("http://127.0.0.1:8765/set_blocks", {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  },
  body: JSON.stringify({ world: "world", blocks }),
});
console.log(await res.json());
