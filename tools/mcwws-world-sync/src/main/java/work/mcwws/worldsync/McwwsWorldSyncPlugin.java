package work.mcwws.worldsync;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public final class McwwsWorldSyncPlugin extends JavaPlugin {
    private SyncConfig config;
    private final LockState lock = new LockState();
    private final DirtyTracker dirty = new DirtyTracker();
    private StagingStore staging;
    private OutboxStore outbox;
    private SyncListenServer listen;
    private SyncPeerClient client;
    private S3Relay relay;
    private HandoverService handover;
    private ForcePushService forcePush;
    private FlushService flush;
    private Path serverRoot;
    private volatile boolean syncEnabled;
    private int httpRequests;
    private volatile int heartbeatTask = -1;
    private boolean appliedOnLoad;
    private volatile boolean peerOnline;
    private final SyncProgress progress = new SyncProgress();
    private volatile boolean admissionResolving;
    private volatile String admissionDetail = "";

    @Override
    public void onLoad() {
        saveDefaultConfig();
        reloadConfig();
        config = new SyncConfig(getConfig());
        serverRoot = getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
        staging = new StagingStore(getDataFolder().toPath(), serverRoot, getLogger());
        outbox = new OutboxStore(getDataFolder().toPath());
        if (staging.hasApplyFlag()) {
            try {
                int n = staging.applyToLive(config);
                appliedOnLoad = n >= 0;
                lock.incrementGeneration();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "onLoad 应用 staging 失败", e);
            }
        }
    }

    @Override
    public void onEnable() {
        if (config == null) {
            reloadConfig();
            config = new SyncConfig(getConfig());
        }
        lock.setSelf(config.nodeId);
        handover = new HandoverService(this);
        forcePush = new ForcePushService(this);
        flush = new FlushService(this);
        getServer().getPluginManager().registerEvents(new JoinGuardListener(this), this);
        getServer().getPluginManager().registerEvents(new DirtyListener(this), this);
        WorldSyncCommand cmd = new WorldSyncCommand(this);
        PluginCommand pc = getCommand("worldsync");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }
        syncEnabled = getConfig().getBoolean("enabled", true);
        if (syncEnabled && !networkConfigured()) {
            getLogger().severe("世界同步缺少有效凭证，拒绝进服。");
        }
        if (!syncEnabled || !networkConfigured()) {
            lock.setJoiningBlocked(true);
            return;
        }
        if (!startNetwork()) {
            lock.setJoiningBlocked(true);
            return;
        }
        getServer().getScheduler().runTask(this, () -> {
            if (lock.hasLock()) {
                handover.restoreHolderWorldRules();
            } else {
                handover.applyStandbyWorldRules();
            }
        });
        scheduleHeartbeat();
        if (appliedOnLoad || config.claimOnEnable) {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                try {
                    claimLock(false);
                } catch (Exception e) {
                    getLogger().warning("自动声明写入锁失败: " + e.getMessage());
                }
            }, 40L);
        } else {
            lock.setJoiningBlocked(true);
        }
        getLogger().info("MCWWS_WorldSync 已启用 node=" + config.nodeId + " transport=" + config.transport);
    }

    @Override
    public void onDisable() {
        stopNetworkAndHeartbeat();
    }

    private void stopNetworkAndHeartbeat() {
        if (heartbeatTask != -1) {
            getServer().getScheduler().cancelTask(heartbeatTask);
            heartbeatTask = -1;
        }
        if (listen != null) {
            listen.stop();
            listen = null;
        }
    }

    private boolean networkConfigured() {
        return config.s3Mode() ? config.s3Ready() : config.tokenOk();
    }

    private void scheduleHeartbeat() {
        if (heartbeatTask != -1) {
            getServer().getScheduler().cancelTask(heartbeatTask);
        }
        heartbeatTask = getServer().getScheduler().scheduleSyncRepeatingTask(
                this, this::tick, 40L, config.heartbeatSeconds * 20L);
    }

    public boolean syncEnabled() {
        return syncEnabled;
    }

    synchronized boolean beginHttpRequest() {
        if (!syncEnabled) {
            return false;
        }
        httpRequests++;
        return true;
    }

    synchronized void endHttpRequest() {
        httpRequests--;
    }

    private boolean syncBusy() {
        var scheduler = getServer().getScheduler();
        return httpRequests > 0 || admissionResolving || handover.busy() || forcePush.busy()
                || progress.active() || lock.handoverPending()
                || scheduler.getActiveWorkers().stream().anyMatch(worker -> worker.getOwner() == this)
                || scheduler.getPendingTasks().stream().anyMatch(
                        task -> task.getOwner() == this && task.getTaskId() != heartbeatTask && !task.isCancelled());
    }

    public synchronized String setSyncEnabled(boolean enabled) {
        if (enabled == syncEnabled && (!enabled || heartbeatTask != -1)) {
            return enabled ? "世界同步已启用。" : "世界同步已暂停。";
        }
        if (syncBusy()) {
            return "同步任务仍在进行，请完成后重试。";
        }
        if (!enabled && (config.nodeId.equals(lock.holder()) || !getServer().getOnlinePlayers().isEmpty())) {
            return "暂停仅允许在无人在线且未持有写入锁的待机节点执行。";
        }
        if (enabled && (!networkConfigured() || staging.hasApplyFlag())) {
            return "无法恢复同步：请检查网络配置，或在重启时先应用待处理的世界文件。";
        }
        if (enabled) {
            if (!startNetwork()) {
                return "同步监听启动失败，请检查控制台。";
            }
            syncEnabled = true;
            scheduleHeartbeat();
        } else {
            syncEnabled = false;
            stopNetworkAndHeartbeat();
            client = null;
            relay = null;
            peerOnline = false;
            lock.setJoiningBlocked(true);
        }
        getConfig().set("enabled", enabled);
        saveConfig();
        return enabled ? "世界同步已恢复，进服仍须通过写入锁检查。" : "世界同步已暂停，本节点拒绝进服。";
    }

    public synchronized String reloadSync() {
        if (syncBusy()) {
            return "同步任务仍在进行，请完成后重试。";
        }
        SyncConfig previous = config;
        reloadConfig();
        SyncConfig updated = new SyncConfig(getConfig());
        boolean enabled = getConfig().getBoolean("enabled", true);
        if (!enabled && syncEnabled) {
            String result = setSyncEnabled(false);
            if (syncEnabled) {
                return result;
            }
        }
        if (!previous.nodeId.equals(updated.nodeId)
                || !previous.mode.equals(updated.mode) || !previous.transport.equals(updated.transport)) {
            return "节点 ID、模式或传输方式变更须停服后生效。";
        }
        if (syncEnabled && !(updated.s3Mode() ? updated.s3Ready() : updated.tokenOk())) {
            return "配置缺少同步凭证，继续使用原配置。";
        }
        config = updated;
        lock.setSelf(updated.nodeId);
        if (!enabled || !syncEnabled || heartbeatTask == -1) {
            return setSyncEnabled(enabled);
        }
        stopNetworkAndHeartbeat();
        if (!startNetwork()) {
            config = previous;
            if (startNetwork()) {
                scheduleHeartbeat();
            }
            return "新配置监听启动失败，已尝试恢复原配置，请检查控制台。";
        }
        scheduleHeartbeat();
        return "已重载世界同步配置和心跳。";
    }

    private boolean startNetwork() {
        if (config.s3Mode()) {
            relay = new S3Relay(this);
            client = null;
            return true;
        }
        relay = null;
        client = new SyncPeerClient(this);
        if (config.listenMode()) {
            try {
                listen = new SyncListenServer(this);
                listen.start();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "无法监听同步端口", e);
                listen = null;
                return false;
            }
        }
        return true;
    }

    private void tick() {
        if (config.s3Mode()) {
            if (relay != null) {
                getServer().getScheduler().runTaskAsynchronously(this, () -> relay.poll());
            }
            if (lock.hasLock()) {
                flush.maybePeriodic();
            }
            return;
        }
        lock.expireIfNeeded();
        if (lock.hasLock()) {
            lock.refreshLease(config.leaseSeconds * 1000L);
            flush.maybePeriodic();
        }
        if (config.connectMode() && client != null) {
            getServer().getScheduler().runTaskAsynchronously(this, this::connectTick);
        }
    }

    private void connectTick() {
        try {
            Map<String, Object> st = client.heartbeat();
            peerOnline = true;
            applyRemoteStatus(st);
            String pushFrom = JsonUtil.str(st, "pushFrom", "");
            if (forcePush != null && !pushFrom.isEmpty() && !pushFrom.equals(config.nodeId)) {
                forcePush.pullFromListen(pushFrom);
                return;
            }
            // Standby pulls the outbox only during a handover; looping here re-downloads every file each heartbeat.
            if (lock.hasLock() && JsonUtil.bool(st, "handoverPending", false)) {
                String from = JsonUtil.str(st, "handoverFrom", "");
                getServer().getScheduler().runTask(this, () -> handover.onRemoteRequest(from));
            }
        } catch (Exception e) {
            peerOnline = false;
            if (config.debug) {
                getLogger().warning("对端心跳失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
    }

    public void applyRemoteStatus(Map<String, Object> st) {
        String holder = JsonUtil.str(st, "holder", "");
        long gen = JsonUtil.lng(st, "generation", lock.generation());
        boolean frozen = JsonUtil.bool(st, "frozen", false);
        long lease = System.currentTimeMillis() + config.leaseSeconds * 1000L;
        boolean wasHolder = lock.hasLock();
        lock.setHolder(holder, gen, lease, frozen);
        lock.setJoiningBlocked(!lock.hasLock());
        lock.setHandover(JsonUtil.bool(st, "handoverPending", false), JsonUtil.str(st, "handoverFrom", ""));
        boolean nowHolder = lock.hasLock();
        if (wasHolder != nowHolder) {
            getServer().getScheduler().runTask(this, () -> {
                if (nowHolder) {
                    handover.restoreHolderWorldRules();
                } else {
                    handover.applyStandbyWorldRules();
                }
            });
        }
    }

    /**
     * @return null when this login may enter. Otherwise the kick text.
     *         Entering a server that does not hold the lock starts a switch:
     *         the other side is paused and its latest files are pulled, then this side restarts.
     */
    public synchronized String admissionMessage() {
        if (!syncEnabled || heartbeatTask == -1) {
            return "本节点的世界同步已暂停或尚未启动。";
        }
        if (lock.hasLock() && !lock.joiningBlocked()) {
            return null;
        }
        if (handover != null && handover.busy()) {
            return progress.kickHint();
        }
        if (otherHolderLive()) {
            if (handover != null) {
                handover.beginAutoTakeover();
            }
            return "正在暂停另一端并拉取最新世界。同步完成后本服会重启，请稍后重新连接。";
        }
        if (config.connectMode() && client != null) {
            if (!admissionResolving) {
                admissionResolving = true;
                getServer().getScheduler().runTaskAsynchronously(this, this::resolveConnectAdmission);
            }
            if (!admissionDetail.isEmpty()) {
                return admissionDetail;
            }
            return "正在确认世界锁，请马上重新连接。";
        }
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                claimLock(false);
            } catch (Exception e) {
                getLogger().warning("进服声明锁失败: " + describe(e));
            }
        });
        return "正在确认世界锁，请马上重新连接。";
    }

    private boolean otherHolderLive() {
        String holder = lock.holder();
        return !holder.isEmpty()
                && !holder.equals(config.nodeId)
                && System.currentTimeMillis() < lock.leaseUntil();
    }

    /** Connect node: learn who holds the lock, then either claim it or start takeover. */
    private void resolveConnectAdmission() {
        try {
            try {
                applyRemoteStatus(client.status());
            } catch (Exception statusEx) {
                getLogger().warning("读取对端锁状态失败: " + describe(statusEx));
            }
            if (otherHolderLive()) {
                admissionDetail = "正在暂停另一端并拉取最新世界。同步完成后本服会重启，请稍后重新连接。";
                getLogger().info("写入锁在 " + lock.holder() + "，开始接管");
                handover.beginAutoTakeover();
                return;
            }
            claimLock(false);
            admissionDetail = "";
        } catch (Exception e) {
            String msg = describe(e);
            if (msg.startsWith("held by ")) {
                String other = msg.substring("held by ".length()).trim();
                lock.setHolder(other, lock.generation(), System.currentTimeMillis() + config.leaseSeconds * 1000L, false);
                admissionDetail = "正在暂停另一端并拉取最新世界。同步完成后本服会重启，请稍后重新连接。";
                getLogger().info("写入锁在 " + other + "，开始接管");
                handover.beginAutoTakeover();
            } else {
                admissionDetail = "确认世界锁失败：" + msg;
                getLogger().warning("进服声明锁失败: " + msg);
            }
        } finally {
            admissionResolving = false;
        }
    }

    private static String describe(Throwable e) {
        if (e == null) {
            return "unknown";
        }
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return msg;
    }

    public void claimLock(boolean forceLog) throws Exception {
        if (config.s3Mode()) {
            relay.claim();
            getServer().getScheduler().runTask(this, () -> handover.restoreHolderWorldRules());
            if (forceLog) {
                getLogger().info("本节点持有云端写入锁 generation=" + lock.generation());
            }
            return;
        }
        if (config.connectMode()) {
            Map<String, Object> st = client.claim();
            applyRemoteStatus(st);
            if (!lock.hasLock()) {
                throw new IOException("协调端未把锁交给本节点（" + JsonUtil.str(st, "holder", "") + "）");
            }
        } else {
            if (!lock.claimLocal(config.leaseSeconds * 1000L)) {
                throw new IOException("无法声明锁，当前持有者=" + lock.holder() + (lock.frozen() ? "（冻结）" : ""));
            }
            lock.setJoiningBlocked(false);
        }
        getServer().getScheduler().runTask(this, () -> handover.restoreHolderWorldRules());
        if (forceLog) {
            getLogger().info("本节点持有写入锁 generation=" + lock.generation());
        }
    }

    public Map<String, Object> statusMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("enabled", syncEnabled);
        m.put("running", syncEnabled && heartbeatTask != -1);
        m.put("nodeId", config.nodeId);
        m.put("mode", config.mode);
        m.put("holder", lock.holder());
        m.put("hasLock", lock.hasLock());
        m.put("frozen", lock.frozen());
        m.put("generation", lock.generation());
        m.put("dirty", dirty.size());
        m.put("players", getServer().getOnlinePlayers().size());
        m.put("handoverPending", lock.handoverPending());
        m.put("handoverFrom", lock.handoverFrom());
        m.put("pushFrom", forcePush == null ? "" : forcePush.winner());
        m.put("joiningBlocked", lock.joiningBlocked());
        m.put("transport", config.transport);
        m.put("peerOnline", peerOnline);
        m.put("leaseUntil", lock.leaseUntil());
        return m;
    }

    public SyncConfig config() {
        return config;
    }

    public LockState lock() {
        return lock;
    }

    public DirtyTracker dirty() {
        return dirty;
    }

    public StagingStore staging() {
        return staging;
    }

    public OutboxStore outbox() {
        return outbox;
    }

    public SyncPeerClient client() {
        return client;
    }

    public S3Relay relay() {
        return relay;
    }

    public void markPeerOnline(boolean online) {
        this.peerOnline = online;
    }

    public HandoverService handover() {
        return handover;
    }

    public ForcePushService forcePush() {
        return forcePush;
    }

    public SyncProgress progress() {
        return progress;
    }

    public FlushService flush() {
        return flush;
    }

    public Path serverRoot() {
        return serverRoot;
    }

    public boolean peerOnline() {
        return peerOnline;
    }
}
