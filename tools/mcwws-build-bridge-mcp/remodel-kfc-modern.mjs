/**
 * Modern multi-volume KFC remodel (QSR, not matchbox).
 *
 * AABB:
 *   Shell:  x -556..-528, z 210..226, y 63..78
 *   Patio:  x -547..-537, z 207..209, y 63..65
 * Avoid: library_garden (x>=-517), macdonalds (z<=198), road z200-204 leaf rows
 *
 * Massing: center entrance bay flush south at z=210; side wings set back to z=212.
 * Barrel vault canopy over entrance; curtain walls; coping glow; patio seating.
 */
import fs from "fs";

const token = fs
  .readFileSync("D:/Minecraft/服务器/26.2/plugins/MCWWS_BuildBridge/config.yml", "utf8")
  .match(/token:\s*(\S+)/)[1];
const BASE = "http://127.0.0.1:8765";
const world = "world";

const X0 = -556,
  X1 = -528;
const Z_BACK = 226;
const Z_WING = 212; // side wings south face (set back)
const Z_ENT = 210; // entrance bay south face (protrudes)
const Z_PATIO0 = 207,
  Z_PATIO1 = 209;
const F = 64;
const GY = 63;
const doorX = -542;
const WING_L1 = -548; // west wing ends (inclusive east edge of west wing)
const WING_R0 = -536; // east wing starts
// center bay x: WING_L1+1 .. WING_R0-1 = -547..-537

const map = new Map();
function set(x, y, z, block) {
  // hard: never touch road leaf band / McD
  if (z <= 204 && z >= 199) return;
  if (z < Z_PATIO0 || z > Z_BACK) return;
  if (x < X0 || x > X1) return;
  if (x >= -527) return;
  map.set(`${x},${y},${z}`, { x, y, z, block });
}
function fill(x1, y1, z1, x2, y2, z2, block) {
  for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
    for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
      for (let z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, block);
}

const RED_A = "red_concrete";
const RED_B = "bricks";
const RED_C = "red_terracotta";
const WHITE_A = "white_concrete_powder";
const WHITE_B = "quartz_pillar[axis=y]";
const WHITE_C = "smooth_quartz";
const FRAME = "gray_concrete";
const FRAME_DK = "iron_block";
const GLASS = "gray_stained_glass";
const GLASS_P = "gray_stained_glass_pane[east=true,north=false,south=false,west=true,waterlogged=false]";

function redMix(x, y, z) {
  const n = Math.abs((x * 3 + y * 5 + z * 7) % 5);
  if (n <= 1) return RED_A;
  if (n <= 3) return RED_B;
  return RED_C;
}
function whiteMix(x, y, z) {
  const n = Math.abs((x + y * 2 + z) % 4);
  if (n === 0) return WHITE_B;
  if (n === 1) return WHITE_A;
  return WHITE_C;
}

// ========== clear old shell + patio (targeted, not world wipe) ==========
fill(X0, F, Z_ENT, X1, F + 14, Z_BACK, "air");
fill(X0, GY, Z_ENT, X1, GY, Z_BACK, "stone");
fill(doorX - 5, F, Z_PATIO0, doorX + 5, F + 3, Z_PATIO1, "air");

// ========== floors ==========
// wings
fill(X0 + 1, F, Z_WING + 1, WING_L1, F, Z_BACK - 1, "white_concrete");
fill(WING_R0, F, Z_WING + 1, X1 - 1, F, Z_BACK - 1, "white_concrete");
// center bay
fill(WING_L1 + 1, F, Z_ENT + 1, WING_R0 - 1, F, Z_BACK - 1, "white_concrete");
// kitchen rear strip
fill(X0 + 1, F, 219, X1 - 1, F, Z_BACK - 1, "smooth_stone");
// aisle carpet on solid
fill(doorX - 1, F + 1, Z_ENT + 1, doorX + 1, F + 1, 217, "red_carpet");
// patio floor
for (let x = doorX - 5; x <= doorX + 5; x++) {
  for (let z = Z_PATIO0; z <= Z_PATIO1; z++) {
    set(x, GY, z, redMix(x, GY, z) === RED_B ? "polished_andesite" : "smooth_stone");
    set(x, F, z, "air");
  }
}
fill(doorX - 5, GY, Z_PATIO0, doorX + 5, GY, Z_PATIO1, "smooth_stone");
for (let x = doorX - 5; x <= doorX + 5; x += 2) {
  for (let z = Z_PATIO0; z <= Z_PATIO1; z += 2) set(x, GY, z, "polished_andesite");
}

// ========== helper: vertical wall column ==========
function wallCol(x, z, y0, y1, matFn) {
  for (let y = y0; y <= y1; y++) set(x, y, z, matFn(x, y, z));
}

// ========== WEST / EAST WING shells (set back) ==========
const wingH = F + 5; // lower roof
const wingRoof = F + 6;
function buildWing(xa, xb) {
  // south face at Z_WING
  for (let x = xa; x <= xb; x++) {
    wallCol(x, Z_WING, F, wingH, (x, y) => (y >= F + 4 ? redMix(x, y, Z_WING) : whiteMix(x, y, Z_WING)));
    // curtain wall openings
    if (x > xa + 1 && x < xb - 1 && x % 3 !== 0) {
      for (let y = F + 1; y <= F + 3; y++) {
        set(x, y, Z_WING, GLASS);
        set(x, y, Z_WING, FRAME); // frame posts every 3 �?overwrite below
      }
    }
  }
  // redo: posts + glass panels
  for (let x = xa; x <= xb; x++) {
    const isPost = x === xa || x === xb || (x - xa) % 3 === 0;
    for (let y = F; y <= wingH; y++) {
      if (y === F) set(x, y, Z_WING, FRAME_DK);
      else if (y >= F + 4) set(x, y, Z_WING, redMix(x, y, Z_WING));
      else if (isPost) set(x, y, Z_WING, FRAME);
      else set(x, y, Z_WING, GLASS);
    }
  }
  // north / side walls
  for (let z = Z_WING; z <= Z_BACK; z++) {
    for (let y = F; y <= wingH; y++) {
      set(xa, y, z, y >= F + 4 ? redMix(xa, y, z) : whiteMix(xa, y, z));
      set(xb, y, z, y >= F + 4 ? redMix(xb, y, z) : whiteMix(xb, y, z));
    }
    set(xa, F, z, FRAME_DK);
    set(xb, F, z, FRAME_DK);
  }
  for (let x = xa; x <= xb; x++) {
    for (let y = F; y <= wingH; y++) {
      set(x, y, Z_BACK, y >= F + 4 ? redMix(x, y, Z_BACK) : whiteMix(x, y, Z_BACK));
    }
  }
  // wing roof deck
  fill(xa, wingRoof, Z_WING, xb, wingRoof, Z_BACK, WHITE_C);
  // coping + hidden glow
  for (let x = xa; x <= xb; x++) {
    set(x, wingRoof, Z_WING, "brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
    set(x, wingRoof - 1, Z_WING, "sea_lantern"); // under coping lip inside
    set(x, wingRoof, Z_BACK, "brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
    set(x, wingRoof - 1, Z_BACK, "sea_lantern");
  }
  // fix: sea lantern under roof edge inside building not outside south �?place at z=Z_WING+1 under slab strip
  for (let x = xa + 1; x <= xb - 1; x++) {
    set(x, wingRoof - 1, Z_WING + 1, "sea_lantern");
    set(x, wingRoof, Z_WING + 1, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  }
}
buildWing(X0, WING_L1);
buildWing(WING_R0, X1);

// clear mistaken sea lanterns on south face exterior that block glass �?rebuild south glass clean
function cleanWingSouth(xa, xb) {
  for (let x = xa; x <= xb; x++) {
    const isPost = x === xa || x === xb || (x - xa) % 3 === 0;
    for (let y = F; y <= wingH; y++) {
      if (y === F) set(x, y, Z_WING, FRAME_DK);
      else if (y >= F + 4) set(x, y, Z_WING, redMix(x, y, Z_WING));
      else if (isPost) set(x, y, Z_WING, FRAME);
      else set(x, y, Z_WING, GLASS);
    }
    // coping on top of south wall
    set(x, wingRoof, Z_WING, "brick_stairs[facing=south,half=top,shape=straight,waterlogged=false]");
    set(x, wingRoof, Z_WING + 1, "white_concrete_powder");
    if (x % 2 === 0) set(x, wingRoof - 1, Z_WING + 1, "sea_lantern");
  }
}
cleanWingSouth(X0, WING_L1);
cleanWingSouth(WING_R0, X1);

// ========== CENTER ENTRANCE BAY (protrudes to Z_ENT) ==========
const bayH = F + 7;
const vaultTop = F + 9;
const bayX0 = WING_L1 + 1,
  bayX1 = WING_R0 - 1;

// side walls of bay (connectors)
for (let z = Z_ENT; z <= Z_BACK; z++) {
  for (let y = F; y <= bayH; y++) {
    set(bayX0, y, z, y >= F + 5 ? redMix(bayX0, y, z) : FRAME);
    set(bayX1, y, z, y >= F + 5 ? redMix(bayX1, y, z) : FRAME);
  }
}
// north back of bay
for (let x = bayX0; x <= bayX1; x++) {
  for (let y = F; y <= bayH; y++) set(x, y, Z_BACK, whiteMix(x, y, Z_BACK));
}

// south curtain wall + door
for (let x = bayX0; x <= bayX1; x++) {
  for (let y = F; y <= bayH; y++) {
    const isDoor = x >= doorX - 1 && x <= doorX + 1 && y >= F && y <= F + 2;
    const isPost = x === bayX0 || x === bayX1 || x === doorX - 2 || x === doorX + 2;
    if (isDoor) set(x, y, Z_ENT, "air");
    else if (y === F) set(x, y, Z_ENT, FRAME_DK);
    else if (y >= F + 5) set(x, y, Z_ENT, redMix(x, y, Z_ENT));
    else if (isPost) set(x, y, Z_ENT, FRAME);
    else set(x, y, Z_ENT, GLASS);
  }
}
// door header
fill(doorX - 2, F + 3, Z_ENT, doorX + 2, F + 3, Z_ENT, FRAME_DK);

// Barrel vault canopy: arches from Z_ENT-1 (patio overhang) back over entrance
// Use stairs to form vault ribs along x for z = Z_ENT-1 and Z_ENT
for (let x = bayX0; x <= bayX1; x++) {
  for (const z of [Z_ENT - 1, Z_ENT, Z_ENT + 1]) {
    // vault profile by height offset from center
    const t = Math.abs(x - doorX) / Math.max(1, (bayX1 - bayX0) / 2);
    const rise = Math.round((1 - t * t) * 3); // 0..3
    const yBase = F + 5;
    for (let dy = 0; dy <= rise + 1; dy++) {
      const y = yBase + dy;
      const mat = dy === rise + 1 ? redMix(x, y, z) : dy % 2 === 0 ? RED_A : RED_B;
      set(x, y, z, mat);
    }
    // inner white soffit
    set(x, yBase + rise, z, WHITE_C);
  }
  // cantilever nose at Z_ENT-1 lower lip
  set(x, F + 5, Z_ENT - 1, "brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
  set(x, F + 4, Z_ENT - 1, "sea_lantern"); // glow under cantilever (supported against vault mass above via stairs stack �?lantern under stairs)
}

// Fix cantilever light: sea lantern must have block above �?vault mass at F+5 provides that for lantern at F+4
// Roof deck over bay
fill(bayX0, vaultTop, Z_ENT, bayX1, vaultTop, Z_BACK, WHITE_C);
for (let x = bayX0; x <= bayX1; x++) {
  set(x, vaultTop, Z_ENT - 1, "brick_stairs[facing=south,half=top,shape=straight,waterlogged=false]");
  set(x, vaultTop, Z_ENT, "white_concrete_powder");
  if (x % 2 === 0) set(x, vaultTop - 1, Z_ENT + 1, "sea_lantern");
  set(x, vaultTop, Z_BACK, "brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
}

// fill roof step between wing and bay
fill(bayX0, wingRoof + 1, Z_WING, bayX1, vaultTop - 1, Z_WING + 2, WHITE_C);

// ========== KFC logo on entrance facade (refined, same content CFK west→east after 180°) ==========
const sy = F + 6;
function mirrorRow(row) {
  return [...row].reverse().join("");
}
function mirrorLetter(patterns) {
  return patterns.map(mirrorRow);
}
const LETTER_K = mirrorLetter(["#  #", "# # ", "##  ", "# # ", "#  #"]);
const LETTER_F = mirrorLetter(["####", "#   ", "### ", "#   ", "#   "]);
const LETTER_C = mirrorLetter([" ###", "#   ", "#   ", "#   ", " ###"]);
function plot(ox, patterns) {
  for (let r = 0; r < patterns.length; r++) {
    const row = patterns[r];
    for (let c = 0; c < row.length; c++) {
      if (row[c] === "#") set(ox + c, sy + (patterns.length - 1 - r), Z_ENT, RED_A);
      else set(ox + c, sy + (patterns.length - 1 - r), Z_ENT, WHITE_C);
    }
  }
}
// backdrop panel
fill(doorX - 8, sy, Z_ENT, doorX + 8, sy + 5, Z_ENT, WHITE_C);
fill(doorX - 8, sy - 1, Z_ENT, doorX + 8, sy - 1, Z_ENT, FRAME);
plot(doorX - 7, LETTER_C);
plot(doorX - 2, LETTER_F);
plot(doorX + 3, LETTER_K);

// ========== INTERIOR partition / cashiers / kitchen (compact quality) ==========
const wallZ = 218;
fill(X0 + 1, F + 1, wallZ, X1 - 1, F + 3, wallZ, WHITE_A);
fill(X0 + 1, F + 4, wallZ, X1 - 1, F + 4, wallZ, RED_A);
function staffDoor(x, hinge) {
  fill(x, F + 1, wallZ, x, F + 2, wallZ, "air");
  set(x, F + 1, wallZ, `oak_door[facing=south,half=lower,hinge=${hinge},open=false,powered=false]`);
  set(x, F + 2, wallZ, `oak_door[facing=south,half=upper,hinge=${hinge},open=false,powered=false]`);
}
staffDoor(X0 + 3, "left");
staffDoor(X1 - 3, "right");
for (const rx of [doorX - 6, doorX, doorX + 6]) {
  fill(rx - 1, F + 1, wallZ, rx + 1, F + 2, wallZ, "air");
  fill(rx - 1, F + 1, wallZ, rx + 1, F + 1, wallZ, WHITE_C);
  fill(rx - 1, F + 2, wallZ, rx + 1, F + 2, wallZ, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  fill(rx - 1, F + 1, wallZ + 1, rx + 1, F + 1, wallZ + 1, WHITE_C);
  set(rx, F + 2, wallZ + 1, "lectern[facing=south,has_book=false,powered=false]");
}
// tables �?correct chair facing
function table(tx, tz) {
  if (tz <= Z_WING) return;
  set(tx, F + 1, tz, "oak_fence");
  set(tx, F + 2, tz, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  set(tx, F + 1, tz + 1, "oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
  set(tx, F + 1, tz - 1, "oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
}
for (const tx of [-552, -549]) for (const tz of [214, 216]) table(tx, tz);
for (const tx of [-535, -532]) for (const tz of [214, 216]) table(tx, tz);

// kitchen cook line
fill(X0 + 2, F + 1, Z_BACK - 1, X1 - 2, F + 1, Z_BACK - 1, "iron_block");
for (let x = X0 + 3; x <= X1 - 3; x++) {
  const k = (x - (X0 + 3)) % 3;
  set(
    x,
    F + 1,
    Z_BACK - 1,
    k === 0 ? "smoker[facing=south,lit=false]" : k === 1 ? "furnace[facing=south,lit=false]" : "blast_furnace[facing=south,lit=false]"
  );
}
fill(X0 + 2, F + 2, Z_BACK - 1, X1 - 2, F + 2, Z_BACK - 1, "iron_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]");
// hanging kitchen lights under roof
for (let x = X0 + 6; x <= X1 - 6; x += 5) {
  set(x, wingRoof - 1, 222, "lantern[hanging=true,waterlogged=false]");
}
set(doorX, vaultTop - 1, 220, "lantern[hanging=true,waterlogged=false]");

// ========== PATIO ==========
function patioTable(tx, tz) {
  set(tx, F, tz, "oak_fence");
  set(tx, F + 1, tz, "smooth_quartz_slab[type=bottom,waterlogged=false]");
  set(tx + 1, F, tz, "oak_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]");
  set(tx - 1, F, tz, "oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]");
}
patioTable(doorX - 3, 208);
patioTable(doorX + 3, 208);
// planters
for (const px of [doorX - 5, doorX + 5]) {
  set(px, F, Z_PATIO0, "flower_pot");
  set(px, F, Z_PATIO1, RED_B);
  set(px, F + 1, Z_PATIO1, "oak_sapling[stage=0]");
}
set(doorX, F, Z_PATIO0, "flower_pot");
set(doorX - 1, F, Z_PATIO0, "flower_pot");
set(doorX + 1, F, Z_PATIO0, "flower_pot");
// patio path link already mixed stone �?keep air above

// ensure door clear
fill(doorX - 1, F, Z_ENT, doorX + 1, F + 2, Z_ENT, "air");
fill(doorX - 1, F, Z_ENT - 1, doorX + 1, F + 2, Z_ENT - 1, "air");

async function postBatch(blocks) {
  const res = await fetch(`${BASE}/set_blocks`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ world, blocks }),
  });
  const json = await res.json();
  if (!res.ok || json.ok === false) throw new Error(json.error || res.statusText);
  return json;
}

const all = [...map.values()];
all.sort((a, b) => (a.block === "air" ? 0 : 1) - (b.block === "air" ? 0 : 1) || a.y - b.y);
console.log("placements", all.length);
for (let i = 0; i < all.length; i += 3500) {
  console.log("batch", i, await postBatch(all.slice(i, i + 3500)));
}
console.log("modern KFC remodel done");
