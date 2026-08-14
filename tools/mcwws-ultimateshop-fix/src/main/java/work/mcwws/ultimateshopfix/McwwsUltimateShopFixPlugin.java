package work.mcwws.ultimateshopfix;

import cn.superiormc.ultimateshop.managers.LocateManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class McwwsUltimateShopFixPlugin extends JavaPlugin implements Listener {

    private static McwwsUltimateShopFixPlugin instance;
    private boolean announced;

    public static McwwsUltimateShopFixPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        if (getServer().getPluginManager().getPlugin("UltimateShop") == null) {
            getLogger().severe("未找到 UltimateShop，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        LocaleNames.reload();
        install();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::install, 20L, 100L);
        getLogger().info("UltimateShop 搜索译名补丁已启用（Paper 26.2 craftDelegate）。");
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isShopReload(event.getMessage())) {
            getServer().getScheduler().runTaskLater(this, this::reinstall, 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (isShopReload(event.getCommand())) {
            getServer().getScheduler().runTaskLater(this, this::reinstall, 20L);
        }
    }

    void reinstall() {
        LocaleNames.reload();
        install();
    }

    void install() {
        LocateManager current = LocateManager.locateManager;
        if (current instanceof SafeLocateManager) {
            return;
        }
        new SafeLocateManager();
        if (!announced) {
            announced = true;
            getLogger().info("已替换 UltimateShop LocateManager，搜索结果可正常显示中文译名。");
        }
    }

    private static boolean isShopReload(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String command = raw.startsWith("/") ? raw.substring(1) : raw;
        command = command.toLowerCase(Locale.ROOT).trim();
        return command.equals("shop reload")
                || command.startsWith("shop reload ")
                || command.equals("ultimateshop reload")
                || command.startsWith("ultimateshop reload ");
    }
}
