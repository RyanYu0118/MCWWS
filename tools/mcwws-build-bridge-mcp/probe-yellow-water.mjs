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
  return ((await r.json()).block || "").replace(/^minecraft:/, "");
}
const yellow = [
  [-586,64,141],[-585,64,141],[-584,64,141],[-576,64,141],[-575,64,141],
  [-592,64,142],[-589,64,142],[-573,64,142],[-571,64,142],[-569,64,142],[-566,64,142],
  [-595,64,143],[-594,64,143],[-564,64,143],[-597,64,144],[-572,64,144],
  [-561,64,144],[-560,64,144],[-559,64,144],[-600,64,147],[-602,64,149],
  [-572,64,149],[-571,64,150],[-571,64,153],[-571,64,155],[-571,64,158],
  [-572,64,160],[-572,64,162],[-572,64,164],[-571,64,167],[-570,64,169],
  [-569,64,171],[-568,64,173],[-567,64,175],[-566,64,176],[-565,64,177],
  [-565,64,178],[-565,64,183],[-565,64,184],[-566,64,185],[-567,64,186],
  [-568,64,187],[-570,64,189],[-571,64,191],[-572,64,193],[-573,64,195],
  [-574,64,196],[-575,64,197],[-576,64,198],
];
console.log("under yellow");
for (const [x, y, z] of yellow) {
  const under = await get(x, 63, z);
  const here = await get(x, 64, z);
  const id = under.split("[")[0];
  if (id === "water" || here.includes("blue") || here.includes("wool")) {
    console.log(x, z, "y63", under, "y64", here);
  }
}
const hits = [];
for (let z = 160; z <= 198; z++) {
  for (let x = -580; x <= -558; x++) {
    const id = (await get(x, 64, z)).split("[")[0];
    if (id.includes("wool") || id.includes("concrete")) hits.push([x, 64, z, id]);
  }
}
console.log("south arm colored", hits);
