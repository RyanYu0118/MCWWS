import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const res = await fetch("http://127.0.0.1:8765/set_blocks", {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    world: "world",
    blocks: (() => {
      const spots = [
        [-577, 147],
        [-577, 149],
        [-577, 151],
        [-577, 153],
        [-577, 155],
        [-578, 148],
        [-578, 150],
        [-578, 152],
        [-578, 154],
        [-576, 149],
        [-576, 152],
      ];
      const blocks = [];
      for (const [x, z] of spots) {
        const h = 4 + (((x + z) % 3) + 3) % 3;
        for (let y = 64; y < 64 + h; y++) {
          blocks.push({ x, y, z, block: "bamboo_block[axis=y]" });
        }
        if ((x + z) % 2 === 0) {
          blocks.push({ x: x - 1, y: 64, z, block: "azalea" });
        }
      }
      return blocks;
    })(),
  }),
});
console.log(await res.json());
