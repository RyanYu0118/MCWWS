package work.mcwws.residencequiet;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class McwwsResidenceQuietPlugin extends JavaPlugin {

    private DenyThrottle throttle;
    private PacketDenyFilter packetFilter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocal();
        getServer().getPluginManager().registerEvents(new ResidenceVisitListener(this), this);
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            packetFilter = new PacketDenyFilter(this);
            packetFilter.register();
            getLogger().info("已挂接 ProtocolLib，领地权限拒绝提示将按「每次进入只提醒一次」节流。");
        } else {
            getLogger().warning("未找到 ProtocolLib，无法拦截领地拒绝刷屏。");
        }
    }

    @Override
    public void onDisable() {
        if (packetFilter != null) {
            packetFilter.unregister();
            packetFilter = null;
        }
        if (throttle != null) {
            throttle.clearAll();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"mcwws-resquiet-reload".equalsIgnoreCase(command.getName())) {
            return false;
        }
        reloadLocal();
        sender.sendMessage(Component.text("MCWWS_ResidenceQuiet reloaded."));
        return true;
    }

    void reloadLocal() {
        reloadConfig();
        throttle = DenyThrottle.fromConfig(getConfig());
    }

    DenyThrottle throttle() {
        return throttle;
    }
}
