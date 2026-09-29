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
function isWall(n) {
  return (
    n === "polished_andesite" ||
    n === "quartz_pillar" ||
    n === "smooth_quartz_stairs" ||
    n === "iron_bars" ||
    n === "smooth_quartz_slab" ||
    n === "lantern"
  );
}
function ch(n) {
  n = nid(n);
  if (n === "grass_block") return "g";
  if (n.includes("andesite")) return "A";
  if (n === "stone") return "s";
  if (n.includes("cobble")) return "c";
  if (n.includes("dirt")) return "d";
  if (n.includes("concrete")) return "C";
  if (n.includes("froglight") || n === "shroomlight") return "*";
  if (n.includes("door") || n.includes("gate")) return "G";
  if (n.includes("slab") || n.includes("stair")) return "/";
  if (isWall(n)) return "W";
  return n[0] || "?";
}

console.log("north wall sample z=26 x=-706..-680 y63/64");
let r63 = "", r64 = "";
for (let x = -706; x <= -680; x++) {
  r63 += ch(await get(x, 63, 26));
  r64 += isWall(nid(await get(x, 64, 26))) ? "W" : ".";
}
console.log("63", r63);
console.log("64", r64);

console.log("\nwest sidewalk south z=190..250 x=-724..-710 y63");
for (let z = 190; z <= 250; z += 2) {
  let row = "";
  for (let x = -724; x <= -710; x++) row += ch(await get(x, 63, z));
  console.log(z, row);
}

console.log("\nend -400..-380 x, z=400..420 y63 and y64");
for (let z = 400; z <= 420; z += 2) {
  let a = "", b = "";
  for (let x = -400; x <= -380; x += 2) {
    a += ch(await get(x, 63, z));
    b += ch(await get(x, 64, z));
  }
  console.log("63", z, a);
  console.log("64", z, b);
}
