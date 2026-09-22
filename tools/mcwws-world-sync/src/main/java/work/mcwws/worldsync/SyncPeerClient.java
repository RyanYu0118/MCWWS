package work.mcwws.worldsync;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SyncPeerClient {
    private final McwwsWorldSyncPlugin plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public SyncPeerClient(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, Object> heartbeat() throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nodeId", plugin.config().nodeId);
        body.put("hasLock", plugin.lock().hasLock());
        body.put("generation", plugin.lock().generation());
        body.put("players", plugin.getServer().getOnlinePlayers().size());
        body.put("dirty", plugin.dirty().size());
        HttpRequest req = HttpIo.authed(plugin.config().peerUrl + "/v1/heartbeat", plugin.config().token)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(body)))
                .build();
        HttpResponse<String> resp = send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new IOException("heartbeat HTTP " + resp.statusCode() + " " + resp.body());
        }
        return JsonUtil.parseObject(resp.body());
    }

    public Map<String, Object> claim() throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nodeId", plugin.config().nodeId);
        return postJson("/v1/lock/claim", body, Duration.ofSeconds(30));
    }

    public Map<String, Object> release() throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nodeId", plugin.config().nodeId);
        return postJson("/v1/lock/release", body, Duration.ofSeconds(30));
    }

    public Map<String, Object> requestHandover() throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", plugin.config().nodeId);
        return postJson("/v1/handover", body, Duration.ofSeconds(30));
    }

    public Map<String, Object> status() throws IOException, InterruptedException {
        HttpRequest req = HttpIo.authed(plugin.config().peerUrl + "/v1/status", plugin.config().token)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<String> resp = send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new IOException("status HTTP " + resp.statusCode());
        }
        return JsonUtil.parseObject(resp.body());
    }

    public void putFile(String rel, Path file) throws IOException, InterruptedException {
        byte[] data = Files.readAllBytes(file);
        String q = URLEncoder.encode(rel, StandardCharsets.UTF_8);
        HttpRequest req = HttpIo.authed(plugin.config().peerUrl + "/v1/file?path=" + q, plugin.config().token)
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
        HttpResponse<String> resp = send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new IOException("put " + rel + " HTTP " + resp.statusCode() + " " + resp.body());
        }
    }

    public List<String> listOutbox() throws IOException, InterruptedException {
        HttpRequest req = HttpIo.authed(plugin.config().peerUrl + "/v1/outbox", plugin.config().token)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new IOException("outbox HTTP " + resp.statusCode());
        }
        Map<String, Object> obj = JsonUtil.parseObject(resp.body());
        return JsonUtil.strList(obj, "files");
    }

    public void pullOutboxFile(String rel) throws IOException, InterruptedException {
        String q = URLEncoder.encode(rel, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(plugin.config().peerUrl + "/v1/outbox/file?path=" + q))
                .header("Authorization", "Bearer " + plugin.config().token)
                .GET()
                .timeout(Duration.ofMinutes(5))
                .build();
        HttpResponse<InputStream> resp = send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() >= 400) {
            resp.body().close();
            throw new IOException("pull " + rel + " HTTP " + resp.statusCode());
        }
        try (InputStream in = resp.body()) {
            plugin.staging().writeStaging(rel, in, -1);
        }
    }

    /**
     * @return false when the listen node has no zip bundle endpoint
     */
    public boolean pullBundle() throws IOException, InterruptedException {
        HttpRequest req = HttpIo.authed(plugin.config().peerUrl + "/v1/outbox/bundle", plugin.config().token)
                .GET()
                .timeout(Duration.ofMinutes(30))
                .build();
        HttpResponse<InputStream> resp = send(req, HttpResponse.BodyHandlers.ofInputStream());
        int code = resp.statusCode();
        if (code == 404 || code == 405) {
            resp.body().close();
            return false;
        }
        if (code >= 400) {
            String err = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("bundle HTTP " + code + " " + err);
        }
        long len = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
        int total = len > 0L && len <= Integer.MAX_VALUE ? (int) len : 1;
        Path tmp = plugin.getDataFolder().toPath().resolve("incoming-bundle.zip");
        Files.createDirectories(tmp.getParent());
        plugin.progress().begin("下载压缩包", total);
        long got = 0L;
        try (InputStream in = resp.body(); java.io.OutputStream out = Files.newOutputStream(tmp)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
                got += n;
                int shown = len > 0L ? (int) Math.min(got, total) : 1;
                plugin.progress().tick(plugin, shown, (got / 1024) + " KB");
            }
        }
        plugin.getLogger().info("压缩包已下载 " + (got / 1024) + " KB，正在解压");
        unzipBundle(tmp);
        Files.deleteIfExists(tmp);
        ackBundle();
        plugin.progress().end(plugin);
        return true;
    }

    private void unzipBundle(Path zip) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!PathPolicy.allowed(name, plugin.config().prefixes, plugin.config().skipGlobs)) {
                    plugin.getLogger().warning("跳过压缩包条目 " + name);
                    continue;
                }
                plugin.staging().writeStaging(name, zin, -1);
                zin.closeEntry();
            }
        }
    }

    private void ackBundle() {
        try {
            HttpRequest req = HttpIo.authed(plugin.config().peerUrl + "/v1/outbox/bundle/ack", plugin.config().token)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400 && resp.statusCode() != 404 && resp.statusCode() != 405) {
                plugin.getLogger().warning("确认压缩包失败 HTTP " + resp.statusCode());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("确认压缩包失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    /** Tell the listen node this file arrived, so a later handover skips it. Old peers may 404; that is fine. */
    public void ackOutbox(String rel) {
        try {
            String q = URLEncoder.encode(rel, StandardCharsets.UTF_8);
            HttpRequest req = HttpIo.authed(plugin.config().peerUrl + "/v1/outbox/ack?path=" + q, plugin.config().token)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400 && resp.statusCode() != 404 && resp.statusCode() != 405) {
                plugin.getLogger().warning("确认已拉取失败 " + rel + " HTTP " + resp.statusCode());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("确认已拉取失败 " + rel + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private <T> HttpResponse<T> send(HttpRequest req, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                HttpResponse<T> resp = http.send(req, handler);
                int code = resp.statusCode();
                if ((code == 502 || code == 503 || code == 504) && attempt < 4) {
                    if (resp.body() instanceof InputStream in) {
                        in.close();
                    }
                    plugin.getLogger().warning("同步网关 " + code + "，第 " + attempt + " 次重试");
                    Thread.sleep(1000L * attempt);
                    continue;
                }
                return resp;
            } catch (IOException e) {
                last = e;
                if (attempt == 4 || !retryable(e)) {
                    throw e;
                }
                plugin.getLogger().warning("传输中断，第 " + attempt + " 次重试: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                Thread.sleep(1000L * attempt);
            }
        }
        throw last == null ? new IOException("send failed") : last;
    }

    private static boolean retryable(IOException e) {
        String all = e.toString();
        String msg = e.getMessage();
        if (msg != null) {
            all = all + " " + msg;
        }
        return all.contains("RST_STREAM")
                || all.contains("GOAWAY")
                || all.contains("connection reset")
                || all.contains("Connection reset")
                || all.contains("EOF")
                || all.contains("timed out")
                || all.contains("header parser")
                || all.contains("Broken pipe");
    }

    private Map<String, Object> postJson(String path, Map<String, Object> body, Duration timeout) throws IOException, InterruptedException {
        HttpRequest req = HttpIo.authed(plugin.config().peerUrl + path, plugin.config().token)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(body)))
                .build();
        HttpResponse<String> resp = send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Map<String, Object> parsed;
        try {
            parsed = JsonUtil.parseObject(resp.body());
        } catch (Exception e) {
            throw new IOException("HTTP " + resp.statusCode() + " " + resp.body(), e);
        }
        if (resp.statusCode() >= 400) {
            throw new IOException(JsonUtil.str(parsed, "error", "HTTP " + resp.statusCode()));
        }
        return parsed;
    }
}
