package work.mcwws.worldsync;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/** Coordinator HTTP API on the listen node. */
public final class SyncListenServer {
    private final McwwsWorldSyncPlugin plugin;
    private HttpServer server;
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();

    public SyncListenServer(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() throws IOException {
        SyncConfig cfg = plugin.config();
        server = HttpServer.create(new InetSocketAddress(cfg.listenHost, cfg.listenPort), 0);
        server.createContext("/v1/status", this::status);
        server.createContext("/v1/heartbeat", this::heartbeat);
        server.createContext("/v1/lock/claim", this::claim);
        server.createContext("/v1/lock/release", this::release);
        server.createContext("/v1/handover", this::handover);
        server.createContext("/v1/file", this::putFile);
        server.createContext("/v1/outbox", this::outboxList);
        server.createContext("/v1/outbox/file", this::outboxFile);
        server.createContext("/v1/outbox/ack", this::outboxAck);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "MCWWS-WorldSync-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        plugin.getLogger().info("WorldSync 监听 " + cfg.listenHost + ":" + cfg.listenPort);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private boolean gate(HttpExchange ex) throws IOException {
        if (!HttpIo.authorize(ex, plugin.config().token)) {
            HttpIo.json(ex, 401, HttpIo.err("unauthorized"));
            return false;
        }
        return true;
    }

    private void status(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        HttpIo.json(ex, 200, plugin.statusMap());
    }

    private void heartbeat(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        Map<String, Object> body = JsonUtil.parseObject(HttpIo.readUtf8(ex));
        String node = JsonUtil.str(body, "nodeId", "");
        lastSeen.put(node, System.currentTimeMillis());
        plugin.lock().expireIfNeeded();
        if (node.equals(plugin.lock().holder()) || plugin.lock().hasLock()) {
            plugin.lock().refreshLease(plugin.config().leaseSeconds * 1000L);
        }
        Map<String, Object> resp = plugin.statusMap();
        resp.put("handoverPending", plugin.lock().handoverPending());
        resp.put("handoverFrom", plugin.lock().handoverFrom());
        HttpIo.json(ex, 200, resp);
    }

    private void claim(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        Map<String, Object> body = JsonUtil.parseObject(HttpIo.readUtf8(ex));
        String node = JsonUtil.str(body, "nodeId", "");
        if (node.isEmpty()) {
            HttpIo.json(ex, 400, HttpIo.err("nodeId required"));
            return;
        }
        plugin.lock().expireIfNeeded();
        String holder = plugin.lock().holder();
        boolean leaseValid = !holder.isEmpty() && System.currentTimeMillis() < plugin.lock().leaseUntil();
        if (leaseValid && !holder.equals(node)) {
            HttpIo.json(ex, 409, HttpIo.err("held by " + holder));
            return;
        }
        plugin.lock().setHolder(node, plugin.lock().generation(), System.currentTimeMillis() + plugin.config().leaseSeconds * 1000L, false);
        plugin.lock().setJoiningBlocked(!plugin.config().nodeId.equals(node));
        HttpIo.json(ex, 200, plugin.statusMap());
    }

    private void release(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        Map<String, Object> body = JsonUtil.parseObject(HttpIo.readUtf8(ex));
        String node = JsonUtil.str(body, "nodeId", "");
        if (!node.isEmpty() && !node.equals(plugin.lock().holder()) && !node.equals(plugin.config().nodeId)) {
            HttpIo.json(ex, 409, HttpIo.err("not holder"));
            return;
        }
        plugin.lock().setHolder("", plugin.lock().generation(), 0, false);
        plugin.lock().setJoiningBlocked(true);
        plugin.lock().setHandover(false, "");
        HttpIo.json(ex, 200, plugin.statusMap());
    }

    private void handover(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        Map<String, Object> body = JsonUtil.parseObject(HttpIo.readUtf8(ex));
        String from = JsonUtil.str(body, "from", plugin.config().nodeId);
        plugin.lock().setHandover(true, from);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.handover().onRemoteRequest(from));
        HttpIo.json(ex, 200, HttpIo.ok("queued", true, "from", from));
    }

    private void putFile(HttpExchange ex) throws IOException {
        if (!"PUT".equals(ex.getRequestMethod()) && !"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        String rel = HttpIo.query(ex, "path");
        if (rel == null || !PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
            HttpIo.json(ex, 400, HttpIo.err("path not allowed"));
            return;
        }
        long len = -1;
        try {
            len = Long.parseLong(HttpIo.header(ex.getRequestHeaders(), "Content-Length"));
        } catch (Exception ignored) {
        }
        try (InputStream in = ex.getRequestBody()) {
            plugin.staging().writeStaging(rel, in, len);
        }
        plugin.outbox().forget(rel);
        HttpIo.json(ex, 200, HttpIo.ok("path", rel));
    }

    private void outboxList(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        List<String> files = plugin.outbox().list();
        Map<String, Object> m = HttpIo.ok("files", files, "count", files.size());
        HttpIo.json(ex, 200, m);
    }

    private void outboxFile(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        String rel = HttpIo.query(ex, "path");
        if (rel == null || !PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
            HttpIo.json(ex, 400, HttpIo.err("path not allowed"));
            return;
        }
        Path file = plugin.outbox().resolve(rel);
        if (!Files.isRegularFile(file)) {
            HttpIo.json(ex, 404, HttpIo.err("missing"));
            return;
        }
        byte[] data = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", "application/octet-stream");
        ex.getResponseHeaders().set("X-Rel-Path", rel);
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(data);
        }
    }

    private void outboxAck(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        String rel = HttpIo.query(ex, "path");
        if (rel == null || !PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
            HttpIo.json(ex, 400, HttpIo.err("path not allowed"));
            return;
        }
        plugin.outbox().forget(rel);
        HttpIo.json(ex, 200, HttpIo.ok("path", rel));
    }

    public List<String> knownPeers() {
        return new ArrayList<>(lastSeen.keySet());
    }
}
