#!/usr/bin/env node
/**
 * Build / refresh Litematica search index.
 *
 * Usage:
 *   node scripts/build-schem-index.mjs
 *   node scripts/build-schem-index.mjs --quick
 *   node scripts/build-schem-index.mjs --max 500
 *   set MCWWS_SCHEM_ROOT=D:\path\to\schematics
 */
import {
  buildIndex,
  DEFAULT_SCHEM_ROOT,
  DEFAULT_INDEX_PATH,
  indexStatus,
} from "../src/schem-lib.js";

const args = process.argv.slice(2);
const quick = args.includes("--quick");
const maxIdx = args.indexOf("--max");
const maxFiles = maxIdx >= 0 ? Number(args[maxIdx + 1]) : Infinity;

console.log("root =", process.env.MCWWS_SCHEM_ROOT || DEFAULT_SCHEM_ROOT);
console.log("index =", process.env.MCWWS_SCHEM_INDEX || DEFAULT_INDEX_PATH);
console.log("parseMeta =", !quick, "maxFiles =", Number.isFinite(maxFiles) ? maxFiles : "all");

const t0 = Date.now();
const index = await buildIndex({
  parseMeta: !quick,
  maxFiles,
  concurrency: 10,
  onProgress: ({ done, total, parsed, reused, failed }) => {
    const pct = ((done / total) * 100).toFixed(1);
    console.log(
      `[${pct}%] ${done}/${total} parsed=${parsed} reused=${reused} failed=${failed}`
    );
  },
});

console.log("done in", ((Date.now() - t0) / 1000).toFixed(1), "s");
console.log(indexStatus());
console.log("entries", index.count);
