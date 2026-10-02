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
function nid(b) {
  return (b || "").split("[")[0];
}
function ch(n) {
  if (n === "air") return ".";
  if (n.includes("copper_bars")) return "B";
  if (n.includes("iron")) return "I";
  return n[0] || "?";
}
for (const y of [64, 65, 66]) {
  let row = "z103 y" + y + " x-720..-698 ";
  for (let x = -720; x <= -698; x++) row += ch(nid(await get(x, y, 103)));
  console.log(row);
}
console.log("z102 north of wall");
for (const y of [64, 65, 66]) {
  let row = "z102 y" + y + " ";
  for (let x = -720; x <= -698; x++) row += ch(nid(await get(x, y, 102)));
  console.log(row);
}
console.log("closed leaf x=-720 z=103-125");
for (const y of [64, 65, 66]) {
  let row = "x-720 y" + y + " ";
  for (let z = 103; z <= 125; z++) row += ch(nid(await get(-720, y, z)));
  console.log(row);
}
