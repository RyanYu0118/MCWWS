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
function isWalk(n) {
  return (
    n === "andesite" ||
    n === "polished_andesite" ||
    n === "stone" ||
    n === "cobblestone" ||
    n === "mossy_cobblestone"
  );
}
function ch(n) {
  if (n === "grass_block") return "g";
  if (isWalk(n)) return n[0] === "a" || n.includes("andesite") ? "A" : n === "stone" ? "s" : "c";
  if (n.includes("dirt")) return "d";
  if (n.includes("concrete")) return "C";
  if (n.includes("leaves") || n.includes("froglight")) return "*";
  return n[0] || "?";
}

console.log("SW 1-block x=-724..-700 z=178..199");
for (let z = 178; z <= 199; z++) {
  let row = "";
  for (let x = -724; x <= -700; x++) row += ch(await get(x, 63, z));
  console.log(String(z).padStart(4), row);
}

console.log("\nsouth inner grass z per x=-720..-640");
for (let x = -720; x <= -640; x++) {
  let zFound = null;
  for (let z = 170; z <= 199; z++) {
    const n = await get(x, 63, z);
    const s = await get(x, 63, z + 1);
    if (n === "grass_block" && isWalk(s)) {
      zFound = z;
      break;
    }
  }
  if (x % 5 === 0 || zFound != null) console.log(x, zFound);
}

console.log("\neast of plaza y63 z=120 x=-680..-560 step 4");
let row = "";
for (let x = -680; x <= -560; x += 4) row += ch(await get(x, 63, 120));
console.log(row);
