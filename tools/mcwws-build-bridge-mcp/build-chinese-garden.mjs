/**
 * One-shot Chinese garden builder for MCWWS_BuildBridge HTTP API.
 * Center: player horizontal pos; groundY = grass_block layer.
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "../..");
const cfg = fs.readFileSync(path.join(root, "plugins/MCWWS_BuildBridge/config.yml"), "utf8");
const token = (cfg.match(/token:\s*(\S+)/) || [])[1];
if (!token || token === '""' || token === "''") throw new Error("missing token");

const BASE = "http://127.0.0.1:8765";
const world = "world";
const cx = -4192;
const cz = -1229;
const gy = 66; // grass_block
const R = 30;

const map = new Map(); // "x,y,z" -> block

function key(x, y, z) {
  return `${x},${y},${z}`;
}
function set(x, y, z, block) {
  map.set(key(x, y, z), { x, y, z, block });
}
function inCircle(x, z, r = R) {
  const dx = x - cx;
  const dz = z - cz;
  return dx * dx + dz * dz <= r * r;
}
function dist(x, z) {
  return Math.hypot(x - cx, z - cz);
}
function hash(x, z) {
  return Math.abs((x * 73856093) ^ (z * 19349663)) >>> 0;
}

// --- clear air column & base grass inside circle ---
for (let x = cx - R; x <= cx + R; x++) {
  for (let z = cz - R; z <= cz + R; z++) {
    if (!inCircle(x, z)) continue;
    set(x, gy, z, "grass_block");
    for (let y = gy + 1; y <= gy + 14; y++) set(x, y, z, "air");
  }
}

// --- outer winding path ring (stone mosaic) ---
for (let x = cx - R; x <= cx + R; x++) {
  for (let z = cz - R; z <= cz + R; z++) {
    const d = dist(x, z);
    if (d >= 26.2 && d <= 29.4) {
      const h = hash(x, z);
      const b =
        h % 7 === 0
          ? "mossy_stone_bricks"
          : h % 5 === 0
            ? "cracked_stone_bricks"
            : h % 3 === 0
              ? "andesite"
              : "stone_bricks";
      set(x, gy, z, b);
      set(x, gy + 1, z, "air");
    }
  }
}

// --- inner gravel/stone walkways (cross + ring) ---
for (let x = cx - R; x <= cx + R; x++) {
  for (let z = cz - R; z <= cz + R; z++) {
    if (!inCircle(x, z, 26)) continue;
    const dx = Math.abs(x - cx);
    const dz = Math.abs(z - cz);
    const d = dist(x, z);
    const onCross = (dx <= 1 && dz <= 18) || (dz <= 1 && dx <= 18);
    const onInnerRing = d >= 11.5 && d <= 13.2;
    if (onCross || onInnerRing) {
      const h = hash(x, z);
      set(x, gy, z, h % 4 === 0 ? "smooth_stone" : "polished_andesite");
    }
  }
}

// --- pond (ellipse) ---
const px = cx - 9;
const pz = cz + 7;
const prx = 8;
const prz = 5.5;
function inPond(x, z) {
  const dx = (x - px) / prx;
  const dz = (z - pz) / prz;
  return dx * dx + dz * dz <= 1;
}
function nearPondRim(x, z) {
  const dx = (x - px) / (prx + 1.2);
  const dz = (z - pz) / (prz + 1.2);
  const outer = dx * dx + dz * dz <= 1;
  return outer && !inPond(x, z);
}
for (let x = px - 12; x <= px + 12; x++) {
  for (let z = pz - 10; z <= pz + 10; z++) {
    if (!inCircle(x, z)) continue;
    if (inPond(x, z)) {
      set(x, gy - 1, z, "dirt");
      set(x, gy, z, "water");
      set(x, gy + 1, z, "air");
      // lily pads sporadically
      if (hash(x, z) % 11 === 0) set(x, gy + 1, z, "lily_pad");
    } else if (nearPondRim(x, z)) {
      const h = hash(x, z);
      set(x, gy, z, h % 2 === 0 ? "stone" : "cobblestone");
      if (h % 9 === 0) set(x, gy + 1, z, "moss_carpet");
    }
  }
}

// stepping stones / flat bridge across pond
for (let t = -7; t <= 7; t++) {
  const x = Math.round(px + t * 0.85);
  const z = Math.round(pz - 1 + (t % 3 === 0 ? 1 : 0));
  if (!inCircle(x, z)) continue;
  set(x, gy, z, "spruce_slab"); // may sit on water edge
  if (inPond(x, z)) {
    set(x, gy, z, "spruce_planks");
    set(x, gy + 1, z, "air");
  }
}

// --- rockery (假山) ---
const rx0 = cx - 16;
const rz0 = cz - 14;
for (let i = 0; i < 55; i++) {
  const ox = (hash(rx0, rz0 + i) % 9) - 4;
  const oz = (hash(rz0, rx0 + i) % 9) - 4;
  const h = 2 + (hash(i, ox) % 5);
  const x = rx0 + ox;
  const z = rz0 + oz;
  if (!inCircle(x, z, 28)) continue;
  for (let y = 0; y < h; y++) {
    const mats = ["cobblestone", "mossy_cobblestone", "andesite", "stone", "tuff"];
    set(x, gy + 1 + y, z, mats[(y + i) % mats.length]);
  }
  if (h >= 3 && hash(x, z) % 3 === 0) {
    set(x, gy + 1 + h, z, hash(x, z) % 2 === 0 ? "azalea" : "flowering_azalea");
  }
  if (hash(x, z) % 5 === 0) set(x + 1, gy + 1, z, "moss_block");
}

// --- bamboo grove ---
const bx = cx + 15;
const bz = cz + 14;
for (let x = bx - 4; x <= bx + 4; x++) {
  for (let z = bz - 4; z <= bz + 4; z++) {
    if (!inCircle(x, z, 28)) continue;
    if (Math.hypot(x - bx, z - bz) > 4.2) continue;
    set(x, gy, z, "podzol");
    if (hash(x, z) % 3 !== 0) {
      const h = 3 + (hash(x, z) % 5);
      for (let y = 1; y <= h; y++) {
        set(x, gy + y, z, "bamboo");
      }
    }
  }
}

// --- cherry trees ---
function cherryTree(tx, tz) {
  if (!inCircle(tx, tz, 27)) return;
  set(tx, gy, tz, "dirt");
  for (let y = 1; y <= 4; y++) set(tx, gy + y, tz, "cherry_log");
  for (let dx = -3; dx <= 3; dx++) {
    for (let dz = -3; dz <= 3; dz++) {
      for (let dy = 3; dy <= 6; dy++) {
        if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 4) > 5) continue;
        if (dx === 0 && dz === 0 && dy <= 4) continue;
        const x = tx + dx;
        const z = tz + dz;
        if (!inCircle(x, z, 29)) continue;
        if (hash(x + dy, z) % 7 === 0) continue;
        set(x, gy + dy, z, "cherry_leaves");
      }
    }
  }
}
cherryTree(cx + 10, cz - 18);
cherryTree(cx - 18, cz + 2);
cherryTree(cx + 4, cz + 18);

// --- moon gate (月亮门) south ---
const mgx = cx;
const mgz = cz - 24;
const mgy = gy + 1;
// pillar bases
for (let dx = -4; dx <= 4; dx++) {
  for (let dz = -1; dz <= 1; dz++) {
    set(mgx + dx, gy, mgz + dz, "smooth_quartz");
  }
}
for (let y = 0; y <= 6; y++) {
  for (let dx = -4; dx <= 4; dx++) {
    for (let dz = -1; dz <= 0; dz++) {
      const onCircle = Math.hypot(dx, y - 3) <= 3.6 && Math.hypot(dx, y - 3) >= 2.2;
      const wall = Math.abs(dx) >= 3 && y <= 5;
      if (onCircle || wall) {
        set(mgx + dx, mgy + y, mgz + dz, "smooth_quartz");
      }
    }
  }
}
// dark oak trim
for (let y = 0; y <= 6; y++) {
  set(mgx - 4, mgy + y, mgz, "dark_oak_log");
  set(mgx + 4, mgy + y, mgz, "dark_oak_log");
}
for (let dx = -4; dx <= 4; dx++) {
  set(mgx + dx, mgy + 6, mgz, "dark_oak_slab");
}

// --- pavilion (亭) east ---
const vx = cx + 14;
const vz = cz - 8;
// platform
for (let dx = -3; dx <= 3; dx++) {
  for (let dz = -3; dz <= 3; dz++) {
    set(vx + dx, gy, vz + dz, "polished_andesite");
    set(vx + dx, gy + 1, vz + dz, Math.abs(dx) === 3 || Math.abs(dz) === 3 ? "stone_brick_slab" : "air");
  }
}
// raise floor
for (let dx = -2; dx <= 2; dx++) {
  for (let dz = -2; dz <= 2; dz++) {
    set(vx + dx, gy + 1, vz + dz, "spruce_planks");
  }
}
// pillars
const pillars = [
  [-2, -2],
  [-2, 2],
  [2, -2],
  [2, 2],
];
for (const [dx, dz] of pillars) {
  for (let y = 2; y <= 5; y++) set(vx + dx, gy + y, vz + dz, "dark_oak_log");
}
// railings
for (let dx = -2; dx <= 2; dx++) {
  set(vx + dx, gy + 2, vz - 2, "spruce_fence");
  set(vx + dx, gy + 2, vz + 2, "spruce_fence");
}
for (let dz = -2; dz <= 2; dz++) {
  set(vx - 2, gy + 2, vz + dz, "spruce_fence");
  set(vx + 2, gy + 2, vz + dz, "spruce_fence");
}
// open entrance west
set(vx - 2, gy + 2, vz, "air");
set(vx - 2, gy + 3, vz, "air");
// roof layers
for (let dx = -3; dx <= 3; dx++) {
  for (let dz = -3; dz <= 3; dz++) {
    if (Math.max(Math.abs(dx), Math.abs(dz)) === 3) set(vx + dx, gy + 5, vz + dz, "dark_oak_stairs");
    if (Math.max(Math.abs(dx), Math.abs(dz)) <= 2) set(vx + dx, gy + 6, vz + dz, "dark_oak_planks");
    if (Math.max(Math.abs(dx), Math.abs(dz)) <= 1) set(vx + dx, gy + 7, vz + dz, "dark_oak_slab");
  }
}
set(vx, gy + 8, vz, "dark_oak_fence");
set(vx, gy + 4, vz, "lantern");
set(vx, gy + 3, vz, "air");

// --- white wall fragment NW ---
const wx = cx - 20;
const wz = cz - 6;
for (let i = 0; i < 10; i++) {
  const x = wx;
  const z = wz + i;
  if (!inCircle(x, z, 28)) continue;
  set(x, gy, z, "smooth_stone");
  for (let y = 1; y <= 3; y++) set(x, gy + y, z, "white_concrete");
  set(x, gy + 4, z, "dark_oak_slab");
  if (i % 3 === 0) {
    set(x, gy + 1, z, "dark_oak_log");
    set(x, gy + 2, z, "dark_oak_log");
    set(x, gy + 3, z, "dark_oak_log");
  }
}

// --- stone lanterns along outer path ---
for (let ang = 0; ang < 360; ang += 40) {
  const rad = (ang * Math.PI) / 180;
  const x = Math.round(cx + Math.cos(rad) * 27.5);
  const z = Math.round(cz + Math.sin(rad) * 27.5);
  if (!inCircle(x, z)) continue;
  set(x, gy, z, "stone_bricks");
  set(x, gy + 1, z, "cobblestone_wall");
  set(x, gy + 2, z, "cobblestone_wall");
  set(x, gy + 3, z, "lantern");
}

// --- flower beds & shrubs ---
const flowers = [
  "pink_tulip",
  "white_tulip",
  "red_tulip",
  "azure_bluet",
  "oxeye_daisy",
  "lilac",
  "peony",
  "rose_bush",
  "flowering_azalea",
  "azalea",
];
for (let x = cx - R; x <= cx + R; x++) {
  for (let z = cz - R; z <= cz + R; z++) {
    if (!inCircle(x, z, 25)) continue;
    const d = dist(x, z);
    if (d < 4) continue;
    if (inPond(x, z) || nearPondRim(x, z)) continue;
    // skip solid structures already marked as non-grass at gy
    const ground = map.get(key(x, gy, z));
    if (ground && ground.block !== "grass_block" && ground.block !== "podzol") continue;
    const above = map.get(key(x, gy + 1, z));
    if (above && above.block !== "air") continue;
    const h = hash(x, z);
    if (h % 17 === 0) {
      set(x, gy + 1, z, flowers[h % flowers.length]);
    } else if (h % 29 === 0) {
      set(x, gy + 1, z, "short_grass");
    } else if (h % 41 === 0) {
      set(x, gy + 1, z, "fern");
    }
  }
}

// --- center plaza (keep open for player) ---
for (let x = cx - 3; x <= cx + 3; x++) {
  for (let z = cz - 3; z <= cz + 3; z++) {
    const d = Math.hypot(x - cx, z - cz);
    if (d <= 3.2) {
      set(x, gy, z, d <= 1.2 ? "polished_diorite" : "smooth_stone");
      for (let y = gy + 1; y <= gy + 4; y++) set(x, y, z, "air");
    }
  }
}
// center water basin
set(cx, gy + 1, cz, "stone_brick_wall");
set(cx, gy + 1, cz - 1, "stone_brick_wall");
set(cx, gy + 1, cz + 1, "stone_brick_wall");
set(cx - 1, gy + 1, cz, "stone_brick_wall");
set(cx + 1, gy + 1, cz, "stone_brick_wall");
set(cx, gy + 1, cz, "water");
set(cx, gy + 2, cz, "air");
// reopen center: simple slab ring instead of enclosed walls
for (let dx = -1; dx <= 1; dx++) {
  for (let dz = -1; dz <= 1; dz++) {
    if (dx === 0 && dz === 0) {
      set(cx, gy, cz, "water");
      set(cx, gy + 1, cz, "air");
    } else if (Math.abs(dx) + Math.abs(dz) === 1) {
      set(cx + dx, gy + 1, cz + dz, "stone_brick_slab");
    } else {
      set(cx + dx, gy + 1, cz + dz, "air");
    }
  }
}

// --- small arched wooden bridge rail near pond ---
for (const [x, z] of [
  [px - 6, pz],
  [px + 6, pz],
]) {
  if (!inCircle(x, z)) continue;
  set(x, gy + 1, z, "spruce_fence");
  set(x, gy + 1, z + 1, "spruce_fence");
}

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
// Prefer solid builds after air clears: sort air first then others by y
all.sort((a, b) => {
  const aa = a.block === "air" ? 0 : 1;
  const bb = b.block === "air" ? 0 : 1;
  if (aa !== bb) return aa - bb;
  return a.y - b.y;
});

const BATCH = 4000;
console.log(`Total placements: ${all.length}`);
for (let i = 0; i < all.length; i += BATCH) {
  const chunk = all.slice(i, i + BATCH);
  const r = await postBatch(chunk);
  console.log(`Batch ${i / BATCH + 1}: changed=${r.changed}`);
}
console.log("Garden done.");
