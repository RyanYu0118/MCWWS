package work.mcwws.worldsync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lock + dirty files via object storage. Standby only writes its staging directory. */
public final class S3Relay {
    private final McwwsWorldSyncPlugin plugin;
    private final OssClient oss;
    private final String lockKey;
    private final String manifestKey;
    private final String dataPrefix;
    private final Path seenFile;
    private final Object gate = new Object();
    private volatile boolean handoverTriggered;
    private volatile long lastWarn;

    public S3Relay(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
        SyncConfig cfg = plugin.config();
        this.oss = new OssClient(cfg.s3Endpoint, cfg.s3Bucket, cfg.s3AccessKey, cfg.s3SecretKey, cfg.s3VirtualHost);
        String prefix = cfg.s3Prefix;
        this.lockKey = prefix + "lock.json";
        this.manifestKey = prefix + "manifest.json";
        this.dataPrefix = prefix + "data/";
        this.seenFile = plugin.getDataFolder().toPath().resolve("cloud-seen.json");
    }

    public void poll() {
        try {
            Map<String, Object> lock = readLock();
            applyLock(lock);
            String handoverTo = JsonUtil.str(lock, "handoverTo", "");
            if (plugin.lock().hasLock()) {
                refreshIfNeeded(lock);
                if (!handoverTo.isEmpty() && !handoverTo.equals(plugin.config().nodeId) && !handoverTriggered) {
                    handoverTriggered = true;
                    String from = handoverTo;
                    plugin.getServer().getScheduler().runTask(plugin, () -> plugin.handover().onRemoteRequest(from));
                }
            } else {
                handoverTriggered = false;
                pullChanged();
            }
            plugin.markPeerOnline(true);
        } catch (Exception e) {
            plugin.markPeerOnline(false);
            long now = System.currentTimeMillis();
            if (now - lastWarn > 120_000L) {
                lastWarn = now;
                plugin.getLogger().warning("对象存储同步失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
    }

    public void claim() throws IOException, InterruptedException {
        synchronized (gate) {
            Map<String, Object> lock = readLock();
            String holder = JsonUtil.str(lock, "holder", "");
            long lease = JsonUtil.lng(lock, "leaseUntil", 0);
            if (!holder.isEmpty() && !holder.equals(plugin.config().nodeId) && lease > System.currentTimeMillis()) {
                throw new IOException("云端锁由 " + holder + " 持有");
            }
            writeLock(plugin.config().nodeId, JsonUtil.lng(lock, "generation", 0) + 1, "", false);
        }
    }

    public void force() throws IOException, InterruptedException {
        synchronized (gate) {
            Map<String, Object> lock = readLock();
            writeLock(plugin.config().nodeId, JsonUtil.lng(lock, "generation", 0) + 1, "", false);
        }
    }

    public void release() throws IOException, InterruptedException {
        synchronized (gate) {
            Map<String, Object> lock = readLock();
            if (!plugin.config().nodeId.equals(JsonUtil.str(lock, "holder", ""))) {
                return;
            }
            writeLock("", JsonUtil.lng(lock, "generation", 0), "", false);
        }
        handoverTriggered = false;
    }

    public void requestHandover() throws IOException, InterruptedException {
        synchronized (gate) {
            Map<String, Object> lock = readLock();
            String holder = JsonUtil.str(lock, "holder", "");
            if (holder.isEmpty()) {
                throw new IOException("云端还没有持锁节点，可直接 /worldsync claim");
            }
            if (holder.equals(plugin.config().nodeId)) {
                throw new IOException("本节点已持锁");
            }
            lock.put("handoverTo", plugin.config().nodeId);
            putJson(lockKey, lock);
        }
    }

    public void putFile(String rel, Path file) throws IOException, InterruptedException {
        String key = dataPrefix + PathPolicy.posix(rel);
        oss.putFile(key, file);
        synchronized (gate) {
            Map<String, Object> manifest = readManifest();
            List<Map<String, Object>> files = fileEntries(manifest);
            long mtime = Files.getLastModifiedTime(file).toMillis();
            long size = Files.size(file);
            boolean found = false;
            for (Map<String, Object> entry : files) {
                if (rel.equals(JsonUtil.str(entry, "path", ""))) {
                    entry.put("mtime", mtime);
                    entry.put("size", size);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("path", rel);
                entry.put("mtime", mtime);
                entry.put("size", size);
                files.add(entry);
            }
            manifest.put("files", files);
            manifest.put("holder", plugin.config().nodeId);
            putJson(manifestKey, manifest);
        }
    }

    public int pullChanged() throws IOException, InterruptedException {
        Map<String, Object> seen = readSeen();
        Map<String, Object> manifest = readManifest();
        int n = 0;
        for (Map<String, Object> entry : fileEntries(manifest)) {
            String rel = JsonUtil.str(entry, "path", "");
            if (!PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
                continue;
            }
            long remoteMtime = JsonUtil.lng(entry, "mtime", 0);
            long localMtime = JsonUtil.lng(seen, rel, 0);
            if (remoteMtime <= localMtime) {
                continue;
            }
            Path dest = PathPolicy.resolveUnder(plugin.staging().stagingRoot(), rel);
            if (!oss.getToFile(dataPrefix + rel, dest)) {
                continue;
            }
            seen.put(rel, remoteMtime);
            n++;
        }
        if (n > 0) {
            writeSeen(seen);
            plugin.getLogger().info("已从对象存储拉下 " + n + " 个文件到 staging");
        }
        return n;
    }

    private void refreshIfNeeded(Map<String, Object> lock) throws IOException, InterruptedException {
        long lease = JsonUtil.lng(lock, "leaseUntil", 0);
        long left = lease - System.currentTimeMillis();
        if (left > plugin.config().leaseSeconds * 500L) {
            return;
        }
        synchronized (gate) {
            writeLock(plugin.config().nodeId, JsonUtil.lng(lock, "generation", plugin.lock().generation()),
                    JsonUtil.str(lock, "handoverTo", ""), false);
        }
    }

    private void applyLock(Map<String, Object> lock) {
        String holder = JsonUtil.str(lock, "holder", "");
        long lease = JsonUtil.lng(lock, "leaseUntil", 0);
        long gen = JsonUtil.lng(lock, "generation", 0);
        boolean expired = !holder.isEmpty() && lease > 0 && lease < System.currentTimeMillis();
        if (expired) {
            plugin.lock().setHolder("", gen, 0, true);
            plugin.lock().setJoiningBlocked(true);
            return;
        }
        plugin.lock().setHolder(holder, gen, lease, false);
        plugin.lock().setJoiningBlocked(!plugin.lock().hasLock());
        plugin.lock().setHandover(!JsonUtil.str(lock, "handoverTo", "").isEmpty(), JsonUtil.str(lock, "handoverTo", ""));
    }

    private void writeLock(String holder, long generation, String handoverTo, boolean frozen) throws IOException, InterruptedException {
        long lease = holder.isEmpty() ? 0 : System.currentTimeMillis() + plugin.config().leaseSeconds * 1000L;
        Map<String, Object> lock = new LinkedHashMap<>();
        lock.put("holder", holder);
        lock.put("generation", generation);
        lock.put("leaseUntil", lease);
        lock.put("handoverTo", handoverTo == null ? "" : handoverTo);
        lock.put("frozen", frozen);
        putJson(lockKey, lock);
        applyLock(lock);
    }

    private Map<String, Object> readLock() throws IOException, InterruptedException {
        byte[] raw = oss.getBytes(lockKey);
        if (raw == null || raw.length == 0) {
            return new LinkedHashMap<>();
        }
        return JsonUtil.parseObject(new String(raw, StandardCharsets.UTF_8));
    }

    private Map<String, Object> readManifest() throws IOException, InterruptedException {
        byte[] raw = oss.getBytes(manifestKey);
        if (raw == null || raw.length == 0) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("files", List.of());
            return empty;
        }
        return JsonUtil.parseObject(new String(raw, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fileEntries(Map<String, Object> manifest) {
        Object v = manifest.get("files");
        List<Map<String, Object>> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    out.add((Map<String, Object>) map);
                }
            }
        }
        return out;
    }

    private void putJson(String key, Map<String, Object> body) throws IOException, InterruptedException {
        byte[] bytes = JsonUtil.stringify(body).getBytes(StandardCharsets.UTF_8);
        oss.putBytes(key, bytes, "application/json");
    }

    private Map<String, Object> readSeen() {
        try {
            if (!Files.isRegularFile(seenFile)) {
                return new LinkedHashMap<>();
            }
            return JsonUtil.parseObject(Files.readString(seenFile));
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private void writeSeen(Map<String, Object> seen) throws IOException {
        Files.createDirectories(seenFile.getParent());
        Files.writeString(seenFile, JsonUtil.stringify(seen));
    }
}
