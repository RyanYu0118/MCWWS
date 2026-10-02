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
  if (n.includes("iron")) return "I";
  if (n.includes("quartz") || n === "iron_bars") return "W";
  if (n.includes("andesite")) return "A";
  if (n.includes("concrete")) return "K";
  if (n === "calcite") return "C";
  if (n.includes("glass")) return "#";
  if (n.includes("door")) return "D";
  if (n === "grass_block") return "g";
  if (n.includes("leaves")) return "L";
  return n[0] || "?";
}

console.log("sel1 x=-720 y64-66 z=103-125");
for (let y = 64; y <= 66; y++) {
  let row = "y" + y + " ";
  for (let z = 103; z <= 125; z++) row += ch(nid(await get(-720, y, z)));
  console.log(row);
}
console.log("sel2 z=103 y64-66 x=-720..-698");
for (let y = 64; y <= 66; y++) {
  let row = "y" + y + " ";
  for (let x = -720; x <= -698; x++) row += ch(nid(await get(x, y, 103)));
  console.log(row);
}
console.log("\ny63 under sel1");
let row = "";
for (let z = 103; z <= 125; z++) row += ch(nid(await get(-720, 63, z)));
console.log(row);
console.log("y63 under sel2");
row = "";
for (let x = -720; x <= -698; x++) row += ch(nid(await get(x, 63, 103)));
console.log(row);

console.log("\nsweep sample y64 (quarter)");
for (let z = 103; z <= 125; z += 2) {
  let r = String(z).padStart(4) + " ";
  for (let x = -720; x <= -698; x += 2) r += ch(nid(await get(x, 64, z)));
  console.log(r);
}
