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
    private FlushService flush;
    private Path serverRoot;
    private int heartbeatTask = -1;
    private boolean appliedOnLoad;
    private volatile boolean peerOnline;

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
        flush = new FlushService(this);
        getServer().getPluginManager().registerEvents(new JoinGuardListener(this), this);
        getServer().getPluginManager().registerEvents(new DirtyListener(this), this);
        WorldSyncCommand cmd = new WorldSyncCommand(this);
        PluginCommand pc = getCommand("worldsync");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }
        if (config.s3Mode()) {
            if (!config.s3Ready()) {
                getLogger().severe("transport=s3 但 s3.bucket / access-key / secret-key 还没填，同步不会启动。");
                lock.setJoiningBlocked(true);
                return;
            }
        } else if (!config.tokenOk()) {
            getLogger().severe("请先在 plugins/MCWWS_WorldSync/config.yml 设置 token（不要用 CHANGE_ME），否则不会启动同步端口。");
            lock.setJoiningBlocked(true);
            return;
        }
        startNetwork();
        getServer().getScheduler().runTask(this, () -> {
            if (lock.hasLock()) {
                handover.restoreHolderWorldRules();
            } else {
                handover.applyStandbyWorldRules();
            }
        });
        int period = config.heartbeatSeconds * 20;
        heartbeatTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, this::tick, 40L, Math.max(20, period));
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
        if (heartbeatTask != -1) {
            getServer().getScheduler().cancelTask(heartbeatTask);
            heartbeatTask = -1;
        }
        if (listen != null) {
            listen.stop();
            listen = null;
        }
    }

    public void reloadSync() {
        if (listen != null) {
            listen.stop();
            listen = null;
        }
        reloadConfig();
        config = new SyncConfig(getConfig());
        lock.setSelf(config.nodeId);
        relay = null;
        if (config.s3Mode() && config.s3Ready()) {
            startNetwork();
            return;
        }
        if (config.tokenOk()) {
            startNetwork();
        }
    }

    private void startNetwork() {
        if (config.s3Mode()) {
            relay = new S3Relay(this);
            client = null;
            return;
        }
        relay = null;
        client = new SyncPeerClient(this);
        if (config.listenMode()) {
            try {
                listen = new SyncListenServer(this);
                listen.start();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "无法监听同步端口", e);
            }
        }
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
            if (!lock.hasLock()) {
                for (String rel : client.listOutbox()) {
                    if (PathPolicy.allowed(rel, config.prefixes, config.skipGlobs)) {
                        client.pullOutboxFile(rel);
                    }
                }
            } else if (JsonUtil.bool(st, "handoverPending", false)) {
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
    public String admissionMessage() {
        if (lock.hasLock() && !lock.joiningBlocked()) {
            return null;
        }
        if (handover != null && handover.busy()) {
            return "正在从另一端同步最新世界，请稍后重新连接。";
        }
        String holder = lock.holder();
        boolean otherLive = !holder.isEmpty()
                && !holder.equals(config.nodeId)
                && System.currentTimeMillis() < lock.leaseUntil();
        if (otherLive) {
            if (handover != null) {
                handover.beginAutoTakeover();
            }
            return "正在暂停另一端并拉取最新世界。同步完成后本服会重启，请稍后重新连接。";
        }
        try {
            claimLock(false);
            if (lock.hasLock() && !lock.joiningBlocked()) {
                return null;
            }
        } catch (Exception e) {
            getLogger().warning("进服声明锁失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
        return "世界锁暂不可用。若另一端仍在运行，请稍后重新连接。";
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
