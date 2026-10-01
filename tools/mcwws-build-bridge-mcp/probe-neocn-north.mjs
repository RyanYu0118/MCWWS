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
  return ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
}
function ch(n) {
  if (n === "grass_block") return "g";
  if (n === "air") return ".";
  if (n === "calcite") return "C";
  if (n.includes("andesite") || n === "stone" || n.includes("cobble")) return "s";
  if (n.includes("water")) return "~";
  if (n.includes("leaves")) return "L";
  if (n.includes("log") || n.includes("wood") || n.includes("plank")) return "P";
  if (n.includes("bamboo")) return "b";
  if (n.includes("cherry")) return "c";
  if (n.includes("brick")) return "B";
  if (n.includes("dirt")) return "d";
  if (n.includes("concrete")) return "K";
  if (n.includes("quartz") || n === "iron_bars" || n === "lantern") return "W";
  return n[0] || "?";
}

console.log("y63 x=-608..-558 z=124..145");
for (let z = 124; z <= 145; z++) {
  let row = "";
  for (let x = -608; x <= -558; x += 2) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}
console.log("y64");
for (let z = 124; z <= 140; z++) {
  let row = "";
  for (let x = -608; x <= -558; x += 2) row += ch(await get(x, 64, z));
  console.log(String(z).padStart(4), row);
}
console.log("west wall x=-606 y63-67 z=127..164 step 3");
for (let z = 127; z <= 164; z += 3) {
  const col = [];
  for (let y = 63; y <= 67; y++) col.push(await get(-606, y, z));
  console.log(z, col.join(" | "));
}
console.log("north edge z=134 x=-606..-560 y63-65");
for (let x = -606; x <= -560; x += 4) {
  console.log(x, await get(x, 63, 134), await get(x, 64, 134), await get(x, 65, 134));
}
