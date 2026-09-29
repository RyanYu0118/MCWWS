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
    n === "mossy_cobblestone" ||
    n.includes("froglight") ||
    n === "shroomlight"
  );
}
function isGrass(n) {
  return n === "grass_block";
}
function ch(n) {
  if (isGrass(n)) return "g";
  if (isWalk(n)) return n.includes("andesite") ? "A" : n === "stone" ? "s" : n.includes("froglight") || n === "shroomlight" ? "*" : "c";
  if (n.includes("dirt")) return "d";
  if (n.includes("slab") || n.includes("stair")) return "/";
  if (n.includes("concrete")) return "C";
  return n[0] || "?";
}

console.log("west inner x=-724..-708 z=250..420 step 4 y63");
for (let z = 250; z <= 420; z += 4) {
  let row = "";
  for (let x = -724; x <= -708; x++) row += ch(await get(x, 63, z));
  console.log(z, row);
}

console.log("\nEW y64 z=400..420 x=-720..-390 step 8");
for (let z = 400; z <= 420; z += 2) {
  let row = "";
  for (let x = -720; x <= -390; x += 8) row += ch(await get(x, 64, z));
  console.log("64", z, row);
}

console.log("\nEW y63 z=408..418 x=-720..-390 step 8");
for (let z = 408; z <= 418; z += 2) {
  let row = "";
  for (let x = -720; x <= -390; x += 8) row += ch(await get(x, 63, z));
  console.log("63", z, row);
}

console.log("\nslope sample: for x=-700,-600,-500,-400 z=405..418 y of first solid");
for (const x of [-700, -650, -600, -550, -500, -450, -400, -391]) {
  for (let z = 405; z <= 418; z++) {
    const a = await get(x, 64, z);
    const b = await get(x, 63, z);
    const c = await get(x, 65, z);
    if (isWalk(a) || isWalk(b) || isGrass(a) || isGrass(b)) {
      console.log("x", x, "z", z, "y63", b, "y64", a, "y65", c);
    }
  }
  console.log("---");
}
