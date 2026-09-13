/**
 * Restore gate pillar bases (fill y64 gaps), remove greenery inside/under walls,
 * connect outer leaf belt up to wall (stop before wall, no pass-through).
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";
const headers = {
  Authorization: `Bearer ${token}`,
  "Content-Type": "application/json",
};

async function api(path, body) {
  const r = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
  return r.json();
}

async function get(x, y, z) {
  const j = await api("/get_block", { world, x, y, z });
  return (j.block || "").replace(/^minecraft:/, "");
}

function id(b) {
  return (b || "").split("[")[0];
}

async function setBlocks(blocks) {
  const chunk = 3000;
  for (let i = 0; i < blocks.length; i += chunk) {
    const slice = blocks.slice(i, i + chunk);
    const j = await api("/set_blocks", { world, blocks: slice });
    if (!j.ok) throw new Error(JSON.stringify(j));
    console.log(`wrote ${i + slice.length}/${blocks.length}`);
  }
}

const LEAF = "oak_leaves[distance=1,persistent=true]";

function isWallBody(b) {
  const i = id(b);
  return (
    i.includes("concrete") ||
    i.includes("quartz") ||
    i.includes("iron_bar") ||
    i.includes("stairs") ||
    i === "end_rod" ||
    i.includes("candle")
  );
}

function isSolidPillarFill(b) {
  // what to put at y64 under wall — match concrete/quartz; iron_bars get white_concrete base
  const i = id(b);
  if (i.includes("light_gray_concrete")) return "light_gray_concrete";
  if (i.includes("white_concrete")) return "white_concrete";
  if (i.includes("quartz")) return i; // keep quartz if somehow at 65
  if (i.includes("iron_bar")) return "white_concrete";
  if (i.includes("stairs")) return null; // don't fill under decorative stairs oddly
  if (isWallBody(b)) return "white_concrete";
  return null;
}

function isDecor(b) {
  const i = id(b);
  return i.includes("leaves") || i === "shroomlight" || i === "vine";
}

function isPave(b) {
  const i = id(b);
  return ["stone", "andesite", "cobblestone"].includes(i);
}

// Scan gate band
const X0 = -712;
const X1 = -698;
const Z0 = 188;
const Z1 = 216;
const Y_SCAN = [64, 65, 66, 67, 68, 69, 70];

const blocks = [];
const wallCells = new Set(); // x,z where wall body exists at y>=65
const wallFill64 = new Map(); // x,z -> block for y64

// 1) Discover wall footprint and choose y64 fill
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    let fill = null;
    let hasWall = false;
    for (const y of Y_SCAN) {
      if (y === 64) continue;
      const b = await get(x, y, z);
      if (!isWallBody(b)) continue;
      hasWall = true;
      const f = isSolidPillarFill(b);
      // prefer concrete from lowest wall tier (y65)
      if (y === 65 && f) fill = f;
      else if (!fill && f) fill = f;
    }
    if (hasWall) {
      wallCells.add(`${x},${z}`);
      if (fill) wallFill64.set(`${x},${z}`, fill);
    }
  }
}

console.log("wall cells", wallCells.size);

// 2) Restore pillar/wall bases at y64; clear decor under/inside wall
for (const key of wallCells) {
  const [x, z] = key.split(",").map(Number);
  const fill = wallFill64.get(key) || "white_concrete";
  const at64 = id(await get(x, 64, z));
  // always clear leaves/shroom/vine/air gaps under wall
  if (at64 === "air" || at64 === "cave_air" || isDecor(at64) || at64 === "grass_block" || at64 === "dirt") {
    blocks.push({ x, y: 64, z, block: fill });
  } else if (isPave(at64)) {
    // pavement at y64 under wall is wrong gap-fill — replace with pillar base
    blocks.push({ x, y: 64, z, block: fill });
  }
  // ensure y63 under wall pier is solid (not dirt hole) — keep pave if plaza, else grass ok outside
  const at63 = id(await get(x, 63, z));
  if (at63 === "dirt" || at63 === "air" || at63 === "cave_air") {
    // if this xz is in plaza z band use mix, else grass
    if (z >= 194 && z <= 210) {
      const bag = ["stone", "stone", "andesite", "cobblestone"];
      blocks.push({ x, y: 63, z, block: bag[(Math.random() * 4) | 0] });
    } else {
      blocks.push({ x, y: 63, z, block: "grass_block" });
    }
  }
}

// 3) Remove greenery that is inside wall OR east of west face under/through wall column band
//    Also remove stray leaves clearly "inside" gate (x>=-705 and z between pier faces on path)
for (let x = X0; x <= X1; x++) {
  for (let z = Z0; z <= Z1; z++) {
    const b64 = await get(x, 64, z);
    if (!isDecor(b64)) continue;
    if (wallCells.has(`${x},${z}`)) {
      // already handled by fill above, but ensure
      const fill = wallFill64.get(`${x},${z}`) || "white_concrete";
      blocks.push({ x, y: 64, z, block: fill });
      continue;
    }
    // leaves east of / between piers on the plaza (inside gate structure band)
    // piers occupy x=-705..-703 roughly; anything with leaf at x>=-705 that has wall neighbor above-level is "inside"
    if (x >= -705 && x <= -700) {
      // if adjacent to wall cell, treat as inside/against wrong side — clear if not on grass exterior
      let nextToWall = false;
      for (const [dx, dz] of [
        [1, 0],
        [-1, 0],
        [0, 1],
        [0, -1],
      ]) {
        if (wallCells.has(`${x + dx},${z + dz}`)) nextToWall = true;
      }
      const g = id(await get(x, 63, z));
      // inside plaza pave under leaves near wall = wrong
      if (nextToWall && isPave(g)) {
        blocks.push({ x, y: 64, z, block: "air" });
      }
      // leaves at x>-705 on north/south rim that stuck through
      if (x > -705 && (z === 194 || z === 210 || z === 195 || z === 209 || z === 193 || z === 211)) {
        // keep only if west of wall and on grass — else clear
        if (x >= -705) blocks.push({ x, y: 64, z, block: "air" });
      }
    }
  }
}

// 4) Connect outer greenery to west face of wall (x=-706), on grass only; mirrored N/S
//    North rim z=193, south rim z=211; also continue along north of pier z=192 if needed
const connectZsNorth = [193];
const connectZsSouth = [211];
const wallWestX = -705;

async function placeLeafIfOk(x, z, preferShroom) {
  if (wallCells.has(`${x},${z}`)) return false; // never through wall
  if (x >= wallWestX) return false; // do not place on/through wall x
  const g = id(await get(x, 63, z));
  if (isPave(g)) return false; // not on road
  if (g !== "grass_block" && g !== "dirt") {
    // allow if we set grass
    blocks.push({ x, y: 63, z, block: "grass_block" });
  } else if (g === "dirt") {
    blocks.push({ x, y: 63, z, block: "grass_block" });
  }
  // clear any gap — leaf sits on grass, no air under (y63 solid)
  blocks.push({
    x,
    y: 64,
    z,
    block: preferShroom ? "shroomlight" : LEAF,
  });
  return true;
}

// Fill gap from existing belt to wall on z=193 and z=211
for (const z of [...connectZsNorth, ...connectZsSouth]) {
  // find westernmost existing leaf on this z, fill eastward to x=-706
  let hasAny = false;
  for (let x = X0; x < wallWestX; x++) {
    if (isDecor(await get(x, 64, z))) hasAny = true;
  }
  // place continuous belt x=-710 .. -706 (or wherever grass), stop at -706
  for (let x = -710; x <= -706; x++) {
    const g = id(await get(x, 63, z));
    if (isPave(g)) continue;
    // shroom every 6 along x
    const shroom = (x - -710) % 6 === 0;
    await placeLeafIfOk(x, z, shroom);
  }
}

// Also ensure north/south faces outside piers (z=192 / z=212) don't need belt —
// user asked connect to wall; west approach on 193/211 is enough.

// Fix dirt holes near corners
for (const [x, z] of [
  [-701, 193],
  [-700, 193],
  [-701, 210],
  [-701, 211],
  [-702, 211],
  [-700, 210],
]) {
  const g = id(await get(x, 63, z));
  if (g === "dirt") {
    if (z >= 194 && z <= 210 && x >= -720) {
      const bag = ["stone", "stone", "andesite", "cobblestone"];
      blocks.push({ x, y: 63, z, block: bag[(Math.random() * 4) | 0] });
    } else {
      blocks.push({ x, y: 63, z, block: "grass_block" });
    }
  }
}

// Dedup by x,y,z last-write-wins
const map = new Map();
for (const b of blocks) map.set(`${b.x},${b.y},${b.z}`, b);
const final = [...map.values()];
console.log("ops", final.length, "wallFills", wallFill64.size);
await setBlocks(final);

// Verify
function sym(b) {
  const i = id(b);
  if (i === "air" || i === "cave_air") return " ";
  if (i.includes("leaves")) return "L";
  if (i === "shroomlight") return "*";
  if (i.includes("white_concrete")) return "W";
  if (i.includes("light_gray_concrete")) return "g";
  if (i.includes("iron_bar")) return "I";
  if (i === "grass_block") return ".";
  if (i === "stone") return "S";
  if (i === "andesite") return "A";
  if (i === "cobblestone") return "C";
  return "?";
}

console.log("--- pillar cols y63-70 ---");
for (const [x, z] of [
  [-705, 194],
  [-704, 194],
  [-703, 194],
  [-705, 193],
  [-704, 193],
  [-705, 210],
  [-704, 210],
  [-705, 211],
]) {
  const col = [];
  for (let y = 63; y <= 70; y++) col.push(sym(await get(x, y, z)));
  console.log(`(${x},${z})`, col.join(""));
}
console.log("--- y64 belt to wall x=-712..-700 z=192..195 ---");
for (let z = 192; z <= 195; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -712; x <= -700; x++) row += sym(await get(x, 64, z));
  console.log(row);
}
console.log("--- y64 south z=209..212 ---");
for (let z = 209; z <= 212; z++) {
  let row = String(z).padStart(3) + "|";
  for (let x = -712; x <= -700; x++) row += sym(await get(x, 64, z));
  console.log(row);
}
