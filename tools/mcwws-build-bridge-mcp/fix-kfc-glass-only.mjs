/**
 * Restore wing south glass AFTER any interior air clears.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";
const F = 64;
const Z_WING = 212;
const X0 = -556,
  X1 = -528;
const WING_L = -548,
  WING_R = -536;
const doorX = -542;

const map = new Map();
function set(x, y, z, block) {
  map.set(`${x},${y},${z}`, { x, y, z, block });
}

function restoreWingGlass(xa, xb) {
  for (let x = xa; x <= xb; x++) {
    const isPost = x === xa || x === xb || (x - xa) % 3 === 0;
    for (let y = F; y <= F + 3; y++) {
      if (y === F) set(x, y, Z_WING, "iron_block");
      else if (isPost) set(x, y, Z_WING, "gray_concrete");
      else set(x, y, Z_WING, "gray_stained_glass");
    }
    for (let y = F + 4; y <= F + 5; y++) {
      const n = Math.abs((x * 3 + y * 5 + Z_WING * 7) % 5);
      set(x, y, Z_WING, n <= 1 ? "red_concrete" : n <= 3 ? "bricks" : "red_terracotta");
    }
  }
}
restoreWingGlass(X0, WING_L);
restoreWingGlass(WING_R, X1);

// side doors only on partition walls, not south glass
for (const x of [WING_L, WING_R]) {
  for (let y = F + 1; y <= F + 3; y++) {
    for (let z = 213; z <= 215; z++) set(x, y, z, "air");
  }
  const facing = x < doorX ? "east" : "west";
  set(x, F + 1, 214, `oak_door[facing=${facing},half=lower,hinge=left,open=false,powered=false]`);
  set(x, F + 2, 214, `oak_door[facing=${facing},half=upper,hinge=left,open=false,powered=false]`);
}

const blocks = [...map.values()];
const res = await fetch(`${BASE}/set_blocks`, {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  },
  body: JSON.stringify({ world, blocks }),
});
const json = await res.json();
console.log(json);
