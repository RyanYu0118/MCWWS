package work.mcwws.worldsync;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SyncConfig {
    public final String nodeId;
    public final String mode;
    public final String token;
    public final String listenHost;
    public final int listenPort;
    public final String peerUrl;
    public final boolean claimOnEnable;
    public final int leaseSeconds;
    public final int heartbeatSeconds;
    public final int flushSeconds;
    public final String denyMessage;
    public final boolean allowBypassPermission;
    public final String kickMessage;
    public final String restartMode;
    public final int shutdownDelayTicks;
    public final List<String> processCommand;
    public final boolean httpRestartEnabled;
    public final String httpRestartMethod;
    public final String httpRestartUrl;
    public final Map<String, String> httpRestartHeaders;
    public final String httpRestartBody;
    public final List<String> prefixes;
    public final List<String> skipGlobs;
    public final boolean debug;
    public final String transport;
    public final String s3Endpoint;
    public final String s3Bucket;
    public final String s3AccessKey;
    public final String s3SecretKey;
    public final String s3Prefix;
    public final boolean s3VirtualHost;

    public SyncConfig(FileConfiguration cfg) {
        nodeId = cfg.getString("node-id", "local").trim();
        mode = cfg.getString("mode", "connect").trim().toLowerCase(Locale.ROOT);
        token = cfg.getString("token", "");
        listenHost = cfg.getString("listen.host", "0.0.0.0");
        listenPort = cfg.getInt("listen.port", 8766);
        String url = cfg.getString("peer-url", "http://127.0.0.1:8766");
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        peerUrl = url;
        claimOnEnable = cfg.getBoolean("claim-on-enable", false);
        leaseSeconds = Math.max(10, cfg.getInt("lease-seconds", 45));
        heartbeatSeconds = Math.max(2, cfg.getInt("heartbeat-seconds", 4));
        flushSeconds = Math.max(5, cfg.getInt("flush-seconds", 15));
        denyMessage = cfg.getString("join.deny-message", "世界正在另一端运行。");
        allowBypassPermission = cfg.getBoolean("join.allow-bypass-permission", false);
        kickMessage = cfg.getString("handover.kick-message", "世界正在切换到另一端。");
        restartMode = cfg.getString("handover.restart-mode", "stop").trim().toLowerCase(Locale.ROOT);
        shutdownDelayTicks = Math.max(1, cfg.getInt("handover.shutdown-delay-ticks", 40));
        processCommand = new ArrayList<>(cfg.getStringList("handover.process.command"));
        httpRestartEnabled = cfg.getBoolean("handover.http.enabled", false);
        httpRestartMethod = cfg.getString("handover.http.method", "POST");
        httpRestartUrl = cfg.getString("handover.http.url", "");
        httpRestartHeaders = new LinkedHashMap<>();
        var headerSec = cfg.getConfigurationSection("handover.http.headers");
        if (headerSec != null) {
            for (String k : headerSec.getKeys(false)) {
                httpRestartHeaders.put(k, String.valueOf(headerSec.get(k)));
            }
        }
        httpRestartBody = cfg.getString("handover.http.body", "");
        prefixes = normalizePrefixes(cfg.getStringList("sync-prefixes"));
        skipGlobs = new ArrayList<>(cfg.getStringList("skip-globs"));
        debug = cfg.getBoolean("debug", false);
        transport = cfg.getString("transport", "direct").trim().toLowerCase(Locale.ROOT);
        s3Endpoint = trimSlash(cfg.getString("s3.endpoint", "https://oss-cn-hangzhou.aliyuncs.com"));
        s3Bucket = cfg.getString("s3.bucket", "").trim();
        s3AccessKey = cfg.getString("s3.access-key", "").trim();
        s3SecretKey = cfg.getString("s3.secret-key", "").trim();
        String prefix = PathPolicy.posix(cfg.getString("s3.prefix", "mcwws-worldsync/"));
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        s3Prefix = prefix;
        s3VirtualHost = !"path".equalsIgnoreCase(cfg.getString("s3.addressing", "virtual"));
    }

    public boolean s3Mode() {
        return "s3".equals(transport);
    }

    public boolean s3Ready() {
        return !s3Bucket.isEmpty() && !s3AccessKey.isEmpty() && !s3SecretKey.isEmpty() && !s3Endpoint.isEmpty();
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    public boolean listenMode() {
        return "listen".equals(mode);
    }

    public boolean connectMode() {
        return "connect".equals(mode);
    }

    public boolean tokenOk() {
        String t = token == null ? "" : token.trim();
        return !t.isEmpty() && !"CHANGE_ME".equals(t);
    }

    private static List<String> normalizePrefixes(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String p : raw) {
            if (p == null || p.isBlank()) {
                continue;
            }
            String n = PathPolicy.posix(p);
            if (n.startsWith("/")) {
                n = n.substring(1);
            }
            out.add(n);
        }
        return out;
    }
}
