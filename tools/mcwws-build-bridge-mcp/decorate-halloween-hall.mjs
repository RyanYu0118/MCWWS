import fs from 'node:fs';

// Halloween lobby dressing inside the existing deepslate ring courtyard.
// Envelope: x -377..-315, z 2747..2809, y 95..101. Never replaces the y=94 platform,
// the ring wall (r>=33 structure), stair flights, or the three gate corridors.
const C = {x: -346, z: 2778};
const dir = 'tools/mcwws-build-bridge-mcp/halloween-hall-20261021/';
fs.mkdirSync(dir, {recursive: true});
const token = fs.readFileSync('plugins/MCWWS_BuildBridge/config.yml', 'utf8').match(/token:\s*(\S+)/)[1];

async function api(endpoint, body) {
  const r = await fetch('http://127.0.0.1:8765/' + endpoint, {
    method: 'POST',
    headers: {Authorization: `Bearer ${token}`, 'Content-Type': 'application/json'},
    body: JSON.stringify({world: 'world', ...body}),
  });
  const j = await r.json();
  if (!r.ok || j.ok === false) throw new Error(JSON.stringify(j));
  return j;
}

async function read(list) {
  let i = 0;
  const result = new Array(list.length);
  await Promise.all(Array.from({length: 16}, async () => {
    while (i < list.length) {
      const n = i++;
      const p = list[n];
      const j = await api('get_block', {x: p.x, y: p.y, z: p.z});
      result[n] = (j.block || 'minecraft:air');
    }
  }));
  return result;
}

const idOf = (b) => (b || 'minecraft:air').split('[')[0];
const airy = (b) => idOf(b) === 'minecraft:air' || idOf(b) === 'minecraft:cave_air';
const key = (x, y, z) => `${x},${y},${z}`;

function rot(x, z, turns) {
  for (let i = 0; i < turns; i++) [x, z] = [-z, x];
  return [x, z];
}
const forbidden = new Set();
for (let turns = 0; turns < 4; turns++) {
  for (let x = 22; x <= 35; x++) {
    for (let z = -14; z <= 11; z++) {
      const [a, c] = rot(x, z, turns);
      forbidden.add(`${a},${c}`);
    }
  }
}
const rays = [[-1, 0], [0, 1], [31.5, 27.5]];
function nearGate(x, z) {
  for (const [dx, dz] of rays) {
    const n = Math.hypot(dx, dz);
    const ux = dx / n;
    const uz = dz / n;
    const along = x * ux + z * uz;
    const perp = Math.abs(x * uz - z * ux);
    if (along > 7 && perp <= 3) return true;
  }
  return false;
}

const plan = new Map();
function propose(dx, y, dz, block) {
  if (y < 95 || y > 101) return;
  const r = Math.hypot(dx, dz);
  if (r > 37.5) return;
  const x = C.x + dx;
  const z = C.z + dz;
  if (x < -377 || x > -315 || z < 2747 || z > 2809) return;
  plan.set(key(x, y, z), {x, y, z, block: block.startsWith('minecraft:') ? block : 'minecraft:' + block});
}

const solidFloor = new Set();
function booth(angle) {
  const a = angle * Math.PI / 180;
  const ux = Math.cos(a);
  const uz = Math.sin(a);
  const tx = -uz;
  const tz = ux;
  const base = 15;
  const cells = [-1, 0, 1].map((t) => [Math.round(ux * base + tx * t), Math.round(uz * base + tz * t), t]);
  const front = [-1, 0, 1].map((t) => [Math.round(ux * (base - 2) + tx * t), Math.round(uz * (base - 2) + tz * t)]);
  const hay = [Math.round(ux * (base + 2)), Math.round(uz * (base + 2))];
  const pot = [Math.round(ux * (base + 1) + tx * 2), Math.round(uz * (base + 1) + tz * 2)];
  return {cells, front, hay, pot, ux, uz};
}

const booths = [0, 115, 150, 205, 235, 270, 305, 340].map(booth);
for (const b of booths) {
  if (b.cells.some(([x, z]) => forbidden.has(`${x},${z}`) || nearGate(x, z) || Math.hypot(x, z) > 21)) continue;
  if (b.front.some(([x, z]) => forbidden.has(`${x},${z}`))) continue;
  for (const [x, z, t] of b.cells) {
    if (t === 0) {
      propose(x, 95, z, 'dark_oak_fence');
      propose(x, 96, z, 'dark_oak_slab[type=bottom,waterlogged=false]');
    } else if (t < 0) {
      propose(x, 95, z, 'polished_blackstone');
      propose(x, 96, z, 'soul_lantern[hanging=false,waterlogged=false]');
    } else {
      const face = Math.abs(b.ux) > Math.abs(b.uz) ? (b.ux > 0 ? 'west' : 'east') : (b.uz > 0 ? 'north' : 'south');
      propose(x, 95, z, `jack_o_lantern[facing=${face}]`);
      propose(x, 96, z, 'orange_candle[candles=3,lit=true,waterlogged=false]');
    }
  }
  propose(b.hay[0], 95, b.hay[1], 'hay_block[axis=y]');
  const rotBanner = (Math.round((Math.atan2(-b.ux, b.uz) * 8) / Math.PI) + 16) % 16;
  propose(b.hay[0], 96, b.hay[1], `orange_banner[rotation=${rotBanner}]`);
  propose(b.pot[0], 95, b.pot[1], 'potted_dead_bush');
  solidFloor.add(`${b.cells[0][0]},${b.cells[0][1]}`);
}

for (let deg = 0; deg < 360; deg += 45) {
  const a = deg * Math.PI / 180;
  const x = Math.round(Math.cos(a) * 4);
  const z = Math.round(Math.sin(a) * 4);
  if (nearGate(x, z) || Math.hypot(x, z) < 2) continue;
  propose(x, 95, z, deg % 90 === 0
    ? 'candle[candles=4,lit=true,waterlogged=false]'
    : 'orange_candle[candles=4,lit=true,waterlogged=false]');
}

for (const [dx, dz] of rays) {
  const n = Math.hypot(dx, dz);
  const ux = dx / n;
  const uz = dz / n;
  const px = -uz;
  const pz = ux;
  for (let s = 3; s <= 28; s++) {
    for (const side of [0]) {
      const x = Math.round(ux * s + px * side);
      const z = Math.round(uz * s + pz * side);
      if (forbidden.has(`${x},${z}`) || Math.hypot(x, z) > 30) continue;
      const k = key(C.x + x, 95, C.z + z);
      if (!plan.has(k)) propose(x, 95, z, s % 2 === 0 ? 'black_carpet' : 'orange_carpet');
    }
  }
}

for (let i = 0; i < 48; i++) {
  const a = (i / 48) * Math.PI * 2;
  const x = Math.round(Math.cos(a) * 31);
  const z = Math.round(Math.sin(a) * 31);
  if (forbidden.has(`${x},${z}`) || nearGate(x, z)) continue;
  if (i % 2 === 0) propose(x, 95, z, 'jack_o_lantern[facing=north]');
}

const candidates = [...plan.values()];
const supportReads = [];
for (const p of candidates) {
  supportReads.push({x: p.x, y: p.y, z: p.z, role: 'self'});
  supportReads.push({x: p.x, y: p.y - 1, z: p.z, role: 'below'});
}
const current = await read(supportReads);
const byKey = new Map();
for (let i = 0; i < supportReads.length; i++) {
  const p = supportReads[i];
  byKey.set(`${p.role}:${key(p.x, p.y, p.z)}`, current[i]);
}

const accepted = [];
const dropped = [];
const plannedKeys = new Set(candidates.map((p) => key(p.x, p.y, p.z)));
for (const p of candidates.sort((a, b) => a.y - b.y)) {
  const self = byKey.get(`self:${key(p.x, p.y, p.z)}`);
  const below = byKey.get(`below:${key(p.x, p.y - 1, p.z)}`);
  const belowKey = key(p.x, p.y - 1, p.z);
  const supported = !airy(below) || plannedKeys.has(belowKey) && accepted.some((q) => key(q.x, q.y, q.z) === belowKey && !q.block.includes('carpet') && !q.block.includes('candle') && !q.block.includes('banner') && !q.block.includes('torch'));
  if (!airy(self) || !supported) {
    dropped.push({...p, self: idOf(self), below: idOf(below)});
    continue;
  }
  accepted.push(p);
}

const torchSpots = [];
for (let i = 0; i < 36; i++) {
  const a = (i / 36) * Math.PI * 2;
  const dx = Math.round(Math.cos(a) * 32);
  const dz = Math.round(Math.sin(a) * 32);
  if (forbidden.has(`${dx},${dz}`) || nearGate(dx, dz)) continue;
  torchSpots.push({x: C.x + dx, y: 97, z: C.z + dz, dx, dz});
}
const torchRead = [];
for (const t of torchSpots) {
  torchRead.push(t);
  for (const [ox, oz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) torchRead.push({x: t.x + ox, y: 97, z: t.z + oz, ox, oz, host: t});
}
const torchNow = await read(torchRead);
let cursor = 0;
const faces = [[1, 0, 'west'], [-1, 0, 'east'], [0, 1, 'north'], [0, -1, 'south']];
for (const t of torchSpots) {
  const self = torchNow[cursor++];
  const neighbors = faces.map(([ox, oz, facing]) => ({ox, oz, facing, block: torchNow[cursor++]}));
  if (!airy(self)) continue;
  const wall = neighbors
    .filter((n) => !airy(n.block) && !idOf(n.block).includes('carpet') && !idOf(n.block).includes('torch') && !idOf(n.block).includes('lantern'))
    .sort((a, b) => Math.hypot(t.dx + b.ox, t.dz + b.oz) - Math.hypot(t.dx + a.ox, t.dz + a.oz))[0];
  if (!wall) continue;
  accepted.push({x: t.x, y: 97, z: t.z, block: `minecraft:soul_wall_torch[facing=${wall.facing}]`});
}

const gallery = [];
for (let i = 0; i < 28; i++) {
  const a = (i / 28) * Math.PI * 2;
  const dx = Math.round(Math.cos(a) * 35);
  const dz = Math.round(Math.sin(a) * 35);
  if (nearGate(dx, dz) || forbidden.has(`${dx},${dz}`)) continue;
  gallery.push({x: C.x + dx, y: 95, z: C.z + dz, kind: i % 2 ? 'candle' : 'jack'});
}
const galleryNow = await read(gallery.flatMap((g) => [g, {x: g.x, y: 94, z: g.z}]));
for (let i = 0; i < gallery.length; i++) {
  const self = galleryNow[i * 2];
  const below = galleryNow[i * 2 + 1];
  if (!airy(self) || airy(below)) continue;
  const g = gallery[i];
  accepted.push({
    x: g.x, y: 95, z: g.z,
    block: g.kind === 'jack'
      ? 'minecraft:jack_o_lantern[facing=north]'
      : 'minecraft:candle[candles=2,lit=true,waterlogged=false]',
  });
}

const unique = new Map();
for (const p of accepted) unique.set(key(p.x, p.y, p.z), p);
const blocks = [...unique.values()].sort((a, b) => a.y - b.y || a.x - b.x || a.z - b.z);
fs.writeFileSync(dir + 'plan.json', JSON.stringify({center: C, bounds: {x: [-377, -315], y: [95, 101], z: [2747, 2809]}, blocks, dropped: dropped.length}, null, 2));
console.log('accepted', blocks.length, 'dropped', dropped.length);
if (!blocks.length) throw new Error('Nothing to place');

const before = await read(blocks);
fs.writeFileSync(dir + 'before.json', JSON.stringify(blocks.map((p, i) => ({...p, previous: before[i]}))));
const dirty = before.filter((b) => !airy(b));
if (dirty.length) throw new Error('Destination occupied: ' + dirty.length);

for (let i = 0; i < blocks.length; i += 200) {
  const chunk = blocks.slice(i, i + 200);
  const result = await api('set_blocks', {blocks: chunk});
  console.log('wrote', i, chunk.length, JSON.stringify(result));
}
const after = await read(blocks);
const failures = [];
for (let i = 0; i < blocks.length; i++) {
  if (idOf(after[i]) !== idOf(blocks[i].block)) failures.push({...blocks[i], actual: after[i]});
}
fs.writeFileSync(dir + 'verification.json', JSON.stringify({placed: blocks.length, failures}, null, 2));
console.log('failures', failures.length);
if (failures.length) console.log(failures.slice(0, 12));
