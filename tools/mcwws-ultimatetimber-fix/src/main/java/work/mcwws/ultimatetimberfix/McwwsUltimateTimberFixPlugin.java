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

        getLogger().info("已启用 UltimateTimber × ExoticGarden 果树兼容（连根砍、补种果树苗）。");
    }

    @Override
    public void onDisable() {
        if (footprintStore != null) {
            footprintStore.save();
        }
        instance = null;
    }
}
