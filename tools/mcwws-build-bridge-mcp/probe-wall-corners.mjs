import fs from "fs";
const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const cache = new Map();
async function get(x, y, z) {
  const k = `${x},${y},${z}`;
  if (cache.has(k)) return cache.get(k);
  const r = await fetch("http://127.0.0.1:8765/get_block", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ world: "world", x, y, z }),
  });
  const n = ((await r.json()).block || "").replace(/^minecraft:/, "").split("[")[0];
  cache.set(k, n);
  return n;
}
function ch(n) {
  if (n === "grass_block") return "g";
  if (n.includes("andesite")) return "A";
  if (n === "stone") return "s";
  if (n.includes("cobble")) return "c";
  if (n.includes("dirt")) return "d";
  if (n.includes("gravel")) return "v";
  if (n.includes("concrete")) return "C";
  if (n.includes("froglight") || n.includes("leaves")) return "*";
  return n[0] || "?";
}

console.log("SW z=165..200 x=-728..-680");
for (let z = 165; z <= 200; z += 2) {
  let row = "";
  for (let x = -728; x <= -680; x += 2) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}

console.log("\nSE z=185..200 x=-680..-600");
for (let z = 185; z <= 200; z += 2) {
  let row = "";
  for (let x = -680; x <= -600; x += 2) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}

console.log("\nN end z=20..40 x=-720..-700");
for (let z = 20; z <= 40; z++) {
  let row = "";
  for (let x = -720; x <= -700; x++) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}
