/**
 * Copy selected chunks from SP June backup into server overworld regions.
 * Damaged approx: x -554..-508, z 199..243 -> chunks x -35..-32, z 12..15
 * Restore with margin: cx -36..-31, cz 11..16
 */
import fs from "fs";
import path from "path";
import provider from "prismarine-provider-anvil";

const Anvil = provider.Anvil("1.21.1");

function resolveSpRegionDir() {
  const roots = ["D:/Minecraft"];
  for (const root of roots) {
    const hits = [];
    const walk = (dir, depth = 0) => {
      if (depth > 8) return;
      let entries;
      try {
        entries = fs.readdirSync(dir, { withFileTypes: true });
      } catch {
        return;
      }
      for (const e of entries) {
        const p = path.join(dir, e.name);
        if (e.isDirectory()) {
          if (e.name === "node_modules" || e.name === ".git") continue;
          walk(p, depth + 1);
        } else if (e.name === "r.-2.0.mca" && p.includes("saves") && p.includes("overworld") && p.includes(`${path.sep}region${path.sep}`)) {
          const st = fs.statSync(p);
          if (st.size === 9277440) hits.push(path.dirname(p));
        }
      }
    };
    walk(root);
    if (hits.length) return hits[0];
  }
  throw new Error("SP backup region dir not found");
}

const serverRegion = "D:/Minecraft/服务器/26.2/world/dimensions/minecraft/overworld/region";
const spRegion = resolveSpRegionDir();
console.log("Backup:", spRegion);
console.log("Server:", serverRegion);

// Backup current damaged regions first
const stamp = new Date().toISOString().replace(/[:.]/g, "-");
const safety = path.join(serverRegion, `_pre_restore_${stamp}`);
fs.mkdirSync(safety, { recursive: true });
for (const f of ["r.-1.0.mca", "r.-2.0.mca"]) {
  const src = path.join(serverRegion, f);
  if (fs.existsSync(src)) {
    fs.copyFileSync(src, path.join(safety, f));
    console.log("Safety copy", f);
  }
}

const srcAnvil = new Anvil(spRegion);
const dstAnvil = new Anvil(serverRegion);

let copied = 0;
let missing = 0;
for (let cx = -36; cx <= -31; cx++) {
  for (let cz = 11; cz <= 16; cz++) {
    const raw = await srcAnvil.loadRaw(cx, cz);
    if (!raw) {
      missing++;
      continue;
    }
    await dstAnvil.saveRaw(cx, cz, raw);
    copied++;
    console.log(`Restored chunk ${cx},${cz}`);
  }
}
await srcAnvil.close?.();
await dstAnvil.close?.();
console.log(`Done. copied=${copied} missing=${missing}`);
