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

public final class SyncPeerClient {
    private final McwwsWorldSyncPlugin plugin;
    private final HttpClient http = HttpClient.newBuilder()
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
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new IOException("put " + rel + " HTTP " + resp.statusCode() + " " + resp.body());
        }
    }

    public List<String> listOutbox() throws IOException, InterruptedException {
        HttpRequest req = HttpIo.authed(plugin.config().peerUrl + "/v1/outbox", plugin.config().token)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
        HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() >= 400) {
            throw new IOException("pull " + rel + " HTTP " + resp.statusCode());
        }
        try (InputStream in = resp.body()) {
            plugin.staging().writeStaging(rel, in, -1);
        }
    }

    private Map<String, Object> postJson(String path, Map<String, Object> body, Duration timeout) throws IOException, InterruptedException {
        HttpRequest req = HttpIo.authed(plugin.config().peerUrl + path, plugin.config().token)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(body)))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
