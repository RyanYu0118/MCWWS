package work.mcwws.worldsync;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HandoverService {
    private final McwwsWorldSyncPlugin plugin;
    private final Map<String, Integer> savedTickSpeed = new HashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public HandoverService(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean busy() {
        return running.get();
    }

    public void applyStandbyWorldRules() {
        for (World world : plugin.getServer().getWorlds()) {
            Integer cur = world.getGameRuleValue(GameRule.RANDOM_TICK_SPEED);
            if (cur == null) {
                cur = 3;
            }
            savedTickSpeed.putIfAbsent(world.getName(), cur);
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        }
    }

    public void restoreHolderWorldRules() {
        for (World world : plugin.getServer().getWorlds()) {
            Integer prev = savedTickSpeed.get(world.getName());
            if (prev != null) {
                world.setGameRule(GameRule.RANDOM_TICK_SPEED, prev);
            }
        }
    }

    /** Called on the current holder (possibly this listen node) when the peer wants the world. */
    public void onRemoteRequest(String from) {
        if (!plugin.lock().hasLock()) {
            plugin.getLogger().info("收到接管请求来自 " + from + "，本节点无锁，忽略冲刷。");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            plugin.getLogger().warning("接管已在进行中");
            return;
        }
        plugin.lock().setJoiningBlocked(true);
        Component kick = Component.text(
                "这一端已暂停。请改连另一节点，同步完成后再进入。",
                NamedTextColor.GOLD);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.kick(kick);
        }
        plugin.getLogger().info("开始作为持锁端移交世界给 " + from);
        plugin.flush().runNow(true, () -> {
            try {
                releaseLock();
                plugin.getServer().getScheduler().runTask(plugin, this::applyStandbyWorldRules);
                plugin.getLogger().info("已释放写入锁，对端可应用 staging 后开服。");
            } catch (Exception e) {
                plugin.getLogger().severe("释放锁失败: " + e.getMessage());
            } finally {
                running.set(false);
            }
        });
    }

    /** Standby node: ask holder to flush, pull files, apply on next boot, restart. */
    public void requestTakeover(org.bukkit.command.CommandSender sender) {
        if (plugin.lock().hasLock()) {
            sender.sendMessage(Component.text("本节点已持有写入锁，无需接管。", NamedTextColor.YELLOW));
            return;
        }
        beginAutoTakeover();
    }

    /** Join-triggered switch. Safe to call more than once. */
    public void beginAutoTakeover() {
        if (plugin.lock().hasLock() && !plugin.lock().joiningBlocked()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("进服触发接管：暂停对端并拉取最新世界");
                if (plugin.config().s3Mode()) {
                    plugin.relay().requestHandover();
                    waitCloudRelease();
                    plugin.relay().pullChanged();
                } else if (plugin.config().connectMode()) {
                    plugin.client().requestHandover();
                    waitAndPull();
                } else {
                    String holder = plugin.lock().holder();
                    if (holder.isEmpty() || holder.equals(plugin.config().nodeId)) {
                        throw new IOException("协调端无对端持锁者，可用 /worldsync claim 直接声明");
                    }
                    plugin.lock().setHandover(true, plugin.config().nodeId);
                    waitAndPull();
                }
                plugin.staging().markApplyOnBoot();
                plugin.getLogger().info("最新数据已写入 staging，即将重启后才允许进入");
                plugin.getServer().getScheduler().runTask(plugin, this::restartAfterApply);
            } catch (Exception e) {
                running.set(false);
                plugin.getLogger().severe("自动接管失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        });
    }

    private void waitAndPull() throws Exception {
        long deadline = System.currentTimeMillis() + 180_000L;
        boolean released = false;
        while (System.currentTimeMillis() < deadline) {
            if (plugin.config().connectMode()) {
                Map<String, Object> st = plugin.client().heartbeat();
                plugin.applyRemoteStatus(st);
                pullOutbox();
                String holder = JsonUtil.str(st, "holder", "");
                if (holder.isEmpty() || plugin.config().nodeId.equals(holder)) {
                    released = true;
                    pullOutbox();
                    break;
                }
            } else {
                plugin.lock().expireIfNeeded();
                if (plugin.lock().holder().isEmpty()) {
                    released = true;
                    break;
                }
            }
            Thread.sleep(1500);
        }
        if (!released) {
            throw new IOException("等待对端释放写入锁超时");
        }
    }

    private void pullOutbox() throws IOException, InterruptedException {
        if (!plugin.config().connectMode()) {
            return;
        }
        for (String rel : plugin.client().listOutbox()) {
            if (PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
                plugin.client().pullOutboxFile(rel);
            }
        }
    }

    private void releaseLock() throws Exception {
        if (plugin.config().s3Mode() && plugin.relay() != null) {
            plugin.relay().release();
        } else if (plugin.config().connectMode() && plugin.client() != null) {
            plugin.client().release();
        }
        plugin.lock().releaseIfSelf();
        plugin.lock().setJoiningBlocked(true);
        plugin.lock().setHandover(false, "");
    }

    private void waitCloudRelease() throws Exception {
        long deadline = System.currentTimeMillis() + 180_000L;
        while (System.currentTimeMillis() < deadline) {
            plugin.relay().poll();
            String holder = plugin.lock().holder();
            if (holder.isEmpty() || plugin.config().nodeId.equals(holder)) {
                return;
            }
            Thread.sleep(1500);
        }
        throw new IOException("等待对端释放云端写入锁超时");
    }

    private void restartAfterApply() {
        String mode = plugin.config().restartMode;
        plugin.getLogger().info("接管重启模式: " + mode);
        if ("none".equals(mode)) {
            running.set(false);
            plugin.getLogger().warning("restart-mode=none，请手动停服后启动（onLoad 会套用 staging）。");
            return;
        }
        if ("http".equals(mode) && plugin.config().httpRestartEnabled) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    callRestartHttp();
                } catch (Exception e) {
                    plugin.getLogger().severe("MCSManager/HTTP 重启失败: " + e.getMessage());
                }
            });
        }
        if ("process".equals(mode) && !plugin.config().processCommand.isEmpty()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    new ProcessBuilder(plugin.config().processCommand).start();
                } catch (IOException e) {
                    plugin.getLogger().severe("重启进程失败: " + e.getMessage());
                }
            });
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getServer().shutdown(), plugin.config().shutdownDelayTicks);
    }

    private void callRestartHttp() throws Exception {
        SyncConfig cfg = plugin.config();
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(cfg.httpRestartUrl))
                .timeout(Duration.ofSeconds(30));
        cfg.httpRestartHeaders.forEach(b::header);
        String method = cfg.httpRestartMethod.toUpperCase();
        HttpRequest.BodyPublisher pub = cfg.httpRestartBody == null || cfg.httpRestartBody.isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(cfg.httpRestartBody, StandardCharsets.UTF_8);
        switch (method) {
            case "GET" -> b.GET();
            case "PUT" -> b.PUT(pub);
            default -> b.POST(pub);
        }
        HttpResponse<String> resp = HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
        plugin.getLogger().info("重启 HTTP " + resp.statusCode() + " " + resp.body());
    }
}
