package work.mcwws.ultimatetimberfix;

import org.bukkit.plugin.java.JavaPlugin;

public final class McwwsUltimateTimberFixPlugin extends JavaPlugin {

    private static McwwsUltimateTimberFixPlugin instance;
    private TreeFootprintStore footprintStore;

    public static McwwsUltimateTimberFixPlugin getInstance() {
        return instance;
    }

    public TreeFootprintStore getFootprintStore() {
        return footprintStore;
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

        footprintStore = new TreeFootprintStore(this);
        footprintStore.load();

        getServer().getPluginManager().registerEvents(new UltimateTimberGuardListener(this), this);
        getServer().getPluginManager().registerEvents(new TreeFootprintListener(this), this);

        getLogger().info("已启用 UltimateTimber × ExoticGarden 果树保护。");
    }

    @Override
    public void onDisable() {
        if (footprintStore != null) {
            footprintStore.save();
        }
        instance = null;
    }
}
