package work.mcwws.ultimatetimberfix;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class McwwsUltimateTimberFixPlugin extends JavaPlugin {

    private static McwwsUltimateTimberFixPlugin instance;
    private TreeFootprintStore footprintStore;
    private FellSessionManager sessionManager;

    public static McwwsUltimateTimberFixPlugin getInstance() {
        return instance;
    }

    public TreeFootprintStore getFootprintStore() {
        return footprintStore;
    }

    public FellSessionManager getSessionManager() {
        return sessionManager;
    }

    public String bypassPermission() {
        return getConfig().getString("bypass-permission", "mcwws.ultimatetimberfix.bypass");
    }

    public boolean debug() {
        return getConfig().getBoolean("debug", false);
    }

    public void debug(String message) {
        if (debug()) {
            getLogger().info("[debug] " + message);
        }
    }

    /**
     * 只认显式授予的 bypass；未显式设置时 OP 会默认拥有任意权限，会误跳过整个果树逻辑。
     */
    public boolean isBypassed(org.bukkit.entity.Player player) {
        if (player == null) {
            return false;
        }
        String node = bypassPermission();
        return player.isPermissionSet(node) && player.hasPermission(node);
    }

    public int footprintScanDelayTicks() {
        return Math.max(1, getConfig().getInt("footprint-scan-delay-ticks", 5));
    }

    public int footprintMaxBlocks() {
        return Math.max(16, getConfig().getInt("footprint-max-blocks", 200));
    }

    public double saplingDropChancePerLeaf() {
        return Math.max(0.0D, getConfig().getDouble("sapling-drop-chance-per-leaf", 5.0D));
    }

    public List<Integer> replantDelaysTicks() {
        List<Integer> delays = getConfig().getIntegerList("replant-delays-ticks");
        if (delays == null || delays.isEmpty()) {
            return List.of(2, 4, 8, 16);
        }
        return delays;
    }

    public long sessionTimeoutMs() {
        return Math.max(1000L, getConfig().getLong("session-timeout-seconds", 20) * 1000L);
    }

    public long sessionCleanupDelayTicks() {
        return Math.max(20L, getConfig().getLong("session-cleanup-delay-ticks", 40));
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("UltimateTimber") == null) {
            getLogger().severe("未找到 UltimateTimber，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("Slimefun") == null) {
            getLogger().severe("未找到 Slimefun，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("ExoticGarden") == null) {
            getLogger().severe("未找到 ExoticGarden，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ExoticGardenRegistry.reload();
        footprintStore = new TreeFootprintStore(this);
        footprintStore.load();
        sessionManager = new FellSessionManager();

        getServer().getPluginManager().registerEvents(new ExoticGardenTimberListener(this), this);
        getServer().getPluginManager().registerEvents(new TreeFootprintListener(this), this);
        getServer().getPluginManager().registerEvents(new FruitTreeChopListener(this), this);

        getLogger().info("已启用 UltimateTimber × ExoticGarden 果树兼容（UT 检测失败时兜底连根砍 + 补种果树苗）。");
    }

    @Override
    public void onDisable() {
        if (footprintStore != null) {
            footprintStore.save();
        }
        instance = null;
    }
}
