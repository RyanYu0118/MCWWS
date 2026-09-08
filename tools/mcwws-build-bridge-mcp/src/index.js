#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const BASE_URL = (process.env.MCWWS_BUILD_URL || "http://127.0.0.1:8765").replace(/\/$/, "");
const TOKEN = process.env.MCWWS_BUILD_TOKEN || "";

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
  const res = await fetch(`${BASE_URL}${path}`, init);
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
  "Place a single block via Bukkit (admin direct write)",
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
  "Fill a cuboid with one block type (volume limited by plugin)",
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
  "Batch place explicit blocks",
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

const transport = new StdioServerTransport();
await server.connect(transport);
