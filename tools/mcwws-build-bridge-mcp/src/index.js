#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { Agent, fetch as undiciFetch } from "undici";
import { z } from "zod";
import {
  buildIndex,
  indexStatus,
  searchSchems,
  schemInfo,
  DEFAULT_SCHEM_ROOT,
} from "./schem-lib.js";

const BASE_URL = (process.env.MCWWS_BUILD_URL || "http://127.0.0.1:8765").replace(/\/$/, "");
const TOKEN = process.env.MCWWS_BUILD_TOKEN || "";
const INSECURE_TLS =
  process.env.MCWWS_BUILD_INSECURE_TLS === "1" ||
  process.env.NODE_TLS_REJECT_UNAUTHORIZED === "0";

// 樱花等自签 HTTPS 隧道需关闭证书校验（仅建议内网/临时联机）
const insecureAgent = INSECURE_TLS
  ? new Agent({ connect: { rejectUnauthorized: false } })
  : undefined;

async function api(method, path, body) {
  if (!TOKEN) {
    throw new Error("MCWWS_BUILD_TOKEN is empty. Set it to plugins/MCWWS_BuildBridge/config.yml http.token");
  }
  const headers = {
    Authorization: `Bearer ${TOKEN}`,
    Accept: "application/json",
  };
  const init = { method, headers };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    init.body = JSON.stringify(body);
  }
  if (insecureAgent) {
    init.dispatcher = insecureAgent;
  }
  const res = await undiciFetch(`${BASE_URL}${path}`, init);
  const text = await res.text();
  let json;
  try {
    json = text ? JSON.parse(text) : {};
  } catch {
    throw new Error(`Non-JSON response (${res.status}): ${text.slice(0, 400)}`);
  }
  if (!res.ok || json.ok === false) {
    throw new Error(json.error || `HTTP ${res.status}`);
  }
  return json;
}

function toolResult(data) {
  return {
    content: [{ type: "text", text: JSON.stringify(data, null, 2) }],
  };
}

function toolError(err) {
  return {
    content: [{ type: "text", text: String(err?.message || err) }],
    isError: true,
  };
}

const server = new McpServer({
  name: "mcwws-build-bridge",
  version: "1.0.0",
});

server.tool(
  "list_players",
  "List online players with world and coordinates",
  {},
  async () => {
    try {
      return toolResult(await api("GET", "/list_players"));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "get_player_pos",
  "Get exact position and look direction of an online player",
  { player: z.string().describe("Exact player name") },
  async ({ player }) => {
    try {
      return toolResult(await api("POST", "/get_player_pos", { player }));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "get_block",
  "Read block id at coordinates",
  {
    world: z.string().optional().describe("World name; default from plugin config"),
    x: z.number().int(),
    y: z.number().int(),
    z: z.number().int(),
  },
  async (args) => {
    try {
      return toolResult(await api("POST", "/get_block", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "set_block",
  "Place a single block via Bukkit (admin direct write; recorded for write_undo)",
  {
    world: z.string().optional(),
    x: z.number().int(),
    y: z.number().int(),
    z: z.number().int(),
    block: z.string().describe("Block id, e.g. stone or minecraft:oak_planks"),
  },
  async (args) => {
    try {
      return toolResult(await api("POST", "/set_block", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fill",
  "Fill a cuboid with one block type (volume limited; recorded for write_undo)",
  {
    world: z.string().optional(),
    x1: z.number().int(),
    y1: z.number().int(),
    z1: z.number().int(),
    x2: z.number().int(),
    y2: z.number().int(),
    z2: z.number().int(),
    block: z.string(),
  },
  async (args) => {
    try {
      return toolResult(await api("POST", "/fill", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "set_blocks",
  "Batch place explicit blocks (recorded for write_undo)",
  {
    world: z.string().optional(),
    blocks: z
      .array(
        z.object({
          x: z.number().int(),
          y: z.number().int(),
          z: z.number().int(),
          block: z.string(),
        })
      )
      .describe("List of block placements"),
  },
  async (args) => {
    try {
      return toolResult(await api("POST", "/set_blocks", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "write_undo",
  "Undo last Bukkit direct write(s) from BuildBridge history (NOT FAWE //undo). Prefer this after set_block/fill/set_blocks.",
  { times: z.number().int().optional().describe("How many write operations to undo, default 1") },
  async (args) => {
    try {
      return toolResult(await api("POST", "/undo", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "write_redo",
  "Redo BuildBridge direct writes previously undone with write_undo",
  { times: z.number().int().optional() },
  async (args) => {
    try {
      return toolResult(await api("POST", "/redo", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "write_history",
  "Show BuildBridge direct-write undo/redo stack status",
  {},
  async () => {
    try {
      return toolResult(await api("GET", "/history"));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_pos",
  "Set FAWE selection corner pos1 or pos2 at coordinates",
  {
    which: z.enum(["pos1", "pos2"]).describe("Selection corner"),
    world: z.string().optional(),
    x: z.number().int(),
    y: z.number().int(),
    z: z.number().int(),
  },
  async (args) => {
    try {
      return toolResult(await api("POST", "/fawe_pos", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_set",
  "FAWE //set on current selection",
  { pattern: z.string().describe("WorldEdit pattern, e.g. stone") },
  async ({ pattern }) => {
    try {
      return toolResult(await api("POST", "/fawe_set", { pattern }));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_replace",
  "FAWE //replace on current selection",
  {
    from: z.string(),
    to: z.string(),
  },
  async (args) => {
    try {
      return toolResult(await api("POST", "/fawe_replace", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_copy",
  "FAWE //copy current selection to clipboard",
  {},
  async () => {
    try {
      return toolResult(await api("POST", "/fawe_copy", {}));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_paste",
  "FAWE //paste clipboard. Offset requires config actor-player.",
  {
    offset_x: z.number().int().optional(),
    offset_y: z.number().int().optional(),
    offset_z: z.number().int().optional(),
    ignore_air: z.boolean().optional(),
  },
  async (args) => {
    try {
      return toolResult(await api("POST", "/fawe_paste", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_undo",
  "FAWE //undo",
  { times: z.number().int().optional().describe("Undo count, default 1") },
  async (args) => {
    try {
      return toolResult(await api("POST", "/fawe_undo", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_redo",
  "FAWE //redo",
  { times: z.number().int().optional() },
  async (args) => {
    try {
      return toolResult(await api("POST", "/fawe_redo", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_schem_list",
  "List schematic files in FAWE schematics directory",
  {},
  async () => {
    try {
      return toolResult(await api("GET", "/fawe_schem_list"));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_schem_load",
  "FAWE //schem load <name>",
  { name: z.string().describe("Schematic file name") },
  async ({ name }) => {
    try {
      return toolResult(await api("POST", "/fawe_schem_load", { name }));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "fawe_schem_paste",
  "Paste loaded schematic at coordinates (requires actor-player online)",
  {
    world: z.string().optional(),
    x: z.number().int(),
    y: z.number().int(),
    z: z.number().int(),
    ignore_air: z.boolean().optional(),
  },
  async (args) => {
    try {
      return toolResult(await api("POST", "/fawe_schem_paste", args));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "schem_index_status",
  "Status of local Litematica reference library index (path, count, last build)",
  {},
  async () => {
    try {
      return toolResult(indexStatus());
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "schem_search",
  "Search local Litematica library by description (uses filename, folder tags, metadata). Call this BEFORE designing buildings to find visual references.",
  {
    query: z
      .string()
      .describe("Chinese or English description, e.g. 现代快餐店 玻璃幕墙 / modern restaurant"),
    limit: z.number().int().min(1).max(50).optional().describe("Max hits, default 12"),
  },
  async ({ query, limit }) => {
    try {
      return toolResult(searchSchems(query, { limit }));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "schem_info",
  "Get one indexed schematic details by relative path or file name",
  {
    path: z.string().describe("Relative path under schem root, or file name"),
  },
  async ({ path: p }) => {
    try {
      return toolResult(schemInfo(p));
    } catch (e) {
      return toolError(e);
    }
  }
);

server.tool(
  "schem_reindex",
  "Rebuild Litematica search index from MCWWS_SCHEM_ROOT (can take minutes for 10k+ files). Prefer CLI scripts/build-schem-index.mjs for full rebuilds.",
  {
    quick: z
      .boolean()
      .optional()
      .describe("If true, only index paths/filenames (no NBT metadata)"),
    max_files: z
      .number()
      .int()
      .optional()
      .describe("Limit files for a partial rebuild"),
  },
  async ({ quick, max_files }) => {
    try {
      const index = await buildIndex({
        parseMeta: !quick,
        maxFiles: max_files || Infinity,
        concurrency: 8,
      });
      return toolResult({
        ok: true,
        root: index.root || DEFAULT_SCHEM_ROOT,
        count: index.count,
        builtAt: index.builtAt,
        parsed: index.parsed,
        reused: index.reused,
        failed: index.failed,
      });
    } catch (e) {
      return toolError(e);
    }
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
