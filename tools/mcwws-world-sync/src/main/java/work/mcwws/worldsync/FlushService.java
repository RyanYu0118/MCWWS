package work.mcwws.worldsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class FlushService {
    private final McwwsWorldSyncPlugin plugin;
    private volatile long lastFlush;
    private volatile long lastFailLog;
    private volatile boolean skippedPeerLogged;

    public FlushService(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
    }

    public void runNow(boolean includePlayers, Runnable after) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!plugin.lock().hasLock()) {
                if (after != null) {
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, after);
                }
                return;
            }
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                world.save();
            }
            plugin.getServer().savePlayers();
            if (includePlayers) {
                Path root = plugin.serverRoot();
                for (String prefix : plugin.config().prefixes) {
                    if (prefix.startsWith("world/") || prefix.equals("world")
                            || prefix.startsWith("world_nether") || prefix.startsWith("world_the_end")) {
                        continue;
                    }
                    Path p = PathPolicy.resolveUnder(root, prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix);
                    try {
                        if (Files.isRegularFile(p)) {
                            plugin.dirty().mark(PathPolicy.relative(root, p));
                        } else if (Files.isDirectory(p)) {
                            try (Stream<Path> walk = Files.walk(p, 6)) {
                                walk.filter(Files::isRegularFile).limit(4000).forEach(f -> {
                                    try {
                                        String rel = PathPolicy.relative(root, f);
                                        if (PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
                                            plugin.dirty().mark(rel);
                                        }
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                });
                            }
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().warning("扫描白名单 " + prefix + ": " + ex.getMessage());
                    }
                }
                Path playerDir = root.resolve("world/playerdata");
                if (Files.isDirectory(playerDir)) {
                    try (Stream<Path> files = Files.list(playerDir)) {
                        files.filter(Files::isRegularFile).forEach(f -> {
                            try {
                                plugin.dirty().mark(PathPolicy.relative(root, f));
                            } catch (IllegalArgumentException ignored) {
                            }
                        });
                    } catch (IOException e) {
                        plugin.getLogger().warning("扫描 playerdata: " + e.getMessage());
                    }
                }
            }
            List<String> paths = plugin.dirty().snapshotAndClear();
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    send(paths);
                    lastFlush = System.currentTimeMillis();
                } catch (Exception e) {
                    plugin.dirty().addAll(paths);
                    long now = System.currentTimeMillis();
                    if (now - lastFailLog > 120_000L) {
                        lastFailLog = now;
                        plugin.getLogger().warning("冲刷暂停（对端未连通，本机可继续玩）: " + describe(e));
                    }
                } finally {
                    if (after != null) {
                        after.run();
                    }
                }
            });
        });
    }

    public void maybePeriodic() {
        if (!plugin.lock().hasLock()) {
            return;
        }
        if (plugin.dirty().size() == 0) {
            return;
        }
        if (plugin.config().connectMode() && !plugin.config().s3Mode() && !plugin.peerOnline()) {
            if (!skippedPeerLogged) {
                skippedPeerLogged = true;
                plugin.getLogger().info("对端未连通，脏区块先留在本机，连上公网后再冲刷。");
            }
            return;
        }
        skippedPeerLogged = false;
        if (System.currentTimeMillis() - lastFlush < plugin.config().flushSeconds * 1000L) {
            return;
        }
        runNow(false, null);
    }

    private static String describe(Throwable e) {
        if (e == null) {
            return "unknown";
        }
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + m;
    }

    private void send(List<String> paths) throws Exception {
        Path root = plugin.serverRoot();
        List<String> sent = new ArrayList<>();
        for (String rel : paths) {
            if (!PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
                continue;
            }
            Path src = PathPolicy.resolveUnder(root, rel);
            if (!Files.isRegularFile(src)) {
                continue;
            }
            if (plugin.config().s3Mode()) {
                plugin.relay().putFile(rel, src);
            } else if (plugin.config().listenMode()) {
                plugin.outbox().put(rel, src);
            } else {
                plugin.client().putFile(rel, src);
            }
            sent.add(rel);
            if (plugin.config().debug) {
                plugin.getLogger().info("synced " + rel);
            }
        }
        if (!sent.isEmpty()) {
                plugin.getLogger().info("已同步 " + sent.size() + " 个文件");
        }
    }
}
