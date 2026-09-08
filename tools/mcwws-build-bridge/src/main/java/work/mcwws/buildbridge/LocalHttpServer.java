package work.mcwws.buildbridge;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class LocalHttpServer {

    private final McwwsBuildBridgePlugin plugin;
    private final BuildApiHandler handler;
    private HttpServer server;
    private ExecutorService executor;

    public LocalHttpServer(McwwsBuildBridgePlugin plugin, BuildApiHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }

    public synchronized void start() throws IOException {
        stop();
        String host = plugin.getConfig().getString("http.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("http.port", 8765);
        InetAddress address = InetAddress.getByName(host);
        if (!address.isLoopbackAddress()) {
            throw new IOException("Refusing to bind non-loopback host: " + host);
        }
        server = HttpServer.create(new InetSocketAddress(address, port), 0);
        server.createContext("/", this::dispatch);
        executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "MCWWS-BuildBridge-HTTP");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);
        server.start();
        plugin.getLogger().info("BuildBridge HTTP on " + host + ":" + port);
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        try {
            InetAddress remote = exchange.getRemoteAddress().getAddress();
            if (remote != null && !remote.isLoopbackAddress()) {
                writeJson(exchange, 403, "{\"ok\":false,\"error\":\"Loopback only\"}");
                return;
            }
            if (!authorize(exchange)) {
                writeJson(exchange, 401, "{\"ok\":false,\"error\":\"Unauthorized\"}");
                return;
            }
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().add("Allow", "GET, POST, OPTIONS");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            String body = readBody(exchange);
            handler.handle(method, path, body, exchange);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "BuildBridge HTTP error", e);
            writeJson(exchange, 500, JsonUtil.stringify(java.util.Map.of(
                    "ok", false,
                    "error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()
            )));
        }
    }

    private boolean authorize(HttpExchange exchange) {
        String token = plugin.getHttpToken();
        if (token == null || token.isBlank()) {
            return false;
        }
        Headers headers = exchange.getRequestHeaders();
        String auth = headers.getFirst("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token.equals(auth.substring(7).trim());
        }
        String headerToken = headers.getFirst("X-MCWWS-Token");
        return token.equals(headerToken);
    }

    static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
        exchange.close();
    }
}
