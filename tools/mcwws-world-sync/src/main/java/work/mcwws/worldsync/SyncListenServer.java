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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        server.createContext("/v1/outbox/bundle", this::outboxBundle);
        server.createContext("/v1/outbox/bundle/ack", this::outboxBundleAck);
        server.createContext("/v1/push/compare", this::pushCompare);
        server.createContext("/v1/push/bundle", this::pushBundle);
        server.createContext("/v1/push/apply", this::pushApply);
        server.createContext("/v1/push/done", this::pushDone);
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

    private void outboxBundle(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        List<String> files = new ArrayList<>();
        for (String rel : plugin.outbox().list()) {
            if (PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
                files.add(rel);
            }
        }
        Path dir = plugin.getDataFolder().toPath();
        Path zip = dir.resolve("outbox-bundle.zip");
        Path manifest = dir.resolve("outbox-bundle.txt");
        plugin.getLogger().info("正在打包 " + files.size() + " 个文件（已跳过日志）");
        long written = writeBundle(zip, files);
        Files.writeString(manifest, String.join("\n", files), StandardCharsets.UTF_8);
        plugin.getLogger().info("压缩包 " + (written / 1024) + " KB，开始发送");
        ex.getResponseHeaders().set("Content-Type", "application/zip");
        ex.sendResponseHeaders(200, written);
        try (OutputStream out = ex.getResponseBody()) {
            Files.copy(zip, out);
        }
    }

    private long writeBundle(Path zip, List<String> files) throws IOException {
        Files.createDirectories(zip.getParent());
        try (ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(zip))) {
            byte[] buf = new byte[65536];
            for (String rel : files) {
                Path src = plugin.outbox().resolve(rel);
                if (!Files.isRegularFile(src)) {
                    continue;
                }
                zout.putNextEntry(new ZipEntry(rel));
                try (InputStream in = Files.newInputStream(src)) {
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        zout.write(buf, 0, n);
                    }
                }
                zout.closeEntry();
            }
        }
        return Files.size(zip);
    }

    private void outboxBundleAck(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        Path manifest = plugin.getDataFolder().toPath().resolve("outbox-bundle.txt");
        if (!Files.isRegularFile(manifest)) {
            HttpIo.json(ex, 404, HttpIo.err("no bundle"));
            return;
        }
        int n = 0;
        for (String rel : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            if (rel.isBlank()) {
                continue;
            }
            plugin.outbox().forget(rel.trim());
            n++;
        }
        Files.deleteIfExists(manifest);
        Files.deleteIfExists(plugin.getDataFolder().toPath().resolve("outbox-bundle.zip"));
        HttpIo.json(ex, 200, HttpIo.ok("forgotten", n));
    }

    private void pushCompare(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        Map<String, Object> body = JsonUtil.parseObject(HttpIo.readUtf8(ex));
        String winner = JsonUtil.str(body, "winner", "");
        Map<String, String> files = new java.util.LinkedHashMap<>();
        Object raw = body.get("files");
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    files.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
        }
        HttpIo.json(ex, 200, plugin.forcePush().compare(winner, files));
    }

    private void pushBundle(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        Path zip = plugin.getDataFolder().toPath().resolve("push-bundle.zip");
        plugin.forcePush().writeDifferZip(zip);
        long len = Files.size(zip);
        ex.getResponseHeaders().set("Content-Type", "application/zip");
        ex.sendResponseHeaders(200, len);
        try (OutputStream out = ex.getResponseBody()) {
            Files.copy(zip, out);
        }
        Files.deleteIfExists(zip);
    }

    private void pushApply(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        try (InputStream in = ex.getRequestBody()) {
            plugin.forcePush().acceptZip(in);
        }
        HttpIo.json(ex, 200, HttpIo.ok("applied", true));
    }

    private void pushDone(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            HttpIo.text(ex, 405, "method");
            return;
        }
        if (!gate(ex)) {
            return;
        }
        plugin.forcePush().clearArmed();
        HttpIo.json(ex, 200, HttpIo.ok("cleared", true));
    }

    public List<String> knownPeers() {
        return new ArrayList<>(lastSeen.keySet());
    }
}
