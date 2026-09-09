package work.mcwws.buildbridge;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildApiHandler {

    private final PlayerQueryService players;
    private final BlockWriteService blocks;
    private final FaweBridgeService fawe;

    public BuildApiHandler(PlayerQueryService players, BlockWriteService blocks, FaweBridgeService fawe) {
        this.players = players;
        this.blocks = blocks;
        this.fawe = fawe;
    }

    public void handle(String method, String path, String body, HttpExchange exchange) throws IOException {
        String normalized = path == null ? "/" : path;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        try {
            if ("GET".equalsIgnoreCase(method) && "/health".equals(normalized)) {
                Map<String, Object> health = new LinkedHashMap<>();
                health.put("ok", true);
                health.put("service", "MCWWS_BuildBridge");
                health.put("fawe", fawe.isAvailable());
                health.put("history", blocks.historyStatus());
                ok(exchange, health);
                return;
            }

            Map<String, Object> req = body == null || body.isBlank()
                    ? Map.of()
                    : JsonUtil.asObject(JsonUtil.parse(body));

            if ("GET".equalsIgnoreCase(method) && "/list_players".equals(normalized)) {
                ok(exchange, players.listPlayers());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/list_players".equals(normalized)) {
                ok(exchange, players.listPlayers());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/get_player_pos".equals(normalized)) {
                String name = JsonUtil.str(req, "player", JsonUtil.str(req, "name", null));
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("player required");
                }
                ok(exchange, players.getPlayerPos(name));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/get_block".equals(normalized)) {
                ok(exchange, blocks.getBlock(
                        JsonUtil.str(req, "world"),
                        JsonUtil.i(req, "x"),
                        JsonUtil.i(req, "y"),
                        JsonUtil.i(req, "z")
                ));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/set_block".equals(normalized)) {
                ok(exchange, blocks.setBlock(
                        JsonUtil.str(req, "world"),
                        JsonUtil.i(req, "x"),
                        JsonUtil.i(req, "y"),
                        JsonUtil.i(req, "z"),
                        JsonUtil.str(req, "block")
                ));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fill".equals(normalized)) {
                ok(exchange, blocks.fill(
                        JsonUtil.str(req, "world"),
                        JsonUtil.i(req, "x1"),
                        JsonUtil.i(req, "y1"),
                        JsonUtil.i(req, "z1"),
                        JsonUtil.i(req, "x2"),
                        JsonUtil.i(req, "y2"),
                        JsonUtil.i(req, "z2"),
                        JsonUtil.str(req, "block")
                ));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/set_blocks".equals(normalized)) {
                List<Object> list = JsonUtil.asArray(req.get("blocks"));
                ok(exchange, blocks.setBlocks(JsonUtil.str(req, "world"), list));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/undo".equals(normalized)) {
                Integer times = JsonUtil.iOrNull(req, "times");
                ok(exchange, blocks.undo(times == null ? 1 : times));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/redo".equals(normalized)) {
                Integer times = JsonUtil.iOrNull(req, "times");
                ok(exchange, blocks.redo(times == null ? 1 : times));
                return;
            }
            if ("GET".equalsIgnoreCase(method) && "/history".equals(normalized)) {
                ok(exchange, blocks.historyStatus());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/history".equals(normalized)) {
                ok(exchange, blocks.historyStatus());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_status".equals(normalized)) {
                ok(exchange, fawe.requireFawe());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_pos".equals(normalized)) {
                ok(exchange, fawe.pos(
                        JsonUtil.str(req, "which", "pos1"),
                        JsonUtil.str(req, "world"),
                        JsonUtil.i(req, "x"),
                        JsonUtil.i(req, "y"),
                        JsonUtil.i(req, "z")
                ));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_set".equals(normalized)) {
                ok(exchange, fawe.set(JsonUtil.str(req, "pattern", JsonUtil.str(req, "block", null))));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_replace".equals(normalized)) {
                ok(exchange, fawe.replace(JsonUtil.str(req, "from"), JsonUtil.str(req, "to")));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_copy".equals(normalized)) {
                ok(exchange, fawe.copy());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_paste".equals(normalized)) {
                ok(exchange, fawe.paste(
                        JsonUtil.iOrNull(req, "offset_x"),
                        JsonUtil.iOrNull(req, "offset_y"),
                        JsonUtil.iOrNull(req, "offset_z"),
                        JsonUtil.bool(req, "ignore_air", false)
                ));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_undo".equals(normalized)) {
                Integer times = JsonUtil.iOrNull(req, "times");
                ok(exchange, fawe.undo(times == null ? 1 : times));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_redo".equals(normalized)) {
                Integer times = JsonUtil.iOrNull(req, "times");
                ok(exchange, fawe.redo(times == null ? 1 : times));
                return;
            }
            if ("GET".equalsIgnoreCase(method) && "/fawe_schem_list".equals(normalized)) {
                ok(exchange, fawe.schemList());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_schem_list".equals(normalized)) {
                ok(exchange, fawe.schemList());
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_schem_load".equals(normalized)) {
                ok(exchange, fawe.schemLoad(JsonUtil.str(req, "name", JsonUtil.str(req, "schematic", null))));
                return;
            }
            if ("POST".equalsIgnoreCase(method) && "/fawe_schem_paste".equals(normalized)) {
                ok(exchange, fawe.schemPaste(
                        JsonUtil.str(req, "world"),
                        JsonUtil.i(req, "x"),
                        JsonUtil.i(req, "y"),
                        JsonUtil.i(req, "z"),
                        JsonUtil.bool(req, "ignore_air", false)
                ));
                return;
            }

            fail(exchange, 404, "Unknown route: " + method + " " + normalized);
        } catch (IllegalArgumentException | IllegalStateException e) {
            fail(exchange, 400, e.getMessage());
        }
    }

    private static void ok(HttpExchange exchange, Map<String, Object> body) throws IOException {
        LocalHttpServer.writeJson(exchange, 200, JsonUtil.stringify(body));
    }

    private static void fail(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", message == null ? "error" : message);
        LocalHttpServer.writeJson(exchange, status, JsonUtil.stringify(body));
    }
}
