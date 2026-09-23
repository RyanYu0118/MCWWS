package work.mcwws.worldsync;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The node that runs the command is the source of truth.
 * Only files whose hash differs are packed and applied on the other node after it restarts.
 */
public final class ForcePushService {
    private final McwwsWorldSyncPlugin plugin;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String winner = "";
    private volatile Map<String, String> winnerHashes = Map.of();
    private volatile List<String> differ = List.of();

    public ForcePushService(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
    }

    public String winner() {
        return winner == null ? "" : winner;
    }

    public boolean busy() {
        return running.get() || !winner().isEmpty();
    }

    public void start(CommandSender sender) {
        if (plugin.config().s3Mode()) {
            sender.sendMessage(Component.text("强制推送只支持直连模式。", NamedTextColor.RED));
            return;
        }
        if (!winner().isEmpty()) {
            sender.sendMessage(Component.text("已在等待对端来取差异。", NamedTextColor.GOLD));
            return;
        }
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage(Component.text("强制推送已在进行：" + plugin.progress().consoleLine(), NamedTextColor.GOLD));
            return;
        }
        sender.sendMessage(Component.text("已开始强制推送。不一致的文件以本端为准，进度在控制台。对端套用后会重启。", NamedTextColor.GOLD));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                world.save();
            }
            plugin.getServer().savePlayers();
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    if (plugin.config().connectMode()) {
                        pushFromConnect();
                    } else {
                        armListen();
                    }
                } catch (Exception e) {
                    running.set(false);
                    winner = "";
                    plugin.getLogger().severe("强制推送失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }
            });
        });
    }

    /** Connect node noticed the listen node armed a push. */
    public void pullFromListen(String from) {
        if (!plugin.config().connectMode() || from == null || from.isEmpty() || from.equals(plugin.config().nodeId)) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("对端 " + from + " 要求以它的文件为准，开始对比");
                Map<String, String> mine = TreeScan.hash(plugin);
                List<String> paths = plugin.client().comparePush(from, mine);
                if (paths.isEmpty()) {
                    plugin.getLogger().info("没有不一致的文件");
                    plugin.client().finishPush();
                    return;
                }
                plugin.getLogger().info("不一致 " + paths.size() + " 个，开始下载推送方版本");
                plugin.client().downloadPushBundle();
                plugin.client().finishPush();
                applyHere();
            } catch (Exception e) {
                plugin.getLogger().severe("接收强制推送失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            } finally {
                running.set(false);
            }
        });
    }

    private void pushFromConnect() throws Exception {
        Map<String, String> mine = TreeScan.hash(plugin);
        List<String> paths = plugin.client().comparePush(plugin.config().nodeId, mine);
        if (paths.isEmpty()) {
            plugin.getLogger().info("没有不一致的文件，对端不用重启");
            running.set(false);
            return;
        }
        plugin.getLogger().info("不一致 " + paths.size() + " 个，正在打包本端版本");
        Path zip = plugin.getDataFolder().toPath().resolve("push-bundle.zip");
        writeZip(zip, paths);
        plugin.client().uploadPushBundle(zip);
        Files.deleteIfExists(zip);
        plugin.getLogger().info("已推送到对端，对端将重启并采用本端文件");
        running.set(false);
    }

    private void armListen() throws IOException {
        winnerHashes = TreeScan.hash(plugin);
        differ = List.of();
        winner = plugin.config().nodeId;
        plugin.getLogger().info("本端校验已完成。等待对端来对比；不一致的文件将以本端为准。");
        running.set(false);
    }

    public Map<String, Object> compare(String winnerId, Map<String, String> remote) throws IOException {
        Map<String, String> local = plugin.config().nodeId.equals(winnerId) && !winnerHashes.isEmpty()
                ? winnerHashes
                : TreeScan.hash(plugin);
        if (plugin.config().nodeId.equals(winnerId)) {
            winnerHashes = local;
        }
        List<String> diff = new ArrayList<>();
        Map<String, String> source = plugin.config().nodeId.equals(winnerId) ? local : remote;
        Map<String, String> other = plugin.config().nodeId.equals(winnerId) ? remote : local;
        for (Map.Entry<String, String> e : source.entrySet()) {
            if (!TreeScan.include(plugin, e.getKey())) {
                continue;
            }
            String theirs = other.get(e.getKey());
            if (theirs == null || !theirs.equalsIgnoreCase(e.getValue())) {
                diff.add(e.getKey());
            }
        }
        differ = diff;
        plugin.getLogger().info("强制推送对比完成，不一致 " + diff.size() + " 个");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("differ", diff);
        out.put("count", diff.size());
        return out;
    }

    public void writeDifferZip(Path zip) throws IOException {
        writeZip(zip, differ);
    }

    public void clearArmed() {
        winner = "";
        winnerHashes = Map.of();
        differ = List.of();
    }

    public void acceptZip(InputStream in) throws IOException {
        Path tmp = plugin.getDataFolder().toPath().resolve("incoming-push.zip");
        Files.createDirectories(tmp.getParent());
        try (OutputStream out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
        }
        unzipToStaging(tmp);
        Files.deleteIfExists(tmp);
        applyHere();
    }

    public void consumeBundle(Path zip) throws IOException {
        unzipToStaging(zip);
        Files.deleteIfExists(zip);
    }

    private void writeZip(Path zip, List<String> rels) throws IOException {
        Files.createDirectories(zip.getParent());
        plugin.progress().begin("打包差异", Math.max(rels.size(), 1));
        plugin.getLogger().info(plugin.progress().consoleLine());
        try (ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(zip))) {
            byte[] buf = new byte[65536];
            int n = 0;
            for (String rel : rels) {
                if (!TreeScan.include(plugin, rel)) {
                    continue;
                }
                Path src = PathPolicy.resolveUnder(plugin.serverRoot(), rel);
                if (!Files.isRegularFile(src)) {
                    continue;
                }
                zout.putNextEntry(new ZipEntry(rel));
                try (InputStream in = Files.newInputStream(src)) {
                    int r;
                    while ((r = in.read(buf)) >= 0) {
                        zout.write(buf, 0, r);
                    }
                }
                zout.closeEntry();
                n++;
                plugin.progress().tick(plugin, n, rel);
            }
        } finally {
            plugin.progress().end(plugin);
        }
        plugin.getLogger().info("差异包 " + (Files.size(zip) / 1024) + " KB");
    }

    private void unzipToStaging(Path zip) throws IOException {
        try (var zin = new java.util.zip.ZipInputStream(Files.newInputStream(zip))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!TreeScan.include(plugin, name)) {
                    continue;
                }
                plugin.staging().writeStaging(name, zin, -1);
                zin.closeEntry();
            }
        }
    }

    private void applyHere() {
        try {
            plugin.staging().markApplyOnBoot();
        } catch (IOException e) {
            plugin.getLogger().severe("标记重启套用失败: " + e.getMessage());
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Component kick = Component.text("对端已强制推送世界，本服即将重启。", NamedTextColor.GOLD);
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.kick(kick);
            }
            plugin.getLogger().info("差异已写入 staging，即将重启后采用推送方文件");
            plugin.handover().restartNow();
        });
    }
}
